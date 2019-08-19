package com.github.terminatornl.tiquality.mixin;

import com.github.terminatornl.tiquality.interfaces.UpdateTyped;
import com.github.terminatornl.tiquality.tracking.UpdateType;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nonnull;

@Mixin(value = Block.class, priority = 1001)
public class MixinBlock implements UpdateTyped {

    private UpdateType tiqualityUpdateType = UpdateType.DEFAULT;

    @Override
    public @Nonnull
    UpdateType getUpdateType() {
        return tiqualityUpdateType;
    }

    @Override
    public void setUpdateType(@Nonnull UpdateType type) {
        tiqualityUpdateType = type;
    }
}
