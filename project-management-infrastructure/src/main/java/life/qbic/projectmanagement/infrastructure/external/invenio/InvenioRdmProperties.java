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

  /**
   * Configuration entry for a single InvenioRDM instance.
   *
   * <p>Bound from indexed properties such as
   * {@code qbic.external-service.invenio-rdm.instances[0].id}.</p>
   *
   * <p>Fields {@code sourceType} and {@code apiVersion} are optional
   * (nullable). Callers must resolve defaults:
   * <ul>
   *   <li>{@code sourceType} → {@literal "INVENIO_RDM"}</li>
   *   <li>{@code apiVersion} → {@literal "12"}</li>
   * </ul></p>
   */
  public record InstanceEntry(
      String id,
      String displayName,
      String baseUrl,
      String sourceType,
      String apiVersion
  ) {}
}
