package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UserCancelEvent<T extends Component> extends ComponentEvent<T> {
    @Serial
    private static final long serialVersionUID = 8068213210561270651L;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UserCancelEvent(T source, boolean fromClient) {
        super(source, fromClient);
    }
}
