module com.neat.flappybirdneat {
    requires javafx.controls;

    opens com.neat.flappybirdneat.history to javafx.base;

    exports com.neat.flappybirdneat;
}
