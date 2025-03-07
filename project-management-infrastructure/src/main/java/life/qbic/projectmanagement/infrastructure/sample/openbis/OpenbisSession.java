package life.qbic.projectmanagement.infrastructure.sample.openbis;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;
import life.qbic.projectmanagement.infrastructure.DataManagerVault;

/**
 * A management object that handles the connection to the openBIS backend via its API.
 * <p>
 * This object will handle the session of openBIS required to interact with the backend and
 * refreshes it once its lifetime has ended.
 * <p>
 * On the openBIS server side, there is expected to be one session alive for every data manager
 * instance running, and it should be closed, when the application terminates.
 * <p>
 * Inside the {@link OpenbisSession} object, a reference to a protected {@link DataManagerVault}
 * instance is used to store active tokens for a session, as well as the openBIS credentials to
 * acquire a new one.
 *
 * @since 1.8.0
 */
public class OpenbisSession {

  public static final String COULD_NOT_AUTHENTICATE_WITH_OPEN_BIS = "Could not authenticate with OpenBIS";
  private static final Logger log = logger(OpenbisSession.class);
  private static final String TOKEN_ALIAS = "OBIS_TOKEN";
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
      return HttpInvokerUtils.createStreamSupportingServiceStub(IDataStoreServerApi.class, url,
          100_000L);
    }

  }
}
