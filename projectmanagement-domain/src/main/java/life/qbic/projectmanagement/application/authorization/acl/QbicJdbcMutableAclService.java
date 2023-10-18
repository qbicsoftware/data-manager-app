package life.qbic.projectmanagement.application.authorization.acl;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component("qbic")
public class QbicJdbcMutableAclService extends JdbcMutableAclService implements MutableAclService {

  public QbicJdbcMutableAclService(DataSource dataSource, LookupStrategy lookupStrategy,
      AclCache aclCache) {
    super(dataSource, lookupStrategy, aclCache);
  }

  public MutableAcl createAcl(ObjectIdentity objectIdentity, List<Sid> sids) throws AlreadyExistsException {
    Assert.notNull(objectIdentity, "Object Identity required");
    if (this.retrieveObjectIdentityPrimaryKey(objectIdentity) != null) {
      throw new AlreadyExistsException("Object identity '" + objectIdentity + "' already exists");
    } else {
      for(Sid sid : sids) {
        this.createObjectIdentity(objectIdentity, sid);
      }
      Acl acl = this.readAclById(objectIdentity);
      Assert.isInstanceOf(MutableAcl.class, acl, "MutableAcl should be been returned");
      System.err.println(acl);
      return (MutableAcl)acl;
    }
  }
}
