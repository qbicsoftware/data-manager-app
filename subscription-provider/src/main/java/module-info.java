module life.qbic.logging.subscription.provider {
  requires life.qbic.logging.subscription.api;
  requires org.slf4j;
  requires java.mail;

  provides life.qbic.logging.subscription.api.Subscriber
      with life.qbic.logging.subscription.provider.MailOnErrorSubscriber;
}

