package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.List;
import life.qbic.projectmanagement.domain.model.project.Contact;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Component;

@Component
public class ContactRepository {

  @PostFilter("hasAnyAuthority('ROLE_ADMIN')")
  public List<Contact> findAll() {
      return dummyContacts();
  }

  private static List<Contact> dummyContacts() {
    return new ArrayList<>(List.of(
        new Contact("Max Mustermann", "max.mustermann@qbic.uni-tuebingen.de"),
        new Contact("David Müller", "david.mueller@qbic.uni-tuebingen.de"),
        new Contact("John Koch", "john.koch@qbic.uni-tuebingen.de"),
        new Contact("Trevor Noah", "trevor.noah@qbic.uni-tuebingen.de"),
        new Contact("Sarah Connor", "sarah.connor@qbic.uni-tuebingen.de"),
        new Contact("Anna Bell", "anna.bell@qbic.uni-tuebingen.de"),
        new Contact("Sophia Turner", "sophia.turner@qbic.uni-tuebingen.de"),
        new Contact("Tylor Smith", "tylor.smith@qbic.uni-tuebingen.de")
    ));
  }


}
