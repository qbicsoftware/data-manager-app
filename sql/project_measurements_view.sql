CREATE VIEW project_measurements AS
SELECT projects.projectId, amountPxpMeasurements, ngs.amountNgsMeasurements
FROM projects_datamanager projects
         LEFT JOIN (SELECT projectId, COUNT(n.measurementCode) as amountNgsMeasurements
                    FROM ngs_measurements n
                    group by projectId) as ngs ON projects.projectId = ngs.projectId
         LEFT JOIN (SELECT projects.projectId, amountPxpMeasurements
                    FROM projects_datamanager projects
                             LEFT JOIN (SELECT projectId                as pID,
                                               COUNT(p.measurementCode) as amountPxpMeasurements
                                        FROM proteomics_measurement p
                                        group by pID) as pxp
                                       ON projects.projectId = pxp.pID) as proteomics
                   ON projects.projectId = proteomics.projectId;
