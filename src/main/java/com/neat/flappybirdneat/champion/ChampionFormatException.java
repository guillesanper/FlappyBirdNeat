package com.neat.flappybirdneat.champion;

import java.io.IOException;

/** A champion file that cannot be read: not JSON, another format, an unsupported version or engine. */
public class ChampionFormatException extends IOException {
    public ChampionFormatException(String message) {
        super(message);
    }

    public ChampionFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
