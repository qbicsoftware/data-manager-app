package life.qbic.datamanager.views.projectOverview;

/**
 * <b> Interface to handle the {@link ProjectOverviewLayout} to the {@link ProjectOverviewHandler}. </b>
 *
 * @since 1.0.0
 */
public interface ProjectOverviewHandlerInterface {

    /**
     * Registers the {@link ProjectOverviewLayout} to the implementing class
     *
     * @param layout The view that is being registered
     * @since 1.0.0
     */
    void handle(ProjectOverviewLayout layout);
}
