package life.qbic.projectmanagement.infrastructure.sample.openbis;

import java.util.Objects;
import life.qbic.projectmanagement.infrastructure.DataManagerVault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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

  public DataManagerVault vault() {
    return vault;
  }

}
