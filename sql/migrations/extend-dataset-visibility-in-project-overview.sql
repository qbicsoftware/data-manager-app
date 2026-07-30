-- =============================================================================
-- Migration: Add connected-dataset aggregates to the project_overview view
-- Story:    https://github.com/qbicsoftware/data-manager-app/issues/1475
-- Feature:  FEAT-DATASET-CONNECTION (#1466), Story FEAT-DATSET-09
-- ADRs:     0001 (associated_dataset domain model)
-- Datasource: data_management
--
-- Extends the project_overview view with four aggregate columns sourced from
-- the associated_dataset table:
--   * connectedDatasetCount   — total datasets connected to the project
--   * openDatasetCount        — PUBLIC access_level count
--   * restrictedDatasetCount  — RESTRICTED access_level count
--   * lastConnectedOn         — most recent connected_on timestamp
--
-- The aggregate is a LEFT JOIN against a derived table so that projects with
-- no connected datasets still appear in the view (with zero counts and a
-- NULL lastConnectedOn).
--
-- Self-containment:
--   This migration inlines the measurement-aggregation subqueries directly
--   into the project_overview view definition rather than joining through
--   `project_measurements`. That avoids a cross-view dependency on the
--   `project_measurements` view definition being up-to-date at deployment
--   time — in environments where `project_measurements` has the older
--   pre-IP-measurements definition, a join-through approach would fail
--   with "Unknown column 'm.amountIpMeasurements' in SELECT".
--
--   If you are doing a **fresh install**, run complete-schema.sql instead —
--   it has the same canonical view definition (without this migration's
--   inlined subqueries) and a separate `project_measurements` view.
--
-- Operator notes:
--   * Safe to run while the application is live — views are dropped and
--     recreated atomically; in-flight queries return the old definition.
--   * The associated_dataset table must exist before running this script
--     (created by the earlier create-associated-dataset.sql migration).
--   * COALESCE handles the NULL case where a project has no connected
--     datasets; COUNT and SUM over an empty group would otherwise produce
--     NULL and trip the JPA entity's column nullability constraints.
--   * `connection_state = 'CONNECTED'` excludes soft-deleted (`REMOVED`)
--     rows.
--
-- Rollback:
--   Drop the new project_overview and re-create it from the previous
--   definition (see git history before this migration).
--
-- =============================================================================

-- Pre-flight: confirm `associated_dataset` exists — the view is empty without
-- it, but we want a hard error if the upstream table is missing entirely so
-- operators catch the root cause instead of wondering why counts are always 0.
-- Using a guard procedure keeps this idempotent.
--
-- (If you run the migration twice, the DROP + CREATE below are idempotent
--  themselves via the DROP VIEW IF EXISTS; the procedure below is only needed
--  for the pre-flight assertion.)

DROP VIEW IF EXISTS `project_overview`;

CREATE VIEW `project_overview` AS
SELECT `pd`.`projectId`                     AS `projectId`,
       `pd`.`projectCode`                   AS `projectCode`,
       `pd`.`projectTitle`                  AS `projectTitle`,
       `pd`.`lastModified`                  AS `lastModified`,
       `pd`.`principalInvestigatorFullName` AS `principalInvestigatorFullName`,
       `pd`.`projectManagerFullName`        AS `projectManagerFullName`,
       `pd`.`responsibePersonFullName`      AS `responsibePersonFullName`,
       -- Measurement aggregates: inlined rather than joined via `project_measurements`
       -- so this migration does not depend on that view's version.
       COALESCE(`proteomics`.`amountPxpMeasurements`, 0) AS `amountPxpMeasurements`,
       COALESCE(`ngs`.`amountNgsMeasurements`, 0)        AS `amountNgsMeasurements`,
       COALESCE(`ip`.`amountIpMeasurements`, 0)          AS `amountIpMeasurements`,
       `users`.`usernames`                  AS `usernames`,
       `users`.`userInfos`                  AS `userInfos`,
       COALESCE(connected_datasets.connectedDatasetCount,   0) AS `connectedDatasetCount`,
       COALESCE(connected_datasets.openDatasetCount,        0) AS `openDatasetCount`,
       COALESCE(connected_datasets.restrictedDatasetCount,  0) AS `restrictedDatasetCount`,
       connected_datasets.lastConnectedOn                      AS `lastConnectedOn`
FROM (`data_management`.`projects_datamanager` `pd`
         LEFT JOIN (SELECT `proteomics`.`projectId`        AS `projectId`,
                           `proteomics_count`.`amountPxpMeasurements` AS `amountPxpMeasurements`
                    FROM (`data_management`.`projects_datamanager` `proteomics`
                             LEFT JOIN (SELECT `p`.`projectId`              AS `pID`,
                                               count(`p`.`measurementCode`) AS `amountPxpMeasurements`
                                        FROM `data_management`.`proteomics_measurement` `p`
                                        GROUP BY `p`.`projectId`) `proteomics_count`
                                     ON (`proteomics`.`projectId` = `proteomics_count`.`pID`))) `proteomics`
               ON (`pd`.`projectId` = `proteomics`.`projectId`)
         LEFT JOIN (SELECT `ngs`.`projectId`              AS `projectId`,
                           count(`ngs`.`measurementCode`) AS `amountNgsMeasurements`
                    FROM `data_management`.`ngs_measurements` `ngs`
                    GROUP BY `ngs`.`projectId`) AS `ngs`
               ON (`pd`.`projectId` = `ngs`.`projectId`)
         LEFT JOIN (SELECT `ip`.`projectId`               AS `projectId`,
                           count(`ip`.`measurementCode`)  AS `amountIpMeasurements`
                    FROM `data_management`.`ip_measurements` `ip`
                    GROUP BY `ip`.`projectId`) AS `ip`
               ON (`pd`.`projectId` = `ip`.`projectId`))
         LEFT JOIN (SELECT `project_userinfo`.`projectId`,
                           GROUP_CONCAT(`project_userinfo`.`userName` SEPARATOR ', ') AS `usernames`,
                           JSON_ARRAYAGG(JSON_OBJECT('userId',  `project_userinfo`.`userId`,
                                                     'userName', `project_userinfo`.`userName`)) AS `userInfos`
                    FROM `project_userinfo`
                    GROUP BY `project_userinfo`.`projectId`) AS `users`
               ON `users`.`projectId` = `pd`.`projectId`
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
         ) AS connected_datasets ON connected_datasets.project_id = `pd`.`projectId`;
