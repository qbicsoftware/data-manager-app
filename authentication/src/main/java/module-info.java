module life.qbic.authentication {
  requires org.slf4j;
  requires java.persistence;
  requires life.qbic.broadcasting;

  exports life.qbic.authentication.domain.user.concept;
  exports life.qbic.authentication.application;
  exports life.qbic.authentication.domain.user.repository;
  exports life.qbic.authentication.domain.user.event;
  exports life.qbic.authentication.domain.user.policy;
  exports life.qbic.authentication.application.user.registration;
  exports life.qbic.authentication.application.user.password;
  exports life.qbic.authentication.application.notification;
  exports life.qbic.authentication.domain.event;
  exports life.qbic.authentication.domain.registry;
  exports life.qbic.authentication.domain.policy;
}
