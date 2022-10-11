package life.qbic.datamanager.views.project.create;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public record ProjectLinkComponent(String type, String reference) {

  public ProjectLinkComponent(String type, String reference){
    this.type = type;
    this.reference = reference;
  }
}
