module life.qbic.logging.subscription.provider {
  requires life.qbic.logging.subscription.api;

  provides life.qbic.logging.subscription.api.Subscriber
      with life.qbic.logging.subscription.provider.EmailOnErrorSubscriber;
}

