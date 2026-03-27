---
description: Apply SQL optimisation rules when writing or reviewing database queries. Use whenever the conversation involves SQL, database design, or query performance.
---

## SQL Optimisation Rules

Follow these rules every time you write or review SQL queries:

- **Use indexed columns** in `WHERE`, `JOIN`, and `ORDER BY` clauses
- **Avoid `SELECT *`** — always list the columns you actually need
- **Prefer JOINs over subqueries** for large datasets; subqueries can prevent index use
- **Use `EXPLAIN`** to verify query plans before deploying to production
- **Batch large writes** — never `DELETE` or `UPDATE` millions of rows in one statement
- **Paginate large reads** with `LIMIT` / `OFFSET` or keyset pagination
- **Normalise for writes, denormalise for reads** — choose based on the workload
