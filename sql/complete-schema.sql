CREATE TABLE IF NOT EXISTS `acl_class`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `class`         varchar(100)        NOT NULL,
    `class_id_type` varchar(100)        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_acl_class` (`class`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `acl_sid`
(
    `id`        bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `principal` tinyint(1)          NOT NULL,
    `sid`       varchar(100)        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_acl_sid` (`sid`, `principal`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `acl_object_identity`
(
    `id`                 bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `object_id_class`    bigint(20) unsigned NOT NULL,
    `object_id_identity` varchar(36)         NOT NULL,
    `parent_object`      bigint(20) unsigned DEFAULT NULL,
    `owner_sid`          bigint(20) unsigned DEFAULT NULL,
    `entries_inheriting` tinyint(1)          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_acl_object_identity` (`object_id_class`, `object_id_identity`),
    KEY `fk_acl_object_identity_parent` (`parent_object`),
    KEY `fk_acl_object_identity_owner` (`owner_sid`),
    CONSTRAINT `fk_acl_object_identity_class` FOREIGN KEY (`object_id_class`) REFERENCES `acl_class` (`id`),
    CONSTRAINT `fk_acl_object_identity_owner` FOREIGN KEY (`owner_sid`) REFERENCES `acl_sid` (`id`),
    CONSTRAINT `fk_acl_object_identity_parent` FOREIGN KEY (`parent_object`) REFERENCES `acl_object_identity` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `acl_entry`
(
    `id`                  bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `acl_object_identity` bigint(20) unsigned NOT NULL,
    `ace_order`           int(11)             NOT NULL,
    `sid`                 bigint(20) unsigned NOT NULL,
    `mask`                int(10) unsigned    NOT NULL,
    `granting`            tinyint(1)          NOT NULL,
    `audit_success`       tinyint(1)          NOT NULL DEFAULT 1,
    `audit_failure`       tinyint(1)          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_acl_entry` (`acl_object_identity`, `ace_order`),
    KEY `fk_acl_entry_acl` (`sid`),
    CONSTRAINT `fk_acl_entry_acl` FOREIGN KEY (`sid`) REFERENCES `acl_sid` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_acl_entry_object` FOREIGN KEY (`acl_object_identity`) REFERENCES `acl_object_identity` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `projects_datamanager`
(
    `projectId`                         varchar(255) NOT NULL,
    `grantLabel`                        varchar(255)  DEFAULT NULL,
    `grantId`                           varchar(255)  DEFAULT NULL,
    `lastModified`                      datetime(6)  NOT NULL,
    `principalInvestigatorEmailAddress` varchar(255)  DEFAULT NULL,
    `principalInvestigatorFullName`     varchar(255)  DEFAULT NULL,
    `principalInvestigatorOidc`       varchar(255) DEFAULT NULL,
    `principalInvestigatorOidcIssuer` varchar(255) DEFAULT NULL,
    `projectCode`                       varchar(255)  DEFAULT NULL,
    `objective`                         varchar(2000) DEFAULT NULL,
    `projectTitle`                      varchar(255)  DEFAULT NULL,
    `projectManagerEmailAddress`        varchar(255)  DEFAULT NULL,
    `projectManagerFullName`            varchar(255)  DEFAULT NULL,
    `projectManagerOidc`              varchar(255) DEFAULT NULL,
    `projectManagerOidcIssuer`        varchar(255) DEFAULT NULL,
    `responsibePersonEmailAddress`      varchar(255)  DEFAULT NULL,
    `responsibePersonFullName`          varchar(255)  DEFAULT NULL,
    `responsiblePersonOidc`       varchar(255) DEFAULT NULL,
    `responsiblePersonOidcIssuer` varchar(255) DEFAULT NULL,
    `version` int NOT NULL,
    PRIMARY KEY (`projectId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiments_datamanager`
(
    `id`               varchar(255) NOT NULL,
    `analyteIconName`  varchar(31)  NOT NULL DEFAULT 'default',
    `experimentName`   varchar(255)          DEFAULT NULL,
    `speciesIconName`  varchar(31)  NOT NULL DEFAULT 'default',
    `specimenIconName` varchar(31)  NOT NULL DEFAULT 'default',
    `project`          varchar(255)          DEFAULT NULL,
    `version` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKgfrw5hlq3iy6ntf32wy0e8hr` (`project`),
    CONSTRAINT `FKgfrw5hlq3iy6ntf32wy0e8hr` FOREIGN KEY (`project`) REFERENCES `projects_datamanager` (`projectId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experimental_group`
(
    `experimentalGroupId` bigint(20) NOT NULL AUTO_INCREMENT,
    `groupNumber`         int          DEFAULT NULL,
    `name`                varchar(255) DEFAULT NULL,
    `sampleSize`          int(11)    NOT NULL,
    `experimentId`        varchar(255) DEFAULT NULL,
    PRIMARY KEY (`experimentalGroupId`),
    KEY `FK25vvdiupupuwmehr3o97dh7fg` (`experimentId`),
    CONSTRAINT `FK25vvdiupupuwmehr3o97dh7fg` FOREIGN KEY (`experimentId`) REFERENCES `experiments_datamanager` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experimental_group_variableLevels`
(
    `experimental_group_experimentalGroupId` bigint(20) NOT NULL,
    `unit`                                   varchar(255) DEFAULT NULL,
    `value`                                  varchar(255) DEFAULT NULL,
    `variableName`                           varchar(255) DEFAULT NULL,
    KEY `FKm81bdrf3y2gcsnmhuhjjj3y9j` (`experimental_group_experimentalGroupId`),
    CONSTRAINT `FKm81bdrf3y2gcsnmhuhjjj3y9j` FOREIGN KEY (`experimental_group_experimentalGroupId`) REFERENCES `experimental_group` (`experimentalGroupId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experimental_variables`
(
    `variableId`   bigint(20) NOT NULL AUTO_INCREMENT,
    `name`         varchar(255) DEFAULT NULL,
    `experimentId` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`variableId`),
    KEY `FK4flido9vdvf9uff0g0rngrvpw` (`experimentId`),
    CONSTRAINT `FK4flido9vdvf9uff0g0rngrvpw` FOREIGN KEY (`experimentId`) REFERENCES `experiments_datamanager` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experimental_variables_levels`
(
    `experimental_variables_variableId` bigint(20) NOT NULL,
    `unit`                              varchar(255) DEFAULT NULL,
    `value`                             varchar(255) DEFAULT NULL,
    KEY `FKfljdny9mdnh19bbm3ii3j4bic` (`experimental_variables_variableId`),
    CONSTRAINT `FKfljdny9mdnh19bbm3ii3j4bic` FOREIGN KEY (`experimental_variables_variableId`) REFERENCES `experimental_variables` (`variableId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiments_datamanager_analytes`
(
    `experiments_datamanager_id` varchar(255) NOT NULL,
    `analytes`                   longtext DEFAULT NULL CHECK ( json_valid(`analytes`)),
    KEY `FKenl95t4n6dn8c90mcc9bdi7d3` (`experiments_datamanager_id`),
    CONSTRAINT `FKenl95t4n6dn8c90mcc9bdi7d3` FOREIGN KEY (`experiments_datamanager_id`) REFERENCES `experiments_datamanager` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiments_datamanager_species`
(
    `experiments_datamanager_id` varchar(255) NOT NULL,
    `species`                    longtext DEFAULT NULL CHECK ( json_valid(`species`)),
    KEY `FK5jif824gfi7ho2dmk4lbp2cri` (`experiments_datamanager_id`),
    CONSTRAINT `FK5jif824gfi7ho2dmk4lbp2cri` FOREIGN KEY (`experiments_datamanager_id`) REFERENCES `experiments_datamanager` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiments_datamanager_specimens`
(
    `experiments_datamanager_id` varchar(255) NOT NULL,
    `specimens`                  longtext DEFAULT NULL CHECK ( json_valid(`specimens`)),
    KEY `FKiebw7ho69dfx9osttd8sin73l` (`experiments_datamanager_id`),
    CONSTRAINT `FKiebw7ho69dfx9osttd8sin73l` FOREIGN KEY (`experiments_datamanager_id`) REFERENCES `experiments_datamanager` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `jobrunr_backgroundjobservers`
(
    `id`                         char(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `workerPoolSize`             int(11)                                                   NOT NULL,
    `pollIntervalInSeconds`      int(11)                                                   NOT NULL,
    `firstHeartbeat`             datetime(6)                                               NOT NULL,
    `lastHeartbeat`              datetime(6)                                               NOT NULL,
    `running`                    int(11)                                                   NOT NULL,
    `systemTotalMemory`          bigint(20)                                                NOT NULL,
    `systemFreeMemory`           bigint(20)                                                NOT NULL,
    `systemCpuLoad`              decimal(3, 2)                                             NOT NULL,
    `processMaxMemory`           bigint(20)                                                NOT NULL,
    `processFreeMemory`          bigint(20)                                                NOT NULL,
    `processAllocatedMemory`     bigint(20)                                                NOT NULL,
    `processCpuLoad`             decimal(3, 2)                                             NOT NULL,
    `deleteSucceededJobsAfter`   varchar(32)  DEFAULT NULL,
    `permanentlyDeleteJobsAfter` varchar(32)  DEFAULT NULL,
    `name`                       varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `jobrunr_bgjobsrvrs_fsthb_idx` (`firstHeartbeat`),
    KEY `jobrunr_bgjobsrvrs_lsthb_idx` (`lastHeartbeat`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `jobrunr_jobs`
(
    `id`             char(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `version`        int(11)                                                   NOT NULL,
    `jobAsJson`      mediumtext   DEFAULT NULL,
    `jobSignature`   varchar(512)                                              NOT NULL,
    `state`          varchar(36)                                               NOT NULL,
    `createdAt`      datetime(6)                                               NOT NULL,
    `updatedAt`      datetime(6)                                               NOT NULL,
    `scheduledAt`    datetime(6)  DEFAULT NULL,
    `recurringJobId` varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `jobrunr_state_idx` (`state`),
    KEY `jobrunr_job_signature_idx` (`jobSignature`),
    KEY `jobrunr_job_created_at_idx` (`createdAt`),
    KEY `jobrunr_job_scheduled_at_idx` (`scheduledAt`),
    KEY `jobrunr_job_rci_idx` (`recurringJobId`),
    KEY `jobrunr_jobs_state_updated_idx` (`state`, `updatedAt`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `jobrunr_metadata`
(
    `id`        varchar(156) NOT NULL,
    `name`      varchar(92)  NOT NULL,
    `owner`     varchar(64)  NOT NULL,
    `value`     text         NOT NULL,
    `createdAt` datetime(6)  NOT NULL,
    `updatedAt` datetime(6)  NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `jobrunr_migrations`
(
    `id`          char(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `script`      varchar(64)                                               NOT NULL,
    `installedOn` varchar(29)                                               NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `jobrunr_recurring_jobs`
(
    `id`        char(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `version`   int(11)                                                    NOT NULL,
    `jobAsJson` text                                                       NOT NULL,
    `createdAt` bigint(20)                                                 NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `jobrunr_recurring_job_created_at_idx` (`createdAt`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ngs_measurements`
(
    `measurement_id`   varchar(255) NOT NULL,
    `facility`         varchar(255) DEFAULT NULL,
    `flowcell`         varchar(255) DEFAULT NULL,
    `instrument`       longtext     DEFAULT NULL CHECK ( json_valid(`instrument`)),
    `libraryKit`       varchar(255) DEFAULT NULL,
    `measurementCode`  varchar(255) DEFAULT NULL,
    `IRI`              varchar(255) DEFAULT NULL,
    `label`            varchar(255) DEFAULT NULL,
    `projectId`        varchar(255) DEFAULT NULL,
    `registrationTime` datetime(6)  DEFAULT NULL,
    `samplePool`       varchar(255) DEFAULT NULL,
    `readType`         varchar(255) DEFAULT NULL,
    `runProtocol`      varchar(255) DEFAULT NULL,
    PRIMARY KEY (`measurement_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ontology_classes`
(
    `id`              bigint(20) NOT NULL,
    `classIri`        varchar(255)  DEFAULT NULL,
    `label`           varchar(1024) DEFAULT NULL,
    `name`            varchar(255)  DEFAULT NULL,
    `description`     varchar(2000) DEFAULT NULL,
    `ontology`        varchar(255)  DEFAULT NULL,
    `ontologyIri`     varchar(255)  DEFAULT NULL,
    `ontologyVersion` varchar(255)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    FULLTEXT KEY `idx_fulltext_name` (`name`),
    FULLTEXT KEY `idx_fulltext_label` (`label`)
) ENGINE = MyISAM
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ontology_classes_SEQ`
(
    `next_val` bigint(20) DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `permissions`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `description` varchar(255) DEFAULT NULL,
    `name`        varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `personal_access_tokens`
(
    `id`                  int(11) NOT NULL AUTO_INCREMENT,
    `creationDate`        datetime(6)    DEFAULT NULL,
    `description`         varchar(255)   DEFAULT NULL,
    `duration`            decimal(21, 0) DEFAULT NULL,
    `tokenId`             varchar(255)   DEFAULT NULL,
    `tokenValueEncrypted` varchar(255)   DEFAULT NULL,
    `userId`              varchar(255)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_n2uptwhyydh9ff51ak8wkpi6g` (`tokenValueEncrypted`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `projects_offers`
(
    `projectIdentifier` varchar(255) NOT NULL,
    `offerIdentifier`   varchar(255) DEFAULT NULL,
    KEY `FKk2sfclbecq7f9htqutkw808xs` (`projectIdentifier`),
    CONSTRAINT `FKk2sfclbecq7f9htqutkw808xs` FOREIGN KEY (`projectIdentifier`) REFERENCES `projects_datamanager` (`projectId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `proteomics_measurement`
(
    `measurement_id`         varchar(255) NOT NULL,
    `digestionEnzyme`        varchar(255) DEFAULT NULL,
    `digestionMethod`        varchar(255) DEFAULT NULL,
    `enrichmentMethod`       varchar(255) DEFAULT NULL,
    `facility`               varchar(255) DEFAULT NULL,
    `injectionVolume`        int(11)      DEFAULT NULL,
    `instrument`             longtext     DEFAULT NULL CHECK ( json_valid(`instrument`)),
    `labelType`              varchar(255) DEFAULT NULL,
    `lcColumn`               varchar(255) DEFAULT NULL,
    `lcmsMethod`             varchar(255) DEFAULT NULL,
    `measurementCode`        varchar(255) DEFAULT NULL,
    `IRI`                    varchar(255) DEFAULT NULL,
    `label`                  varchar(255) DEFAULT NULL,
    `projectId`              varchar(255) DEFAULT NULL,
    `registration`           datetime(6)  DEFAULT NULL,
    `samplePool`             varchar(255) DEFAULT NULL,
    `technicalReplicateName` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`measurement_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `purchase_offer`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT,
    `file_content` longblob     DEFAULT NULL,
    `fileName`     varchar(255) DEFAULT NULL,
    `signed`       bit(1)     NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quality_control_upload`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `experiment_id` varchar(255) DEFAULT NULL,
    `file_content`  longblob     DEFAULT NULL,
    `fileName`      varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quality_control`
(
    `id`                      bigint(20) NOT NULL AUTO_INCREMENT,
    `projectId`               varchar(255) DEFAULT NULL,
    `providedOn`              datetime(6)  DEFAULT NULL,
    `qualityControlReference` bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_ltx4csqq0sndl0vm9tnly0tem` (`qualityControlReference`),
    CONSTRAINT `FKmxonhr463uotnq2u62dt0gga1` FOREIGN KEY (`qualityControlReference`) REFERENCES `quality_control_upload` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `roles`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `description` varchar(255) DEFAULT NULL,
    `name`        varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `role_permission`
(
    `userRoleId`   bigint(20) NOT NULL,
    `permissionId` bigint(20) NOT NULL,
    KEY `FKqrvf0677p2qt9ymasidbqi8sf` (`permissionId`),
    KEY `FKe3r4gqu0shl2iox9v427uvwdw` (`userRoleId`),
    CONSTRAINT `FKe3r4gqu0shl2iox9v427uvwdw` FOREIGN KEY (`userRoleId`) REFERENCES `roles` (`id`),
    CONSTRAINT `FKqrvf0677p2qt9ymasidbqi8sf` FOREIGN KEY (`permissionId`) REFERENCES `permissions` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sample`
(
    `sample_id`           varchar(255) NOT NULL,
    `analysis_method`     varchar(255) DEFAULT NULL,
    `assigned_batch_id`   varchar(255) DEFAULT NULL,
    `comment`             varchar(255) DEFAULT NULL,
    `experiment_id`       varchar(255) DEFAULT NULL,
    `experimentalGroupId` bigint(20)   DEFAULT NULL,
    `label`               varchar(255) DEFAULT NULL,
    `organism_id`         varchar(255) DEFAULT NULL,
    `code`                varchar(255) DEFAULT NULL,
    `analyte`             text         DEFAULT NULL,
    `species`             text         DEFAULT NULL,
    `specimen`            text         DEFAULT NULL,
    PRIMARY KEY (`sample_id`),
    KEY `FK5glb6os4rlub6rb133xi1xj0y` (`experimentalGroupId`),
    CONSTRAINT `FK5glb6os4rlub6rb133xi1xj0y` FOREIGN KEY (`experimentalGroupId`) REFERENCES `experimental_group` (`experimentalGroupId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sample_batches`
(
    `id`           varchar(255) NOT NULL,
    `createdOn`    datetime(6)  DEFAULT NULL,
    `batchLabel`   varchar(255) DEFAULT NULL,
    `lastModified` datetime(6)  DEFAULT NULL,
    `isPilot`      bit(1)       DEFAULT NULL,
    `version`      int(11)      NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sample_batches_sampleid`
(
    `batch_id`  varchar(255) NOT NULL,
    `sample_id` varchar(255) DEFAULT NULL,
    KEY `FKqkox27nsufoigg0vkdpqryga` (`batch_id`),
    CONSTRAINT `FKqkox27nsufoigg0vkdpqryga` FOREIGN KEY (`batch_id`) REFERENCES `sample_batches` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sample_statistics_entry`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `projectCode`   varchar(255) DEFAULT NULL,
    `projectId`     varchar(255) DEFAULT NULL,
    `sampleCounter` int(11)    NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `service_purchase`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT,
    `projectId`      varchar(255) DEFAULT NULL,
    `purchasedOn`    datetime(6)  DEFAULT NULL,
    `offerReference` bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_8cs2g0gnxrgknqy4dhri6ltil` (`offerReference`),
    CONSTRAINT `FKhi0nqiffluv4ov42lkt6u03ac` FOREIGN KEY (`offerReference`) REFERENCES `purchase_offer` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `specific_measurement_metadata_ngs`
(
    `measurement_id` varchar(255) NOT NULL,
    `comment`        varchar(255) DEFAULT NULL,
    `indexI5`        varchar(255) DEFAULT NULL,
    `indexI7`        varchar(255) DEFAULT NULL,
    `sample_id`      varchar(255) DEFAULT NULL,
    KEY `FK936j925a6pi06ojgafesihm5b` (`measurement_id`),
    CONSTRAINT `FK936j925a6pi06ojgafesihm5b` FOREIGN KEY (`measurement_id`) REFERENCES `ngs_measurements` (`measurement_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `specific_measurement_metadata_pxp`
(
    `measurement_id` varchar(255) NOT NULL,
    `comment`        varchar(255) DEFAULT NULL,
    `fractionName`   varchar(255) DEFAULT NULL,
    `label`          varchar(255) DEFAULT NULL,
    `sample_id`      varchar(255) DEFAULT NULL,
    KEY `FKn0pfbmn6xtywvflgsf5q1hbrh` (`measurement_id`),
    CONSTRAINT `FKn0pfbmn6xtywvflgsf5q1hbrh` FOREIGN KEY (`measurement_id`) REFERENCES `proteomics_measurement` (`measurement_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_role`
(
    `id`     bigint(20) NOT NULL AUTO_INCREMENT,
    `userId` varchar(255) DEFAULT NULL,
    `roleId` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKgjtm8gpb7flxkryafvaanmykq` (`roleId`),
    CONSTRAINT `FKgjtm8gpb7flxkryafvaanmykq` FOREIGN KEY (`roleId`) REFERENCES `roles` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `users`
(
    `id`                varchar(255) NOT NULL,
    `active`            bit(1)       NOT NULL,
    `email`             varchar(255) DEFAULT NULL,
    `encryptedPassword` varchar(255) DEFAULT NULL,
    `fullName`          varchar(255) DEFAULT NULL,
    `registrationDate`  datetime(6)  DEFAULT NULL,
    `userName`          varchar(255) DEFAULT NULL,
    `oidcId`            varchar(255) DEFAULT NULL,
    `oidcIssuer`        varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiments_datamanager_confounding_variables`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `experimentId` VARCHAR(255) NOT NULL,
    `name`         VARCHAR(255) NOT NULL,
    KEY `FK_experiment_id` (`experimentId`),
    CONSTRAINT `FK_experiment_id` FOREIGN KEY (`experimentId`) REFERENCES `experiments_datamanager` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `confounding_variable_levels`
(
    `id`         bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `sampleId`   VARCHAR(255) NOT NULL,
    `variableId` bigint(20)   NOT NULL,
    `value`      VARCHAR(255) NOT NULL,
    KEY `FK_sample_id` (`sampleId`),
    CONSTRAINT `FK_sample_id` FOREIGN KEY (`sampleId`) REFERENCES `sample` (`sample_id`),
    KEY `FK_variable_id` (`variableId`),
    CONSTRAINT `FK_variable_id` FOREIGN KEY (`variableId`) REFERENCES `experiments_datamanager_confounding_variables` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `announcements`
(
    `id`         bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `start_time` datetime(6)  NOT NULL,
    `end_time`   datetime(6)  NOT NULL,
    `message`    VARCHAR(255) NOT NULL
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE utf8mb4_unicode_ci;



DROP VIEW IF EXISTS data_management.project_measurements;

CREATE view data_management.project_measurements as
SELECT `projects`.`projectId`                            AS `projectId`,

       COALESCE(`proteomics`.`amountPxpMeasurements`, 0) AS `amountPxpMeasurements`, -- do not allow null values
       COALESCE(`ngs`.`amountNgsMeasurements`, 0)        AS `amountNgsMeasurements`  -- do not allow null values
FROM (projects_datamanager projects LEFT JOIN (SELECT ngs.`projectId`              AS `projectId`,
                                                      count(ngs.`measurementCode`) AS `amountNgsMeasurements`
                                               FROM ngs_measurements ngs
                                               GROUP BY ngs.`projectId`) AS `ngs`
      ON (`projects`.`projectId` = `ngs`.`projectId`))
         LEFT JOIN (SELECT `projects`.`projectId`        AS `projectId`,
                           `pxp`.`amountPxpMeasurements` AS `amountPxpMeasurements`
                    from (`data_management`.`projects_datamanager` `projects` left join (select `p`.`projectId`              AS `pID`,
                                                                                                count(`p`.`measurementCode`) AS `amountPxpMeasurements`
                                                                                         from `data_management`.`proteomics_measurement` `p`
                                                                                         group by `p`.`projectId`) `pxp`
                          on (`projects`.`projectId` = `pxp`.`pID`))) `proteomics`
                   on (`projects`.`projectId` = `proteomics`.`projectId`);

DROP VIEW IF EXISTS data_management.project_userinfo;

CREATE VIEW data_management.project_userinfo as
SELECT `o_identity`.`object_id_identity` AS `projectId`,
       `u`.`userName`                    AS `userName`,
       `u`.`id`                          AS `userId`
FROM (((`data_management`.`acl_entry` LEFT JOIN (SELECT `sid`.`id`        AS `id`,
                                                        `sid`.`principal` AS `principal`,
                                                        `sid`.`sid`       AS `sid`
                                                 FROM `data_management`.`acl_sid` `sid`
                                                 WHERE `sid`.`principal` = 1) `sid`
        on (`data_management`.`acl_entry`.`sid` = `sid`.`id`)) LEFT JOIN `data_management`.`users` `u`
       on (`sid`.`sid` = `u`.`id`)) LEFT JOIN `data_management`.`acl_object_identity` `o_identity`
      on (`data_management`.`acl_entry`.`acl_object_identity` = `o_identity`.`id`))
WHERE `sid`.`sid` IS NOT NULL;

DROP VIEW IF EXISTS data_management.project_overview;

CREATE VIEW project_overview AS
SELECT `pd`.`projectId`                     AS `projectId`,
       `pd`.`projectCode`                   AS `projectCode`,
       `pd`.`projectTitle`                  AS `projectTitle`,
       `pd`.`lastModified`                  AS `lastModified`,
       `pd`.`principalInvestigatorFullName` AS `principalInvestigatorFullName`,
       `pd`.`projectManagerFullName`        AS `projectManagerFullName`,
       `pd`.`responsibePersonFullName`      AS `responsibePersonFullName`,
       `m`.`amountNgsMeasurements`          AS `amountNgsMeasurements`,
       `m`.`amountPxpMeasurements`          AS `amountPxpMeasurements`,
       `users`.`usernames`                  AS `usernames`,
       `users`.`userInfos`                  AS `userInfos`
FROM projects_datamanager pd
         LEFT JOIN project_measurements m ON pd.projectId = m.projectId
         LEFT JOIN (SELECT project_userinfo.projectId,
                           GROUP_CONCAT(project_userinfo.userName SEPARATOR ', ') AS `usernames`,
                           JSON_ARRAYAGG(JSON_OBJECT('userId', project_userinfo.userId, 'userName',
                                                     project_userinfo.userName)) AS `userInfos`
                    FROM project_userinfo
                    GROUP BY projectId) AS users ON users.projectId = pd.projectId;
