package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.Objects;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@SpringComponent
@UIScope
public class OfferSearchComponent extends Composite<ComboBox<OfferPreview>> {

  private final OfferLookupService offerLookupService;

  public OfferSearchComponent(
      @Autowired OfferLookupService offerLookupService) {
    Objects.requireNonNull(offerLookupService);
    this.offerLookupService = offerLookupService;
    setItems();
    setRenderer();
    setLabels();
  }

  public Registration addValueChangeListener(
      ValueChangeListener<? super ComponentValueChangeEvent<ComboBox<OfferPreview>, OfferPreview>> listener) {
    return getContent().addValueChangeListener(listener);
  }

  public void clear() {
    getContent().clear();
  }

  private void setLabels() {
    getContent().setItemLabelGenerator(it -> it.offerId().id());
  }

  private void setRenderer() {
    getContent().setRenderer(new ComponentRenderer<>(OfferSearchComponent::textFromPreview));
  }

  private static Text textFromPreview(OfferPreview preview) {
    return new Text(preview.offerId().id() + ", " + preview.getProjectTitle().title());
  }

  private void setItems() {
    getContent().setItems(
        query -> offerLookupService
            .findOfferContainingProjectTitleOrId(
                query.getFilter().orElse(""),
                query.getFilter().orElse(""),
                query.getOffset(),
                query.getLimit())
            .stream());
  }

}
