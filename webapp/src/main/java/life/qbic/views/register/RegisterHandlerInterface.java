package life.qbic.views.register;

/**
 * <b> Interface to register the {@link RegisterLayout} to the {@link RegisterHandler}. </b>
 *
 * @since 1.0.0
 */
public interface RegisterHandlerInterface {

    /**
     * Registers a {@link RegisterLayout} to an implementing class
     * @param registerLayout The view that is being registerd
     * @return true, if registration was successful
     */
    boolean register(RegisterLayout registerLayout);
}
