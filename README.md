# Durable Execution Engine (Java)

## How Sequence Tracking Handles Loops
The engine uses an `AtomicInteger` sequence counter per workflow run.
Each call to `step()` increments the counter, generating a key like
`stepId_seq_1`, `stepId_seq_2`, etc. This means even if the same
step ID is used in a loop, each iteration gets a unique key in the DB.

## Thread Safety
Parallel steps use `CompletableFuture` with a thread pool.
All SQLite writes in `SQLiteStore` are `synchronized` methods.
SQLite WAL mode is enabled for concurrent read access.
The `AtomicInteger` sequence counter is thread-safe by design.

## Zombie Step Problem
If a crash occurs AFTER a step runs but BEFORE the DB commit, on restart
the step will re-execute (idempotency must be ensured by the step itself).
The `INSERT OR REPLACE` ensures no duplicate keys corrupt the state.