package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.UpdateType;

import javax.annotation.Nonnull;

public interface UpdateTyped {
    void setUpdateType(@Nonnull UpdateType type);
    @Nonnull UpdateType getUpdateType();
}
