open module life.qbic.logging {
  requires org.slf4j;
  requires logback.core;
  requires org.spockframework.core;
  requires org.apache.groovy;
  requires java.desktop;

  exports life.qbic.logging.impl.slf4j;
}
