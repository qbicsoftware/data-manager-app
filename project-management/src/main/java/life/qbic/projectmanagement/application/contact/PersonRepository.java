package life.qbic.projectmanagement.application.contact;

import java.util.List;
import life.qbic.projectmanagement.domain.model.project.Contact;


/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface PersonRepository {

  List<Contact> findAll(String query, int limit, int offset);

}
