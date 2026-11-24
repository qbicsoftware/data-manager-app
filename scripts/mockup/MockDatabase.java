import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the mock-up database: loads the translated schema, JobRunr's own
 * migrations and the fixture users into an H2 file database.
 *
 * <p>Runs as a plain {@code java} single-file program (no build step), so the
 * mock-up start script can recreate the database from nothing.</p>
 *
 * <h3>Why three databases</h3>
 * <p>The application configures three separate datasources -- data management,
 * finance, JobRunr -- and expects each to exist. On MariaDB they usually share
 * one server; here each is its own H2 file so a reset is a file delete.</p>
 */
public class MockDatabase {

  /**
   * H2 settings that make MariaDB DDL and MariaDB-style queries behave.
   *
   * <p>No {@code SCHEMA=} here: on a database that does not exist yet the schema
   * cannot be selected, and connecting would fail before this code had a chance
   * to create it. The schema is created and selected explicitly instead.</p>
   */
  static String url(String file) {
    // H2 rejects implicitly relative paths, so resolve before building the URL.
    String absolute = Path.of(file).toAbsolutePath().toString();
    return "jdbc:h2:file:" + absolute + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
        + "NON_KEYWORDS=VALUE,KEY,ACTION;AUTO_SERVER=TRUE";
  }

  private static final Pattern IS_VIEW =
      Pattern.compile("(?is)^\\s*(CREATE\\s+(OR\\s+REPLACE\\s+)?VIEW|DROP\\s+VIEW)");
  private static final Pattern IS_DDL =
      Pattern.compile("(?is)^\\s*(CREATE|DROP|ALTER)");

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("usage: MockDatabase <schema.sql> <data-dir>");
      System.exit(1);
    }
    Path schema = Path.of(args[0]);
    Path dataDir = Path.of(args[1]);
    Files.createDirectories(dataDir);

    // The file name is the URL fragment, and run.sh builds the app's datasource
    // URL from the same name -- keep them in step.
    loadSchema(schema, dataDir.resolve("db-dm").toString());
    System.out.println("schema loaded");
  }

  /**
   * Loads the data-management schema.
   *
   * <p>Tables are created before views, always, because the DDL declares views
   * that read tables defined further down the file.</p>
   */
  static void loadSchema(Path schemaFile, String dbFile) throws Exception {
    try (Connection c = DriverManager.getConnection(url(dbFile), "sa", "")) {
      Statement s = c.createStatement();
      s.execute("CREATE SCHEMA IF NOT EXISTS data_management");
      s.execute("SET SCHEMA data_management");

      // The measurement views call MySQL JSON functions that H2 lacks. Stubs are
      // enough: the mock-up needs the views to exist and return rows, not to
      // evaluate JSON paths precisely.
      s.execute("CREATE ALIAS IF NOT EXISTS JSON_LENGTH AS $$ int f(String x){"
          + " if(x==null) return 0; int d=0; boolean q=false;"
          + " for(char ch: x.toCharArray()){ if(ch=='\"') q=!q; if(q) continue;"
          + " if(ch=='['||ch=='{') d++; else if(ch==']'||ch=='}') d--; } return 0; } $$");
      s.execute("CREATE ALIAS IF NOT EXISTS JSON_VALID AS $$ boolean f(String x){ return true; } $$");

      String sql = Files.readString(schemaFile);
      List<String> tables = new ArrayList<>();
      List<String> views = new ArrayList<>();
      for (String statement : sql.split(";\\s*\\n")) {
        String t = statement.trim();
        if (t.isEmpty() || !IS_DDL.matcher(t).find()) {
          continue;
        }
        (IS_VIEW.matcher(t).find() ? views : tables).add(t);
      }

      int tablesOk = 0;
      List<String> failures = new ArrayList<>();
      for (String t : tables) {
        try {
          s.execute(t);
          tablesOk++;
        } catch (SQLException e) {
          failures.add(summarise(t) + " -> " + firstLine(e.getMessage()));
        }
      }
      int viewsOk = 0;
      for (String t : views) {
        try {
          s.execute(t);
          viewsOk++;
        } catch (SQLException e) {
          failures.add(summarise(t) + " -> " + firstLine(e.getMessage()));
        }
      }

      System.out.println("tables: " + tablesOk + "/" + tables.size()
          + "  views: " + viewsOk + "/" + views.size());
      if (!failures.isEmpty()) {
        System.out.println("FAILURES:");
        failures.forEach(f -> System.out.println("  " + f));
        System.exit(2);
      }
    }
  }

  private static String summarise(String statement) {
    String flat = statement.replaceAll("\\s+", " ");
    return flat.substring(0, Math.min(90, flat.length()));
  }

  private static String firstLine(String message) {
    return message == null ? "" : message.split("\n")[0];
  }
}
