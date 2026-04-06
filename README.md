🏗️ High-Level Architecture

Architecture Style:

Monolithic Spring Boot 3 application

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
