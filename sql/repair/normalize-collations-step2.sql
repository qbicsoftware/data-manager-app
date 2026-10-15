-- =============================================================================
-- INSTANCE REPAIR, STEP 2: convert the application tables to utf8mb4_unicode_ci
--
-- Companion to sql/repair/normalize-collations.sql, which holds the rationale, the
-- inspection queries, view recreation and verification. This file is the conversion.
--
-- WHY THE FOREIGN KEYS COME OFF FIRST
--
-- MariaDB refuses to change the character set of a column that participates in a
-- foreign key, even with FOREIGN_KEY_CHECKS=0:
--
--   [HY000][1832] Cannot change column 'experimentId': used in a foreign key
--                 constraint 'data_management/FK25vvdiupupuwmehr3o97dh7fg'
--
-- FOREIGN_KEY_CHECKS=0 disables *validation*, not the structural check. So the 31
-- constraints are dropped, every table is converted, and the constraints are added
-- back exactly as sql/complete-schema.sql declares them. No data is touched.
--
-- BEFORE RUNNING
--   * Back up:  mysqldump --single-transaction --routines --triggers --events \
--                   -u <user> -h <host> <database> > before-collation-repair.sql
--   * Run Step 1 of normalize-collations.sql first. If it lists tables that are not in
--     section 2b below (Hibernate-created: measurement, batch, user_project_view_state,
--     ...), add their ALTER ... CONVERT here too -- and add their foreign keys, if any,
--     to sections 2a and 2c.
--   * This rebuilds every table and holds a metadata lock. Maintenance window.
--
-- AFTER RUNNING
--   * Recreate every view. A view captures its columns' collations at creation time, so
--     project_userinfo, project_overview, v_ngs_measurement_sample_json,
--     v_pxp_measurement_sample_json and v_ip_measurement_sample_json must be rebuilt
--     from sql/complete-schema.sql, and the two scripts in sql/migrations/ re-applied.
--   * Verify with Step 5 of normalize-collations.sql.
--
-- NON-APPLICATION TABLES
--   * JobRunr and ShedLock own their schemas; nothing compares their text columns to
--     application columns. Deliberately excluded -- see Step 4 of the companion file.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 2a. Drop the 31 foreign key constraints.
-- -----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `acl_object_identity` DROP FOREIGN KEY `fk_acl_object_identity_class`;
ALTER TABLE `acl_object_identity` DROP FOREIGN KEY `fk_acl_object_identity_owner`;
ALTER TABLE `acl_object_identity` DROP FOREIGN KEY `fk_acl_object_identity_parent`;
ALTER TABLE `acl_entry` DROP FOREIGN KEY `fk_acl_entry_acl`;
ALTER TABLE `acl_entry` DROP FOREIGN KEY `fk_acl_entry_object`;
ALTER TABLE `experiments_datamanager` DROP FOREIGN KEY `FKgfrw5hlq3iy6ntf32wy0e8hr`;
ALTER TABLE `experimental_group` DROP FOREIGN KEY `FK25vvdiupupuwmehr3o97dh7fg`;
ALTER TABLE `experimental_group_variableLevels` DROP FOREIGN KEY `FKm81bdrf3y2gcsnmhuhjjj3y9j`;
ALTER TABLE `experimental_variables` DROP FOREIGN KEY `FK4flido9vdvf9uff0g0rngrvpw`;
ALTER TABLE `experimental_variables_levels` DROP FOREIGN KEY `FKfljdny9mdnh19bbm3ii3j4bic`;
ALTER TABLE `experiments_datamanager_analytes` DROP FOREIGN KEY `FKenl95t4n6dn8c90mcc9bdi7d3`;
ALTER TABLE `experiments_datamanager_species` DROP FOREIGN KEY `FK5jif824gfi7ho2dmk4lbp2cri`;
ALTER TABLE `experiments_datamanager_specimens` DROP FOREIGN KEY `FKiebw7ho69dfx9osttd8sin73l`;
ALTER TABLE `pinned_projects` DROP FOREIGN KEY `fk_pinned_projects_project`;
ALTER TABLE `projects_offers` DROP FOREIGN KEY `FKk2sfclbecq7f9htqutkw808xs`;
ALTER TABLE `quality_control` DROP FOREIGN KEY `FKmxonhr463uotnq2u62dt0gga1`;
ALTER TABLE `role_permission` DROP FOREIGN KEY `FKe3r4gqu0shl2iox9v427uvwdw`;
ALTER TABLE `role_permission` DROP FOREIGN KEY `FKqrvf0677p2qt9ymasidbqi8sf`;
ALTER TABLE `sample` DROP FOREIGN KEY `FK5glb6os4rlub6rb133xi1xj0y`;
ALTER TABLE `sample_batches_sampleid` DROP FOREIGN KEY `FKqkox27nsufoigg0vkdpqryga`;
ALTER TABLE `service_purchase` DROP FOREIGN KEY `FKhi0nqiffluv4ov42lkt6u03ac`;
ALTER TABLE `specific_measurement_metadata_ngs` DROP FOREIGN KEY `FK936j925a6pi06ojgafesihm5b`;
ALTER TABLE `specific_measurement_metadata_ngs` DROP FOREIGN KEY `FK_ngs_measurement_sample`;
ALTER TABLE `specific_measurement_metadata_pxp` DROP FOREIGN KEY `FKn0pfbmn6xtywvflgsf5q1hbrh`;
ALTER TABLE `specific_measurement_metadata_pxp` DROP FOREIGN KEY `FK_pxp_measurement_sample`;
ALTER TABLE `specific_measurement_metadata_ip` DROP FOREIGN KEY `FKip_measurement_specific`;
ALTER TABLE `specific_measurement_metadata_ip` DROP FOREIGN KEY `FK_ip_measurement_sample`;
ALTER TABLE `user_role` DROP FOREIGN KEY `FKgjtm8gpb7flxkryafvaanmykq`;
ALTER TABLE `experiments_datamanager_confounding_variables` DROP FOREIGN KEY `FK_experiment_id`;
ALTER TABLE `confounding_variable_levels` DROP FOREIGN KEY `FK_sample_id`;
ALTER TABLE `confounding_variable_levels` DROP FOREIGN KEY `FK_variable_id`;


-- -----------------------------------------------------------------------------
-- 2b. Convert the 48 application tables.
-- -----------------------------------------------------------------------------
ALTER TABLE `acl_class` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `acl_entry` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `acl_object_identity` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `acl_sid` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `announcements` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `associated_dataset` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `confounding_variable_levels` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experimental_group` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experimental_group_variableLevels` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experimental_variables` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experimental_variables_levels` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experiments_datamanager` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experiments_datamanager_analytes` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experiments_datamanager_confounding_variables` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experiments_datamanager_species` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `experiments_datamanager_specimens` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `group_membership` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ip_measurements` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ngs_measurements` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ontology_classes` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ontology_classes_SEQ` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `permissions` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `personal_access_tokens` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `pinned_projects` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `profile_picture` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `profile_picture_audit` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `projects_datamanager` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `projects_offers` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `proteomics_measurement` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `purchase_offer` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `quality_control` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `quality_control_upload` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `remote_measurement_data` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `role_permission` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `roles` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `sample` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `sample_batches` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `sample_batches_sampleid` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `sample_statistics_entry` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `service_purchase` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `specific_measurement_metadata_ip` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `specific_measurement_metadata_ngs` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `specific_measurement_metadata_pxp` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `sync_control` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `user_external_credential` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `user_group` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `user_role` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `users` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Re-enable validation before the constraints are added back, so ADD CONSTRAINT
-- checks the existing rows. An orphan row would have violated the constraint before
-- this repair too; if an ADD fails, that is a pre-existing data problem, not a
-- conversion problem.
SET FOREIGN_KEY_CHECKS = 1;


-- -----------------------------------------------------------------------------
-- 2c. Recreate the 31 foreign key constraints, exactly as declared.
-- -----------------------------------------------------------------------------
ALTER TABLE `acl_object_identity` ADD CONSTRAINT `fk_acl_object_identity_class` FOREIGN KEY (`object_id_class`) REFERENCES `acl_class` (`id`);
ALTER TABLE `acl_object_identity` ADD CONSTRAINT `fk_acl_object_identity_owner` FOREIGN KEY (`owner_sid`) REFERENCES `acl_sid` (`id`);
ALTER TABLE `acl_object_identity` ADD CONSTRAINT `fk_acl_object_identity_parent` FOREIGN KEY (`parent_object`) REFERENCES `acl_object_identity` (`id`);
ALTER TABLE `acl_entry` ADD CONSTRAINT `fk_acl_entry_acl` FOREIGN KEY (`sid`) REFERENCES `acl_sid` (`id`) ON DELETE CASCADE;
ALTER TABLE `acl_entry` ADD CONSTRAINT `fk_acl_entry_object` FOREIGN KEY (`acl_object_identity`) REFERENCES `acl_object_identity` (`id`);
ALTER TABLE `experiments_datamanager` ADD CONSTRAINT `FKgfrw5hlq3iy6ntf32wy0e8hr` FOREIGN KEY (`project`) REFERENCES `projects_datamanager` (`projectId`);
ALTER TABLE `experimental_group` ADD CONSTRAINT `FK25vvdiupupuwmehr3o97dh7fg` FOREIGN KEY (`experimentId`) REFERENCES `experiments_datamanager` (`id`);
ALTER TABLE `experimental_group_variableLevels` ADD CONSTRAINT `FKm81bdrf3y2gcsnmhuhjjj3y9j` FOREIGN KEY (`experimental_group_experimentalGroupId`) REFERENCES `experimental_group` (`experimentalGroupId`);
ALTER TABLE `experimental_variables` ADD CONSTRAINT `FK4flido9vdvf9uff0g0rngrvpw` FOREIGN KEY (`experimentId`) REFERENCES `experiments_datamanager` (`id`);
ALTER TABLE `experimental_variables_levels` ADD CONSTRAINT `FKfljdny9mdnh19bbm3ii3j4bic` FOREIGN KEY (`experimental_variables_variableId`) REFERENCES `experimental_variables` (`variableId`);
ALTER TABLE `experiments_datamanager_analytes` ADD CONSTRAINT `FKenl95t4n6dn8c90mcc9bdi7d3` FOREIGN KEY (`experiments_datamanager_id`) REFERENCES `experiments_datamanager` (`id`);
ALTER TABLE `experiments_datamanager_species` ADD CONSTRAINT `FK5jif824gfi7ho2dmk4lbp2cri` FOREIGN KEY (`experiments_datamanager_id`) REFERENCES `experiments_datamanager` (`id`);
ALTER TABLE `experiments_datamanager_specimens` ADD CONSTRAINT `FKiebw7ho69dfx9osttd8sin73l` FOREIGN KEY (`experiments_datamanager_id`) REFERENCES `experiments_datamanager` (`id`);
ALTER TABLE `pinned_projects` ADD CONSTRAINT `fk_pinned_projects_project` FOREIGN KEY (`projectId`) REFERENCES `projects_datamanager` (`projectId`) ON DELETE CASCADE;
ALTER TABLE `projects_offers` ADD CONSTRAINT `FKk2sfclbecq7f9htqutkw808xs` FOREIGN KEY (`projectIdentifier`) REFERENCES `projects_datamanager` (`projectId`);
ALTER TABLE `quality_control` ADD CONSTRAINT `FKmxonhr463uotnq2u62dt0gga1` FOREIGN KEY (`qualityControlReference`) REFERENCES `quality_control_upload` (`id`);
ALTER TABLE `role_permission` ADD CONSTRAINT `FKe3r4gqu0shl2iox9v427uvwdw` FOREIGN KEY (`userRoleId`) REFERENCES `roles` (`id`);
ALTER TABLE `role_permission` ADD CONSTRAINT `FKqrvf0677p2qt9ymasidbqi8sf` FOREIGN KEY (`permissionId`) REFERENCES `permissions` (`id`);
ALTER TABLE `sample` ADD CONSTRAINT `FK5glb6os4rlub6rb133xi1xj0y` FOREIGN KEY (`experimentalGroupId`) REFERENCES `experimental_group` (`experimentalGroupId`);
ALTER TABLE `sample_batches_sampleid` ADD CONSTRAINT `FKqkox27nsufoigg0vkdpqryga` FOREIGN KEY (`batch_id`) REFERENCES `sample_batches` (`id`);
ALTER TABLE `service_purchase` ADD CONSTRAINT `FKhi0nqiffluv4ov42lkt6u03ac` FOREIGN KEY (`offerReference`) REFERENCES `purchase_offer` (`id`);
ALTER TABLE `specific_measurement_metadata_ngs` ADD CONSTRAINT `FK936j925a6pi06ojgafesihm5b` FOREIGN KEY (`measurement_id`) REFERENCES `ngs_measurements` (`measurement_id`);
ALTER TABLE `specific_measurement_metadata_ngs` ADD CONSTRAINT `FK_ngs_measurement_sample` FOREIGN KEY (`sample_id`) REFERENCES `sample` (`sample_id`);
ALTER TABLE `specific_measurement_metadata_pxp` ADD CONSTRAINT `FKn0pfbmn6xtywvflgsf5q1hbrh` FOREIGN KEY (`measurement_id`) REFERENCES `proteomics_measurement` (`measurement_id`);
ALTER TABLE `specific_measurement_metadata_pxp` ADD CONSTRAINT `FK_pxp_measurement_sample` FOREIGN KEY (`sample_id`) REFERENCES `sample` (`sample_id`);
ALTER TABLE `specific_measurement_metadata_ip` ADD CONSTRAINT `FKip_measurement_specific` FOREIGN KEY (`measurement_id`) REFERENCES `ip_measurements` (`measurement_id`);
ALTER TABLE `specific_measurement_metadata_ip` ADD CONSTRAINT `FK_ip_measurement_sample` FOREIGN KEY (`sample_id`) REFERENCES `sample` (`sample_id`);
ALTER TABLE `user_role` ADD CONSTRAINT `FKgjtm8gpb7flxkryafvaanmykq` FOREIGN KEY (`roleId`) REFERENCES `roles` (`id`);
ALTER TABLE `experiments_datamanager_confounding_variables` ADD CONSTRAINT `FK_experiment_id` FOREIGN KEY (`experimentId`) REFERENCES `experiments_datamanager` (`id`);
ALTER TABLE `confounding_variable_levels` ADD CONSTRAINT `FK_sample_id` FOREIGN KEY (`sampleId`) REFERENCES `sample` (`sample_id`);
ALTER TABLE `confounding_variable_levels` ADD CONSTRAINT `FK_variable_id` FOREIGN KEY (`variableId`) REFERENCES `experiments_datamanager_confounding_variables` (`id`);

-- If an ADD CONSTRAINT fails, the constraint is missing until fixed -- do not leave the
-- instance in that state. The definitions above are copied verbatim from
-- sql/complete-schema.sql.


-- -----------------------------------------------------------------------------
-- 2d. Verify the constraint set is complete.
-- -----------------------------------------------------------------------------
-- Expected: 31 rows.
SELECT COUNT(*) AS foreign_keys
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY';

-- Expected: one row per constraint; UPDATE_RULE/DELETE_RULE should match the schema
-- (two constraints carry ON DELETE CASCADE: fk_acl_entry_acl and fk_pinned_projects_project).
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME, UPDATE_RULE, DELETE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, CONSTRAINT_NAME;
