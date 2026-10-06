package com.neat.flappybirdneat.cli;

/** Invalid command-line arguments; the message is shown to the user as is. */
public class UsageException extends Exception {
    public UsageException(String message) {
        super(message);
    }
}
