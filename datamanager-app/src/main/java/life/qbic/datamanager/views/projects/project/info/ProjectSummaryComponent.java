package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.MeasurementType.GENOMICS;
import static life.qbic.datamanager.views.MeasurementType.IMMUNOPEPTIDOMICS;
import static life.qbic.datamanager.views.MeasurementType.PROTEOMICS;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.InputStream;
import java.io.Serial;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ByteBufferIteratorInputStream;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.RequestCache;
import life.qbic.datamanager.RequestCache.CacheException;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.CollapsibleDetails;
import life.qbic.datamanager.views.general.DetailBox;
import life.qbic.datamanager.views.general.DetailBox.Header;
import life.qbic.datamanager.views.general.Heading;
import life.qbic.datamanager.views.general.IconLabel;
import life.qbic.datamanager.views.general.OntologyTermDisplay;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.funding.BoundFundingField;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.general.funding.FundingInputForm;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.Section.SectionBuilder;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.datamanager.views.general.section.SectionTitle.Size;
import life.qbic.datamanager.views.general.utils.Restorable.Snapshot;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditContactsComponent;
import life.qbic.datamanager.views.projects.edit.ProjectDesignForm;
import life.qbic.datamanager.views.projects.project.settings.ProjectSettingsMain;
import life.qbic.datamanager.views.strategy.scope.ReadScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.UserScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.WriteScopeStrategy;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationCreationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationCreationResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.OntologyTerm;
import life.qbic.projectmanagement.application.api.AsyncProjectService.PrincipalInvestigator;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectContact;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectManager;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsible;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsibleDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsibleDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RequestFailedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.UnknownRequestException;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.contact.PersonLookupService;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Contact.OidcInformation;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

/**
 * <b>Project Summary Component</b>
 *
 * <p>Gives the user an overview about general project information, such as:</p>
 *
 * <ul>
 *   <li>title</li>
 *   <li>objective</li>
 *   <li>shared with other users</li>
 *   <li>funding</li>
 *   <li>contacts</li>
 * </ul>
 *
 * @since <version tag>
 */
@UIScope
@SpringComponent
public class ProjectSummaryComponent extends PageArea {

  public static final String FIXED_MEDIUM_WIDTH_CSS = "fixed-medium-width";
  public static final String PROJECT_UPDATED_SUCCESS = "project.updated.success";
  public static final String CANCEL_BUTTON_TEXT = "Cancel";
  public static final String SAVE_BUTTON_TEXT = "Save";
  private static final Logger log = logger(ProjectSummaryComponent.class);
  private final transient ProjectInformationService projectInformationService;
  private final Section headerSection;
  private final DownloadComponent downloadComponent;
  private final ProjectProgressSection projectProgressSection;
  private final ProjectExperimentsSection projectExperimentsSection;
  private final Div identityMeta = new Div();
  private transient String identityLastModified = "";
  private final transient AsyncProjectService asyncProjectService;
  private final Div projectMetadataBand = new Div();
  private Context context;

  @Autowired
  public ProjectSummaryComponent(ProjectInformationService projectInformationService,
      AsyncProjectService asyncProjectService,
      ProjectQueryService projectQueryService,
      AssociatedDatasetService associatedDatasetService) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.headerSection = new SectionBuilder().build();
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.downloadComponent = new DownloadComponent();
    this.projectProgressSection = new ProjectProgressSection(
        Objects.requireNonNull(projectQueryService), projectInformationService,
        Objects.requireNonNull(associatedDatasetService));
    this.projectExperimentsSection = new ProjectExperimentsSection(
        Objects.requireNonNull(projectQueryService));
    this.projectExperimentsSection.addAddExperimentListener(
        event -> fireEvent(new AddExperimentRequestedEvent(this, event.isFromClient())));
    this.projectProgressSection.addProgressLoadedListener(
        event -> updateIdentityMeta(event.progress()));

    addClassName("project-details-component");

    add(headerSection);
    add(projectProgressSection);
    add(projectExperimentsSection);
    add(projectMetadataBand);
    add(downloadComponent);
  }


  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
    var projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("No project id provided"));
    var projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow(() -> new ApplicationException("No project with given ID found"));
    var fullProject = projectInformationService.find(projectId)
        .orElseThrow(() -> new ApplicationException("No project found"));
    setContent(projectOverview, fullProject);
    projectProgressSection.setProjectId(projectId.value());
    projectExperimentsSection.setProjectId(projectId.value());
  }

  /**
   * Delegates the "add experiment" action of the experiments section to the owning page, which runs
   * the creation workflow.
   */
  public Registration addAddExperimentListener(
      ComponentEventListener<AddExperimentRequestedEvent> listener) {
    return addListener(AddExperimentRequestedEvent.class, listener);
  }

  /**
   * Fired when the user asks to create a new experiment, from either the identity band action or the
   * experiments section. The owning page runs the creation workflow.
   */
  public static class AddExperimentRequestedEvent extends
      ComponentEvent<ProjectSummaryComponent> {

    @Serial
    private static final long serialVersionUID = 1L;

    public AddExperimentRequestedEvent(ProjectSummaryComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }


  private void setContent(ProjectOverview projectOverview, Project fullProject) {
    Objects.requireNonNull(projectOverview);
    buildHeaderSection(projectOverview, fullProject);
    buildMetadataBand(projectOverview, fullProject);
  }


  /**
   * The contacts row of the identity band: "Contacts", one chip per role, and "Edit contacts".
   * Editing stays in a dialog until the project settings surface exists.
   */
  private Div contactsRow(Project project) {
    Div row = new Div();
    row.addClassName("project-contacts");
    Span label = new Span("Contacts");
    label.addClassName("project-contacts-label");
    row.add(label);
    project.getResponsiblePerson().ifPresent(contact -> row.add(contactChip("Resp.", contact)));
    row.add(contactChip("PM", project.getProjectManager()));
    row.add(contactChip("PI", project.getPrincipalInvestigator()));
    Button edit = new Button("Edit contacts");
    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    edit.addClassName("project-contacts-edit");
    edit.addClickListener(event -> openProjectSettings(project.getId().value()));
    row.add(edit);
    return row;
  }

  private Span contactChip(String role, Contact contact) {
    Span chip = new Span();
    chip.addClassName("project-contact");
    Span roleChip = new Span(role);
    roleChip.addClassName("project-contact-role");
    Span name = new Span(contact.fullName());
    name.addClassName("project-contact-name");
    chip.add(roleChip, name);

    // The ORCID iD is not shown as text — it is clutter for everyone but its owner — so the logo
    // carries the link to the public record instead. A missing iD is simply not shown, so the
    // remaining chips stay aligned.
    contact.oidcInformation().flatMap(oidc -> Arrays.stream(OidcType.values())
            .filter(type -> type.getIssuer().equals(oidc.oidcIssuer()))
            .findFirst()
            .map(type -> orcidLink(type, oidc.oidcId())))
        .ifPresent(chip::add);

    var mail = new Anchor("mailto:" + contact.emailAddress());
    mail.getElement().setAttribute("title", contact.emailAddress());
    Icon envelope = new Icon(VaadinIcon.ENVELOPE);
    envelope.addClassName("project-contact-glyph");
    mail.add(envelope);
    mail.addClassName("project-contact-action");
    chip.add(mail);
    return chip;
  }

  private Anchor orcidLink(OidcType type, String oidcId) {
    Anchor orcid = new Anchor(type.getUrlFor(oidcId));
    orcid.setTarget(AnchorTarget.BLANK);
    orcid.getElement().setAttribute("title", "ORCID: " + oidcId);
    OidcLogo logo = new OidcLogo(type);
    logo.addClassName("project-contact-logo");
    orcid.add(logo);
    orcid.addClassName("project-contact-action");
    return orcid;
  }


  /**
   * Submits multiple {@link ProjectUpdateRequest} to the service.
   *
   * @param requests a {@link Collection} of {@link ProjectUpdateRequest}.
   * @since 1.10.0
   */


  /*
  Handler for successful project updates
   */

  /*
  Handler for request failures. This covers failures the user can
  re-try it again, so the last request needs to be cached in the current user
  session and ui scope.
   */

  /*
  Handler for unknown requests. This happens when the wrong service method
  has been called, so be sure to check the requests the service API can handle.
   */

  /*
  Handler for access denied exceptions. This usually happens, when the service method called
  requires rights the current logged-in user does not have.
   */

  /**
   * Project metadata is edited on the Project settings page, never in a dialog beside the title:
   * editing is rare and never urgent, so it does not compete with the content, and every editor
   * lives in one place instead of one dialog per band.
   */
  private void openProjectSettings(String projectId) {
    getUI().ifPresent(ui -> ui.navigate(ProjectSettingsMain.class,
        new RouteParameters(new RouteParam("projectId", projectId))));
  }

  private void buildMetadataBand(ProjectOverview projectOverview, Project project) {
    projectMetadataBand.removeAll();
    projectMetadataBand.addClassName("project-metadata");

    Div head = new Div();
    head.addClassName("project-metadata-head");
    Span title = new Span("Project details");
    title.addClassName("project-metadata-title");
    Button edit = new Button("Edit project details");
    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    edit.addClassName("project-metadata-edit");
    edit.addClickListener(event -> openProjectSettings(project.getId().value()));
    head.add(title, edit);

    Div facts = new Div();
    facts.addClassName("project-metadata-facts");
    // Funding leads: it is the only fact here with an editor behind it, and the one most often
    // missing. The project code is deliberately absent -- the identity band already prints it.
    facts.add(fundingFact(project));
    facts.add(assayTypesFact(projectOverview));
    facts.add(datasetAccessFact(projectOverview));

    projectMetadataBand.add(head, facts);
  }

  /**
   * The funding fact. Absence is rendered as an absent value, not as a sentence: the band is a row
   * of values, and a gap a curator can act on should look like a gap.
   */
  private static Div fundingFact(Project project) {
    return project.funding()
        .map(funding -> {
          String grant = funding.grant() == null ? "" : funding.grant().strip();
          String grantId = funding.grantId() == null ? "" : funding.grantId().strip();
          if (grant.isEmpty() && grantId.isEmpty()) {
            return fact("Funding", "Not recorded", true);
          }
          if (grant.isEmpty()) {
            return fact("Funding", grantId);
          }
          if (grantId.isEmpty()) {
            return fact("Funding", grant);
          }
          return fact("Funding", "%s (%s)".formatted(grant, grantId));
        })
        .orElseGet(() -> fact("Funding", "Not recorded", true));
  }

  /**
   * The assay types present in the project, derived from which measurement tables carry rows.
   *
   * <p>This is a technology class -- Genomics, Proteomics, Immunopeptidomics -- and deliberately
   * not an analysis type such as RNA-Seq or 16S. The analysis method is not modelled yet, and it
   * belongs on the measurement rather than on the sample, so it must not be inferred here: the
   * sample does not know whether its DNA was sequenced as WGS or as a 16S amplicon, and one
   * extract can legitimately yield both.
   */
  private static Div assayTypesFact(ProjectOverview overview) {
    List<String> types = new ArrayList<>();
    if (overview.ngsMeasurementCount() > 0) {
      types.add("Genomics");
    }
    if (overview.pxpMeasurementCount() > 0) {
      types.add("Proteomics");
    }
    if (overview.ipMeasurementCount() > 0) {
      types.add("Immunopeptidomics");
    }
    return types.isEmpty()
        ? fact("Assay types", "None recorded", true)
        : fact("Assay types", String.join(" \u00b7 ", types));
  }

  /**
   * Data access, read from the access levels of the datasets connected to the project. The project
   * itself carries no access level, so the honest reading of the prototype's "Data access" fact is
   * the distribution across its connected datasets.
   */
  private static Div datasetAccessFact(ProjectOverview overview) {
    int connected = overview.connectedDatasetCount();
    if (connected == 0) {
      return fact("Data access", "No datasets connected yet", true);
    }
    return fact("Data access", "%d dataset%s \u00b7 %d open, %d restricted".formatted(
        connected, connected == 1 ? "" : "s",
        overview.openDatasetCount(), overview.restrictedDatasetCount()));
  }

  private static Div fact(String label, String value) {
    return fact(label, value, false);
  }

  private static Div fact(String label, String value, boolean missing) {
    Div fact = new Div();
    fact.addClassName("project-metadata-fact");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-metadata-fact-label");
    Span valueSpan = new Span(value);
    valueSpan.addClassName("project-metadata-fact-value");
    if (missing) {
      valueSpan.addClassName("project-metadata-fact-value--missing");
    }
    fact.add(labelSpan, valueSpan);
    return fact;
  }

  /**
   * The identity band: title and code, the activity line, the contacts and the objective. All of
   * it is read-only; the affordances route to the settings page.
   */
  private void buildHeaderSection(ProjectOverview projectOverview, Project project) {
    Objects.requireNonNull(projectOverview);

    // Identity band: <title> <code>, the way the prototype names the project in its band.
    SectionTitle title = new SectionTitle();
    title.setSize(Size.LARGE);
    Span titleText = new Span(projectOverview.projectTitle());
    Span code = new Span(projectOverview.projectCode());
    code.addClassNames("project-identity-code", "identifier");
    title.add(titleText, code);

    Button crateExportBtn = new Button("Export summary");
    crateExportBtn.addClickListener(event -> triggerRoCrateDownload());
    Button addExperimentBtn = new Button("Add experiment");
    addExperimentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addExperimentBtn.addClickListener(click -> fireEvent(
        new AddExperimentRequestedEvent(this, click.isFromClient())));

    SectionHeader header = new SectionHeader(title,
        new ActionBar(addExperimentBtn, crateExportBtn));
    header.setSmallTrailingMargin();

    identityMeta.addClassName("project-identity-meta");
    identityLastModified = "Last modified on %s".formatted(DateTimeFormat.asJavaFormatter(
            DateTimeFormat.SIMPLE_DATE_SHORT, ZoneId.systemDefault())
        .format(projectOverview.lastModified()));
    renderIdentityMeta(0, 0);

    var sectionContent = new SectionContent();
    sectionContent.add(identityMeta);
    sectionContent.add(contactsRow(project));
    sectionContent.add(objectiveBlock(project));
    headerSection.setHeader(header);
    headerSection.setContent(sectionContent);
  }

  private void updateIdentityMeta(List<ExperimentProgress> progress) {
    int attention = (int) progress.stream()
        .filter(entry -> entry.measurementsWithoutPrimaryData() > 0)
        .count();
    renderIdentityMeta(progress.size(), attention);
  }

  private void renderIdentityMeta(int experiments, int attention) {
    identityMeta.removeAll();
    identityMeta.add(new Span(identityLastModified));
    identityMeta.add(separator());
    identityMeta.add(new Span(experiments == 1 ? "1 experiment" : experiments + " experiments"));
    if (attention > 0) {
      identityMeta.add(separator());
      identityMeta.add(attentionChip(attention));
    }
  }

  /**
   * The attention rollup. Attention can hit several experiments at once, so the chip is a
   * project-level count that brings the reader to the per-experiment rows rather than jumping
   * into one arbitrary experiment.
   */
  private Div attentionChip(int attention) {
    Div chip = new Div();
    chip.addClassName("project-identity-attention");
    chip.addClassName("project-identity-attention--action");
    chip.getElement().setAttribute("title", "Show which experiments need attention");
    chip.add(new Span(attention == 1
        ? "1 experiment needs attention"
        : attention + " experiments need attention"));
    chip.add(VaadinIcon.ANGLE_RIGHT.create());
    Utility.activateOnClickAndKeys(chip, "button", this::scrollToAttention);
    return chip;
  }

  private void scrollToAttention() {
    getUI().ifPresent(ui -> ui.getPage().executeJs(
        "document.querySelector('.project-progress-attention')"
            + "?.scrollIntoView({behavior: 'smooth', block: 'center'})"));
  }

  private static Span separator() {
    Span separator = new Span("\u00b7");
    separator.addClassName("project-identity-separator");
    return separator;
  }

  /**
   * The objective under the identity block: expanded by default, clamped to a few lines with a
   * "Show more" toggle, exactly as the prototype presents it (never a collapsed grey bar).
   */
  private static Div objectiveBlock(Project project) {
    Div block = new Div();
    block.addClassName("project-objective");
    Span text = new Span(project.getProjectIntent().objective().objective());
    text.addClassName("project-objective-text");
    Button toggle = new Button("Show more");
    toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    toggle.addClassName("project-objective-toggle");
    toggle.addClickListener(click -> {
      boolean expanded = text.hasClassName("is-expanded");
      text.getClassNames().set("is-expanded", !expanded);
      toggle.setText(expanded ? "Show more" : "Show less");
    });
    block.add(text, toggle);
    return block;
  }

  private InputStream forSummary(ProjectId projectId) {
    var byteBufferIterator = asyncProjectService.roCrateSummary(projectId.value())
        .timeout(Duration.ofSeconds(10)).toIterable()
        .iterator();
    return new ByteBufferIteratorInputStream(byteBufferIterator);
  }

  private void triggerRoCrateDownload() {
    var projectCode = projectInformationService.find(context.projectId().orElseThrow())
        .map(Project::getProjectCode)
        .map(ProjectCode::value)
        .orElse("");
    downloadComponent.trigger(new DownloadStreamProvider() {
      @Override
      public String getFilename() {
        return FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectCode,
            "project summary", "zip");
      }

      @Override
      public InputStream getStream() {
        return forSummary(context.projectId().orElseThrow());
      }

      @Override
      public String getContentType() {
        return "application/zip";
      }

      @Override
      public Optional<Long> contentLength() {
        return Optional.empty();
      }


    });

  }

  public AvatarGroup createAvatarGroup(Collection<UserInfo> userInfo) {
    AvatarGroup avatarGroup = new AvatarGroup();
    userInfo.forEach(user -> avatarGroup.add(new UserAvatarGroupItem(user.userName(),
        user.userId())));
    avatarGroup.setMaxItemsVisible(3);
    return avatarGroup;
  }


}
