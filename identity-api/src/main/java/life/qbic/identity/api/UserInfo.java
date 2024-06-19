package life.qbic.identity.api;

import java.io.Serializable;

/**
 * <b>User Information DTO</b>
 *
 * <p>DTO to exchange user information between the user domain and the client calling a service</p>
 *
 * @since 1.0.0
 */
public record UserInfo(String id, String fullName, String emailAddress, String platformUserName,
                       String encryptedPassword,
                       boolean isActive) implements Serializable {

}
