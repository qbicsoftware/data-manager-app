open module life.qbic.vaadinfrontend {
  requires life.qbic.newshandler;
  requires spring.boot.autoconfigure;
  requires spring.boot;
  requires spring.context;
  requires org.slf4j;
  requires life.qbic.authentication;
  requires flow.server;
  requires life.qbic.broadcasting;
  requires vaadin.ordered.layout.flow;
  requires vaadin.app.layout.flow;
  requires flow.html.components;
  requires spring.security.core;
  requires vaadin.button.flow;
  requires spring.security.crypto;
  requires spring.security.web;
  requires spring.beans;
  requires vaadin.icons.flow;
  requires vaadin.spring;
  requires spring.security.config;
  requires java.annotation;
  requires vaadin.text.field.flow;
  requires vaadin.notification.flow;
  requires vaadin.login.flow;
  requires org.apache.commons.lang3;
  requires org.apache.logging.log4j;
  requires spring.core;
  requires spring.web;
  requires org.apache.tomcat.embed.core;
  requires spring.data.commons;
  requires life.qbic.mariadbconnector;
  requires spring.data.jpa;

  exports life.qbic.datamanager;

}
