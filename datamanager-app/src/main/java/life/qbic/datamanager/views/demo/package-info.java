/**
 * Throwaway prototypes and component demos. <b>Not production code.</b>
 *
 * <p>Everything in this package exists to be shown to stakeholders and then deleted. It is
 * deliberately monolithic, backed by canned data, and shaped as little like production code as
 * possible, so that nobody is tempted to lift a piece of it into a real view.</p>
 *
 * <h3>The contract</h3>
 *
 * <ul>
 *   <li><b>No production class may reference this package.</b> The dependency points one way only.
 *       {@code PrototypeIsolationSpec} fails the build if that breaks.</li>
 *   <li><b>No prototype ships a route outside the {@code development} profile.</b> Every view here
 *       carries {@code @Profile("development")}; the code is still compiled into the artifact, so the
 *       profile is what keeps it unwired, not what keeps it out.</li>
 *   <li><b>This package is not a design system.</b> The {@code pt-*} selectors are not a source: any
 *       visual rule that survives review belongs in the {@code data-manager-app-design-system}
 *       repository, not copied out of here.</li>
 *   <li><b>Decisions live in {@code docs/plans/}, not here.</b> A prototype is evidence for a
 *       decision. The decision is written down in the plan; implementation starts only from an
 *       approved Story in {@code docs/features.md}.</li>
 *   <li><b>This package is meant to shrink.</b> A prototype is deleted once the behaviour it
 *       demonstrates is implemented, or once it stops being reviewed.</li>
 * </ul>
 *
 * <p>See {@code docs/plans/Project-management-refactor.md}, section "The prototype is evidence, not
 * specification", for the rules that follow from this.</p>
 *
 * @since 1.20.0
 */
package life.qbic.datamanager.views.demo;
