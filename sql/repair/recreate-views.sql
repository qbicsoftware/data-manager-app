-- =============================================================================
-- INSTANCE REPAIR, STEP 3: recreate the views from sql/complete-schema.sql
--
-- A view captures the collation of its columns when it is created. Views created before the
-- table conversion keep the old collations and keep raising "Illegal mix of collations",
-- even though the tables underneath are fixed. They must be rebuilt.
--
-- Definitions are copied from sql/complete-schema.sql, with two changes so this script is
-- safe to re-run:
--   * the `data_management.` qualifier is dropped, so views are created in whichever
--     database the script is run against;
--   * CREATE VIEW becomes CREATE OR REPLACE VIEW.
--
-- Dependency order -- project_overview reads project_measurements and project_userinfo, so
-- it is dropped first and created last:
--   drop    project_overview, v_*_json, project_measurements, project_userinfo
--   create  project_measurements, project_userinfo, project_overview, v_*_json
--
-- The two read-model scripts under sql/migrations/ must then be re-applied; they belong to
-- that release's migrations and are not duplicated here:
--   mysql ... < sql/migrations/create-project-page-read-models.sql
--   mysql ... < sql/migrations/create-experiment-recent-activity-views.sql
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 3a. Drop, in dependency order.
-- -----------------------------------------------------------------------------
DROP VIEW IF EXISTS `project_overview`;
DROP VIEW IF EXISTS `v_ngs_measurement_sample_json`;
DROP VIEW IF EXISTS `v_pxp_measurement_sample_json`;
DROP VIEW IF EXISTS `v_ip_measurement_sample_json`;
DROP VIEW IF EXISTS `project_measurements`;
DROP VIEW IF EXISTS `project_userinfo`;


-- -----------------------------------------------------------------------------
-- 3b. Recreate, in dependency order.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW project_measurements as
SELECT `projects`.`projectId`                            AS `projectId`,

       COALESCE(`proteomics`.`amountPxpMeasurements`, 0) AS `amountPxpMeasurements`, -- do not allow null values
       COALESCE(`ngs`.`amountNgsMeasurements`, 0)        AS `amountNgsMeasurements`,  -- do not allow null values
       COALESCE(`ip`.`amountIpMeasurements`, 0)          AS `amountIpMeasurements`    -- do not allow null values
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
                   on (`projects`.`projectId` = `proteomics`.`projectId`)
         LEFT JOIN (SELECT `ip`.`projectId`              AS `projectId`,
                           count(`ip`.`measurementCode`) AS `amountIpMeasurements`
                    FROM `data_management`.`ip_measurements` `ip`
                    GROUP BY `ip`.`projectId`) AS `ip`
                   ON (`projects`.`projectId` = `ip`.`projectId`);

CREATE OR REPLACE VIEW project_userinfo as
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

CREATE OR REPLACE VIEW project_overview AS
SELECT `pd`.`projectId`                     AS `projectId`,
       `pd`.`projectCode`                   AS `projectCode`,
       `pd`.`projectTitle`                  AS `projectTitle`,
       `pd`.`lastModified`                  AS `lastModified`,
       `pd`.`principalInvestigatorFullName` AS `principalInvestigatorFullName`,
       `pd`.`projectManagerFullName`        AS `projectManagerFullName`,
       `pd`.`responsibePersonFullName`      AS `responsibePersonFullName`,
       `m`.`amountNgsMeasurements`          AS `amountNgsMeasurements`,
       `m`.`amountPxpMeasurements`          AS `amountPxpMeasurements`,
       `m`.`amountIpMeasurements`           AS `amountIpMeasurements`,
       `users`.`usernames`                  AS `usernames`,
       `users`.`userInfos`                  AS `userInfos`,
       COALESCE(connected_datasets.connectedDatasetCount,   0) AS `connectedDatasetCount`,
       COALESCE(connected_datasets.openDatasetCount,        0) AS `openDatasetCount`,
       COALESCE(connected_datasets.restrictedDatasetCount,  0) AS `restrictedDatasetCount`,
       connected_datasets.lastConnectedOn                      AS `lastConnectedOn`
FROM projects_datamanager pd
         LEFT JOIN project_measurements m ON pd.projectId = m.projectId
         LEFT JOIN (SELECT project_userinfo.projectId,
                           GROUP_CONCAT(project_userinfo.userName SEPARATOR ', ') AS `usernames`,
                           JSON_ARRAYAGG(JSON_OBJECT('userId', project_userinfo.userId, 'userName',
                                                     project_userinfo.userName))  AS `userInfos`
                    FROM project_userinfo
                    GROUP BY projectId) AS users ON users.projectId = pd.projectId
         LEFT JOIN (
             SELECT
                 `project_id`,
                 COUNT(*)                                                            AS connectedDatasetCount,
                 SUM(CASE WHEN `access_level` = 'PUBLIC'     THEN 1 ELSE 0 END)      AS openDatasetCount,
                 SUM(CASE WHEN `access_level` = 'RESTRICTED' THEN 1 ELSE 0 END)      AS restrictedDatasetCount,
                 MAX(`connected_on`)                                                 AS lastConnectedOn
             FROM `associated_dataset`
             WHERE `connection_state` = 'CONNECTED'
             GROUP BY `project_id`
         ) AS connected_datasets ON connected_datasets.project_id = pd.projectId;

CREATE OR REPLACE VIEW v_ngs_measurement_sample_json AS
SELECT m.measurement_id,
       m.facility,
       m.flowcell,
       m.instrument,
       m.libraryKit,
       m.measurementCode,
       m.measurementName,
       m.IRI,
       m.label                                                                           AS measurement_label,
       m.projectId,
       m.registrationTime,
       m.samplePool,
       m.readType,
       m.runProtocol,

    /* Per-measurement JSON array of samples (keeps sample fields paired) */
       IFNULL((SELECT JSON_ARRAYAGG(
                              JSON_OBJECT(
                                      'sample_id', s.sample_id,
                                      'code', s.code,
                                      'label', s.label,
                                      'indexI7', smm2.indexI7,
                                      'indexI5', smm2.indexI5,
                                      'comment', smm2.comment
                              )
                              ORDER BY s.code
                      )
               FROM specific_measurement_metadata_ngs smm2
                        LEFT JOIN sample s ON s.sample_id = smm2.sample_id
               WHERE smm2.measurement_id = m.measurement_id),
              JSON_ARRAY())                                                              AS samples_json,

    /* Per-measurement distinct experiment IDs (top-level, not in JSON) */
       (SELECT GROUP_CONCAT(DISTINCT s2.experiment_id ORDER BY s2.experiment_id SEPARATOR ',')
        FROM specific_measurement_metadata_ngs smm2
                 JOIN sample s2 ON s2.sample_id = smm2.sample_id
        WHERE smm2.measurement_id = m.measurement_id)                                    AS experiment_ids,

    /* Single experiment_id if unique; NULL if mixed or none */
       (SELECT CASE
                   WHEN COUNT(DISTINCT s2.experiment_id) = 1
                       THEN MIN(s2.experiment_id)
                   ELSE NULL END
        FROM specific_measurement_metadata_ngs smm2
                 JOIN sample s2 ON s2.sample_id = smm2.sample_id
        WHERE smm2.measurement_id = m.measurement_id)                                    AS experiment_id,

    /* Handy count */
       JSON_LENGTH(IFNULL((SELECT JSON_ARRAYAGG(JSON_OBJECT('sample_id', s.sample_id))
                           FROM specific_measurement_metadata_ngs smm2
                                    LEFT JOIN sample s ON s.sample_id = smm2.sample_id
                           WHERE smm2.measurement_id = m.measurement_id),
                          JSON_ARRAY()))                                                 AS sample_count,

    /* remote_measurement_data via measurementCode */
       rmd.file_count,
       rmd.file_types,
       rmd.registration_at,
       rmd.total_filesize_bytes,
       rmd.updated_at                                                                    AS rmd_updated_at,
       rmd.deleted                                                                       AS rmd_deleted,
       rmd.last_sync_at
FROM ngs_measurements m
         INNER JOIN remote_measurement_data rmd
                    ON rmd.measurement_id = m.measurementCode;

CREATE OR REPLACE VIEW v_pxp_measurement_sample_json AS
SELECT p.measurement_id,
       p.digestionEnzyme,
       p.digestionMethod,
       p.enrichmentMethod,
       p.facility,
       p.injectionVolume,
       p.instrument,
       p.labelType,
       p.lcColumn,
       p.lcmsMethod,
       p.measurementCode,
       p.measurementName,
       p.IRI,
       p.label                                                                           AS measurement_label,
       p.projectId,
       p.registration,
       p.samplePool,
       p.technicalReplicateName,

    /* Per-measurement JSON array of samples (keeps sample fields paired) */
       IFNULL((SELECT JSON_ARRAYAGG(
                              JSON_OBJECT(
                                      'sample_id', s.sample_id,
                                      'code', s.code,
                                      'label', s.label,
                                      'fractionName', pxp2.fractionName,
                                      'metaLabel', pxp2.label,
                                      'comment', pxp2.comment
                              )
                              ORDER BY s.code
                      )
               FROM specific_measurement_metadata_pxp pxp2
                        LEFT JOIN sample s ON s.sample_id = pxp2.sample_id
               WHERE pxp2.measurement_id = p.measurement_id),
              JSON_ARRAY())                                                              AS samples_json,

    /* Per-measurement distinct experiment IDs (top-level, not in JSON) */
       (SELECT GROUP_CONCAT(DISTINCT s2.experiment_id ORDER BY s2.experiment_id SEPARATOR ',')
        FROM specific_measurement_metadata_pxp pxp2
                 JOIN sample s2 ON s2.sample_id = pxp2.sample_id
        WHERE pxp2.measurement_id = p.measurement_id)                                    AS experiment_ids,

    /* Single experiment_id if unique; NULL if mixed or none */
       (SELECT CASE
                   WHEN COUNT(DISTINCT s2.experiment_id) = 1
                       THEN MIN(s2.experiment_id)
                   ELSE NULL END
        FROM specific_measurement_metadata_pxp pxp2
                 JOIN sample s2 ON s2.sample_id = pxp2.sample_id
        WHERE pxp2.measurement_id = p.measurement_id)                                    AS experiment_id,

    /* Handy count */
       JSON_LENGTH(IFNULL((SELECT JSON_ARRAYAGG(JSON_OBJECT('sample_id', s.sample_id))
                           FROM specific_measurement_metadata_pxp pxp2
                                    LEFT JOIN sample s ON s.sample_id = pxp2.sample_id
                           WHERE pxp2.measurement_id = p.measurement_id),
                          JSON_ARRAY()))                                                 AS sample_count,

    /* remote_measurement_data via measurementCode */
       rmd.file_count,
       rmd.file_types,
       rmd.registration_at,
       rmd.total_filesize_bytes,
       rmd.updated_at                                                                    AS rmd_updated_at,
       rmd.deleted                                                                       AS rmd_deleted,
       rmd.last_sync_at
FROM proteomics_measurement p
         INNER JOIN remote_measurement_data rmd
                    ON rmd.measurement_id = p.measurementCode;

CREATE OR REPLACE VIEW v_ip_measurement_sample_json AS
SELECT ip.measurement_id,
       ip.facility,
       ip.mhcAntibody,
       ip.mhcTypingMethod,
       ip.enrichmentMethod,
       ip.lcmsMethod,
       ip.lcColumn,
       ip.dataAcquisition,
       ip.massRange,
       ip.retentionTimeRange,
       ip.chargeRange,
       ip.ionMobilityRange,
       ip.sampleMass,
       ip.sampleVolume,
       ip.cycleFractionName,
       ip.prepDate,
       ip.msRunDate,
       ip.comment,
       ip.instrument,
       ip.instrumentName,
       ip.samplePool,
       ip.measurementCode,
       ip.measurementName,
       ip.IRI,
       ip.label                                                                        AS measurement_label,
       ip.projectId,
       ip.registrationTime,

       /* Per-measurement JSON array of samples (keeps sample fields paired) */
       IFNULL((SELECT JSON_ARRAYAGG(JSON_OBJECT(
               'sample_id', s.sample_id,
               'code', s.code,
               'label', s.label
       )
       )
       FROM specific_measurement_metadata_ip smm
       LEFT JOIN sample s ON s.sample_id = smm.sample_id
       WHERE smm.measurement_id = ip.measurement_id
       ORDER BY s.code),
              JSON_ARRAY())                                                          AS samples_json,

       /* Per-measurement distinct experiment IDs (top-level, not in JSON) */
       (SELECT GROUP_CONCAT(DISTINCT s2.experiment_id ORDER BY s2.experiment_id SEPARATOR ',')
        FROM specific_measurement_metadata_ip smm2
        JOIN sample s2 ON s2.sample_id = smm2.sample_id
        WHERE smm2.measurement_id = ip.measurement_id)                              AS experiment_ids,

       /* Single experiment_id if unique; NULL if mixed or none */
       (SELECT CASE
                   WHEN COUNT(DISTINCT s2.experiment_id) = 1
                       THEN MIN(s2.experiment_id)
                   ELSE NULL END
        FROM specific_measurement_metadata_ip smm2
        JOIN sample s2 ON s2.sample_id = smm2.sample_id
        WHERE smm2.measurement_id = ip.measurement_id)                              AS experiment_id,

       /* Handy count */
       JSON_LENGTH(IFNULL((SELECT JSON_ARRAYAGG(JSON_OBJECT('sample_id', s.sample_id))
                           FROM specific_measurement_metadata_ip smm3
                           LEFT JOIN sample s ON s.sample_id = smm3.sample_id
                           WHERE smm3.measurement_id = ip.measurement_id),
                          JSON_ARRAY()))                                                     AS sample_count,

       /* remote_measurement_data via measurementCode */
       rmd.file_count,
       rmd.file_types,
       rmd.registration_at,
       rmd.total_filesize_bytes,
       rmd.updated_at                                                                AS rmd_updated_at,
       rmd.deleted                                                                   AS rmd_deleted,
       rmd.last_sync_at
FROM ip_measurements ip
         INNER JOIN remote_measurement_data rmd
                    ON rmd.measurement_id = ip.measurementCode;
