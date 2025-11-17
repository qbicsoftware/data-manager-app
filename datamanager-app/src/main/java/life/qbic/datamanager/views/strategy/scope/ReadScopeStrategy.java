package life.qbic.datamanager.views.strategy.scope;

import java.util.Objects;
import life.qbic.datamanager.views.general.section.Controllable;


/**
 * <b>Read Scope Strategy</b>
 *
 * <p>This {@link UserScopeStrategy} implementation regulates passed
 * components of type {@link Controllable}</p> by disabling them, when the strategy is called via
 * {@link ReadScopeStrategy#execute()}.
 * <p>
 * Other related implementations: {@link WriteScopeStrategy}.
 *
 * @since 1.6.0
 */
public class ReadScopeStrategy implements UserScopeStrategy {

  private final Controllable component;

  public ReadScopeStrategy(Controllable component) {
    this.component = Objects.requireNonNull(component);
  }

  @Override
  public void execute() {
    component.disableControls();
  }
}
