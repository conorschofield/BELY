-- ============================================================================
-- Add EditDisabled global setting
-- ============================================================================

INSERT IGNORE INTO `setting_type` VALUES (19,'ItemDomainLogbook.List.Display.EditDisabled','Disable editing of log entries globally.','false');
ALTER TABLE `log_topic` ADD COLUMN `email_list` text DEFAULT NULL COMMENT 'Comma-separated email addresses to notify on new log entry';