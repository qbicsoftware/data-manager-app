module life.qbic.finances {
  requires spring.context;
  requires spring.beans;
  requires spring.data.commons;

  // We need that to have JPA support
  requires spring.boot.starter.data.jpa;
  requires spring.data.jpa;
  // We need that to have persistence support with the default Hibernate usage
  requires org.hibernate.orm.core;
  requires java.persistence;


  opens life.qbic.projectmanagement.application.finances.offer to org.hibernate.orm.core, spring.core;
  exports life.qbic.projectmanagement.application.finances.offer;
  exports life.qbic.projectmanagement.domain.finances.offer;
  opens life.qbic.projectmanagement.domain.finances.offer to org.hibernate.orm.core, spring.core;
}
