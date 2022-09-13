module life.qbic.mariadbconnector {
  requires spring.context;
  requires spring.beans;
  requires spring.data.commons;
  requires life.qbic.authentication;
  requires spring.data.jpa;
  requires life.qbic.finances;

  exports life.qbic.authentication.persistence;
  exports life.qbic.finance.persistence to spring.beans;
}
