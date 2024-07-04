CREATE VIEW project_userinfo AS
SELECT o_identity.object_id_identity AS `projectId`,
       u.userName                    AS `userName`,
       u.id                          AS `userId`
FROM acl_entry
         LEFT JOIN (SELECT *
                    FROM acl_sid AS sid
                    WHERE sid.principal = 1) AS sid ON acl_entry.sid = sid.id
         LEFT JOIN users u ON sid.sid = u.id
         LEFT JOIN acl_object_identity o_identity ON acl_entry.acl_object_identity = o_identity.id
WHERE sid.sid IS NOT NULL;


CREATE VIEW project_overview AS
SELECT pd.projectId,
       projectCode,
       projectTitle,
       lastModified,
       principalInvestigatorFullName,
       projectManagerFullName,
       responsibePersonFullName,
       amountNgsMeasurements,
       amountPxpMeasurements,
       users.usernames,
       users.userInfos
FROM projects_datamanager pd
         LEFT JOIN project_measurements m ON pd.projectId = m.projectId
         LEFT JOIN (SELECT projectId,
                           GROUP_CONCAT(userName SEPARATOR ', ') AS `usernames`,
                           JSON_ARRAYAGG(
                                   JSON_OBJECT(
                                           'userId', userId,
                                           'userName', userName
                                   ))                            AS `userInfos`
                    FROM project_userinfo
                    GROUP BY projectId) as users ON users.projectId = pd.projectId;
