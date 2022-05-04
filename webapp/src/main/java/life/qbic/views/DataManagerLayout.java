package life.qbic.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * <b>Main layout of the app</b>
 *
 * <p>Defines the look of the header and the title</p>
 *
 * @since 1.0.0
 */
public abstract class DataManagerLayout extends AppLayout {
    private HorizontalLayout headerLayout;

    protected DataManagerLayout() {
        createHeaderContent();
    }

    private void createHeaderContent() {
        createHeaderLayout();

        addToNavbar(headerLayout);
    }

    private void createHeaderLayout() {
        H1 appName = styleHeaderTitle();
        headerLayout = new HorizontalLayout(appName);

        styleHeaderLayout();
    }

    private void styleHeaderLayout() {
        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setWidth("100%");
        headerLayout.addClassNames("py-0", "px-m");
    }

    private H1 styleHeaderTitle() {
        H1 appName = new H1("Data Manager");
        appName.addClassNames("text-l", "m-m");
        return appName;
    }
}
