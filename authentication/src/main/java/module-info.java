open module life.qbic.authentication {
  requires org.slf4j;
  requires java.persistence;

  exports life.qbic.identity.domain.user.concept;
  exports life.qbic.identity.application;
  exports life.qbic.identity.domain.user.repository;
  exports life.qbic.identity.domain.user.event;
  exports life.qbic.identity.domain.user.policy;
  exports life.qbic.identity.application.user.registration;
  exports life.qbic.identity.application.user.password;
  exports life.qbic.identity.application.notification;
  exports life.qbic.identity.domain.event;
  exports life.qbic.identity.domain;
}
