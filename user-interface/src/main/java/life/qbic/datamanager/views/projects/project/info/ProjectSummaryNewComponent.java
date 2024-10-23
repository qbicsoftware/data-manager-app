package life.qbic.datamanager.views.projects.project.info;

import static life.qbic.datamanager.views.MeasurementType.GENOMICS;
import static life.qbic.datamanager.views.MeasurementType.PROTEOMICS;

import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import edu.kit.datamanager.ro_crate.writer.RoCrateWriter;
import edu.kit.datamanager.ro_crate.writer.ZipWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.download.DownloadContentProvider;
import life.qbic.datamanager.download.DownloadProvider;
import life.qbic.datamanager.export.TempDirectory;
import life.qbic.datamanager.export.rocrate.ROCreateBuilder;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.DetailBox;
import life.qbic.datamanager.views.general.section.HeadingWithIcon;
import life.qbic.datamanager.views.general.section.OntologyTermDisplay;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.Section.SectionBuilder;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.datamanager.views.general.section.SectionTitle.Size;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.projectmanagement.application.ContactRepository;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@UIScope
@SpringComponent
public class ProjectSummaryNewComponent extends PageArea {

  private static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm";

  private final ProjectInformationService projectInformationService;
  private final ROCreateBuilder roCrateBuilder;
  private final TempDirectory tempDirectory;
  private final ExperimentInformationService experimentInformationService;
  private Section headerSection;
  private Section projectDesignSection;
  private Section experimentInformationSection;
  private Section fundingInformationSection;
  private Section projectContactsSection;
  private Context context;
  private DownloadProvider downloadProvider;

  @Autowired
  public ProjectSummaryNewComponent(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      ContactRepository contactRepository,
      UserPermissions userPermissions,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      ROCreateBuilder rOCreateBuilder, TempDirectory tempDirectory) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.headerSection = new SectionBuilder().build();
    this.projectDesignSection = new SectionBuilder().build();
    this.experimentInformationSection = new SectionBuilder().build();
    this.fundingInformationSection = new SectionBuilder().build();
    this.projectContactsSection = new SectionBuilder().build();
    this.tempDirectory = Objects.requireNonNull(tempDirectory);
    this.roCrateBuilder = Objects.requireNonNull(rOCreateBuilder);
    addClassName("project-details-component");
    downloadProvider = new DownloadProvider(null);
    add(downloadProvider);
    add(headerSection);
    add(projectDesignSection);
    add(experimentInformationSection);
    add(fundingInformationSection);
    add(projectContactsSection);
    this.experimentInformationService = experimentInformationService;
  }

  private String formatDate(Instant date) {
    var formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.systemDefault());
    return formatter.format(date);
  }

  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
    var projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("No project id provided"));
    var projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow(() -> new ApplicationException("No project with given ID found"));
    var fullProject = projectInformationService.find(projectId)
        .orElseThrow(() -> new ApplicationException("No project found"));
    var experiments = experimentInformationService.findAllForProject(projectId);
    setContent(projectOverview, fullProject, experiments);

  }

  private void setContent(ProjectOverview projectInformation, Project fullProject, List<Experiment> experiments) {
    Objects.requireNonNull(projectInformation);
    buildHeaderSection(projectInformation);
    buildDesignSection(projectInformation, fullProject);
    buildExperimentInformationSection(projectInformation, experiments);
    buildFundingInformationSection(projectInformation);
    buildProjectContactsInfoSection(projectInformation);
  }

  private void buildProjectContactsInfoSection(ProjectOverview projectInformation) {
    fundingInformationSection.setHeader(
        new SectionHeader(new SectionTitle("Project Contacts"), new ActionBar(new Button("Edit"))));
  }

  private void buildFundingInformationSection(ProjectOverview projectInformation) {
    fundingInformationSection.setHeader(
        new SectionHeader(new SectionTitle("Funding Information"), new ActionBar(new Button("Edit"))));
  }

  private void buildExperimentInformationSection(ProjectOverview projectInformation, List<Experiment> experiments) {
    experimentInformationSection.setHeader(
        new SectionHeader(new SectionTitle("Experiment Information")));
    var speciesBox = new DetailBox();
    var speciesHeader = new DetailBox.Header(VaadinIcon.MALE.create(), "Species");
    speciesBox.setHeader(speciesHeader);
    speciesBox.setContent(buildSpeciesInfo(experiments));
    speciesBox.addClassNames("fixed-medium-width");

    var specimenBox = new DetailBox();
    var specimenHeader = new DetailBox.Header(VaadinIcon.DROP.create(), "Specimen");
    specimenBox.setHeader(specimenHeader);
    specimenBox.setContent(buildSpecimenInfo(experiments));
    specimenBox.addClassName("fixed-medium-width");

    var analyteBox = new DetailBox();
    var analyteHeader = new DetailBox.Header(VaadinIcon.CLUSTER.create(), "Analytes");
    analyteBox.setHeader(analyteHeader);
    analyteBox.setContent(buildAnalyteInfo(experiments));
    analyteBox.addClassName("fixed-medium-width");

    var sectionContent = new SectionContent();
    sectionContent.add(speciesBox);
    sectionContent.add(specimenBox);
    sectionContent.add(analyteBox);
    sectionContent.addClassNames("horizontal-list", "gap-medium", "wrapping-flex-container");
    experimentInformationSection.setContent(sectionContent);
  }

  private Div buildSpeciesInfo(List<Experiment> experiments) {
    var ontologyTerms = extractSpecies(experiments);
    return buildOntologyInfo(ontologyTerms);
  }

  private Div buildSpecimenInfo(List<Experiment> experiments) {
    var ontologyTerms = extractSpecimen(experiments);
    return buildOntologyInfo(ontologyTerms);
  }

  private Div buildAnalyteInfo(List<Experiment> experiments) {
    var ontologyTerms = extractAnalyte(experiments);
    return buildOntologyInfo(ontologyTerms);
  }

  private Div buildOntologyInfo(List<OntologyTerm> terms) {
    var container = new Div();
    terms.stream().map(this::convert).forEach(container::add);
    container.addClassNames("vertical-list", "gap-small");
    return container;
  }

  private OntologyTermDisplay convert(OntologyTerm ontologyTerm) {
    return new OntologyTermDisplay(ontologyTerm.getLabel(), ontologyTerm.getOboId(), ontologyTerm.getClassIri());
  }

  private List<OntologyTerm> extractSpecies(List<Experiment> experiments) {
    return experiments.stream().flatMap(experiment -> experiment.getSpecies().stream()).toList();
  }

  private List<OntologyTerm> extractSpecimen(List<Experiment> experiments) {
    return experiments.stream().flatMap(experiment -> experiment.getSpecimens().stream()).toList();
  }

  private List<OntologyTerm> extractAnalyte(List<Experiment> experiments) {
    return experiments.stream().flatMap(experiment -> experiment.getAnalytes().stream()).toList();
  }

  private void buildDesignSection(ProjectOverview projectInformation, Project project) {
    projectDesignSection.setHeader(
        new SectionHeader(new SectionTitle("Project Design"), new ActionBar(new Button("Edit"))));
    var content = new SectionContent();
    content.add(
        HeadingWithIcon.withIconAndText(VaadinIcon.NOTEBOOK.create(), "Project ID and Title"));
    content.add(new SimpleParagraph("%s - %s".formatted(projectInformation.projectCode(),
        projectInformation.projectTitle())));
    content.add(HeadingWithIcon.withIconAndText(VaadinIcon.MODAL_LIST.create(), "Objective"));
    content.add(new SimpleParagraph(project.getProjectIntent().objective().objective()));
    projectDesignSection.setContent(content);
  }

  private void buildHeaderSection(ProjectOverview projectOverview) {
    Objects.requireNonNull(projectOverview);
    var header = new SectionHeader(
        new SectionTitle("%s - %s".formatted(projectOverview.projectCode(),
            projectOverview.projectTitle()), Size.LARGE));
    var crateExportBtn = new Button("Export as RO-Crate");
    crateExportBtn.addClickListener(event -> {
      try {
        triggerRoCrateDownload();
      } catch (IOException e) {
        throw new ApplicationException("An error occurred while exporting RO-Crate", e);
      }
    });
    ActionBar actionBar = new ActionBar(crateExportBtn);
    header.setActionBar(actionBar);
    header.setSmallTrailingMargin();

    var sectionContent = new SectionContent();
    sectionContent.add(createAvatarGroup(projectOverview.collaboratorUserInfos()));
    sectionContent.add(createTags(projectOverview));

    header.setSectionNote(new SectionNote(
        "Last modified on %s".formatted(formatDate(projectOverview.lastModified()))));
    headerSection.setHeader(header);
    headerSection.setContent(sectionContent);
  }

  private void triggerRoCrateDownload() throws IOException {
    ProjectId projectId = context.projectId().orElseThrow();
    Project project = projectInformationService.find(projectId).orElseThrow();
    var tempBuildDir = tempDirectory.createDirectory();
    var zippedRoCrateDir = tempDirectory.createDirectory();
    try {
      var roCrate = roCrateBuilder.projectSummary(project, tempBuildDir);
      var roCrateZipWriter = new RoCrateWriter(new ZipWriter());
      var zippedRoCrateFile = zippedRoCrateDir.resolve(
          "%s-project-summary-ro-crate.zip".formatted(project.getProjectCode().value()));
      roCrateZipWriter.save(roCrate, zippedRoCrateFile.toString());
      remove(downloadProvider);
      var cachedZipContent = Files.readAllBytes(zippedRoCrateFile);
      downloadProvider = new DownloadProvider(new DownloadContentProvider() {
        @Override
        public byte[] getContent() {
          return cachedZipContent;
        }

        @Override
        public String getFileName() {
          return zippedRoCrateFile.getFileName().toString();
        }
      });
      add(downloadProvider);
      downloadProvider.trigger();
    } catch (IOException e) {
      throw new ApplicationException("Error exporting ro-crate.zip", e);
    } finally {
      deleteTempDir(tempBuildDir.toFile());
      deleteTempDir(zippedRoCrateDir.toFile());
    }
  }

  private boolean deleteTempDir(File dir) throws IOException {
    File[] files = dir.listFiles(); //null if not a directory
    // https://docs.oracle.com/javase/8/docs/api/java/io/File.html#listFiles--
    if (files != null) {
      for (File file : files) {
        if (!deleteTempDir(file)) {
          return false;
        }
      }
    }
    return dir.delete();
  }

  public AvatarGroup createAvatarGroup(Collection<UserInfo> userInfo) {
    AvatarGroup avatarGroup = new AvatarGroup();
    userInfo.forEach(user -> avatarGroup.add(new UserAvatarGroupItem(user.userName(),
        user.userId())));
    avatarGroup.setMaxItemsVisible(3);
    return avatarGroup;
  }

  private Div createTags(ProjectOverview projectOverview) {
    var tags = new Div();
    tags.addClassName("tag-list");
    buildTags(projectOverview).forEach(tags::add);
    return tags;
  }

  private List<Tag> buildTags(ProjectOverview projectInformation) {
    var tags = new ArrayList<Tag>();
    if (projectInformation.ngsMeasurementCount() != null) {
      tags.add(TagFactory.forMeasurement(GENOMICS));
    }
    if (projectInformation.pxpMeasurementCount() != null) {
      tags.add(TagFactory.forMeasurement(PROTEOMICS));
    }
    return tags;
  }
}
