package com.github.terminatornl.tiquality.interfaces;

import com.github.terminatornl.tiquality.profiling.ReferencedTickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public interface TiqualitySimpleTickable extends UpdateTyped {

    /**
     * Checks if this tickable is loaded, eg: chunk load status
     *
     * @return chunk status
     */
    boolean tiquality_isLoaded();

    /**
     * Method to actually run the update on the tickable.
     */
    void tiquality_doUpdateTick();

    /**
     * Method to get the position of the object
     *
     * @return the position
     */
    BlockPos tiquality_getPos();

    /**
     * Method to get the world of the object
     *
     * @return the world
     */
    World tiquality_getWorld();

    /**
     * Gets the reference to this tickable
     *
     * @return the reference
     */
    @Nullable
    ReferencedTickable.Reference getId();

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
     *
     * @return marked
     */
    boolean tiquality_isMarked();
}
