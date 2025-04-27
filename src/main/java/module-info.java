module com.neat.flappybirdneat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    opens com.neat.flappybirdneat to javafx.fxml;
    exports com.neat.flappybirdneat;
}