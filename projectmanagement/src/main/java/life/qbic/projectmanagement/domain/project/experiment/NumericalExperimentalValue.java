package life.qbic.projectmanagement.domain.project.experiment;

public interface NumericalExperimentalValue extends ExperimentalValue {

  Number getNumericalValue();

  class SimpleNumericalValue implements NumericalExperimentalValue{

    @Override
    public String unit() {
      return "cm";
    }

    @Override
    public String value() {
      return "10";
    }

    @Override
    public Number getNumericalValue() {
      return 10;
    }
  }

}
