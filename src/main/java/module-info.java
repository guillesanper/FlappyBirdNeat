module com.neat.flappybirdneat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires org.jfree.jfreechart;
    requires jfreechart.fx;

    opens com.neat.flappybirdneat to javafx.fxml;
    opens com.neat.flappybirdneat.history to javafx.base;

    exports com.neat.flappybirdneat;
}