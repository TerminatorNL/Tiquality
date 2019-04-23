package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.ForgetFulProgrammerException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
     * Priority: Won't tick if no tracker is assigned, but when a tracker ran out of time
     * it will be executed before anything else. (Placed in front of the queue)
     */
    PRIORITY,
    /**
     * Never ticks. Ever.
     */
    TICK_DENIED,
    /**
     * Always_tick: Will always tick, regardless if a tracker is assigned.
     */
    ALWAYS_TICK;

    public enum Type{
        ENTITY,
        BLOCK
    }

    /**
     * Check if this tracker must tick.
     * @param tracker a tracker, or null if none has been assigned
     * @return true if the block MUST tick instantly. false if it's not required.
     */
    public boolean mustTick(@Nullable Tracker tracker){
        switch (this){
            case DEFAULT:
            case PRIORITY:
            case TICK_DENIED:
                return false;
            case NATURAL:
                return tracker == null;
            case ALWAYS_TICK:
                return true;
            default:
                throw new ForgetFulProgrammerException();
        }
    }

    public static ITextComponent getArguments(Type type, TextFormatting textColour){
        List<ITextComponent> list = new LinkedList<>();
        Iterator<UpdateType> iterator = Arrays.asList(UpdateType.values()).iterator();
        while(iterator.hasNext()){
            list.add(iterator.next().getText(type));
            if(iterator.hasNext()){
                list.add(new TextComponentString(textColour + " | "));
            }else{
                break;
            }
        }
        list.add(new TextComponentString(textColour + ">"));

        TextComponentString builder = new TextComponentString(textColour + "<");
        for(ITextComponent text : list) {
            builder.appendSibling(text);
        }
        return builder;
    }

    public ITextComponent getText(Type type){
        Style style = new Style();
        if(type == Type.BLOCK) {
            switch (this) {
                case DEFAULT:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Only ticks when a tracker is assigned AND has time to tick.\nCan be throttled")));
                    break;
                case PRIORITY:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Like " + TextFormatting.GRAY + "DEFAULT" + TextFormatting.RESET + ", but ticks before everything else.\nCan be throttled")));
                    break;
                case TICK_DENIED:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Never ticks.")));
                    break;
                case NATURAL:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Ticks when no tracker is assigned.\nWhen a tracker has been assigned, it can be throttled if no time is left.")));
                    break;
                case ALWAYS_TICK:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Always ticks.\nNever throttled")));
                    break;
                default:
                    throw new ForgetFulProgrammerException();
            }
        }else if(type == Type.ENTITY){
            switch (this) {
                case DEFAULT:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Ticks when no tracker is assigned.\nWhen a tracker has been assigned, it can be throttled if no time is left.")));
                    break;
                case PRIORITY:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Like " + TextFormatting.GRAY + "DEFAULT" + TextFormatting.RESET + ", but ticks before everything else.\nCan be throttled")));
                    break;
                case TICK_DENIED:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Never ticks.")));
                    break;
                case NATURAL:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Like DEFAULT")));
                    break;
                case ALWAYS_TICK:
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Always ticks.\nNever throttled")));
                    break;
                default:
                    throw new ForgetFulProgrammerException();
            }
        }
        switch (this){
            case DEFAULT:
                return new TextComponentString(TextFormatting.GRAY + "DEFAULT").setStyle(style);
            case PRIORITY:
                return new TextComponentString(TextFormatting.AQUA + "PRIORITY").setStyle(style);
            case TICK_DENIED:
                return new TextComponentString(TextFormatting.DARK_RED + "TICK-DENIED").setStyle(style);
            case NATURAL:
                return new TextComponentString(TextFormatting.GOLD + "NATURAL").setStyle(style);
            case ALWAYS_TICK:
                return new TextComponentString(TextFormatting.GREEN + "ALWAYS_TICK").setStyle(style);
            default:
                throw new ForgetFulProgrammerException();
        }
    }
}
