package life.qbic.datamanager.views.general;

/**
 * <b>Allowed File Extension</b>
 *
 * <p>Enumeration of all allowed File extensions in the context of the Quality Control upload.
 * Additionally provides the MIME_type and a short description as outlined by the mozilla docs <a
 * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types">...</a>
 * </p>
 */
public enum AllowedFileExtension {

  EXCEL(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "Microsoft Excel (OpenXML)"),
  PDF(".pdf", "application/pdf",
      "Adobe Portable Document Format (PDF)"),
  WORD(
      ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "Microsoft Word (OpenXML)");

  private final String extension;
  private final String mimeType;
  private final String description;


  AllowedFileExtension(String extension, String mimeType, String description) {
    this.extension = extension;
    this.mimeType = mimeType;
    this.description = description;
  }

  public String extension() {
    return extension;
  }

  public String mimetype() {
    return mimeType;
  }

  public String description() {
    return description;
  }
}
