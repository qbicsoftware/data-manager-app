module webapp {
  requires life.qbic.authentication;
  requires vaadin.ordered.layout.flow;
  requires vaadin.app.layout.flow;
  requires spring.context;
  requires spring.boot.autoconfigure;
  requires flow.server;
  requires spring.boot;
  requires org.slf4j;
  requires vaadin.spring;
  requires spring.security.config;
  requires spring.security.crypto;
  requires spring.security.core;
  requires spring.security.web;
  requires spring.beans;
  requires vaadin.button.flow;
  requires org.apache.logging.log4j;
  requires flow.html.components;
  requires vaadin.text.field.flow;
  requires java.annotation;
  requires newsreader;
  requires vaadin.notification.flow;
  requires vaadin.icons.flow;
  requires vaadin.login.flow;
  requires org.apache.commons.lang3;
  requires org.apache.tomcat.embed.core;
  requires life.qbic.broadcasting;

  exports life.qbic.datamanager;
}
