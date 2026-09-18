package com.expensetracker.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic storage abstraction. Services depend on this interface only, so the CSV
 * implementation could be replaced by a database without touching business logic.
 *
 * @param <K> key type
 * @param <T> entity type
 */
public interface Repository<K, T> {

    List<T> findAll();

    Optional<T> findById(K id);

    /** Inserts the entity, or replaces the existing one with the same key. */
    T save(T entity);

    /** @return {@code true} if an entity was removed */
    boolean deleteById(K id);

    int count();
}
