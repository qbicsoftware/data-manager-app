package life.qbic.projectmanagement.infrastructure.external.invenio;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for InvenioRDM instance registry.
 *
 * <p>Instances are admin-configured (ADR-0002 I2). Example
 * {@code application.properties}:</p>
 *
 * <pre>
 * qbic.external-service.invenio-rdm.instances[0].id=zenodo
 * qbic.external-service.invenio-rdm.instances[0].display-name=Zenodo (zenodo.org)
 * qbic.external-service.invenio-rdm.instances[0].base-url=https://zenodo.org
 * qbic.external-service.invenio-rdm.instances[1].id=fdat
 * qbic.external-service.invenio-rdm.instances[1].display-name=FDAT (fdat.uni-tuebingen.de)
 * qbic.external-service.invenio-rdm.instances[1].base-url=https://fdat.uni-tuebingen.de
 * </pre>
 *
 * @since 1.12.0
 */
@ConfigurationProperties(prefix = "qbic.external-service.invenio-rdm")
@Validated
public class InvenioRdmProperties {

  private List<InstanceEntry> instances = new ArrayList<>();

  public List<InstanceEntry> getInstances() {
    return instances;
  }

  public void setInstances(List<InstanceEntry> instances) {
    this.instances = instances;
  }

  public static class InstanceEntry {

    private String id;
    private String displayName;
    private String baseUrl;
    private String sourceType = "INVENIO_RDM";
    /**
     * Targeted API version of the Invenio REST API (see
     * <a href="https://inveniosoftware.github.io/invenio-openapi/">Invenio OpenAPI</a>).
     *
     * <p>Reserved for future per-instance adapter dispatch. The current
     * implementation ({@link InvenioRdmDatasetSource}) targets version
     * 12.0.0 of the spec. When an instance drifts to a different API
     * version, this field will be used by a version-aware factory to
     * route to a dedicated adapter implementation without impacting the
     * port contract.</p>
     *
     * <p>Default: "12".</p>
     */
    private String apiVersion = "12";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
  }
}
