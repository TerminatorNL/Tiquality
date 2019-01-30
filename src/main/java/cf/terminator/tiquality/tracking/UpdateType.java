package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.ForgetFulProgrammerException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;

public enum UpdateType {
    /**
     * Default: Will not tick if no tracker is assigned
     */
    DEFAULT,
    /**
     * Natural: Will tick if no tracker is assigned,
     * but can be throttled if a tracker ran out of time.
     */
    NATURAL,
    /**
     * Always_tick: Will always tick, regardless if a tracker is assigned.
     */
    ALWAYS_TICK;

    /**
     * Check if this tracker must tick.
     * @param tracker a tracker, or null if none has been assigned
     * @return true if the block MUST tick instantly. false if it's not required.
     */
    public boolean mustTick(@Nullable Tracker tracker){
        switch (this){
            case DEFAULT:
                return false;
            case NATURAL:
                return tracker == null;
            case ALWAYS_TICK:
                return true;
            default:
                throw new ForgetFulProgrammerException();
        }
    }

    public ITextComponent getText(){
        switch (this){
            case DEFAULT:
                return new TextComponentString(TextFormatting.GRAY + "DEFAULT");
            case NATURAL:
                return new TextComponentString(TextFormatting.GOLD + "NATURAL");
            case ALWAYS_TICK:
                return new TextComponentString(TextFormatting.GREEN + "ALWAYS_TICK");
            default:
                throw new ForgetFulProgrammerException();
        }
    }
}
