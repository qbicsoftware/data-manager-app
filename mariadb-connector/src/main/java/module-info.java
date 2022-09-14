module life.qbic.mariadbconnector {
  requires spring.context;
  requires spring.beans;
  requires spring.data.commons;
  requires life.qbic.authentication;
  requires life.qbic.projectmanagement;
  requires spring.data.jpa;

  exports life.qbic.authentication.persistence;
}
