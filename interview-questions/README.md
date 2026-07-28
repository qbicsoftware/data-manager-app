# Data Manager — Interview Assignments

Three short code-reading exercises focused on API contracts, validation and domain models. No need
to navigate the full codebase — every file you need is linked below.

## 1. API Contract Reading

> The AsyncProjectService interface defines how the UI communicates with the backend. It uses Java
> records for requests/responses and reactive types (Mono) for async operations. Your job is to read
> the contract and understand what a client must provide.

#### Relevant Files

* [service_api.md](service_api.md)
* [AsyncProjectService.java](../project-management/src/main/java/life/qbic/projectmanagement/application/api/AsyncProjectService.java)

**Hint:** In AsyncProjectService.java, focus on the ProjectCreationRequest and
ProjectCreationResponse records near the top (first ~80 lines). You do not need to read the entire
file.

#### Questions

1. Look at the `ProjectCreationRequest` record. Which fields are **required** (must not be `null`)
   according to the compact canonical constructor? Which field is explicitly allowed to be `null`?
2. The record provides `optionalFundingInformation()` as an alternative to the `funding()` getter.
   Why is this safer for the caller?
3. Every request and response carries a `requestId` field. In your own words, why is this useful
   when the API is asynchronous?
4. `AsyncProjectService` returns `Mono<ProjectCreationResponse>` instead of
   `ProjectCreationResponse` directly. In one or two sentences, explain what `Mono` means in this
   context. (You do not need to explain reactive streams in depth — just the client-visible
   consequence.)

## 2. Validation & Authorization

> Before measurement metadata is saved, it passes through a validation service. This service also
> guards access with Spring Security annotations. You will read the validation result model and the
> service facade to understand the flow.

#### Relevant Files

* [ValidationResult.java](../project-management/src/main/java/life/qbic/projectmanagement/application/ValidationResult.java)
* [MeasurementValidationService.java](../project-management/src/main/java/life/qbic/projectmanagement/application/measurement/validation/MeasurementValidationService.java)

**Hint:** Start with `ValidationResult.java` — it is short and self-contained. Then read
`MeasurementValidationService.java` and notice how it uses `ValidationResult`.

#### Questions

1. In `ValidationResult`, what is the difference between a **failure** and a **warning**? If a
   validation result contains warnings but no failures, does `allPassed()` return `true` or `false`?
2. How do you create a `ValidationResult` that represents success? How do you create one that
   contains a list of failure messages?
3. Look at `MeasurementValidationService`. It has methods like `validateNGS`, `validatePxp`, and
   `validateIP`. List two things these three methods have in common (e.g., return type, security
   annotation, parameter pattern).
4. The annotation `@PreAuthorize("hasPermission(#projectId, `
   life.qbic.projectmanagement.domain.model.project.Project`, `WRITE`)")` appears on every public
   method. In plain language, what does this mean for a user who wants to validate measurement data?
5. `MeasurementValidationService` delegates to validator components (e.g.,
   `measurementNgsValidator`) but does not contain the actual validation rules itself. What is the
   name of this design pattern, and why is it useful?

# 3. Domain Model & Bug Hunt

> `NGSMeasurement` is a JPA entity that represents a Next-Generation Sequencing measurement. It
> enforces business rules (e.g., pooled measurements must have indices) and emits domain events. The
> validator mirrors some of these rules. Your job is to compare the domain object with its validator
> and spot an actual bug in the codebase.

#### Relevant Files

* [NGSMeasurement.java](../project-management/src/main/java/life/qbic/projectmanagement/domain/model/measurement/NGSMeasurement.java)
* [MeasurementNGSValidator.java](../project-management/src/main/java/life/qbic/projectmanagement/application/measurement/validation/MeasurementNGSValidator.java)

**Hint:** In `MeasurementNGSValidator.java`, you only need to look at the `NGS_PROPERTY` enum and
the inner `ValidationPolicy` class. In `NGSMeasurement.java`, focus on the factory methods and the
setter methods that emit events.

#### Questions

1. `NGSMeasurement` has two factory methods: `createSingleMeasurement` and `createWithPool`. What is
   the key difference between them in terms of input parameters and business rules?
2. When a pooled measurement is created, what must be true about every entry in
   `specificMeasurementMetadata`? What happens if this rule is violated?
1. The `NGS_PROPERTY` enum lists the column headers expected in an uploaded Excel file. List the
   five fields that are checked as **mandatory** during registration validation (hint: look at
   `validateMandatoryDataRegistration` in the inner `ValidationPolicy`).
1. Bug hunt: There is a method annotated with `@Deprecated` inside `ValidationPolicy` called
   `validateMandatoryDataForUpdate`. Read it carefully. There is a subtle bug in how the validation
   results are combined. Can you spot it? Explain what goes wrong and how you would fix it.
1. Every time a setter like `setMethod(...)` or `setOrganisation(...)` is called, an event is
   emitted. If a caller sets the method _and_ the organisation in the same transaction, how many
   `MeasurementUpdatedEvent` objects are fired? Is this a problem? Why or why not?
