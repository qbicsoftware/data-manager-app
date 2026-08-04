module life.qbic.datamanagerbom {
    requires javafx.controls;
    requires javafx.fxml;


    opens life.qbic.datamanagerbom to javafx.fxml;
    exports life.qbic.datamanagerbom;
}