package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.UpdateTyped;
import cf.terminator.tiquality.tracking.UpdateType;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nonnull;

@Mixin(value = Block.class, priority = 1001)
public class MixinBlock implements UpdateTyped {

    private UpdateType tiqualityUpdateType = UpdateType.DEFAULT;

    @Override
    public void setUpdateType(@Nonnull UpdateType type) {
        tiqualityUpdateType = type;
    }

    @Override
    public @Nonnull UpdateType getUpdateType() {
        return tiqualityUpdateType;
    }
}
