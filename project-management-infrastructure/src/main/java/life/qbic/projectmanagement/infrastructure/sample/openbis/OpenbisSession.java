package life.qbic.projectmanagement.infrastructure.sample.openbis;

import static life.qbic.logging.service.LoggerFactory.logger;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.infrastructure.DataManagerVault;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class OpenbisSession {

  private static final Logger log = logger(OpenbisSession.class);

  private static final String TOKEN_ALIAS = "OBIS_TOKEN";
  public static final String COULD_NOT_AUTHENTICATE_WITH_OPEN_BIS = "Could not authenticate with OpenBIS";
  private final DataManagerVault vault;
  private final IApplicationServerApi apiV3;

  public OpenbisSession(DataManagerVault vault, String applicationServerUrl) {
    this.vault = Objects.requireNonNull(vault);
    this.apiV3 = ApiV3.applicationServer(applicationServerUrl);
    this.login();
  }

  private void login() {
    String token = apiV3.login(vault.read(VaultConfig.OPENBIS_USER_ALIAS).orElseThrow(),
        vault.read(VaultConfig.OPENBIS_PASSWORD).orElseThrow());

    if (token == null) {
      throw new OpenBisSessionException(COULD_NOT_AUTHENTICATE_WITH_OPEN_BIS);
    }

    vault.add(TOKEN_ALIAS, token);
    if (!apiV3.isSessionActive(vault.read(TOKEN_ALIAS).orElseThrow())) {
      throw new OpenBisSessionException(COULD_NOT_AUTHENTICATE_WITH_OPEN_BIS);
    }
    log.debug("Successfully authenticated with OpenBIS");
  }

  public String getToken() throws OpenBisSessionException {
    var token = vault.read(TOKEN_ALIAS).orElseThrow();
    if (!apiV3.isSessionActive(token)) {
      login();
      token = vault.read(TOKEN_ALIAS).orElseThrow(() -> new OpenBisSessionException(
          COULD_NOT_AUTHENTICATE_WITH_OPEN_BIS));
    }
    return token;
  }

  public void logout() throws OpenBisSessionException {
    apiV3.logout(vault.read(TOKEN_ALIAS).orElseThrow());
  }


  public static class OpenBisSessionException extends RuntimeException {

    public OpenBisSessionException(String message) {
      super(message);
    }
  }

  public static class ApiV3 {

    private ApiV3() {
    }

    public static IApplicationServerApi applicationServer(String url) {
      return HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, url, 100_000L);
    }
    public static IDataStoreServerApi dataStoreServer(String url) {
      return HttpInvokerUtils.createStreamSupportingServiceStub(IDataStoreServerApi.class, url, 100_000L);
    }

  }
}
