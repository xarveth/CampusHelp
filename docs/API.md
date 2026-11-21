# CampusHelp API

Base URL: `http://localhost:8080/api`

All endpoints except `POST /auth/*`, `GET /categories`, and `GET /uploads/*` require
a JWT in the `Authorization: Bearer <token>` header. Tokens are issued by
login/register and expire after 24 hours. Roles: `STUDENT`, `ADMIN`, `STAFF`.

Error responses: `{ "error": "<message>" }` with the matching HTTP status
(400 validation, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict).

## Auth

### POST /auth/register — student registration
```json
// Request
{ "name": "Himanshu Yadav", "email": "himanshu@campushelp.edu", "password": "campus123" }

// Response 201
{ "token": "<jwt>", "userId": 2, "name": "Himanshu Yadav",
  "email": "himanshu@campushelp.edu", "role": "STUDENT" }
```

### POST /auth/login
```json
// Request
{ "email": "admin@campushelp.edu", "password": "campus123" }

// Response 200
{ "token": "<jwt>", "userId": 1, "name": "Campus Administrator",
  "email": "admin@campushelp.edu", "role": "ADMIN" }
```

### GET /auth/me — current user (auth required)
```json
{ "id": 2, "name": "Himanshu Yadav", "email": "himanshu@campushelp.edu", "role": "STUDENT" }
```

## Categories

### GET /categories — public list of the 7 categories
```json
[ { "id": 1, "name": "Classroom" }, { "id": 2, "name": "Laboratory" } ]
```

### POST /categories — admin only
```json
// Request
{ "name": "Hostel" }
// Response 201
{ "id": 8, "name": "Hostel" }
```

### DELETE /categories/{id} — admin only, 204 No Content

## Complaints (student + admin)

### POST /complaints — submit a complaint (any logged-in user; new complaints are Pending)
```json
// Request
{ "subject": "Computers not working", "categoryId": 2,
  "description": "Several computers in the computer laboratory are not starting." }

// Response 201
{
  "id": 1, "code": "CH-1001", "subject": "Computers not working",
  "description": "Several computers in the computer laboratory are not starting.",
  "category": "Laboratory", "categoryId": 2, "status": "PENDING",
  "studentName": "Himanshu Yadav", "studentEmail": "himanshu@campushelp.edu",
  "photoUrl": null, "assignedToId": null, "assignedToName": null, "rating": null,
  "createdAt": "2026-11-20T09:30:00",
  "history": [
    { "oldStatus": null, "newStatus": "PENDING", "changedBy": "System",
      "changedAt": "2026-11-20T09:30:00" }
  ]
}
```

### POST /complaints (multipart) — submit with a photo
Same fields as above, sent as `multipart/form-data` with an optional `photo`
file part. Only JPG, PNG, or WebP images, max 5 MB. The response is the same
complaint object with `photoUrl` set, e.g. `"/api/uploads/<uuid>.jpg"`.

### GET /uploads/{filename} — download a complaint photo (public, no JWT needed)
Returns the image file. Filenames are random UUIDs, so the URL is not guessable.

### POST /complaints/{id}/rating — rate a resolved complaint (student owner only)
Only when the complaint is `RESOLVED`, and only once per complaint (a second
rating is rejected with 409).
```json
// Request
{ "stars": 5, "comment": "Fixed quickly, thank you!" }

// Response 201 — the complaint object, now with
// "rating": { "stars": 5, "comment": "Fixed quickly, thank you!", "createdAt": "..." }
```

### GET /complaints/mine — the logged-in student's own complaints, newest first
Returns an array of complaint objects in the shape above.

### GET /complaints/{id} — one complaint with its status history
A student can only fetch their own complaint (403 otherwise); a staff member
only complaints assigned to them; an admin can fetch any.

## Admin

### GET /admin/dashboard — counts + category-wise totals + status distribution + average rating + 5 most recent (admin only)
```json
{
  "total": 1, "pending": 1, "inProgress": 0, "resolved": 0,
  "byStatus": { "PENDING": 1, "IN_PROGRESS": 0, "RESOLVED": 0 },
  "byCategory": { "Laboratory": 1 },
  "averageRating": 5.0, "ratingCount": 1,
  "recent": [ { "...": "complaint object, see POST /complaints" } ]
}
```
`averageRating` is null when no complaint has been rated yet. The frontend
draws the `byCategory` and `byStatus` numbers as plain CSS bar charts.

### GET /admin/complaints — all complaints (admin only)
Optional query params: `?status=PENDING|IN_PROGRESS|RESOLVED`, `?q=<search text>`.
Search matches complaint code, subject, student name, or category.

### GET /admin/complaints/{id} — one complaint with history (admin only)

### PATCH /admin/complaints/{id}/status — update status (admin only)
Every change writes a row to `complaint_status_history` (old status, new status,
admin, timestamp).
```json
// Request
{ "status": "IN_PROGRESS" }

// Response 200 — the updated complaint object, history included
```

### POST /admin/staff — create a staff account (admin only)
```json
// Request
{ "name": "Ravi Kumar", "email": "staff@campushelp.edu", "password": "campus123" }

// Response 201
{ "id": 3, "name": "Ravi Kumar", "email": "staff@campushelp.edu" }
```
The seeded staff account is `staff@campushelp.edu` / `campus123`.

### GET /admin/staff — list staff accounts (admin only)
```json
[ { "id": 3, "name": "Ravi Kumar", "email": "staff@campushelp.edu" } ]
```

### PATCH /admin/complaints/{id}/assign — assign a complaint to staff (admin only)
```json
// Request
{ "staffId": 3 }

// Response 200 — the complaint object, with "assignedToId": 3, "assignedToName": "Ravi Kumar"
```

## Staff

### GET /staff/complaints — complaints assigned to the logged-in staff member
Returns an array of complaint objects, newest first. Staff see only their own
assigned complaints.

### GET /staff/complaints/{id} — one assigned complaint (403 if not assigned to them)

### PATCH /staff/complaints/{id}/status — update status of an assigned complaint
Same rules as the admin endpoint; every change writes a status-history row.
