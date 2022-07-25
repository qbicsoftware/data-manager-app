module mariadb.connector {
  requires spring.context;
  requires life.qbic.authentication;
  requires spring.beans;
  requires spring.data.commons;

  exports life.qbic.authentication.persistence;
}
