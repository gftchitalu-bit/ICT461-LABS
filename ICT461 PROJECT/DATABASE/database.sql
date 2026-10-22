CREATE DATABASE smart_zaqa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_zaqa;

-- 1. USERS
CREATE TABLE users (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(120) NOT NULL,
  email         VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,              -- PHP password_hash(), never plain text
  phone         VARCHAR(30) NULL,
  role          ENUM('applicant','reviewer','admin') NOT NULL DEFAULT 'applicant',
  is_active     TINYINT(1) NOT NULL DEFAULT 1,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. LOGIN TOKENS (the frontend sends a token header)
CREATE TABLE api_tokens (
  id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id     INT UNSIGNED NOT NULL,
  token_hash  CHAR(64) NOT NULL UNIQUE,             -- store SHA-256 of the token, not the token
  expires_at  DATETIME NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX (expires_at)
) ENGINE=InnoDB;

-- 3. INSTITUTIONS (reference data, synthetic at first)
CREATE TABLE institutions (
  id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name    VARCHAR(200) NOT NULL UNIQUE,
  country VARCHAR(80) NOT NULL DEFAULT 'Zambia'
) ENGINE=InnoDB;

-- 4. APPLICATIONS (frontend "items")
CREATE TABLE applications (
  id                INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  reference_no      VARCHAR(30) NOT NULL UNIQUE,    -- e.g. SQV-2026-000123
  applicant_id      INT UNSIGNED NOT NULL,
  assigned_to       INT UNSIGNED NULL,              -- reviewer
  institution_id    INT UNSIGNED NULL,
  qualification_title VARCHAR(200) NOT NULL,
  award_date        DATE NULL,
  certificate_no    VARCHAR(80) NULL,
  purpose           VARCHAR(255) NULL,
  status            ENUM('draft','submitted','incomplete','under_review',
                         'awaiting_institution','info_requested',
                         'completed','rejected') NOT NULL DEFAULT 'draft',
  next_action       VARCHAR(255) NULL,              -- shown to applicant ("Upload transcript")
  due_date          DATE NULL,                      -- for overdue detection
  submitted_at      DATETIME NULL,
  completed_at      DATETIME NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (applicant_id)   REFERENCES users(id),
  FOREIGN KEY (assigned_to)    REFERENCES users(id) ON DELETE SET NULL,
  FOREIGN KEY (institution_id) REFERENCES institutions(id) ON DELETE SET NULL,
  INDEX idx_app_status (status),
  INDEX idx_app_applicant (applicant_id),
  INDEX idx_app_due (due_date)
) ENGINE=InnoDB;

-- 5. REQUIRED DOCUMENT TYPES (drives the completeness checker)
CREATE TABLE document_types (
  id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(40) NOT NULL UNIQUE,          -- 'CERTIFICATE','ID','TRANSCRIPT'
  label       VARCHAR(120) NOT NULL,
  is_required TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- 6. UPLOADED DOCUMENTS
CREATE TABLE documents (
  id               INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  application_id   INT UNSIGNED NOT NULL,
  document_type_id INT UNSIGNED NOT NULL,
  original_name    VARCHAR(255) NOT NULL,
  stored_name      VARCHAR(255) NOT NULL UNIQUE,    -- random name, file kept outside htdocs
  mime_type        VARCHAR(100) NOT NULL,
  size_bytes       INT UNSIGNED NOT NULL,
  sha256           CHAR(64) NOT NULL,
  is_readable      TINYINT(1) NULL,                 -- result of basic readability check
  uploaded_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (application_id)   REFERENCES applications(id) ON DELETE CASCADE,
  FOREIGN KEY (document_type_id) REFERENCES document_types(id),
  INDEX (application_id)
) ENGINE=InnoDB;

-- 7. OCR RESULTS (assistive only, human reviewed)
CREATE TABLE ocr_results (
  id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  document_id     INT UNSIGNED NOT NULL,
  raw_text        MEDIUMTEXT NULL,
  ocr_status      ENUM('pending','done','failed') NOT NULL DEFAULT 'pending',
  confidence      DECIMAL(5,2) NULL,
  ext_name        VARCHAR(200) NULL,                -- extracted candidates
  ext_institution VARCHAR(200) NULL,
  ext_title       VARCHAR(200) NULL,
  ext_award_date  VARCHAR(40)  NULL,
  ext_cert_no     VARCHAR(80)  NULL,
  mismatch_flags  JSON NULL,                        -- fields differing from the form
  reviewed_by     INT UNSIGNED NULL,
  reviewed_at     DATETIME NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 8. STATUS HISTORY (the tracking timeline)
CREATE TABLE status_history (
  id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  application_id INT UNSIGNED NOT NULL,
  from_status    VARCHAR(30) NULL,
  to_status      VARCHAR(30) NOT NULL,
  note           VARCHAR(500) NULL,
  changed_by     INT UNSIGNED NULL,
  changed_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
  FOREIGN KEY (changed_by)     REFERENCES users(id) ON DELETE SET NULL,
  INDEX (application_id, changed_at)
) ENGINE=InnoDB;

-- 9. NOTIFICATIONS (also pushed over WebSocket)
CREATE TABLE notifications (
  id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id        INT UNSIGNED NOT NULL,
  application_id INT UNSIGNED NULL,
  type           ENUM('notification','message') NOT NULL DEFAULT 'notification',
  title          VARCHAR(150) NOT NULL,
  body           VARCHAR(500) NULL,
  is_read        TINYINT(1) NOT NULL DEFAULT 0,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id)        REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
  INDEX (user_id, is_read)
) ENGINE=InnoDB;

-- 10. AUDIT LOG (admin audit page)
CREATE TABLE audit_logs (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id     INT UNSIGNED NULL,
  action      VARCHAR(60) NOT NULL,                 -- 'LOGIN','UPLOAD','STATUS_CHANGE','ROLE_CHANGE'
  entity_type VARCHAR(40) NULL,
  entity_id   INT UNSIGNED NULL,
  details     JSON NULL,
  ip_address  VARCHAR(45) NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX (created_at),
  INDEX (user_id)
) ENGINE=InnoDB;

-- SEED DATA
INSERT INTO document_types (code, label, is_required) VALUES
 ('CERTIFICATE','Qualification certificate',1),
 ('TRANSCRIPT','Academic transcript',1),
 ('ID','National ID or passport',1),
 ('OTHER','Other supporting document',0);