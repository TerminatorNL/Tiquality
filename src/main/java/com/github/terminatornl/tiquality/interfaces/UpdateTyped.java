package com.github.terminatornl.tiquality.interfaces;

import com.github.terminatornl.tiquality.tracking.UpdateType;

import javax.annotation.Nonnull;

public interface UpdateTyped {
    @Nonnull
    UpdateType getUpdateType();

    void setUpdateType(@Nonnull UpdateType type);
}
