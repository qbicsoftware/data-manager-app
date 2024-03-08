package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;

import java.io.Serializable;


/**
 * Raw Data Download Information Component
 * <p></p>
 * Informs the user about the steps necessary to download the raw data associated with the {@link MeasurementMetadata}
 * of the {@link Experiment} of interest
 */

@SpringComponent
@UIScope
@PermitAll
public class RawDataDownloadInformationComponent extends PageArea implements Serializable {

    public RawDataDownloadInformationComponent() {
        addClassName("raw-data-download-information-component");
    }
}
