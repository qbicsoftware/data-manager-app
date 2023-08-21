package life.qbic.datamanager.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.stream.IntStream;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.transaction.annotation.Transactional;

//@Component
public class ProjectAclService implements Serializable {

  @Serial
  private static final long serialVersionUID = -1200723780637333207L;
  private final MutableAclService aclService;

  public ProjectAclService(@Autowired MutableAclService aclService) {
    this.aclService = aclService;
  }

  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.project.Project', 'DELETE')")
  public void removeProject(ProjectId projectId) {
    //FixME Delete Project
    ObjectIdentity identity = new ObjectIdentityImpl(Project.class, projectId);
    aclService.deleteAcl(identity, true);
  }

  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.project.Project', 'CREATE')")
  public void createProject(ProjectId projectId) {

  }

  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.project.Project', 'WRITE')")
  public void updateProject(ProjectId projectId) {

  }

  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.project.Project', 'READ')")
  public void accessProject(ProjectId projectId) {

  }

  @Transactional
  public boolean updateACL(ProjectId projectId, List<?> permissions) {
    if (null == permissions || permissions.isEmpty()) {
      return false;
    }

    ObjectIdentity oi = new ObjectIdentityImpl(Project.class, projectId);
    MutableAcl acl;

    try {
      acl = (MutableAcl) aclService.readAclById(oi);
      if (acl != null) {
        clearACEs(acl);
      }
    } catch (NotFoundException ex) {
      acl = aclService.createAcl(oi);
    }

    for (var setting : permissions) {
      //FixMe what should happen here
    }
    aclService.updateAcl(acl);
    return true;
  }

  private void clearACEs(MutableAcl acl) {
    try {
      int count = acl.getEntries().size();
      IntStream.iterate(count - 1, index -> index >= 0, index -> index + 1).forEach(acl::deleteAce);
    } catch (NotFoundException ex) {
      //ToDo What should happen here
    }
  }


}
