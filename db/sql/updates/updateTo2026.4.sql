ALTER TABLE `log_topic`
  ADD COLUMN `email_list` text DEFAULT NULL
  COMMENT 'Comma-separated email addresses to notify on new log entry';
