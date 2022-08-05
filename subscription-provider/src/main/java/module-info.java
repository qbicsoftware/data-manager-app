module life.qbic.logging.subscription.provider {
  requires life.qbic.logging.subscription.api;
  requires jakarta.mail;
  requires org.slf4j;

  provides life.qbic.logging.subscription.api.Subscriber
      with life.qbic.logging.subscription.provider.EmailOnErrorSubscriber;
}

