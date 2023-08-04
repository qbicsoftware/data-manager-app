package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ComponentEventListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.create.DefineExperimentComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentInformationContent;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

/**
 * <b>ExperimentInformationDialog</b>
 *
 * <p>Dialog to create or edit experiment information by providing the minimal required information
 * in the {@link DefineExperimentComponent}</p>
 *
 * @since 1.0.0
 */

public class ExperimentInformationDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 2142928219461555700L;
  private final String TITLE = "Experimental Design";
  private final DefineExperimentComponent defineExperimentComponent;
  private final List<ComponentEventListener<CancelEvent<ExperimentInformationDialog>>> cancelEventListeners = new ArrayList<>();
  private final List<ComponentEventListener<ConfirmEvent<ExperimentInformationDialog>>> confirmEventListeners = new ArrayList<>();
  private final MODE mode;

  public ExperimentInformationDialog(
      ExperimentalDesignSearchService experimentalDesignSearchService) {
    this(experimentalDesignSearchService, false);
  }

  private ExperimentInformationDialog(
      ExperimentalDesignSearchService experimentalDesignSearchService, boolean mode) {
    super();
    this.mode = mode ? MODE.EDIT : MODE.ADD;
    addClassName("experiment-information-dialog");
    defineExperimentComponent = new DefineExperimentComponent(experimentalDesignSearchService);
    layoutComponent();
    initDialogueContent();
    configureComponent();
  }

  private void layoutComponent() {
    setHeaderTitle(TITLE);
    add(defineExperimentComponent);
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    final DialogFooter footer = getFooter();
    footer.add(this.cancelButton, this.confirmButton);
  }

  private void initDialogueContent() {
    add(defineExperimentComponent);
  }

  private void configureComponent() {
    configureCancelling();
    configureConfirmation();
  }

  /**
   * Creates a new dialog prefilled with experiment information.
   *
   * @param experimentalDesignSearchService the service providing the selectable options for the
   *                                        analyte, specimen and species within this dialog
   * @param experimentName                  experimentName to be preset within the dialog
   * @param species                         List of {@link Species} to be preset within the dialog
   * @param specimen                        List of {@link Specimen} to be preset within the dialog
   * @param analytes                        List of {@link Analyte} to be preset within the dialog
   * @return a new instance of the dialog
   */
  public static ExperimentInformationDialog prefilled(
      ExperimentalDesignSearchService experimentalDesignSearchService,
      String experimentName, Collection<Species> species, Collection<Specimen> specimen,
      Collection<Analyte> analytes) {
    return editDialog(experimentalDesignSearchService, experimentName, species, specimen, analytes);
  }

  private static ExperimentInformationDialog editDialog(
      ExperimentalDesignSearchService experimentalDesignSearchService, String experimentName,
      Collection<Species> species, Collection<Specimen> specimen, Collection<Analyte> analytes) {
    ExperimentInformationDialog experimentInformationDialog = new ExperimentInformationDialog(
        experimentalDesignSearchService, true);
    experimentInformationDialog.setExperimentInformation(experimentName, species, specimen,
        analytes);
    return experimentInformationDialog;
  }

  private void setExperimentInformation(String experimentName,
      Collection<Species> species, Collection<Specimen> specimen, Collection<Analyte> analytes) {
    defineExperimentComponent.setExperimentInformation(experimentName, species, specimen, analytes);
  }

  private boolean isInputValid() {
    return defineExperimentComponent.isValid();
  }

  private void configureConfirmation() {
    this.confirmButton.addClickListener(event -> fireConfirmEvent());
  }

  private void configureCancelling() {
    this.cancelButton.addClickListener(cancelListener -> fireCancelEvent());
  }

  private void fireConfirmEvent() {
    if (isInputValid()) {
      this.confirmEventListeners.forEach(
          listener -> listener.onComponentEvent(new ConfirmEvent<>(this, true)));
    }
  }

  private void fireCancelEvent() {
    this.cancelEventListeners.forEach(
        listener -> listener.onComponentEvent(new CancelEvent<>(this, true)));
  }


  /**
   * Adds a listener for {@link ConfirmEvent}s
   *
   * @param listener the listener to add
   */
  public void addConfirmEventListener(
      final ComponentEventListener<ConfirmEvent<ExperimentInformationDialog>> listener) {
    this.confirmEventListeners.add(listener);
  }

  /**
   * Adds a listener for {@link CancelEvent}s
   *
   * @param listener the listener to add
   */
  public void addCancelEventListener(
      final ComponentEventListener<CancelEvent<ExperimentInformationDialog>> listener) {
    this.cancelEventListeners.add(listener);
  }

  /**
   * Provides the content set in the fields of this dialog
   *
   * @return {@link ExperimentInformationContent} providing the information filled by the user
   * within this dialog
   */
  public ExperimentInformationContent content() {
    return new ExperimentInformationContent(
        defineExperimentComponent.experimentNameField.getValue(),
        defineExperimentComponent.speciesBox.getValue().stream().toList(),
        defineExperimentComponent.specimenBox.getValue().stream().toList(),
        defineExperimentComponent.analyteBox.getValue().stream().toList());
  }

  private enum MODE {
    ADD, EDIT
  }
}
