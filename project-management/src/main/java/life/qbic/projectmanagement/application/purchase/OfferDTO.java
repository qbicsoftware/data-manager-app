package life.qbic.projectmanagement.application.purchase;

import java.nio.charset.Charset;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record OfferDTO(String offerId, boolean signed, String fileName, Byte[] content,
                       Charset charset) {

}
