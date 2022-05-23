package life.qbic.domain.usermanagement.registration;

/**
 * Output interface for the user registration use case
 *
 * @since 1.0.0
 */
public interface RegisterUserOutput {

    /**
     * Callback is executed, when the user registration has been successful.
     *
     * @since 1.0.0
     */
    void onSuccess();

    /**
     * Callback is executed, when the user registration failed.
     *
     * @param reason the reason for the user registration failure
     * @since 1.0.0
     */
    void onFailure(String reason);

}
