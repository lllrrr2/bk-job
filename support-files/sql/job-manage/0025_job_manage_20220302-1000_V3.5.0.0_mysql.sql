  -- Update `application` schema
  ALTER TABLE `application` MODIFY `app_id` BIGINT(20) UNSIGNED AUTO_INCREMENT;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.columns
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'application'
                  AND COLUMN_NAME = 'bk_scope_type') THEN
    ALTER TABLE application ADD COLUMN bk_scope_type VARCHAR(32) NOT NULL DEFAULT '';
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.columns
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'application'
                  AND COLUMN_NAME = 'bk_scope_id') THEN
    ALTER TABLE application ADD COLUMN bk_scope_id VARCHAR(32) NOT NULL DEFAULT '';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'application'
                    AND COLUMN_NAME = 'attrs') THEN
    ALTER TABLE application ADD COLUMN attrs text;
  END IF;
  
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'application'
                    AND COLUMN_NAME = 'is_deleted') THEN
    ALTER TABLE application ADD COLUMN is_deleted tinyint(1) UNSIGNED NOT NULL DEFAULT 0;
  END IF;

  IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'application'
                    AND INDEX_NAME = 'app_type') THEN
    ALTER TABLE application DROP INDEX `app_type`;
  END IF;
  
  -- Update `host` schema
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host'
                    AND COLUMN_NAME = 'cloud_ip') THEN
    ALTER TABLE host ADD COLUMN cloud_ip VARCHAR(65) NOT NULL DEFAULT '';
  END IF;

  -- Update data
  UPDATE application SET bk_scope_type = 'biz' WHERE app_type = 1 AND (bk_scope_type IS NULL OR bk_scope_type = '');
  UPDATE application SET bk_scope_type = 'biz_set' WHERE app_type in (2,3) AND (bk_scope_type IS NULL OR bk_scope_type = '');
  UPDATE application SET bk_scope_id = app_id WHERE bk_scope_id is NULL OR bk_scope_id = '';
  UPDATE host SET cloud_ip = concat(cloud_area_id,':',ip) WHERE cloud_ip='';
  -- 结束事务，提交表数据变更
  COMMIT;
  

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'host'
                    AND INDEX_NAME = 'idx_cloud_ip') THEN
      ALTER TABLE host ADD INDEX idx_cloud_ip(`cloud_ip`);
  END IF;
  
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'application'
                    AND INDEX_NAME = 'uk_scope_id_scope_type') THEN
    ALTER TABLE application ADD UNIQUE INDEX uk_scope_id_scope_type(`bk_scope_id`,`bk_scope_type`);
  END IF;
  
  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
