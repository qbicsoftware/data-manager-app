package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.datamanager.views.notifications.NotificationLevel;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.context.annotation.Profile;

/**
 * <b>Connected Datasets Demo</b>
 *
 * <p>Pure UI prototype for the "Connect associated datasets" feature. Showcases all user
 * stories (Stories 01–08) for integrating public and restricted InvenioRDM datasets (Zenodo / FDAT)
 * with a Data Manager project.</p>
 *
 * <p>Available only when the <code>development</code> Spring profile is active.</p>
 *
 * @since 1.12.0
 */
@Profile("development")
@Route("demo/connected-datasets")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
public class ConnectedDatasetsDemo extends Div {

  private static final Logger log = LoggerFactory.logger(ConnectedDatasetsDemo.class);

  private Span publicHeaderSpan;
  private Span privateHeaderSpan;

  private final MessageSourceNotificationFactory messageFactory;

  private final List<ConnectedDataset> publicDatasets = new ArrayList<>();
  private final List<ConnectedDataset> privateDatasets = new ArrayList<>();
  private final List<SearchResult> mockSearchResults = new ArrayList<>();

  private Grid<ConnectedDataset> publicGrid;
  private Grid<ConnectedDataset> privateGrid;
  private Grid<SearchResult> searchResultGrid;

  public ConnectedDatasetsDemo(MessageSourceNotificationFactory messageFactory) {
    this.messageFactory = Objects.requireNonNull(messageFactory);
    initMockData();
    buildUI();
  }

  /* ========================= Mock data ========================= */

  private void initMockData() {
    publicDatasets.add(new ConnectedDataset("ds-1",
        "Proteomics dataset of E. coli stress response",
        "High-throughput LC-MS/MS proteomics data used for the manuscript 'E. coli under heat shock'.",
        "10.5281/zenodo.12345", "Zenodo", "v1.0", "Dr. Anna Schmidt",
        "https://zenodo.org/record/12345", false,
        Instant.parse("2025-03-15T10:00:00Z")));
    publicDatasets.add(new ConnectedDataset("ds-2",
        "Supplementary figures and statistical reports",
        "Extended Data Figures 1–12 and source data for the main text.",
        "10.5281/zenodo.12346", "Zenodo", "v2.1", "Dr. Anna Schmidt",
        "https://zenodo.org/record/12346", false,
        Instant.parse("2025-04-01T14:30:00Z")));
    publicDatasets.add(new ConnectedDataset("ds-3",
        "Protocol for sample preparation",
        "Step-by-step protocol for enrichment and digestion of proteins.",
        "10.1234/fdat.789", "FDAT", "v1.0", "Prof. Max Müller",
        "https://fdat.uni-tuebingen.de/records/789", false,
        Instant.parse("2025-05-10T09:00:00Z")));

    privateDatasets.add(new ConnectedDataset("ds-4",
        "Draft NGS data — embargoed",
        "Raw sequencing reads for the pilot study. Embargoed until publication.",
        "10.5281/zenodo.99999", "Zenodo", "v0.9-beta", "Dr. Anna Schmidt",
        "https://zenodo.org/record/99999", true,
        Instant.parse("2025-06-01T11:00:00Z")));
    privateDatasets.add(new ConnectedDataset("ds-5",
        "Internal collaboration results",
        "Preliminary results shared with the collaborating group in Heidelberg.",
        "10.1234/fdat.111", "FDAT", "v1.2", "Prof. Max Müller",
        "https://fdat.uni-tuebingen.de/records/111", true,
        Instant.parse("2025-06-10T16:45:00Z")));

    // Search result mock pool
    mockSearchResults.add(new SearchResult("sr-1",
        "Whole-genome sequencing of S. aureus isolates",
        "10.5281/zenodo.77777", "Zenodo", "v1.0",
        "https://zenodo.org/record/77777"));
    mockSearchResults.add(new SearchResult("sr-2",
        "Metabolomics reference standards",
        "10.5281/zenodo.77778", "Zenodo", "v2.0",
        "https://zenodo.org/record/77778"));
    mockSearchResults.add(new SearchResult("sr-3",
        "Clinical metadata schema",
        "10.1234/fdat.999", "FDAT", "v1.0",
        "https://fdat.uni-tuebingen.de/records/999"));
    mockSearchResults.add(new SearchResult("sr-4",
        "Imaging dataset: confocal microscopy",
        "10.5281/zenodo.77779", "Zenodo", "v1.1",
        "https://zenodo.org/record/77779"));
    mockSearchResults.add(new SearchResult("sr-5",
        "Software pipeline release 2025-Q2",
        "10.1234/fdat.1000", "FDAT", "v3.0",
        "https://fdat.uni-tuebingen.de/records/1000"));
  }

  /* ========================= UI construction ========================= */

  private void buildUI() {
    addClassNames("padding-horizontal-07", "padding-vertical-04", "flex-vertical", "gap-04");

    Div title = new Div("Connected Datasets — UI Prototype");
    title.addClassName("heading-1");

    Div subtitle = new Div(
        "Prototype for InvenioRDM dataset integration covering Stories 01–08. "
            + "This view is only visible in the development profile.");
    subtitle.addClassName("normal-body-text");

    Div publicSection = createPublicDatasetsSection();
    Div privateSection = createPrivateDatasetsSection();

    add(title, subtitle, publicSection, privateSection);
  }

  private Div createPublicDatasetsSection() {
    Div section = new Div();
    section.addClassNames("border", "rounded-02", "padding-03", "flex-vertical", "gap-03",
        "width-full");

    publicHeaderSpan = new Span("Public Datasets (%d)".formatted(publicDatasets.size()));
    Div header = new Div();
    header.addClassNames("flex-horizontal", "gap-02", "flex-align-items-center");
    Component globeIcon = VaadinIcon.GLOBE.create();
    globeIcon.addClassName("color-primary-text");
    header.add(globeIcon, publicHeaderSpan);
    header.addClassName("heading-2");

    publicGrid = createConnectedDatasetGrid(publicDatasets, false);

    Button connectBtn = new Button("Connect Dataset", VaadinIcon.PLUS.create());
    connectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectBtn.addClickListener(e -> openConnectDialog(false));

    Button syncAllBtn = new Button("Sync All", VaadinIcon.REFRESH.create());
    syncAllBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    syncAllBtn.addClickListener(e -> syncAll(false));

    Div actions = new Div(connectBtn, syncAllBtn);
    actions.addClassNames("flex-horizontal", "gap-04", "padding-vertical-02");

    section.add(header, actions, publicGrid);
    return section;
  }

  private Div createPrivateDatasetsSection() {
    Div section = new Div();
    section.addClassNames("border", "rounded-02", "padding-03", "flex-vertical", "gap-03",
        "width-full");

    privateHeaderSpan = new Span(
        "Private / Restricted Datasets (%d)".formatted(privateDatasets.size()));
    Div header = new Div();
    header.addClassNames("flex-horizontal", "gap-02", "flex-align-items-center");
    Component lockIcon = VaadinIcon.LOCK.create();
    lockIcon.addClassName("color-primary-text");
    header.add(lockIcon, privateHeaderSpan);
    header.addClassName("heading-2");

    // Info banner for token requirement (Story 05)
    Div tokenBanner = new Div(
        VaadinIcon.INFO_CIRCLE.create(),
        new Span(
            " Restricted dataset search requires a personal access token for the target InvenioRDM instance. Configure tokens in your Account settings."));
    tokenBanner.addClassNames("flex-horizontal", "gap-02", "padding-03", "border", "rounded-02",
        "background-color-secondary");

    privateGrid = createConnectedDatasetGrid(privateDatasets, true);

    Button connectBtn = new Button("Connect Restricted Dataset", VaadinIcon.PLUS.create());
    connectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectBtn.addClickListener(e -> openConnectDialog(true));

    Button syncAllBtn = new Button("Sync All", VaadinIcon.REFRESH.create());
    syncAllBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    syncAllBtn.addClickListener(e -> syncAll(true));

    Div actions = new Div(connectBtn, syncAllBtn);
    actions.addClassNames("flex-horizontal", "gap-04", "padding-vertical-02");

    Div content = new Div(tokenBanner, actions, privateGrid);
    content.addClassNames("flex-vertical", "gap-03");
    section.add(header, content);
    return section;
  }

  /* ========================= Grid helpers ========================= */

  private Grid<ConnectedDataset> createConnectedDatasetGrid(List<ConnectedDataset> items,
      boolean restricted) {
    Grid<ConnectedDataset> grid = new Grid<>();
    grid.addColumn(ConnectedDataset::title).setHeader("Title").setKey("title")
        .setAutoWidth(true).setFlexGrow(1);
    grid.addColumn(ConnectedDataset::doi).setHeader("DOI").setKey("doi")
        .setAutoWidth(true);
    grid.addColumn(ConnectedDataset::platform).setHeader("Platform").setKey("platform")
        .setAutoWidth(true);
    grid.addColumn(ConnectedDataset::version).setHeader("Version").setKey("version")
        .setAutoWidth(true);
    grid.addColumn(ConnectedDataset::connectedBy).setHeader("Connected By").setKey("connectedBy")
        .setAutoWidth(true);
    grid.addColumn(new ComponentRenderer<>(ds -> buildRowActions(ds, restricted)))
        .setHeader("Actions").setKey("actions").setAutoWidth(true).setFlexGrow(0);

    grid.setItems(items);
    grid.addClassName("width-full");
    grid.setAllRowsVisible(true);
    return grid;
  }

  private Component buildRowActions(ConnectedDataset ds, boolean restricted) {
    Anchor anchor = new Anchor(ds.accessLink(), VaadinIcon.EXTERNAL_LINK.create());
    anchor.setTarget("_blank");
    anchor.addClassName("color-primary-text");

    Button syncBtn = new Button(VaadinIcon.REFRESH.create());
    syncBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    syncBtn.setTooltipText("Sync dataset with host platform");
    syncBtn.addClickListener(e -> syncDataset(ds, restricted));

    Button removeBtn = new Button(VaadinIcon.TRASH.create());
    removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    removeBtn.setTooltipText("Remove connection");
    removeBtn.addClickListener(e -> removeDataset(ds, restricted));

    Div actions = new Div(anchor, syncBtn, removeBtn);
    actions.addClassNames("flex-horizontal", "gap-02", "flex-align-items-center");
    return actions;
  }

  /* ========================= Actions ========================= */

  private void syncDataset(ConnectedDataset ds, boolean restricted) {
    String msgPrefix = restricted ? "Restricted" : "Public";
    Notification.show(msgPrefix + " dataset sync started: " + ds.title(), 3000,
        Position.MIDDLE);

    UI ui = UI.getCurrent();
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(1500);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }).thenRunAsync(() -> ui.access(() -> {
      // Simulate version bump
      String newVersion = ds.version().equals("v1.0") ? "v1.1" : "v1.0";
      ConnectedDataset updated = ds.withVersion(newVersion);
      if (restricted) {
        replaceInList(privateDatasets, updated);
      } else {
        replaceInList(publicDatasets, updated);
      }
      refreshGrids();
      Notification.show(
          msgPrefix + " dataset synced successfully. New version: " + newVersion, 4000,
          Position.BOTTOM_START);
    }));
  }

  private void syncAll(boolean restricted) {
    List<ConnectedDataset> target = restricted ? privateDatasets : publicDatasets;
    if (target.isEmpty()) {
      Notification.show("No datasets to sync.", 3000, Position.MIDDLE);
      return;
    }
    Notification.show("Syncing all %d %s datasets...".formatted(target.size(),
        restricted ? "restricted" : "public"), 3000, Position.MIDDLE);

    UI ui = UI.getCurrent();
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(1200);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }).thenRunAsync(() -> ui.access(() -> {
      Notification.show(
          "All %s datasets synced. %d updated.".formatted(restricted ? "restricted" : "public",
              target.size()), 4000, Position.BOTTOM_START);
    }));
  }

  private void removeDataset(ConnectedDataset ds, boolean restricted) {
    DatasetDeletionConfirmationDialog confirmDialog = new DatasetDeletionConfirmationDialog(
        ds.title());
    confirmDialog.open();
    confirmDialog.addConfirmListener(event -> {
      if (restricted) {
        privateDatasets.removeIf(it -> it.id().equals(ds.id()));
      } else {
        publicDatasets.removeIf(it -> it.id().equals(ds.id()));
      }
      refreshGrids();
      Notification.show("Dataset connection removed: " + ds.title(), 3000,
          Position.BOTTOM_START);
      confirmDialog.close();
    });
    confirmDialog.addCancelListener(event -> confirmDialog.close());
  }

  private void refreshGrids() {
    publicGrid.setItems(new ArrayList<>(publicDatasets));
    privateGrid.setItems(new ArrayList<>(privateDatasets));
    publicHeaderSpan.setText(
        "Public Datasets (%d)".formatted(publicDatasets.size()));
    privateHeaderSpan.setText(
        "Private / Restricted Datasets (%d)".formatted(privateDatasets.size()));
  }

  private void replaceInList(List<ConnectedDataset> list, ConnectedDataset updated) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).id().equals(updated.id())) {
        list.set(i, updated);
        return;
      }
    }
  }

  /* ========================= Connect Dialog ========================= */

  private void openConnectDialog(boolean restricted) {
    AppDialog dialog = AppDialog.large();
    String title = restricted ? "Connect Restricted Dataset" : "Connect Public Dataset";
    DialogHeader.with(dialog, title);

    // Platform selector
    ComboBox<String> platformBox = new ComboBox<>("InvenioRDM Instance");
    platformBox.setItems("Zenodo", "FDAT (University of Tübingen)");
    platformBox.setValue("Zenodo");
    platformBox.setWidthFull();

    TextField searchField = new TextField("Search term");
    searchField.setPlaceholder("Search by title, author, keyword...");
    searchField.setClearButtonVisible(true);
    searchField.setWidthFull();
    searchField.setValueChangeMode(ValueChangeMode.LAZY);

    Button searchBtn = new Button("Search", VaadinIcon.SEARCH.create());
    searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Div searchRow = new Div(platformBox, searchField, searchBtn);
    searchRow.addClassNames("flex-horizontal", "gap-04", "flex-align-items-end");

    // Results grid
    searchResultGrid = new Grid<>();
    searchResultGrid.addColumn(SearchResult::title).setHeader("Title").setKey("title")
        .setAutoWidth(true);
    searchResultGrid.addColumn(SearchResult::doi).setHeader("DOI").setKey("doi")
        .setAutoWidth(true);
    searchResultGrid.addColumn(SearchResult::platform).setHeader("Platform").setKey("platform")
        .setAutoWidth(true);
    searchResultGrid.addColumn(SearchResult::version).setHeader("Version").setKey("version")
        .setAutoWidth(true);
    searchResultGrid.setSelectionMode(Grid.SelectionMode.MULTI);
    searchResultGrid.setMinHeight(200, Unit.PIXELS);
    searchResultGrid.addClassName("width-full");

    Div resultsContainer = new Div(searchResultGrid);
    resultsContainer.addClassNames("flex-vertical", "gap-02");

    Div body = new Div(searchRow, resultsContainer);
    body.addClassNames("flex-vertical", "gap-04");
    DialogBody.withoutUserInput(dialog, body);

    DialogFooter.with(dialog, "Cancel", "Connect");

    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      var selected = searchResultGrid.getSelectedItems();
      if (selected.isEmpty()) {
        Notification.show("Please select at least one dataset to connect.", 3000,
            Position.MIDDLE);
        return;
      }
      for (SearchResult sr : selected) {
        ConnectedDataset newDs = new ConnectedDataset(sr.id(), sr.title(),
            "(description will be fetched on sync)", sr.doi(), sr.platform(), sr.version(),
            "Current User", sr.accessLink(), restricted, Instant.now());
        if (restricted) {
          privateDatasets.add(newDs);
        } else {
          publicDatasets.add(newDs);
        }
      }
      refreshGrids();
      Notification.show("Connected %d %s dataset(s).".formatted(selected.size(),
          restricted ? "restricted" : "public"), 3000, Position.BOTTOM_START);
      dialog.close();
    });

    // Initial "empty search" result: show paginated-like results
    performMockSearch("");

    searchBtn.addClickListener(e -> performMockSearch(searchField.getValue()));
    searchField.addValueChangeListener(e -> performMockSearch(e.getValue()));

    dialog.open();
  }

  private void performMockSearch(String term) {
    String lower = term == null ? "" : term.toLowerCase();
    List<SearchResult> results = mockSearchResults.stream()
        .filter(sr -> sr.title().toLowerCase().contains(lower)
            || sr.doi().toLowerCase().contains(lower)
            || lower.isBlank())
        .toList();
    searchResultGrid.setItems(results);
  }

  /* ========================= Inner classes ========================= */

  /**
   * Domain-like record representing a dataset already connected to the project.
   */
  private record ConnectedDataset(String id, String title, String description, String doi,
                                  String platform, String version, String connectedBy,
                                  String accessLink, boolean restricted, Instant connectedAt) {

    ConnectedDataset withVersion(String newVersion) {
      return new ConnectedDataset(id, title, description, doi, platform, newVersion, connectedBy,
          accessLink, restricted, connectedAt);
    }
  }

  /**
   * Domain-like record representing a search result from an InvenioRDM query.
   */
  private record SearchResult(String id, String title, String doi, String platform,
                              String version, String accessLink) {

  }

  /**
   * Reusable confirmation dialog for removing a dataset connection.
   */
  private static class DatasetDeletionConfirmationDialog extends NotificationDialog {

    DatasetDeletionConfirmationDialog(String datasetTitle) {
      super(NotificationLevel.WARNING);
      withTitle("Remove dataset connection");
      withContent(new Span(
          "Are you sure you want to remove the connection to '" + datasetTitle
              + "'? This does not delete the dataset on the remote platform."));
      setCancelable(true);
      setConfirmText("Remove");
    }
  }
}
