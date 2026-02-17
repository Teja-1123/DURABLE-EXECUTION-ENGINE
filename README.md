# Durable Execution Engine (Java)

# 🚀 Durable Execution Engine

A lightweight workflow engine in Java that provides **step-level durability**, 
**crash recovery**, and **idempotent execution** using SQLite persistence.

---

## 📌 Architecture

The Durable Execution Engine ensures that completed workflow steps are never re-executed after a crash.

### 🔹 High-Level Execution Flow

```
WorkflowEngine.runWorkflow(workflowId, workflow)
        ↓
Create DurableContext (workflowId + SQLiteStore)
        ↓
workflow.run(ctx)  ← Normal Java Code
        ↓
ctx.step(id, type, fn)
        ↓
Generate Unique step_key (id + sequence)
        ↓
Check SQLite DB
        ↓
IF COMPLETED → Return cached result
ELSE → Execute function → Save to DB → Return result
```

### 🔹 Core Components

- **WorkflowEngine** → Starts or resumes workflow execution.
- **DurableContext** → Maintains workflow state and sequence counter.
- **Step Primitive (`step()`)** → Adds durability to side-effect operations.
- **SQLiteStore** → Persists step results.
- **Logical Sequence Counter** → Ensures unique step identification in loops.

---

## 🧠 Key Features

- ✅ Step-level memoization
- ✅ Crash recovery
- ✅ Parallel step execution
- ✅ SQLite persistence
- ✅ Loop-safe sequence handling
- ✅ Idempotent workflow design

---

## ⚙️ How to Run

### 1️⃣ Build Project

```bash
mvn clean install
```

---

### 2️⃣ Run Unit Tests

```bash
mvn test
```

Expected Output:

```
Tests run: 4, Failures: 0, Errors: 0
BUILD SUCCESS
```

---

### 3️⃣ Run Application

If `exec-maven-plugin` is configured:

```bash
mvn exec:java
```

---

### 4️⃣ Simulate Crash (Optional)

Run workflow with crash simulation enabled.

After crash, run again:

```bash
mvn exec:java
```

You should see:

```
[SKIP] Step 'createRecord' already completed.
```

This proves durability and memoization.

---

### 5️⃣ Reset Workflow (Optional)

Delete the SQLite database file:

```
workflow.db
```

Then rerun:

```bash
mvn exec:java
```

---

## 🛡 Crash Recovery Example

### First Run:

```
[RUN] Executing step: createRecord
[DONE] Step 'createRecord' saved to DB.
SIMULATED CRASH
```

### Second Run:

```
[SKIP] Step 'createRecord' already completed.
[RUN] Executing step: provisionLaptop
Workflow COMPLETED successfully.
```

---

## 📂 Project Structure

```
src/
 ├── engine/
 ├── persistence/
 ├── examples/
 └── test/

pom.xml
workflow.db
```

---

## 🎯 Why This Matters

This engine demonstrates:

- Durable workflow execution
- Safe replay behavior
- Prevention of duplicate side effects
- Production-inspired backend design


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
