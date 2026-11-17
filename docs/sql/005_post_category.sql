-- Apply once after 001–004. Existing posts become "other".
ALTER TABLE post ADD COLUMN category VARCHAR(16) NOT NULL DEFAULT 'other';
