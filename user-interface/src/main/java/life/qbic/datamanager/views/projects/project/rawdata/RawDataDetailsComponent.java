package life.qbic.datamanager.views.projects.project.rawdata;


import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import java.io.Serializable;

/**
 * Raw Data Details Component
 * <p></p>
 * Enables the user to manage the registered RawData by providing the ability to
 * access and search the raw data, and enabling them to download the raw data of interest
 */

@SpringComponent
@UIScope
@PermitAll
public class RawDataDetailsComponent extends PageArea implements Serializable {

    public RawDataDetailsComponent() {
        addClassName("raw-data-details-component");
    }

    public void setSearchedRawDataValue(String searchTerm) {
        //Todo implement
    }

    public void setContext(Context context){
        //Todo implement
    }
}
