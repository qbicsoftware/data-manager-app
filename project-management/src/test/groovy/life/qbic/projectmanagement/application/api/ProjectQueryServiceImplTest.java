package life.qbic.projectmanagement.application.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import life.qbic.projectmanagement.application.api.ProjectQueryService.ProjectSampleFilter;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.ProjectPageReadRepository;
import life.qbic.projectmanagement.application.query.ProjectSampleOverview;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link ProjectQueryServiceImpl}.
 *
 * <p>The read port is mocked; the reactive boundary (scheduler hop + security context propagation)
 * is exercised for real, so the tests also prove the query service does not deadlock or lose the
 * result across the scheduler hop.</p>
 */
class ProjectQueryServiceImplTest {

  private final ProjectPageReadRepository readRepository = mock(ProjectPageReadRepository.class);
  private final ProjectQueryServiceImpl underTest =
      new ProjectQueryServiceImpl(readRepository, Schedulers.boundedElastic());

  @BeforeEach
  void setUp() {
    // make sure no test takes forever
    StepVerifier.setDefaultTimeout(Duration.of(5, ChronoUnit.SECONDS));
  }

  @AfterEach
  void resetTimeout() {
    StepVerifier.resetDefaultTimeout();
  }

  @Test
  @DisplayName("experiment progress is streamed from the read port")
  void streamsExperimentProgress() {
    ExperimentProgress progress = mock(ExperimentProgress.class);
    when(readRepository.findExperimentProgress("P1")).thenReturn(List.of(progress));

    StepVerifier.create(underTest.getExperimentProgress("P1"))
        .expectNext(progress)
        .verifyComplete();
  }

  @Test
  @DisplayName("project samples are streamed with the given offset, limit and filter")
  void streamsProjectSamples() {
    ProjectSampleOverview row = mock(ProjectSampleOverview.class);
    ProjectSampleFilter filter = ProjectSampleFilter.all();
    when(readRepository.findProjectSamples("P1", 0, 12, filter)).thenReturn(List.of(row));

    StepVerifier.create(underTest.getProjectSamples("P1", 0, 12, filter))
        .expectNext(row)
        .verifyComplete();
  }

  @Test
  @DisplayName("the project sample count is emitted for the pager total")
  void emitsProjectSampleCount() {
    when(readRepository.countProjectSamples(eq("P1"), any())).thenReturn(42L);

    StepVerifier.create(underTest.countProjectSamples("P1", ProjectSampleFilter.all()))
        .expectNext(42L)
        .verifyComplete();
  }

  @Test
  @DisplayName("a failed read is mapped to a RequestFailedException")
  void mapsReadFailure() {
    when(readRepository.findExperimentProgress("P1"))
        .thenThrow(new IllegalStateException("boom"));

    StepVerifier.create(underTest.getExperimentProgress("P1"))
        .expectError(ProjectQueryService.RequestFailedException.class)
        .verify();
  }

  @Test
  @DisplayName("a security denial is mapped to an AccessDeniedException")
  void mapsAccessDenial() {
    when(readRepository.findExperimentProgress("P1"))
        .thenThrow(new org.springframework.security.access.AccessDeniedException("nope"));

    StepVerifier.create(underTest.getExperimentProgress("P1"))
        .expectError(ProjectQueryService.AccessDeniedException.class)
        .verify();
  }
}
