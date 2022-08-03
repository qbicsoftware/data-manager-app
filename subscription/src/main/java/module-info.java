module life.qbic.logging.subscription {
  requires life.qbic.logging.subscription.api;

  provides life.qbic.logging.subscription.api.Subscriber
      with life.qbic.logging.subscription.impl.EmailOnErrorSubscriber;
}

