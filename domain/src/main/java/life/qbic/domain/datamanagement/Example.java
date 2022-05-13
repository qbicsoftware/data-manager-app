package life.qbic.domain.datamanagement;

import java.util.Random;

public class Example {

  private static final int NUMBER_UPPER_BOUND = Integer.MAX_VALUE;

  public static Example create() {
    return new Example();
  }

  private Example() {
  }

  public int spitOutNumber() {
    Random random = new Random();
    return random.nextInt(NUMBER_UPPER_BOUND);
  }

}
