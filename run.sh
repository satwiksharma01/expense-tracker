#!/usr/bin/env sh
# Builds the jar if needed, then starts the application. Arguments are passed through,
# e.g.  ./run.sh --demo   or   ./run.sh report 2026-09
set -e
cd "$(dirname "$0")"

if [ ! -f expense-tracker.jar ]; then
    ./build.sh
fi
exec java -jar expense-tracker.jar "$@"
