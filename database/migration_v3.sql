-- CampusHelp — migration v3 (detailed tracking, analytics, notifications, duplicates)
-- Run against an existing database:  mysql -u root -p campushelp < database/migration_v3.sql
-- Safe to re-run: every change is guarded by an information_schema check.
-- Fresh installs do not need this file: database/schema.sql already includes it.
--
-- What it does:
--   1. Renames the old PENDING status to SUBMITTED (new 6-step workflow).
--   2. Adds priority, resolved_at (average resolution time) and duplicate_of_id.
--   3. Adds a note column to the status history (reopen reasons, admin notes).
--   4. Creates notifications, complaint_updates (comments) and
--      complaint_same_issue ("I have the same problem") tables.

USE campushelp;

-- 1. PENDING -> SUBMITTED (complaints + history, both status columns)
UPDATE complaints SET status = 'SUBMITTED' WHERE status = 'PENDING';
UPDATE complaint_status_history SET old_status = 'SUBMITTED' WHERE old_status = 'PENDING';
UPDATE complaint_status_history SET new_status = 'SUBMITTED' WHERE new_status = 'PENDING';

-- 2a. Priority on complaints (default MEDIUM)
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'campushelp' AND TABLE_NAME = 'complaints' AND COLUMN_NAME = 'priority'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE complaints ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT ''MEDIUM'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2b. Resolved timestamp (average resolution time)
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'campushelp' AND TABLE_NAME = 'complaints' AND COLUMN_NAME = 'resolved_at'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE complaints ADD COLUMN resolved_at DATETIME NULL AFTER priority',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill resolved_at from the history (latest time a complaint became RESOLVED)
UPDATE complaints c
SET c.resolved_at = (
  SELECT MAX(h.changed_at) FROM complaint_status_history h
  WHERE h.complaint_id = c.complaint_id AND h.new_status = 'RESOLVED'
)
WHERE c.status = 'RESOLVED' AND c.resolved_at IS NULL;

-- 2c. Duplicate link (this complaint is a duplicate of an earlier one)
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'campushelp' AND TABLE_NAME = 'complaints' AND COLUMN_NAME = 'duplicate_of_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE complaints ADD COLUMN duplicate_of_id BIGINT NULL AFTER resolved_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = 'campushelp'
    AND TABLE_NAME = 'complaints'
    AND CONSTRAINT_NAME = 'fk_complaints_duplicate_of'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE complaints ADD CONSTRAINT fk_complaints_duplicate_of FOREIGN KEY (duplicate_of_id) REFERENCES complaints (complaint_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Note on status history (reopen reason, admin note)
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'campushelp' AND TABLE_NAME = 'complaint_status_history' AND COLUMN_NAME = 'note'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE complaint_status_history ADD COLUMN note VARCHAR(500) NULL AFTER new_status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4a. Notifications (in-app bell)
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    complaint_id    BIGINT NULL,
    message         VARCHAR(500) NOT NULL,
    is_read         TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)      REFERENCES users (user_id),
    FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id)
);

-- 4b. Admin/staff updates (comments) on a complaint
CREATE TABLE IF NOT EXISTS complaint_updates (
    update_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    author_id    BIGINT NOT NULL,
    message      TEXT NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id),
    FOREIGN KEY (author_id)    REFERENCES users (user_id)
);

-- 4c. "I have the same problem" reports
CREATE TABLE IF NOT EXISTS complaint_same_issue (
    same_issue_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id  BIGINT NOT NULL,
    student_id    BIGINT NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_same_issue (complaint_id, student_id),
    FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id),
    FOREIGN KEY (student_id)   REFERENCES users (user_id)
);
