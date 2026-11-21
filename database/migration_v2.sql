-- CampusHelp — migration v2 (photo attachments, staff assignment, ratings)
-- Run against an existing database:  mysql -u root -p campushelp < database/migration_v2.sql
-- Safe to re-run: every change is guarded by an information_schema check.
-- Fresh installs do not need this file: database/schema.sql already includes it.

USE campushelp;

-- 1. Photo attachments + staff assignment on complaints
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'campushelp' AND TABLE_NAME = 'complaints' AND COLUMN_NAME = 'photo_url'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE complaints ADD COLUMN photo_url VARCHAR(500) NULL AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'campushelp' AND TABLE_NAME = 'complaints' AND COLUMN_NAME = 'assigned_to_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE complaints ADD COLUMN assigned_to_id BIGINT NULL AFTER photo_url',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Foreign key for the assignee (only if it does not exist yet)
SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = 'campushelp'
    AND TABLE_NAME = 'complaints'
    AND CONSTRAINT_NAME = 'fk_complaints_assigned_to'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE complaints ADD CONSTRAINT fk_complaints_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (user_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Ratings (one per complaint)
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

-- 3. Seed staff user (password "campus123" — same BCrypt hash as the other seed users)
INSERT INTO users (user_id, name, email, password_hash, role) VALUES
  (3, 'Ravi Kumar', 'staff@campushelp.edu',
   '$2b$10$PYTbvWFMnY84SEHNxGC5O.amJ6N4E.lMCVGR18awP9U0KgyWcfkwO', 'STAFF')
AS new ON DUPLICATE KEY UPDATE name = new.name;
