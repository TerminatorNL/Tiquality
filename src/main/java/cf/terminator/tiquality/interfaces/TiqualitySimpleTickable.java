package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.store.TickLogger;
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
}
