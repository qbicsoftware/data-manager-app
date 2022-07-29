module life.qbic.newshandler {
  requires spring.beans;
  requires spring.context;
  requires life.qbic.commons;
  requires life.qbic.logging;
  requires jakarta.mail;
  requires jakarta.activation;

  exports life.qbic.newshandler.usermanagement.registration;
  exports life.qbic.newshandler.usermanagement.email;
  exports life.qbic.newshandler.usermanagement.passwordreset;

  opens life.qbic.newshandler.usermanagement.email to spring.core;

}
