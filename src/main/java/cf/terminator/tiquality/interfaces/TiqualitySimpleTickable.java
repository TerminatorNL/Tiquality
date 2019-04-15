package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface TiqualitySimpleTickable {

    /**
     * Method to actually run the update on the tickable.
     */
    void doUpdateTick();


    /**
     * Method to get the position of the object
     * @return the position
     */
    BlockPos getPos();


    /**
     * Method to get the world of the object
     * @return the world
     */
    World getWorld();

    /**
     * Gets the location object of this Tickable
     * @return the location
     */
    TickLogger.Location getLocation();

    /**
     * Gets the type of this Tickable
     * @return the type
     */
    TickType getType();

    enum TickType{
        BLOCK,          /* A block update */
        BLOCK_RANDOM,   /* A random block update */
        TILE_ENTITY,    /* A tile entity update */
        ENTITY,         /* An entity update */
        OTHER           /* Any other type of update, like metrics */
    }

    /**
     * Marks this tickable
     */
    void tiquality_mark();

    /**
     * Unmarks this tickable
     */
    void tiquality_unMark();

    /**
     * Checks if this tickable is marked
     * @return marked
     */
    boolean tiquality_isMarked();
}
