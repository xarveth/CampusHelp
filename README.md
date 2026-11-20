# CampusHelp — Student Grievance & Complaint Management System

A full-stack web application where students submit campus complaints and track
them, and administrators review them, update their status, and see reports.

**Author:** Himanshu Yadav, Roll No. 2401450100016, B.Tech CSE 3rd Year

## Tech Stack

| Layer    | Technology |
| -------- | ---------- |
| Frontend | React.js (Vite), React Router |
| Backend  | Java 17, Spring Boot 3.3, Spring Data JPA / Hibernate, Spring Security + JWT |
| Database | MySQL 8 |
| Auth     | BCrypt password hashing, JWT in `Authorization: Bearer` header |

## Project Structure

```
campushelp/
├── backend/            Spring Boot app (Maven)
│   └── src/main/java/com/campushelp/
│       ├── entity/     User, Category, Complaint, StatusHistory, Role, ComplaintStatus
│       ├── repository/ Spring Data JPA repositories
│       ├── service/    AuthService, ComplaintService, AdminService
│       ├── controller/ AuthController, ComplaintController, AdminController, CategoryController
│       ├── security/   JwtUtil, JwtFilter, SecurityConfig
│       ├── dto/        request/response records
│       └── config/     GlobalExceptionHandler, DataSeeder
├── frontend/           React + Vite app
│   └── src/
│       ├── api.js      fetch client (JWT from localStorage)
│       ├── pages/      Login, Register, StudentDashboard, AdminDashboard, ComplaintDetail
│       └── styles.css
├── database/schema.sql MySQL DDL + seed data
└── docs/API.md         Full endpoint reference
```

## How It Works

1. A student registers and logs in.
2. The student submits a complaint (subject, category, description). It gets a
   unique ID like `CH-1001` and starts as **Pending**.
3. The admin sees it on the dashboard, reviews it, and changes the status to
   **In Progress** and then **Resolved**. Every change is recorded in the
   status history with the old status, new status, admin, and timestamp.
4. The student logs in and sees the latest status and full history. A student
   can only view their own complaints — the backend enforces this even if the
   complaint ID is changed in the URL.

Categories: Classroom, Laboratory, Internet, Electricity, Cleanliness, Water, Other.
Statuses (only these three): Pending, In Progress, Resolved.

## Prerequisites

- Java 17+ and Maven 3.9+
- Node.js 18+ and npm
- MySQL 8 running on localhost:3306

## Setup

### 1. Database

```bash
mysql -u root -p < database/schema.sql
```

This creates the `campushelp` database, the 4 tables (`users`,
`complaint_categories`, `complaints`, `complaint_status_history`), the 7
categories, the admin and Himanshu Yadav accounts, and the worked example CH-1001.

If you prefer, skip the user/complaint inserts in `schema.sql` — the backend's
`DataSeeder` creates the same accounts and CH-1001 automatically the first
time it runs against an empty `users` table.

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080/api`.

Configuration lives in `src/main/resources/application.properties`. The
defaults assume MySQL user `root` with no password. Override with environment
variables if yours differ:

```bash
DB_URL=jdbc:mysql://localhost:3306/campushelp DB_USERNAME=root DB_PASSWORD=secret \
JWT_SECRET=<a-long-random-secret> mvn spring-boot:run
```

> Set a real `JWT_SECRET` in production — the default in
> `application.properties` is for local development only.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. For a production build: `npm run build`
(output in `frontend/dist/`).

## Demo Logins (password: `campus123` for both)

| Role    | Email | What to do |
| ------- | ----- | ---------- |
| Student | `himanshu@campushelp.edu` | Open CH-1001, submit a new complaint, watch it appear as Pending |
| Admin   | `admin@campushelp.edu` | Dashboard totals, search/filter, open CH-1001, move it to In Progress, then Resolved |

## Viva Demo Flow (2 minutes)

1. Login as Himanshu Yadav → CH-1001 is Pending (Laboratory, "Computers not working").
2. Submit a new complaint (e.g. Internet, "Wi-Fi not working in library") → gets
   the next CH- ID, status Pending.
3. Logout, login as admin → dashboard shows 2 total, 2 pending; category counts
   updated.
4. Open CH-1001 → change status to In Progress → the timeline shows
   "Pending → In Progress · by Campus Administrator" with a timestamp.
5. Login again as Himanshu Yadav → CH-1001 now shows In Progress with full history.

## API Summary

See [docs/API.md](docs/API.md) for full request/response examples.

| Method | Endpoint | Role |
| ------ | -------- | ---- |
| POST | `/api/auth/register` | public |
| POST | `/api/auth/login` | public |
| GET | `/api/auth/me` | any logged-in user |
| GET | `/api/categories` | public |
| POST / DELETE | `/api/categories` | ADMIN |
| POST | `/api/complaints` | any logged-in user |
| GET | `/api/complaints/mine` | STUDENT (own only) |
| GET | `/api/complaints/{id}` | owner or ADMIN |
| GET | `/api/admin/dashboard` | ADMIN |
| GET | `/api/admin/complaints?status=&q=` | ADMIN |
| GET | `/api/admin/complaints/{id}` | ADMIN |
| PATCH | `/api/admin/complaints/{id}/status` | ADMIN |

## Notes & Limitations

- This is the real project codebase (React + Spring Boot + MySQL as specified).
  It is not a hosted demo — run it locally with the steps above.
- Email notifications, photo attachments, priority levels, and escalation are
  future enhancements (see the project spec) and are intentionally not in v1.
- If Maven cannot download dependencies (offline machine), run
  `mvn -o spring-boot:run` after one successful online build populates the
  local `~/.m2` cache.
