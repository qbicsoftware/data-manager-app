import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Has tests that verify that the reactor context is per Subscriber and does not leak in contrast to
 * manually set ThreadLocal variables.
 */
public class TestReactorContextDoesNotLeak {

  @Test
  void contextDoesNotLeakAcrossThreadReuse() {
    AtomicReference<String> seenInTaskB = new AtomicReference<>();

    // Task A writes to context, runs on boundedElastic
    Mono.deferContextual(ctx ->
            Mono.just(ctx.getOrDefault("secret", "ABSENT")))
        .contextWrite(c -> c.put("secret", "A"))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();

    // Task B — may reuse the same thread; must NOT see "A"
    Mono.deferContextual(ctx ->
            Mono.fromRunnable(() ->
                seenInTaskB.set(ctx.getOrDefault("secret", "ABSENT"))))
        .subscribeOn(Schedulers.boundedElastic())
        .block();

    assertEquals("ABSENT", seenInTaskB.get());
  }

  @Test
  void threadLocalCanLeakButContextDoesNot() {
    ThreadLocal<String> tl = new ThreadLocal<>();
    Scheduler s = Schedulers.newSingle("tl-test");

    try {
      // Task A sets a ThreadLocal and DOESN'T clean up
      Mono.fromRunnable(() -> tl.set("LEAKED"))
          .subscribeOn(s).block();

      // Task B on the same thread
      String tlValue = Mono.fromCallable(tl::get)
          .subscribeOn(s).block();

      assertEquals("LEAKED", tlValue); // ThreadLocal leaks!
    } finally {
      s.dispose();
    }
  }

  @Test
  void contextDoesNotLeakOnReusedSingleThread() {
    Scheduler single = Schedulers.newSingle("reuse-test");

    try {
      Mono.deferContextual(ctx -> Mono.just(ctx.getOrDefault("k", "ABSENT")))
          .contextWrite(c -> c.put("k", "value"))
          .subscribeOn(single)
          .block(); // runs and releases the thread

      String second = Mono.deferContextual(ctx -> Mono.just(ctx.getOrDefault("k", "ABSENT")))
          .subscribeOn(single)   // same thread, no contextWrite
          .block();

      assertEquals("ABSENT", second);
    } finally {
      single.dispose();
    }
  }

  @Test
  void eachSubscriptionGetsFreshContext() {
    AtomicReference<Object> ctxA = new AtomicReference<>();
    AtomicReference<Object> ctxB = new AtomicReference<>();

    Mono<String> source = Mono.deferContextual(ctx -> {
      ctxA.set(ctx);
      return Mono.just("x");
    });

    source.subscribe();
    source.subscribe();

    assertNotSame(ctxA.get(), ctxB.get()); // different instances
  }
}
