module newsreader {
  requires spring.beans;
  requires spring.context;
  requires jakarta.mail;
  exports life.qbic.newsreader.usermanagement.registration;
  exports life.qbic.newsreader.usermanagement.email;
  exports life.qbic.newsreader.usermanagement.passwordreset;
}
