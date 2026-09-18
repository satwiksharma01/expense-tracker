package com.expensetracker.repository;

import com.expensetracker.exception.StorageException;
import com.expensetracker.util.AppLogger;
import com.expensetracker.util.CsvUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * A {@link Repository} backed by a CSV file with an in-memory index.
 *
 * <ul>
 *   <li>Reads are served from a {@link LinkedHashMap}, so lookups by key are O(1).</li>
 *   <li>Every change is written to a temporary file which then replaces the real file,
 *       so a crash mid-write never leaves a half-written data file behind.</li>
 *   <li>If writing fails, the in-memory change is rolled back so memory and disk agree.</li>
 *   <li>Corrupt rows are skipped and logged instead of preventing the application from starting.</li>
 * </ul>
 *
 * @param <K> key type
 * @param <T> entity type
 */
public class CsvRepository<K, T> implements Repository<K, T> {

    private static final Logger LOG = AppLogger.get(CsvRepository.class);

    private final Path file;
    private final CsvMapper<T> mapper;
    private final Function<T, K> keyExtractor;
    private final Map<K, T> entities = new LinkedHashMap<>();
    private int skippedRows;

    /**
     * @param file         the CSV file, or {@code null} to keep data in memory only
     * @param mapper       converts entities to and from CSV rows
     * @param keyExtractor returns the unique key of an entity
     */
    public CsvRepository(Path file, CsvMapper<T> mapper, Function<T, K> keyExtractor) {
        this.file = file;
        this.mapper = mapper;
        this.keyExtractor = keyExtractor;
        if (file != null) {
            load();
        }
    }

    public static <K, T> CsvRepository<K, T> inMemory(CsvMapper<T> mapper, Function<T, K> keyExtractor) {
        return new CsvRepository<>(null, mapper, keyExtractor);
    }

    @Override
    public synchronized List<T> findAll() {
        return List.copyOf(entities.values());
    }

    @Override
    public synchronized Optional<T> findById(K id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public synchronized T save(T entity) {
        K key = keyExtractor.apply(entity);
        T previous = entities.put(key, entity);
        try {
            persist();
        } catch (StorageException e) {
            if (previous == null) {
                entities.remove(key);
            } else {
                entities.put(key, previous);
            }
            throw e;
        }
        return entity;
    }

    @Override
    public synchronized boolean deleteById(K id) {
        T removed = entities.remove(id);
        if (removed == null) {
            return false;
        }
        try {
            persist();
        } catch (StorageException e) {
            entities.put(id, removed);
            throw e;
        }
        return true;
    }

    @Override
    public synchronized int count() {
        return entities.size();
    }

    /** Number of rows that could not be parsed when the file was loaded. */
    public int skippedRows() {
        return skippedRows;
    }

    private void load() {
        if (!Files.exists(file)) {
            LOG.info(() -> "No data file yet at " + file + ", starting empty");
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }
                loadRow(line, lineNumber);
            }
        } catch (IOException e) {
            throw new StorageException("Could not read " + file + ": " + e.getMessage(), e);
        }
        LOG.info(() -> "Loaded " + entities.size() + " rows from " + file.getFileName());
    }

    private void loadRow(String line, int lineNumber) {
        try {
            T entity = mapper.fromRow(CsvUtil.parseLine(line));
            entities.put(keyExtractor.apply(entity), entity);
        } catch (RuntimeException e) {
            skippedRows++;
            LOG.warning(() -> "Skipping corrupt row " + lineNumber + " in " + file.getFileName()
                    + ": " + e.getMessage());
        }
    }

    private void persist() {
        if (file == null) {
            return;
        }
        List<String> lines = new ArrayList<>(entities.size() + 1);
        lines.add(CsvUtil.toLine(mapper.header()));
        for (T entity : entities.values()) {
            lines.add(CsvUtil.toLine(mapper.toRow(entity)));
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.severe(() -> "Failed to write " + file + ": " + e);
            throw new StorageException("Could not save data to " + file + ": " + e.getMessage(), e);
        }
    }
}
