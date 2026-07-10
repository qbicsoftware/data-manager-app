# Connected Resource Information

## Version

| Date       | Description                                                                                                          |
|------------|----------------------------------------------------------------------------------------------------------------------|
| 2026-06-25 | Second version fixing minor spelling mistakes, adding linked experiments as a medium-prio property and enhancing the community description. |
| 2026-06-23 | First version — Introduces the properties for high and medium accessibility priority for dataset information          |

---

# Metadata properties

## High-Prio Properties

Properties of a resource that are crucial to access fast.

| Type             | Motivation                                                                                                                                                                                                 | Example                          |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| Title            | Contextual information about what the resource is about                                                                                                                                                    | RNAdeseq analysis of ...         |
| PID              | Unambiguously identifier of the full remote resource                                                                                                                                                       | 10.5281/zenodo.20809410          |
| Access Status    | To give a feeling of security that the resource has access restrictions as intended for the project. Also, a record can have public metadata, whereas the files themselves can be restricted (access-controlled). | Record: public / Files: restricted |
| Version          | Last known version of the connected dataset                                                                                                                                                                | v1                               |
| Access Link      | If available, shown to give collaborators read access to the resource                                                                                                                                      | URL                              |
| Publication Date | To get a timely reference to see when the dataset has been published                                                                                                                                       | July 1, 2026                     |

---

## Medium-Prio Properties

Properties of a resource that are important, but don't need to be as prominently accessible as High-Prio properties.

| Type              | Motivation                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | Example                                                          |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| Connected By      | To highlight which collaboration partner connected the dataset                                                                                                                                                                                                                                                                                                                                                                                                          | John Doe                                                         |
| Resource Provider | To see where the resource is hosted                                                                                                                                                                                                                                                                                                                                                                                                                                     | Zenodo, FDAT, ....                                               |
| Creator           | Person who created the dataset on the provider                                                                                                                                                                                                                                                                                                                                                                                                                           | Jane Doe                                                         |
| Resource Type     | What the resource category is                                                                                                                                                                                                                                                                                                                                                                                                                                            | Article, Dataset, Audio, Publication, Book, Workflow, Software, ... |
| Community         | An organisational entity with its own submission policies for submitted records enforced by community curators on the provider's site. These policies can be specified by the funding entity or by domain experts within the community. Only if the curators approve a submitted draft application is the record published within the community. Mandatory for FDAT records, since in that instance the record metadata can be set to private and would be unfindable without a community association. Optional for records published in zenodo, since the record metadata is always visible. | QBiC, CMFI, SFB209, iFIT, ...                                    |
| Linked Experiment | One or multiple experiments within the project, which are most closely associated with the provided resource. This could be because they provided the original data used in the generation of the resource (e.g. primary or secondary analysis results --> nf-core pipeline analysis) or a presentation/poster highlighting the experiment.                                                                                                                               | Explorative analysis to identify novel transcript isoforms for heat regulative proteins under temperature duress. |
