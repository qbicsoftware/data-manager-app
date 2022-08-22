module life.qbic.logging {
  requires org.slf4j;
  requires life.qbic.logging.subscription.api;

  exports life.qbic.logging.api;
  exports life.qbic.logging.service;
  exports life.qbic.logging.impl.logger;

}
