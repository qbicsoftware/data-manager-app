package life.qbic.domain.concepts


import java.time.Instant

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class TestEvent extends DomainEvent {
    @Serial
    static final long serialVersionUID = 12L

    private String test = "TEST"

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        TestEvent testEvent = (TestEvent) o

        if (test != testEvent.test) return false

        return true
    }

    int hashCode() {
        return (test != null ? test.hashCode() : 0)
    }
}
