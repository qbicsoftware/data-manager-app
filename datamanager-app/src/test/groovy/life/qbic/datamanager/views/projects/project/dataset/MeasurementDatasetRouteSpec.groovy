package life.qbic.datamanager.views.projects.project.dataset

import com.vaadin.flow.component.Component
import com.vaadin.flow.router.Route
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Guards the {@code @Route} declarations of the whole module against the two ways a route
 * annotation breaks the application at startup.
 *
 * <p>Both defects occurred during this rework, and neither was a compile error: the build stayed
 * green and the application died in {@code VaadinServletContextInitializer} with a Tomcat listener
 * stack trace. Booting Spring to catch that costs minutes and needs a database, so the rules are
 * checked against the compiled classes instead — exact, and fast.</p>
 *
 * <ol>
 *   <li><b>A route class must be a {@code Component}.</b> Vaadin rejects the entire application
 *       when one is not. A redirect that only implements {@code BeforeEnterObserver} is not
 *       enough; the class has to extend something component-shaped.</li>
 *   <li><b>No two classes may claim one path.</b> This arises naturally while migrating: the old
 *       view keeps its annotation and a redirect is added for the same path.</li>
 * </ol>
 *
 * <p>The check reads the compiled output rather than the sources, because {@link
 * Component#class} assignability is exactly what Vaadin tests, and a source-text approximation
 * would not see a base class defined elsewhere.</p>
 */
class MeasurementDatasetRouteSpec extends Specification {

  @Unroll
  def "every compiled @Route class is a Vaadin Component (#className)"() {
    given: "the route classes found on the compiled classpath"
    def routeClasses = compiledRouteClasses()

    expect: "the scan is not vacuous -- if this fails the detector is broken, not the code"
    routeClasses.size() > 5

    and: "Vaadin's own precondition holds for every one of them"
    routeClasses.every { it.value }
  }

  def "no route path is claimed by more than one class"() {
    given:
    def claims = [:]
    compiledRouteClasses().keySet().each { clazz ->
      def route = clazz.getAnnotation(Route)
      if (route != null) {
        claims.computeIfAbsent(route.value(), { [] }) << clazz.name
      }
    }

    expect: "two classes on one path is a startup failure"
    def duplicates = claims.findAll { path, classes -> classes.size() > 1 }
    duplicates.isEmpty()
  }

  @Unroll
  def "the '#path' path is declared exactly once"() {
    given:
    def claims = [:]
    compiledRouteClasses().keySet().each { clazz ->
      def route = clazz.getAnnotation(Route)
      if (route != null) {
        claims.computeIfAbsent(route.value(), { [] }) << clazz.name
      }
    }

    expect: "the final path segment is matched exactly: '/dataset' is also a suffix of '/datasets',"
    // so a substring test would report the project-level Datasets page as a duplicate of the
    // experiment-level dataset page. Compare the whole last segment instead.
    def matching = claims.findAll { key, value ->
      def segments = key.split("/") as List
      segments.isEmpty() ? false : segments.last() == path
    }
    matching.size() == expectedCount

    where:
    path          | expectedCount
    "measurements"| 1
    "rawdata"     | 1
    "dataset"     | 1
    "datasets"    | 1
  }

  /**
   * Collects every class on the compiled output that carries {@code @Route}, mapped to whether it
   * satisfies Vaadin's requirement of being a {@link Component}.
   */
  private static Map<Class<?>, Boolean> compiledRouteClasses() {
    File classesDir = compiledClassesDirectory()
    Map<Class<?>, Boolean> result = new LinkedHashMap<>()
    classesDir.eachFileRecurse { file ->
      if (!file.isFile() || !file.name.endsWith(".class")) {
        return
      }
      String relative = classesDir.toPath().relativize(file.toPath()).toString()
      String className = relative
          .replace(File.separator, ".")
          .replaceAll(/\.class$/, "")
      try {
        Class<?> clazz = Class.forName(className, false,
            Thread.currentThread().contextClassLoader)
        if (clazz.getAnnotation(Route) != null) {
          result.put(clazz, Component.isAssignableFrom(clazz))
        }
      } catch (Throwable ignored) {
        // Classes that cannot be loaded without their full runtime are not routes we can judge;
        // the alternative is booting the application for every one of them.
      }
    }
    return result
  }

  private static File compiledClassesDirectory() {
    def candidates = [
        new File("target/classes"),
        new File("datamanager-app/target/classes")
    ]
    def found = candidates.find { it.isDirectory() }
    assert found != null:
        "compiled classes not found; run the build before this spec " +
            "(looked in ${candidates*.absolutePath})"
    return found
  }
}
