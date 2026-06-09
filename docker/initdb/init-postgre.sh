#!/bin/bash
set -e

echo "========== Initializing Database =========="

echo ">>> Running DDL scripts..."
for f in /initdb/ddl/postgre/*.sql; do
    [ -f "$f" ] || continue
    echo "    DDL: $(basename "$f")"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$f"
done

echo ">>> Running DML scripts..."
for f in /initdb/dml/*.sql; do
    [ -f "$f" ] || continue
    echo "    DML: $(basename "$f")"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$f"
done

echo "========== Database Initialization Complete =========="
