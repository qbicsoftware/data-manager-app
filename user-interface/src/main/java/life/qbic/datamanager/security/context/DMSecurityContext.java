package life.qbic.datamanager.security.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DMSecurityContext {

  public static final String NAME = "DATA_MANAGER_SECURITY_CONTEXT";
  private final ArrayList<Object> principals;

  private DMSecurityContext(List<Object> principals) {
    this.principals = new ArrayList<>(principals);
  }

  public List<Object> principals() {
    return principals;
  }

  public boolean hasPrincipal(Class<?> clazz) {
    for (Object principal : principals) {
      if (clazz.isAssignableFrom(principal.getClass())) {
        return true;
      }
    }
    return false;
  }

  public Optional<Object> getPrincipal(Class<?> clazz) {
    for (Object principal : principals) {
      if (clazz.isAssignableFrom(principal.getClass())) {
        return Optional.of(principal);
      }
    }
    return Optional.empty();
  }

  public static class Builder {

    List<Object> principals;

    public Builder() {
      principals = new ArrayList<>();
    }

    public Builder addPrincipal(Object principal) {
      principals.add(principal);
      return this;
    }

    public DMSecurityContext build() {
      return new DMSecurityContext(principals);
    }

  }


}


