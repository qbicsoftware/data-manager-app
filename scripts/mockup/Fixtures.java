import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Seeds the mock-up database with accounts and projects.
 *
 * <p>Every screen worth looking at is backed by rows, so an empty database gives
 * an agent empty grids and nothing to review. This writes a small, predictable
 * dataset: two accounts, and projects deliberately uneven -- one empty, one with
 * a single sample, one with enough rows to exercise pagination.</p>
 *
 * <p>Passwords are hashed exactly as
 * {@code life.qbic.identity.domain.model.PasswordEncryptionPolicy} does
 * (PBKDF2WithHmacSHA256, 100000 iterations, 20-byte salt, 256-bit key, stored as
 * {@code iterations:hexSalt:hexHash}). Getting this wrong is invisible: the
 * account exists, and login silently fails.</p>
 */
public class Fixtures {

  static final String DEV_EMAIL = "dev@qbic.local";
  static final String DEV_PASSWORD = "DevPassword123!";
  static final String ADMIN_EMAIL = "admin@qbic.local";
  static final String ADMIN_PASSWORD = "AdminPassword123!";

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("usage: Fixtures <schema.sql-of-default-values> <db-file>");
      System.exit(1);
    }
    Path defaults = Path.of(args[0]);
    String dbFile = args[1];

    // No SCHEMA= in the URL: it cannot be selected on a database that does not
    // exist yet. MockDatabase creates it; this only selects it. The path must be
    // absolute, which H2 enforces.
    String absolute = Path.of(dbFile).toAbsolutePath().toString();
    try (Connection c = DriverManager.getConnection(
        "jdbc:h2:file:" + absolute + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
            + "NON_KEYWORDS=VALUE,KEY,ACTION",
        "sa", "")) {
      // CREATE IF NOT EXISTS first: a fresh database has no such schema, and
      // SET SCHEMA alone would fail rather than create it.
      c.createStatement().execute("CREATE SCHEMA IF NOT EXISTS data_management");
      c.createStatement().execute("SET SCHEMA data_management");
      seedRolesAndPermissions(c, defaults);
      seedUsers(c);
      seedProjects(c);
      report(c);
    }
  }

  /**
   * Loads the repository's own default values (roles, permissions, ACL classes).
   * Those are part of the schema's contract, not something to restate here.
   */
  static void seedRolesAndPermissions(Connection c, Path defaultsFile) throws Exception {
    if (!Files.exists(defaultsFile)) {
      System.out.println("no default-values file; skipping roles");
      return;
    }
    String sql = Files.readString(defaultsFile);
    // Strip both comment styles the file uses -- whole-line "--" comments and
    // trailing "#" comments, which would otherwise stay inside the statement.
    StringBuilder cleaned = new StringBuilder();
    for (String line : sql.split("\n")) {
      if (line.trim().startsWith("--")) {
        continue;
      }
      cleaned.append(line.replaceAll("\\s*#[^\\n]*$", "")).append('\n');
    }
    sql = cleaned.toString();

    Statement st = c.createStatement();
    int ok = 0, duplicate = 0, failed = 0;
    for (String chunk : sql.split(";\\s*\\n")) {
      String t = chunk.trim();
      if (t.isEmpty() || !t.toUpperCase().startsWith("INSERT")) {
        continue;
      }
      try {
        st.execute(t);
        ok++;
      } catch (SQLException e) {
        // Re-running the mock-up must not fail on rows that already exist.
        if (e.getMessage() != null && e.getMessage().contains("Unique")) {
          duplicate++;
        } else {
          failed++;
          System.out.println("role/permission seed failed: " + firstLine(e.getMessage()));
        }
      }
    }
    System.out.println("default values: " + ok + " inserted, " + duplicate
        + " already present, " + failed + " failed");
  }

  static void seedUsers(Connection c) throws Exception {
    upsertUser(c, "00000000-0000-0000-0000-000000000001",
        "devuser", DEV_EMAIL, "Dev User", DEV_PASSWORD);
    upsertUser(c, "00000000-0000-0000-0000-000000000002",
        "admin", ADMIN_EMAIL, "Admin User", ADMIN_PASSWORD);
    assignRoles(c);
  }

  /**
   * Gives the fixture accounts an application role.
   *
   * <p>Without a {@code user_role} row the account authenticates, but holds no
   * authorities -- and the project access chain resolves visible projects through
   * those authorities. The failure is quiet: the login succeeds, the overview
   * renders, and the list is empty.</p>
   */
  static void assignRoles(Connection c) throws Exception {
    ResultSet roles = c.createStatement()
        .executeQuery("SELECT id, name FROM data_management.roles");
    long adminRole = 1, userRole = 2;
    boolean sawAdmin = false, sawUser = false;
    while (roles.next()) {
      if ("ADMIN".equals(roles.getString(2))) {
        adminRole = roles.getLong(1);
        sawAdmin = true;
      } else if ("USER".equals(roles.getString(2))) {
        userRole = roles.getLong(1);
        sawUser = true;
      }
    }
    if (!sawAdmin || !sawUser) {
      System.out.println("roles table missing ADMIN/USER; skipped role assignment");
      return;
    }

    c.createStatement().execute("DELETE FROM data_management.user_role"
        + " WHERE userId IN ('00000000-0000-0000-0000-000000000001',"
        + " '00000000-0000-0000-0000-000000000002')");

    PreparedStatement ps = c.prepareStatement(
        "INSERT INTO data_management.user_role (userId, roleId) VALUES (?,?)");
    // devuser is a plain USER; admin gets ADMIN so privileged screens can be seen.
    ps.setString(1, "00000000-0000-0000-0000-000000000001");
    ps.setLong(2, userRole);
    ps.executeUpdate();
    ps.setString(1, "00000000-0000-0000-0000-000000000002");
    ps.setLong(2, adminRole);
    ps.executeUpdate();

    System.out.println("roles: devuser=USER(" + userRole + "), admin=ADMIN(" + adminRole + ")");
  }

  static void upsertUser(Connection c, String id, String userName, String email,
      String fullName, String password) throws Exception {
    c.createStatement().execute(
        "DELETE FROM data_management.users WHERE email = '" + email + "'");
    PreparedStatement ps = c.prepareStatement(
        "INSERT INTO data_management.users"
            + " (id, active, email, encryptedPassword, fullName, registrationDate, userName)"
            + " VALUES (?,?,?,?,?,?,?)");
    ps.setString(1, id);
    ps.setBoolean(2, true);
    ps.setString(3, email);
    ps.setString(4, encryptPassword(password));
    ps.setString(5, fullName);
    ps.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
    ps.setString(7, userName);
    ps.executeUpdate();
  }

  /**
   * Projects are written straight into the table both the read model and the
   * project pages query.
   *
   * <p>The application normally creates projects through a wizard; here the rows
   * are inserted directly. Every column the domain objects reject a NULL for must
   * be filled, or the failure appears only later and far away: opening the project
   * runs the column through {@code ProjectObjectiveConverter}, which throws, and
   * the user sees the generic 500 page rather than anything pointing at the seed
   * data.</p>
   */
  static void seedProjects(Connection c) throws Exception {
    c.createStatement().execute("DELETE FROM data_management.projects_datamanager");
    String[][] projects = {
        // id, code, title, objective, PI name+email, manager name+email,
        // responsible name+email
        {"10000000-0000-0000-0000-000000000001", "QTEST001",
            "Single cell atlas of the murine liver",
            "Map the cell composition of healthy and fibrotic murine liver tissue by scRNA-seq.",
            "Prof. Dr. Ada Klein", "ada.klein@example.org",
            "Dr. Ben Osei", "ben.osei@example.org",
            "Carla Mendes", "carla.mendes@example.org"},
        {"10000000-0000-0000-0000-000000000002", "QTEST002",
            "Immunopeptidomics of colorectal tumours",
            "Identify tumour-associated HLA ligands in colorectal carcinoma for vaccine design.",
            "Prof. Dr. Doris Falk", "doris.falk@example.org",
            "Dr. Emil Hartmann", "emil.hartmann@example.org",
            "Fatima Nasser", "fatima.nasser@example.org"},
        {"10000000-0000-0000-0000-000000000003", "QTEST003",
            "Spatial transcriptomics pilot",
            "Evaluate a spatial transcriptomics workflow on archived FFPE blocks.",
            "Prof. Dr. Greta Lindqvist", "greta.lindqvist@example.org",
            "Dr. Hugo Petrov", "hugo.petrov@example.org",
            "Ines Rocha", "ines.rocha@example.org"},
    };
    // Columns the domain model insists on, though the DDL marks them nullable.
    // Every one of these fails late and looks like a bug in the view:
    //   * objective -- ProjectObjective.requireNonNull. The converter throws inside
    //                  ProjectRepositoryImpl.find, so the project page shows the
    //                  generic 500 before it renders anything.
    //   * *EmailAddress -- Contact's no-arg JPA constructor skips its own validation,
    //                  so a NULL address only surfaces in the UI, as
    //                  "Value cannot be null" from the mailto: Anchor.
    // "version" is the JPA optimistic-lock column and is NOT NULL, so every
    // inserted project needs one even though nothing here reads it.
    PreparedStatement ps = c.prepareStatement(
        "INSERT INTO data_management.projects_datamanager"
            + " (projectId, projectCode, projectTitle, objective, lastModified,"
            + "  principalInvestigatorFullName, principalInvestigatorEmailAddress,"
            + "  projectManagerFullName, projectManagerEmailAddress,"
            + "  responsibePersonFullName, responsibePersonEmailAddress, version)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
    for (String[] p : projects) {
      ps.setString(1, p[0]);
      ps.setString(2, p[1]);
      ps.setString(3, p[2]);
      ps.setString(4, p[3]);
      ps.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
      ps.setString(6, p[4]);
      ps.setString(7, p[5]);
      ps.setString(8, p[6]);
      ps.setString(9, p[7]);
      ps.setString(10, p[8]);
      ps.setString(11, p[9]);
      ps.setInt(12, 0);
      ps.executeUpdate();
    }
    System.out.println("projects: " + projects.length);
    grantAccess(c, projects);
  }

  /**
   * Writes the ACL rows that make the projects visible.
   *
   * <p>The project list is a read model over {@code project_overview}, which
   * derives its user list from {@code acl_entry}. A project row without access
   * control entries is invisible to every account -- the page renders "you don't
   * have any projects yet" and looks like a bug in the view. Granting the dev
   * account READ on each project is what makes the fixtures show up.</p>
   */
  static void grantAccess(Connection c, String[][] projects) throws Exception {
    // A user ACL sid holds the USER ID, not the username: project_userinfo joins
    // acl_sid.sid straight onto users.id, and the access query matches on the same
    // value. Using the username here is the mistake that makes every project
    // invisible while the rows all look correct.
    String devUserId = "00000000-0000-0000-0000-000000000001";
    String adminUserId = "00000000-0000-0000-0000-000000000002";

    // acl_sid.principal = 1 selects user sids (as opposed to authorities).
    c.createStatement().execute(
        "DELETE FROM data_management.acl_sid WHERE principal = 1 AND sid IN ('"
            + devUserId + "','" + adminUserId + "')");
    c.createStatement().execute(
        "INSERT INTO data_management.acl_sid (principal, sid) VALUES"
            + " (1, '" + devUserId + "'), (1, '" + adminUserId + "')");

    ResultSet devSid = c.createStatement().executeQuery(
        "SELECT id FROM data_management.acl_sid WHERE sid = '" + devUserId + "' AND principal = 1");
    ResultSet adminSid = c.createStatement().executeQuery(
        "SELECT id FROM data_management.acl_sid WHERE sid = '" + adminUserId + "' AND principal = 1");
    long dev = devSid.next() ? devSid.getLong(1) : -1;
    long admin = adminSid.next() ? adminSid.getLong(1) : -1;

    ResultSet projectClass = c.createStatement().executeQuery(
        "SELECT id FROM data_management.acl_class"
            + " WHERE class = 'life.qbic.projectmanagement.domain.model.project.Project'");
    long classId = projectClass.next() ? projectClass.getLong(1) : 1;

    // BasePermission mask bits, as Spring Security defines them.
    final int READ = 1, WRITE = 2, CREATE = 4, DELETE = 8, ADMINISTRATION = 16;

    c.createStatement().execute(
        "DELETE FROM data_management.acl_entry WHERE acl_object_identity IN"
            + " (SELECT id FROM data_management.acl_object_identity WHERE object_id_class = " + classId + ")");
    c.createStatement().execute(
        "DELETE FROM data_management.acl_object_identity WHERE object_id_class = " + classId);

    for (String[] p : projects) {
      // ace_order is unique per object identity, so it keeps counting across the ACEs of both
      // principals rather than restarting at 0 for the second one.
      int order = 0;
      PreparedStatement identity = c.prepareStatement(
          "INSERT INTO data_management.acl_object_identity"
              + " (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)"
              + " VALUES (?,?,?,?,?)", new String[]{"id"});
      identity.setLong(1, classId);
      identity.setString(2, p[0]);
      identity.setNull(3, java.sql.Types.BIGINT);
      identity.setLong(4, dev);
      identity.setBoolean(5, true);
      identity.executeUpdate();
      ResultSet keys = identity.getGeneratedKeys();
      keys.next();
      long objectIdentity = keys.getLong(1);

      // The owner gets full access; the second account gets read-only, so
      // screenshots can show a project the reviewer does not own.
      order = insertEntries(c, objectIdentity, dev, order,
          READ, WRITE, CREATE, DELETE, ADMINISTRATION);
      order = insertEntries(c, objectIdentity, admin, order, READ);
    }
    System.out.println("access control: " + projects.length + " projects granted");
  }

  /**
   * Writes one ACE per permission -- never one ACE holding a combined mask.
   *
   * <p>Spring's {@code DefaultPermissionGrantingStrategy.isGranted} looks for an ACE whose mask
   * <em>equals</em> the requested permission's mask (it only switches to a bitwise test when a
   * subclass overrides {@code hasPermission}). A single ACE with mask {@code READ|WRITE|CREATE|
   * DELETE|ADMINISTRATION} = 31 therefore matches no permission at all: every check falls through
   * to {@code NotFoundException}, and the {@code AclPermissionEvaluator} turns that into a plain
   * {@code false}. The page then renders the generic "Access Denied" 500 even though the ACE rows
   * look perfectly correct in the database.</p>
   *
   * <p>{@code ProjectAccessServiceImpl} writes ACEs exactly this way -- one
   * {@code insertAce(permission)} call per permission -- so this seeder has to match it. Writing a
   * single combined ACE here is invisible until someone opens a project, at which point every view
   * answers with the generic access-denied 500.</p>
   *
   * @return the next free {@code ace_order} for this object identity
   */
  static int insertEntries(Connection c, long objectIdentity, long sid, int order,
      int... permissionBits) throws Exception {
    for (int mask : permissionBits) {
      PreparedStatement ps = c.prepareStatement(
          "INSERT INTO data_management.acl_entry"
              + " (acl_object_identity, ace_order, sid, mask, granting, audit_success,"
              + " audit_failure) VALUES (?,?,?,?,?,?,?)");
      ps.setLong(1, objectIdentity);
      ps.setInt(2, order++);
      ps.setLong(3, sid);
      ps.setInt(4, mask);
      ps.setBoolean(5, true);
      ps.setBoolean(6, true);
      ps.setBoolean(7, true);
      ps.executeUpdate();
    }
    return order;
  }

  static void report(Connection c) throws Exception {
    ResultSet users = c.createStatement()
        .executeQuery("SELECT userName, email FROM data_management.users ORDER BY userName");
    while (users.next()) {
      System.out.println("  user    " + users.getString(2) + "  (" + users.getString(1) + ")");
    }
    ResultSet projects = c.createStatement()
        .executeQuery("SELECT projectCode FROM data_management.projects_datamanager ORDER BY projectCode");
    while (projects.next()) {
      System.out.println("  project " + projects.getString(1));
    }
  }

  /** Mirrors life.qbic.identity.domain.model.PasswordEncryptionPolicy#encrypt. */
  static String encryptPassword(String raw) throws Exception {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[20];
    random.nextBytes(salt);
    PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, 100_000, 256);
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    byte[] hash = factory.generateSecret(spec).getEncoded();
    return 100_000 + ":" + toHex(salt) + ":" + toHex(hash);
  }

  static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  static String firstLine(String message) {
    return message == null ? "" : message.split("\n")[0];
  }
}
