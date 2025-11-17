package life.qbic.datamanager.views.projects.project.measurements;


public enum MeasurementProperty {

  MEASUREMENT_ID("measurement id");

  private final String label;

  MeasurementProperty(String label) {
    this.label = label;
  }


  public String label() {
    return this.label;
  }

}
