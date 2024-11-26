package life.qbic.datamanager.views.strategy.scope;

import java.util.Objects;
import life.qbic.datamanager.views.general.section.Controllable;

/**
 * <b>Write Scope Strategy</b>
 *
 * <p>This {@link UserScopeStrategy} implementation regulates passed
 * components of type {@link Controllable}</p> by enabling them, when the strategy is called via
 * {@link WriteScopeStrategy#execute()}.
 * <p>
 * Other related implementations: {@link ReadScopeStrategy}.
 *
 * @since 1.6.0
 */
public class WriteScopeStrategy implements UserScopeStrategy {

  private final Controllable component;

  public WriteScopeStrategy(Controllable component) {
    this.component = Objects.requireNonNull(component);
  }

  @Override
  public void execute() {
    component.enableControls();
  }
}
