package life.qbic.projectmanagement.application.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectDesign;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateRequest;
import life.qbic.projectmanagement.application.api.fair.DigitalObjectFactory;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.api.template.TemplateService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
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
        measurementValidationService
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
                && projectUpdateResponse.hasRequestId() && projectUpdateResponse.requestId()
                .equals(requestId))
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
        measurementValidationService
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
                && projectUpdateResponse.hasRequestId() && projectUpdateResponse.requestId()
                .equals(requestId))
        .expectComplete()
        .verify(Duration.of(3, ChronoUnit.SECONDS));
  }
}
