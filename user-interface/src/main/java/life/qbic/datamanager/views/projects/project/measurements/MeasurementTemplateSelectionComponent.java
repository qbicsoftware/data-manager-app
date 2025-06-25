package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.views.general.DomainSelectionComponent;
import life.qbic.datamanager.views.general.DomainSelectionEvent;
import life.qbic.datamanager.views.general.download.DownloadComponent;

/**
 * <b>Measurement Template Selection Component</b>
 * <p>
 * A {@link DomainSelectionComponent} that provides a
 * {@link com.vaadin.flow.component.radiobutton.RadioButtonGroup} to select a domain a measurement
 * template shall be made available.
 * <p>
 * The template itself will be generated and made available via a download button, and the content
 * of the template based on the selected domain.
 * <p>
 * The component also lets clients register to {@link DomainSelectionEvent} that is fired every time
 * the user makes changes in the domain selection.
 *
 * @since 1.11.0
 */
public class MeasurementTemplateSelectionComponent extends Div implements
    DomainSelectionComponent<Div, DomainSelectionEvent<Div>> {

  private final Map<Domain, DownloadStreamProvider> downloadProviders;
  private Domain selectedDomain;
  private DownloadComponent downloadComponent = new DownloadComponent();
  private RadioButtonGroup<String> domainOptions;

  private final List<ComponentEventListener<DomainSelectionEvent<Div>>> listeners = new ArrayList<>();

  /**
   * @param downloadProviders the available download providers with templates for different domain.
   *                          The {@link Map} must provide {@link DownloadStreamProvider} for every
   *                          {@link Domain}, else an {@link IllegalArgumentException} will be
   *                          thrown.
   * @since 1.11.0
   */
  public MeasurementTemplateSelectionComponent(
      Map<Domain, DownloadStreamProvider> downloadProviders) {
    Objects.requireNonNull(downloadProviders);
    if (hasMissingDomains(downloadProviders.keySet())) {
      throw new IllegalArgumentException("Not all domains have a download provider");
    }
    this.downloadProviders = Map.copyOf(downloadProviders);
    var templateDownloadButton = new Button("Download metadata template");
    templateDownloadButton.addClickListener(event -> {
      triggerDownloadBasedOnSelection(selectedDomain);
    });
    this.domainOptions = createRadioButtonGroup(
        Arrays.stream(Domain.values()).map(Domain::name).toList());
    // every time the user changes the domain, we update the state and fire an event to inform registered listeners
    domainOptions.addValueChangeListener(event -> {
      setSelectedDomainFromString(event.getValue());
      fireEvent(new DomainSelectionEvent<>(this, true));
    });

    add(domainOptions);
    add(templateDownloadButton);
    add(downloadComponent);

    setDefaultSelection(Domain.Genomics);
  }

  private void setDefaultSelection(Domain domain) {
    domainOptions.setValue(domain.name());
    setSelectedDomainFromString(domain.name());
  }

  private void setSelectedDomainFromString(String selectedDomain) {
    this.selectedDomain = Domain.valueOf(selectedDomain);
  }

  public void setSelectedDomain(Domain selectedDomain) {
    setDefaultSelection(selectedDomain);
  }

  private static RadioButtonGroup<String> createRadioButtonGroup(List<String> options) {
    var radioButtonGroup = new RadioButtonGroup();
    radioButtonGroup.setItems(options);
    radioButtonGroup.setLabel("Select a domain button");
    return radioButtonGroup;
  }

  private static boolean hasMissingDomains(Collection<Domain> domains) {
    return !containsAllDomains(domains);
  }

  private static boolean containsAllDomains(Collection<Domain> domains) {
    var confirmedDomains = new EnumMap<Domain, Boolean>(Domain.class);
    // 1. Init the lookup table with all observations set to false
    for (Domain domain : Arrays.stream(Domain.values()).toList()) {
      confirmedDomains.put(domain, false);
    }
    // 2. Record observed domains
    for (Domain domain : domains) {
      confirmedDomains.put(domain, true);
    }
    return !confirmedDomains.containsValue(false);
  }

  private void triggerDownloadBasedOnSelection(Domain domain) {
    var provider = downloadProviders.get(domain);
    // Failsafe, if passed provider is null
    if (provider == null) {
      throw new IllegalStateException("No download provider found for " + selectedDomain);
    }
    downloadFromProvider(provider);
  }

  private void downloadFromProvider(DownloadStreamProvider provider) {
    Objects.requireNonNull(provider);
    getUI().ifPresent(ui -> ui.access(() -> downloadComponent.trigger(provider)));
  }

  @Override
  public Registration addDomainSelectionListener(
      ComponentEventListener<DomainSelectionEvent<Div>> listener) {
    return ComponentUtil.addListener(this, DomainSelectionEvent.class,
        (ComponentEventListener) listener);
  }


  public Domain selectedDomain() {
    return selectedDomain;
  }

  public enum Domain {
    Genomics, Proteomics
  }
}
