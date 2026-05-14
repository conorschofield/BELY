-- Add log_link table for many-to-many log entry linking.

CREATE TABLE IF NOT EXISTS `log_link` (
  `log_id` int(11) unsigned NOT NULL,
  `linked_log_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`log_id`, `linked_log_id`),
  KEY `log_link_k1` (`log_id`),
  KEY `log_link_k2` (`linked_log_id`),
  CONSTRAINT `log_link_fk1` FOREIGN KEY (`log_id`) REFERENCES `log` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `log_link_fk2` FOREIGN KEY (`linked_log_id`) REFERENCES `log` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
