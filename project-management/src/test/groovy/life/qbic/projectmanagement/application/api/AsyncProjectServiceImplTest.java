package life.qbic.projectmanagement.application.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentUpdateResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariable;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariableAdditions;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariables;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectDesign;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateRequest;
import life.qbic.projectmanagement.application.api.fair.DigitalObjectFactory;
import life.qbic.projectmanagement.application.api.template.TemplateService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class AsyncProjectServiceImplTest {

  ProjectInformationService projectServiceMock = mock(ProjectInformationService.class);
  SampleInformationService sampleServiceMock = mock(SampleInformationService.class);
  DigitalObjectFactory digitalObjectFactory = mock(DigitalObjectFactory.class);
  SampleValidationService sampleValidationService = mock(SampleValidationService.class);
  MeasurementValidationService measurementValidationService = mock(
      MeasurementValidationService.class);
  TemplateService templateService = mock(TemplateService.class);
  ExperimentInformationService experimentInformationService = mock(ExperimentInformationService.class);
  ExperimentInformationService experimentInformationServiceMock = mock(
      ExperimentInformationService.class);
  TerminologyService terminologyService = mock(TerminologyService.class);
  SpeciesLookupService taxaService = mock(SpeciesLookupService.class);

  @BeforeEach
  void setUp() {
    //make sure that there is no test takeing forever
    StepVerifier.setDefaultTimeout(Duration.of(3, ChronoUnit.SECONDS));
  }

  @AfterEach
  void afterEach() {
    StepVerifier.resetDefaultTimeout();
  }

  @Test
  @DisplayName("Test that the update completes for ProjectDesign")
  void updateProjectDesignCompletes() {

    AsyncProjectServiceImpl underTest = new AsyncProjectServiceImpl(
        projectServiceMock,
        sampleServiceMock,
        Schedulers.boundedElastic(),
        digitalObjectFactory,
        templateService,
        sampleValidationService,
        measurementValidationService,
        experimentInformationServiceMock,
        terminologyService,
        taxaService
    );

    String projectId = UUID.randomUUID().toString();
    ProjectDesign requestBody = new ProjectDesign("neq title", "new objective");
    String requestId = UUID.randomUUID().toString();
    ProjectUpdateRequest projectUpdateRequest = new ProjectUpdateRequest(projectId,
        requestBody, requestId);

    var testPublisher = underTest.update(projectUpdateRequest)
        .doOnSuccess(success -> {
          Mockito.verify(projectServiceMock, Mockito.times(1)).updateTitle(any(), any());
          Mockito.verify(projectServiceMock, Mockito.times(1)).updateObjective(any(), any());
        });

    StepVerifier.create(testPublisher)
        .expectNextMatches(projectUpdateResponse ->
            projectUpdateResponse.projectId().equals(projectId)
                && projectUpdateResponse.responseBody() instanceof ProjectDesign
                && requestId
                .equals(projectUpdateResponse.requestId()))
        .expectComplete()
        .verify(Duration.of(3, ChronoUnit.SECONDS));
  }


  @Test
  @DisplayName("Test that the update retries for ProjectDesign")
  void updateProjectDesignRepeats() {

    AsyncProjectServiceImpl underTest = new AsyncProjectServiceImpl(
        projectServiceMock,
        sampleServiceMock,
        Schedulers.boundedElastic(),
        digitalObjectFactory,
        templateService,
        sampleValidationService,
        measurementValidationService,
        experimentInformationServiceMock,
        terminologyService,
        taxaService
    );

    String projectId = UUID.randomUUID().toString();
    ProjectDesign requestBody = new ProjectDesign("new title", "new objective");
    String requestId = UUID.randomUUID().toString();
    ProjectUpdateRequest projectUpdateRequest = new ProjectUpdateRequest(projectId,
        requestBody, requestId);

    // throw exception the first time the method is called to simulate failure
    Mockito.doThrow(new RuntimeException("Testing exception to trigger retry 1"))
        .doNothing()
        .when(projectServiceMock).updateTitle(any(), any());

    // throw exception the first time the method is called to simulate failure
    Mockito.doThrow(new RuntimeException("Testing exception to trigger retry 2"))
        .doNothing()
        .when(projectServiceMock).updateObjective(any(), any());

    var testPublisher = underTest.update(projectUpdateRequest)
        .doOnSuccess(success -> {
          Mockito.verify(projectServiceMock, atLeast(1)).updateTitle(any(), any());
          Mockito.verify(projectServiceMock, atLeast(1)).updateObjective(any(), any());
        });

    StepVerifier.create(testPublisher)
        .expectNextMatches(projectUpdateResponse ->
            projectUpdateResponse.projectId().equals(projectId)
                && projectUpdateResponse.responseBody() instanceof ProjectDesign
                && requestId
                .equals(projectUpdateResponse.requestId()))
        .expectComplete()
        .verify(Duration.of(3, ChronoUnit.SECONDS));
  }

  @Test
  @DisplayName("Test that the update completes for adding experimental variables")
  void creatingExperimentalVariablesCompletes() {

    Experiment experimentMock = mock(Experiment.class);
    ExperimentRepository mockExperimentRepo = mock(
        ExperimentRepository.class);
    ExperimentInformationService experimentInformationService = new ExperimentInformationService(
        mockExperimentRepo, mock(ProjectRepository.class),
        mock(SampleInformationService.class));

    AsyncProjectServiceImpl underTest = new AsyncProjectServiceImpl(projectServiceMock,
        sampleServiceMock,
        Schedulers.boundedElastic(),
        digitalObjectFactory,
        templateService,
        sampleValidationService,
        measurementValidationService,
        experimentInformationService,
        terminologyService,
        taxaService);

    String projectId = UUID.randomUUID().toString();
    String experimentId = UUID.randomUUID().toString();

    when(mockExperimentRepo.find(ExperimentId.parse(experimentId))).thenReturn(
        Optional.of(experimentMock));
    int numVars = 4;
    var request = addExperimentalVariablesRequest(projectId, experimentId, numVars);

    var testPublisher = underTest.update(request);

    Predicate<ExperimentUpdateResponse> matchesExpectedOutcome = experimentUpdateResponse ->
        experimentUpdateResponse.experimentId().equals(experimentId)
            && experimentUpdateResponse.body() instanceof ExperimentalVariables
            && experimentUpdateResponse.requestId().equals(request.requestId())
            && ((ExperimentalVariables) experimentUpdateResponse.body()).experimentalVariables()
            .size() == numVars;

    StepVerifier.create(testPublisher)
        .expectNextMatches(matchesExpectedOutcome)
        .expectComplete()
        .verify();
  }

  private static ExperimentUpdateRequest addExperimentalVariablesRequest(String projectId,
      String experimentId, int numVars) {
    List<ExperimentalVariable> experimentalVariables = new ArrayList<>();
    for (int i = 0; i < numVars; i++) {
      experimentalVariables.add(new ExperimentalVariable(generateRandomString(),
          Set.of(generateRandomString(), generateRandomString()), generateRandomString()
      ));
    }

    ExperimentalVariableAdditions additions = new ExperimentalVariableAdditions(
        experimentalVariables);
    return new ExperimentUpdateRequest(projectId, experimentId, additions);
  }

  private static String generateRandomString() {
    Random random = new Random();
    int length = random.nextInt(1, 64);
    int upperCharsStart = 65; //A
    int upperCharsStop = 90; // Z
    int lowerCharsStart = 97; //a
    int lowerCharsStop = 122; //z
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      int randomChar = random.nextInt(upperCharsStart, lowerCharsStop);
      while (randomChar > upperCharsStop && randomChar < lowerCharsStart) {
        randomChar = random.nextInt(upperCharsStart, lowerCharsStop);
      }
      sb.append((char) randomChar);
    }
    return sb.toString();
  }
}
