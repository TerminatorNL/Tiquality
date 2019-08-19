package com.github.terminatornl.tiquality.api;

import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.profiling.ProfilingKey;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Contains exceptions to make sure people who use the API deal with potential problems
 */
public class TiqualityException extends Exception {

    TiqualityException(String s) {
        super(s);
    }

    public ITextComponent getTextComponent() {
        return new TextComponentString(TextFormatting.RED + getMessage());
    }

    /**
     * Thrown when a player tracker isn't found in the system.
     */
    public static class PlayerTrackerNotFoundException extends TiqualityException {
        public PlayerTrackerNotFoundException(GameProfile profile) {
            super("No PlayerTracker exists for: " + profile.getName() + " with UUID: " + profile.getId());
        }
    }

    /**
     * Thrown when incorrect usage of Tiquality's internals is detected.
     */
    public static class ReadTheDocsException extends TiqualityException {
        public ReadTheDocsException(String text) {
            super("Welp! Looks like a programmer didn't read the documentation for Tiquality. Message: " + text);
        }
    }

    /**
     * Thrown when a profiler start was attempted if the tracker is unable to profile
     */
    public static class TrackerCannotProfileException extends TiqualityException {
        public TrackerCannotProfileException(Tracker tracker) {
            super("Tracker " + tracker.toString() + " does not support profiling!");
        }
    }

    /**
     * Thrown when a profiler start was attempted if the tracker is already profiling
     */
    public static class TrackerAlreadyProfilingException extends TiqualityException {
        public TrackerAlreadyProfilingException(Tracker tracker) {
            super("Tracker " + tracker.toString() + " is already profiling!");
        }
    }

    /**
     * Thrown when a profiler start was attempted if the tracker is already profiling
     */
    public static class TrackerWasNotProfilingException extends TiqualityException {
        public TrackerWasNotProfilingException(Tracker tracker) {
            super("Tracker " + tracker.toString() + " was not profiling!");
        }
    }

    /**
     * Thrown when a profiler was profiling, but the key to stop the profiler was invalid.
     * Can be used to obtain keys from running trackers. Note that the key is not a security measure, but
     * ensures collisions are at least known the the programmer.
     */
    public static class InvalidKeyException extends TiqualityException {

        private final ProfilingKey key;

        public InvalidKeyException(Tracker tracker, ProfilingKey key) {
            super("Used wrong key to stop profiling on tracker: " + tracker.toString());
            this.key = key;
        }

        public ProfilingKey getExpectedKey() {
            return key;
        }


    }
}
