package cf.terminator.tiquality.tracking.update;

import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TickTimeMeasurement implements TiqualitySimpleTickable {
    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void doUpdateTick() {

    }

    /**
     * Method to get the position of the object
     *
     * @return the position
     */
    @Override
    public BlockPos getPos() {
        return null;
    }

    /**
     * Method to get the world of the object
     *
     * @return the world
     */
    @Override
    public World getWorld() {
        return null;
    }

    /**
     * Gets the location object of this Tickable
     *
     * @return the location
     */
    @Override
    public TickLogger.Location getLocation() {
        return null;
    }

    /**
     * Gets the type of this Tickable
     *
     * @return the type
     */
    @Override
    public TickType getType() {
        return null;
    }

    /**
     * Marks this tickable
     */
    @Override
    public void tiquality_mark() {

    }

    /**
     * Unmarks this tickable
     */
    @Override
    public void tiquality_unMark() {

    }

    /**
     * Checks if this tickable is marked
     *
     * @return marked
     */
    @Override
    public boolean tiquality_isMarked() {
        return true;
    }
}
