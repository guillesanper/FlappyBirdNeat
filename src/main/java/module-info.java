module com.neat.flappybirdneat {
    requires javafx.controls;
    requires org.slf4j;

    opens com.neat.flappybirdneat.history to javafx.base;

    exports com.neat.flappybirdneat;
}
