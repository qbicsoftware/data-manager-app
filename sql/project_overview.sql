CREATE VIEW project_overview 
	AS SELECT 
    pd.projectId, 
    projectCode,
    projectTitle,
    lastModified,
    principalInvestigatorFullName,
    projectManagerFullName, 
    responsibePersonFullName, 
    amountNgsMeasurements, 
    amountPxpMeasurements,
    users.usernames 
    FROM projects_datamanager pd 
    LEFT JOIN project_measurements m ON pd.projectId = m.projectId 
    LEFT JOIN 
	(SELECT projectId, GROUP_CONCAT(userName SEPARATOR ', ' ) as usernames FROM project_usernames GROUP BY projectId) 
		as users ON users.projectId = pd.projectId;