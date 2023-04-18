package life.qbic.datamanager.views.events;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

import java.io.Serial;

/**
 * <b>User Cancel Event</b>
 * <p>
 * An event that is fired by an components that indicates that a user wants to cancel the current
 * task within the component.
 *
 * @since 1.0.0
 */
public class UserCancelEvent<T extends Component> extends ComponentEvent<T> {
    @Serial
    private static final long serialVersionUID = 8068213210561270651L;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side.
     *
     * @param source     the source component
     */
    public UserCancelEvent(T source) {
        super(source, true);
    }

}
