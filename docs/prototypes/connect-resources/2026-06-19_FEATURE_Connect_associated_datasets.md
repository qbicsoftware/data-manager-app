Connect datasets with research projects

Versions
2026-06-19	First draft: Outlining the background and idea as well as the first user stories for 		connecting open and restricted published datasets on an InventioRDM instance
Background
To get a scientific finding ready for publication, good scientific practice requests scientists to share associated data. While the German science council has made this part of their GWP (Gute Wissenschaftliche Praxis), also funding bodies and journals include the publication of associated data increasingly as hard requirement.
The University of Tübingen’s life science data management platform Data Manager shall assist scientists in managing high throughput data and connecting further datasets associated with the research project to have a single access point for their data and metadata.
A popular publication platform for all sorts of datasets and widely accepted by both, science community as well as funding bodies and journals, are public InvenioRDM installations such as Zenodo.  These platforms are used for smaller datasets, such as figures, results or additional protocols to create a persistent and sharable public reference via a digital object identifier (DOI). Due to its flexibility and ease of use, they are an important way to get various types of supportive data ready for publication.
The idea
As a new feature, Data Manager shall enable users to connect associated datasets managed on a public InvenioRDM instance from within the main project managed in the application.
The vision is to simplify data management for the various assets that are usually part of a scientific project and provide a central access point to retain the overview. At the same time, users shall not be burdened with yet another platform they need to transfer their data from to make them ready for publication.
If the integration turns out to be successful, additional features like the direct creation of datasets on these InvenioRDM instances from within Data Manager can be put into focus and evaluation.
Data Manager becomes the hub. InvenioRDM remains the publishing platform.
The integration is based on FAIR Signposting, providing machine-actionable, machine-readable relationships between the project in Data Manager and the associated datasets on InvenioRDM. This demonstrates true FAIR integration — the connections are not manual bookmarks, but discoverable by other tools and services.

> **⚠️ Note (2026-07-10):** The integration mechanism described above has been **revised**. FAIR Signposting cannot be used because of a severe bug in InvenioRDM's serialization of Signposting metadata; the fix is scheduled for a future major release that would delay the feature unacceptably. The current implementation uses **conventional API-based integration** against the InvenioRDM OpenAPI. See feature issue [#1466](https://github.com/qbicsoftware/data-manager-app/issues/1466) for the current design.
Initial scope
•	Target platforms: Zenodo and FDAT (University of Tübingen public InvenioRDM instance)

If the integration turns out to be successful, additional features like the direct creation of datasets on these InvenioRDM instances from within Data Manager can be put into focus and evaluation.

InvenioRDM OpenAPI Specification
See https://inveniosoftware.github.io/invenio-openapi/

User Stories
Personas:
-	QBiC project manager
-	Researcher
-	Data Steward

User Stories of a researcher
Story 01 – Connecting open, published datasets
As a researcher, I want to connect an open, published dataset on InvenioRDM with my project, so I can retain an overview of associated data for myself and collaborators on the project.
Acceptance Criteria:
-	Only users with write access can connect datasets in a project
-	Users can access a Search Public Dataset section for available InvenioRDM instances
-	Users can select the InvenioRDM instance they want to search in (e.g. Zenodo, FDAT)
-	If no search term is provided, a paginated result of the search is shown to the user
-	Users can enter a search term, and the system will present them with the matching search result
-	Clicking Connect successfully adds the selected datasets to the project in the system
-	If the connection fails, the system shall inform users about the failure
-	If the connection is successful, the system shall inform users about success

References:
None.

Story 02 – Viewing connected open, published datasets
As a researcher, I want to see connected datasets with the current project, so I get an overview of associated datasets.
Acceptance Criteria:
-	Users can access a Public Datasets section for connected datasets with the project
-	The system provides dataset information for every connected dataset
-	The system provides information about the user that connected the dataset
-	The system provides an Access Link, that enables all project members to access the dataset without an account on the target InvenioRDM instance
     References:
-	Dataset Information stakeholder document (tbd)

Story 03 – Remove a connected open, published dataset
As a researcher, I want to remove an existing dataset connection, so I can restore the integrity of associated data with the project.
Acceptance Criteria:
-	Users with write access can click a Remove button for connected datasets to remove the connection with the current project
-	If the removal was successful, users get a confirmation notification and the view refreshes
-	If the removal fails, users get an error notification with further instructions

Story 04 – Sync a connected open, published dataset
As a researcher, I want to sync a connected dataset with its hosted platform, so I have the latest information about the dataset associated with the project.
Acceptance Criteria:
-	Users with write access can click a Sync button for a connected dataset to trigger a synchronization with the host system of the dataset
-	Users with write access can click a Sync All button to trigger a synchronization for all datasets connected in the project
-	If a dataset has a new version, the system updates the local entry with the latest version and notifies users about the update
-	If the synchronization is successful, the system informs users about the number of updated datasets
-	If the synchronization fails, the system informs users about the failure and next steps.

Story 05 – Connecting an access restricted, published dataset
As a researcher, I want to connect an access restricted dataset with a project so I can collaborate and share with collaborators before opening publication.
Acceptance Criteria:
-	Users with write access can enter a Search Restricted Datasets section and search for published restricted datasets on the target InvenioRDM instance
-	Users can select the InvenioRDM  instance they want to search in (e.g. Zenodo, FDAT)
-	If no search term is provided, a paginated result of the search is shown to the user
-	Users can enter a search term, and the system will present them with the matching search result
-	Clicking Connect successfully adds the selected datasets to the project in the system
-	If the connection fails, the system shall inform users about the failure
-	If the connection is successful, the system shall inform users about success
-	If users have not configured an InvenioRDM instance in their account with an authorization token, notify users and navigate them to the user account section

Story 06 – Viewing connected access-restricted, published datasets
As a researcher, I want to see connected restricted datasets with the current project, so I get an overview of associated restricted datasets.
Acceptance Criteria:
-	Users can access a Private Datasets section for connected datasets with the project
-	The system provides dataset information for every connected dataset
-	The system provides information about the user that connected the dataset
-	The system provides an Access Link, that enables all project members to access the dataset without an account on the target InvenioRDM instance
     Story 07 – Remove a connected restricted, published dataset
     As a researcher, I want to remove an existing dataset connection, so I can restore the integrity of associated data with the project.
     Acceptance Criteria:
-	Users with write access can click a Remove button for connected restricted datasets to remove the connection with the current project
-	If the removal was successful, users get a confirmation notification and the view refreshes
-	If the removal fails, users get an error notification with further instructions

Story 08 – Sync a connected restricted, published dataset
As a researcher, I want to sync a connected dataset with its hosted platform, so I have the latest information about the dataset associated with the project.
Acceptance Criteria:
-	Users with write access can click a Sync button for a connected dataset to trigger a synchronization with the host system of the dataset
-	Users with write access can click a Sync All button to trigger a synchronization for all datasets connected in the project
-	If a dataset has a new version, the system updates the local entry with the latest version and notifies users about the update
-	If the synchronization is successful, the system informs users about the number of updated datasets
-	If the synchronization fails, the system informs users about the failure and next steps.


Story 14 – Add credentials for an InvenioRDM instance

As a researcher, I want to provide my access token for an available InvenioRDM instance, so that I can connect access-restricted resources in a project.

Acceptance Criteria:
- The system offers users a way in their account settings to configure InvenioRDM instances 
- The system displays users available InvenioRDM instances to configure
- Tokens are validated via the InvenioRDM instance
- Users are informed that if the token is invalid, in which case the token must not be added to the user's account.
- Users are informed that a valid token has been added and that the InvenioRDM instance is now connected
- The system highlights a description of what the benefits of providing tokens for an InvenioRDM instance are (the connection of access-restricted datasets to a project) 


Story 15 – Remove credentials for a configured InvenioRDM instance

As a researcher, I want to remove my access token for an available InvenioRDM instance, because the token is not valid anymore.

Acceptance Criteria:

- After users execute a remove action, the token gets deleted from the system and the users cannot add access-restricted records from this InvenioRDM instance anymore to their projects
- Users are informed that the token has been successfully removed 
- The system displays users that the InvenioRDM instance is not configured anymore for access-restricted access 




