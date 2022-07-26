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

  exports life.qbic.authentication.domain.user.repository.jpa to spring.beans;

  // We need that to have JPA support
  requires spring.boot.starter.data.jpa;
  requires spring.data.jpa;
  // We need that to have persistence support with the default Hibernate usage
  requires org.hibernate.orm.core;

  opens life.qbic.authentication.domain.user.concept;

}
