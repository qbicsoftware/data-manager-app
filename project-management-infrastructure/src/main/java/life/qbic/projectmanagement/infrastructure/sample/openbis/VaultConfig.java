package life.qbic.projectmanagement.infrastructure.sample.openbis;

import java.util.Objects;
import life.qbic.projectmanagement.infrastructure.DataManagerVault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Initialises credentials for the {@link DataManagerVault} to be used in the application.
 * <p>
 * To make sure any object gets the initialised vault, inject the {@link VaultConfig} component.
 * <p>
 * In order to simplify access to secrets, it is advised to export their vault alias as constants
 * (e.g. {@link VaultConfig#OPENBIS_USER_ALIAS}).
 *
 * @since 1.8.0
 */
@Component
public class VaultConfig {

  public static final String OPENBIS_USER_ALIAS = "OBIS_USER";
  public static final String OPENBIS_PASSWORD = "OBIS_PASSWORD";
  private final DataManagerVault vault;

  public VaultConfig(
      @Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Autowired DataManagerVault vault) {
    Objects.requireNonNull(vault);
    vault.add(OPENBIS_USER_ALIAS, userName);
    vault.add(OPENBIS_PASSWORD, password);
    this.vault = vault;
    userName = "";
    password = "";
  }

  /**
   * Access to the initialised {@link DataManagerVault}.
   *
   * @return vault with credentials
   * @since 1.8.0
   */
  public DataManagerVault vault() {
    return vault;
  }

}
