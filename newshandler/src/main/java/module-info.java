module life.qbic.newshandler {
  requires spring.beans;
  requires spring.context;
  requires jakarta.mail;
  requires life.qbic.commons;
  requires life.qbic.logging;
  requires logback.classic;

  exports life.qbic.newshandler.usermanagement.registration;
  exports life.qbic.newshandler.usermanagement.email;
  exports life.qbic.newshandler.usermanagement.passwordreset;

  opens life.qbic.newshandler.usermanagement.email to spring.core;

}
