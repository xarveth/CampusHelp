-- CampusHelp — MySQL schema
-- Run:  mysql -u root -p < database/schema.sql
-- This creates the `campushelp` database, the 5 tables, and seed data.
-- NOTE: the three seeded users below use a real BCrypt hash for the
-- password "campus123" (verified). If you run the Spring Boot backend, its
-- DataSeeder creates the same users automatically (only when the users table
-- is empty), so you can skip the user/complaint inserts here and let the app
-- seed them.

CREATE DATABASE IF NOT EXISTS campushelp
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campushelp;

CREATE TABLE IF NOT EXISTS users (
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS complaint_categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_code VARCHAR(20) NOT NULL UNIQUE,
    student_id     BIGINT NOT NULL,
    category_id    BIGINT NOT NULL,
    subject        VARCHAR(200) NOT NULL,
    description    TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    photo_url      VARCHAR(500) NULL,
    assigned_to_id BIGINT NULL,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id)  REFERENCES users (user_id),
    FOREIGN KEY (category_id) REFERENCES complaint_categories (category_id),
    FOREIGN KEY (assigned_to_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS complaint_ratings (
    rating_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    student_id   BIGINT NOT NULL,
    stars        INT NOT NULL,
    comment      VARCHAR(500) NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rating_complaint (complaint_id),
    FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id),
    FOREIGN KEY (student_id)   REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS complaint_status_history (
    history_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    admin_id    BIGINT NULL,
    old_status  VARCHAR(20) NULL,
    new_status  VARCHAR(20) NOT NULL,
    changed_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id),
    FOREIGN KEY (admin_id)     REFERENCES users (user_id)
);

-- Seed: 7 categories
INSERT INTO complaint_categories (name) VALUES
  ('Classroom'), ('Laboratory'), ('Internet'), ('Electricity'),
  ('Cleanliness'), ('Water'), ('Other')
AS new ON DUPLICATE KEY UPDATE name = new.name;

-- Seed users (password "campus123", BCrypt hash generated and verified).
INSERT INTO users (user_id, name, email, password_hash, role) VALUES
  (1, 'Campus Administrator', 'admin@campushelp.edu',
   '$2b$10$PYTbvWFMnY84SEHNxGC5O.amJ6N4E.lMCVGR18awP9U0KgyWcfkwO', 'ADMIN'),
  (2, 'Himanshu Yadav', 'himanshu@campushelp.edu',
   '$2b$10$PYTbvWFMnY84SEHNxGC5O.amJ6N4E.lMCVGR18awP9U0KgyWcfkwO', 'STUDENT'),
  (3, 'Ravi Kumar', 'staff@campushelp.edu',
   '$2b$10$PYTbvWFMnY84SEHNxGC5O.amJ6N4E.lMCVGR18awP9U0KgyWcfkwO', 'STAFF')
AS new ON DUPLICATE KEY UPDATE name = new.name;

-- Seed worked example: CH-1001 (Laboratory, Pending)
INSERT INTO complaints (complaint_id, complaint_code, student_id, category_id, subject, description, status) VALUES
  (1, 'CH-1001', 2, 2, 'Computers not working',
   'Several computers in the computer laboratory are not starting. This is affecting practical sessions.',
   'PENDING')
AS new ON DUPLICATE KEY UPDATE subject = new.subject;

INSERT INTO complaint_status_history (complaint_id, admin_id, old_status, new_status) VALUES
  (1, NULL, NULL, 'PENDING');
