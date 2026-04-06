# 📚 Library Management API – Test Cases README

## 🔧 Base URL

```
BASE=http://localhost:8080
```

---

## 🧪 Test Data Setup

### ➕ Create Books

#### 1. Create "The Hobbit"

```bash
curl -s -X POST "$BASE/api/books" -H "Content-Type: application/json" -d '{
  "title":"The Hobbit",
  "author":"J.R.R. Tolkien",
  "isbn":"978-0-261-10221-4",
  "publishedYear":1937,
  "availabilityStatus":"AVAILABLE"
}'
```

#### 2. Create "1984"

```bash
curl -s -X POST "$BASE/api/books" -H "Content-Type: application/json" -d '{
  "title":"1984",
  "author":"George Orwell",
  "isbn":"9780141036144",
  "publishedYear":1949,
  "availabilityStatus":"BORROWED"
}'
```

---

## 📖 Book APIs

### 🔍 Get Book by ID

```bash
curl -s "$BASE/api/books/1"
```

✅ Expected:

* Returns book with ID `1`

---

### 📄 Get All Books (Pagination + Sorting)

```bash
curl -s "$BASE/api/books?page=0&size=10&sort=title,asc"
```

✅ Expected:

* Sorted list by `title`
* Pagination metadata included

---

### 🔎 Filter Books

#### By Author + Year

```bash
curl -s "$BASE/api/books?author=tolkien&publishedYear=1937&page=0&size=10"
```

#### By Year Only

```bash
curl -s "$BASE/api/books?publishedYear=1937&page=0&size=10"
```

✅ Expected:

* Filtered results based on query params

---

### 🔍 Search Books

```bash
curl -s "$BASE/api/books/search?q=hobbit&page=0&size=10"
```

✅ Expected:

* Matches title/author containing "hobbit"

---

### ✏️ Update Book

#### Update Book 1

```bash
curl -s -X PUT "$BASE/api/books/1" -H "Content-Type: application/json" -d '{
  "title":"The Hobbit",
  "author":"J.R.R. Tolkien",
  "isbn":"978-0-261-10221-4",
  "publishedYear":1937,
  "availabilityStatus":"AVAILABLE"
}'
```

#### Update Book 2 (Change to AVAILABLE)

```bash
curl -s -X PUT "$BASE/api/books/2" -H "Content-Type: application/json" -d '{
  "title":"1984",
  "author":"George Orwell",
  "isbn":"9780141036144",
  "publishedYear":1949,
  "availabilityStatus":"AVAILABLE"
}'
```

✅ Expected:

* Updated book returned in response

---

## 👤 User APIs

### ➕ Create User

```bash
curl -s -X POST "$BASE/api/users" -H "Content-Type: application/json" -d '{"username":"alice"}'
```

### 🔍 Get User

```bash
curl -s "$BASE/api/users/1"
```

---

## ❤️ Wishlist APIs

### ➕ Add Book to Wishlist

#### Add Book 1

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/wishlist" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"bookId":1}'
```

#### Add Book 2

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/wishlist" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"bookId":2}'
```

✅ Expected:

* HTTP `200` or `201`

---

### ❌ Duplicate Wishlist Entry

```bash
curl -s -i -X POST "$BASE/api/wishlist" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"bookId":1}'
```

✅ Expected:

* HTTP `409 Conflict`

```json
{"error":"Book is already on this user's wishlist."}
```

---

### ❌ Invalid Book ID

```bash
curl -s -i -X POST "$BASE/api/wishlist" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"bookId":999999}'
```

✅ Expected:

* HTTP `404 Not Found`

```json
{"error":"Book not found: 999999"}
```

---

## 📊 Pagination Validation

```bash
curl -s "$BASE/api/books?page=0&size=5" | jq .
```

✅ Expected:

* Correct pagination fields:

  * `totalElements`
  * `totalPages`
  * `numberOfElements`

---

## ✅ Summary of Test Coverage

* ✔️ Create books
* ✔️ Fetch single book
* ✔️ Pagination & sorting
* ✔️ Filtering
* ✔️ Search
* ✔️ Update
* ✔️ User creation
* ✔️ Wishlist add
* ✔️ Duplicate handling (409)
* ✔️ Not found handling (404)

---

## 🗑️ Delete & Restore Book APIs

### 🗑️ Delete Book

#### Delete Book by ID

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE "$BASE/api/books/1"
```

✅ Expected:

* HTTP `204 No Content`

---

### ❌ Fetch Deleted Book

```bash
curl -s -i "$BASE/api/books/1"
```

✅ Expected:

* HTTP `404 Not Found`

```json
{"error":"Book not found: 1"}
```

---

### 📄 Verify Remaining Books

```bash
curl -s "$BASE/api/books?page=0&size=50"
```

✅ Expected:

* Deleted book is NOT present in results

---

## ♻️ Restore Book

#### Restore Deleted Book

```bash
curl -s -X POST "$BASE/api/books/1/restore"
```

✅ Expected:

```json
{
  "id":1,
  "title":"The Hobbit",
  "author":"J.R.R. Tolkien",
  "isbn":"9780261102214",
  "publishedYear":1937,
  "availabilityStatus":"AVAILABLE"
}
```

---

### 🔍 Verify Restored Book

```bash
curl -s "$BASE/api/books/1"
```

✅ Expected:

* Book is available again

---

## ⚠️ Edge Cases

### ❌ Delete Already Deleted Book

```bash
curl -s -i -X DELETE "$BASE/api/books/1"
```

(After deleting once already)

✅ Expected:

* HTTP `404 Not Found`

```json
{"error":"Book not found: 1"}
```

---

### ♻️ Restore Again (Idempotency Check)

```bash
curl -s -X POST "$BASE/api/books/1/restore"
```

✅ Expected:

* Book restored successfully (if soft delete)
* No duplication or error

---

## ✅ Summary of Delete/Restore Tests

* ✔️ Delete book (204)
* ✔️ Fetch deleted book → 404
* ✔️ Verify list excludes deleted book
* ✔️ Restore book
* ✔️ Fetch restored book → success
* ✔️ Delete non-existing book → 404
* ✔️ Restore idempotency behavior

---


## ⚠️ Notes

* Ensure application is running before testing
* Use `jq` for pretty-printing JSON (optional)
* IDs are assumed to start from `1`

---

## 🚀 Ready to Run

Execute the commands sequentially to validate full API functionality.

---

🏗️ High-Level Architecture

Layered architecture:

Controller → Service → Repository → DB

Database:

File-backed H2 database (persists across restarts)
🔁 Asynchronous Processing Pipeline

Instead of in-memory events, system uses DB-backed outbox pattern:

Flow:
API writes to async_events table
Scheduler A → expands into notifications
Scheduler B → processes notifications
🔷 Architecture Diagram (Logical)
Client API
   ↓
Books / Users / Wishlist APIs
   ↓
H2 Database
   ├── books
   ├── users
   ├── wishlist_entries
   ├── async_events   (Outbox)
   └── notifications  (Queue)

Schedulers:
   → AsyncEvent Processor
   → Notification Dispatcher
3. 🧩 Main Components
Layer	Responsibility
Controllers (/api/books, /api/users, /api/wishlist)	Handle HTTP requests, validation, pagination
BookService	CRUD, filtering, search, soft delete, ISBN validation, event enqueue
AsyncEventEnqueueService	Writes event to async_events (non-blocking)
AsyncEventProcessorService	Reads events, expands into notifications (fan-out)
WishlistNotificationWriterService	Batch inserts notifications using REQUIRES_NEW
NotificationDispatchService	Processes notifications (log/send)
Schedulers	Run cron jobs for async stages
Global Exception Handler	Standard error handling (400, 404, 409)
4. 🗄️ Data Model (Conceptual)
📘 books
id, title, author, isbn, published_year
availability_status
deleted (soft delete flag)
👤 users
id, name (minimal identity)
❤️ wishlist_entries
user_id
book_id
⚠️ No JPA relations → scalar FKs (performance-friendly)
📤 async_events (Outbox Table)
id
book_id
availability_status
status (PENDING → PROCESSED)
🔔 notifications
id
user_id
book_id
book_title (snapshot)
type (WISHLIST)
status (PENDING → PROCESSED)
5. 🔄 Critical Flows
5.1 📗 Book Return → Notification Flow

Client updates:

BORROWED → AVAILABLE
Transaction commits
Event inserted into async_events
Scheduler A:
Picks events (SKIP LOCKED)
Validates book state
Fetches wishlist users
Creates notification entries
Scheduler B:
Processes notifications
Logs / sends
Marks as PROCESSED
5.2 🗑️ Soft Delete Flow
DELETE /api/books/{id} → sets deleted = true
Hidden from queries (not physically removed)

Restore via:

POST /api/books/{id}/restore

📌 ISBN uniqueness still enforced (even if soft deleted)

5.3 🔍 Search & Listing
List API
Filters: author (partial), publishedYear
Uses JPA Specifications
Excludes deleted records
Search API
Uses JPQL LIKE
Searches title + author
6. ⚙️ Multi-Instance & Concurrency Design
Key Techniques:
✅ SKIP LOCKED
Used in:
async_events
notifications
Ensures:
No duplicate processing across nodes
Safe parallel schedulers
✅ Outbox Pattern
Ensures reliability
Avoids:
ApplicationEventPublisher limitations
In-memory event loss across nodes
✅ Parallel Fan-out
Uses thread pool
Batch processing
REQUIRES_NEW transactions per batch
