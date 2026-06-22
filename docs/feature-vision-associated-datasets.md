# Connect associated datasets with DM projects

## Background

To get a scientific finding ready for publication, good scientific practice requests scientists to share associated data. While the German science council has made this part of their GWP (Gute Wissenschaftliche Praxis), also funding bodies and journals include the publication of associated data increasingly as hard requirement.

The University of Tübingen's life science data management platform DataManager shall assist scientists in managing high throughput data and connecting further datasets associated with the research project to have a single access point for their data and metadata.

A popular publication platform for all sorts of datasets and widely accepted by both, science community as well as funding bodies and journals, are public InvenioRDM installations such as Zenodo. These platforms are used for smaller datasets, such as figures, results or additional protocols to create a persistent and sharable public reference via a digital object identifier (DOI). Due to its flexibility and ease of use, they are an important way to get various types of supportive data ready for publication.

## The idea

As a new feature, DataManager shall enable users to connect associated datasets managed on a public InvenioRDM instance from within the main project managed in the application.

The vision is to simplify data management for the various assets that are usually part of a scientific project and provide a central access point to retain the overview. At the same time, users shall not be burdened with yet another platform they need to transfer their data from to make them ready for publication.

**Data Manager becomes the hub. InvenioRDM remains the publishing platform.**

The integration is based on **FAIR Signposting**, providing machine-actionable, machine-readable relationships between the project in DataManager and the associated datasets on InvenioRDM. This demonstrates true FAIR integration — the connections are not manual bookmarks, but discoverable by other tools and services.

### Initial scope

- **Target platforms:** Zenodo and FDAT (University of Tübingen public InvenioRDM instance)
- **Authentication:** OAuth2 flow with refresh tokens for a seamless user experience
- **Search:** "Search platform" (public datasets) and "My datasets" (authenticated/private)
- **Linkage type:** Live linkage — dataset metadata stays synchronized from InvenioRDM
- **Linkage scope:** Projects and experiments (experiment-level linkage is an optional hook, inheriting to the parent project)
- **Architecture:** One-to-many (one DM project → multiple datasets), ready for many-to-many in the future
- **Bidirectional linkage:** The DM project link is recorded inside the InvenioRDM dataset as a machine-actionable connection

If the integration turns out to be successful, additional features like the direct creation of datasets on these InvenioRDM instances from within DataManager can put into focus and evaluation.
