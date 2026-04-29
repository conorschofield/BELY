-- Update to 2026.5: Replace single log_topic_id on log table with many-to-many join table.

-- Step 1: Create the join table
CREATE TABLE IF NOT EXISTS `log_log_topic` (
  `log_id` int(11) unsigned NOT NULL,
  `log_topic_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`log_id`, `log_topic_id`),
  KEY `log_log_topic_k1` (`log_topic_id`),
  CONSTRAINT `log_log_topic_fk1` FOREIGN KEY (`log_id`) REFERENCES `log` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `log_log_topic_fk2` FOREIGN KEY (`log_topic_id`) REFERENCES `log_topic` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Step 2: Migrate existing single-category assignments to the join table
INSERT IGNORE INTO `log_log_topic` (`log_id`, `log_topic_id`)
  SELECT `id`, `log_topic_id` FROM `log` WHERE `log_topic_id` IS NOT NULL;

-- Step 3: Drop the old FK constraint and column from the log table
ALTER TABLE `log` DROP FOREIGN KEY `log_fk2`;
ALTER TABLE `log` DROP KEY `log_k2`;
ALTER TABLE `log` DROP COLUMN `log_topic_id`;
