package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface HasValidators<T> {

  void addValidator(Validator<T> validator);

  void removeValidator(Validator<T> validator);

  ValidationResult applyValidators();

}
