package life.qbic.datamanager.signposting.http.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record RawLink(String rawURI, Map<String, RawParam> rawWebLinks) {

}
