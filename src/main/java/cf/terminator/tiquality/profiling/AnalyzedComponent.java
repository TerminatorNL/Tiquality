package cf.terminator.tiquality.profiling;

import cf.terminator.tiquality.api.Location;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class AnalyzedComponent implements Comparable<AnalyzedComponent>, IMessage {

    private String clazz;
    private TickTime times;
    private ReferencedTickable.ReferenceId reference;
    private Location<Integer, BlockPos> lastKnownLocation;
    private ResourceLocation resourceLocation;
    private String friendlyName;

    public AnalyzedComponent(ByteBuf byteBuf){
        fromBytes(byteBuf);
    }

    private AnalyzedComponent(@Nonnull String clazz, @Nonnull TickTime times, @Nonnull ReferencedTickable.ReferenceId reference, @Nullable Location<Integer, BlockPos> lastKnownLocation, @Nonnull String friendlyName, @Nullable ResourceLocation resourceLocation){
        this.clazz = clazz;
        this.times = times;
        this.reference = reference;
        this.lastKnownLocation = lastKnownLocation;
        this.friendlyName = friendlyName;
        this.resourceLocation = resourceLocation;
    }

    @Override
    public String toString(){
        return times.toString() + " " + friendlyName;
    }

    @Override
    public int compareTo(@Nonnull AnalyzedComponent o) {
        return this.times.compareTo(o.times);
    }

    public String getFriendlyName(){
        return friendlyName;
    }

    public TickTime getTimes(){
        return times;
    }

    public String getReferencedClass(){
        return clazz;
    }

    public ResourceLocation getResourceLocation(){
        return resourceLocation;
    }

    public Location<Integer, BlockPos> getLastKnownLocation(){
        return lastKnownLocation;
    }

    public String getLocationString(){
        if(lastKnownLocation != null) {
            return TextFormatting.GRAY + "D" + TextFormatting.WHITE + lastKnownLocation.getWorld()
                    + TextFormatting.GRAY + "X" + TextFormatting.WHITE + lastKnownLocation.getPos().getX()
                    + TextFormatting.GRAY + "Y" + TextFormatting.WHITE + lastKnownLocation.getPos().getY()
                    + TextFormatting.GRAY + "Z" + TextFormatting.WHITE + lastKnownLocation.getPos().getZ();
        }else{
            return TextFormatting.RED + "Unknown";
        }
    }

    public ReferencedTickable.ReferenceId getReferenceId(){
        return reference;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, clazz);
        times.toBytes(buf);
        reference.toBytes(buf);
        buf.writeInt(lastKnownLocation.getWorld());
        buf.writeInt(lastKnownLocation.getPos().getX());
        buf.writeInt(lastKnownLocation.getPos().getY());
        buf.writeInt(lastKnownLocation.getPos().getZ());
        ByteBufUtils.writeUTF8String(buf, friendlyName);

        if(resourceLocation != null){
            ByteBufUtils.writeUTF8String(buf, resourceLocation.getNamespace());
            ByteBufUtils.writeUTF8String(buf, resourceLocation.getPath());
            buf.writeBoolean(true);
        }else{
            buf.writeBoolean(false);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.clazz = ByteBufUtils.readUTF8String(buf);
        this.times = new TickTime(buf);
        this.reference = new ReferencedTickable.ReferenceId(buf);
        int dimension = buf.readInt();
        int posX = buf.readInt();
        int posY = buf.readInt();
        int posZ = buf.readInt();
        this.lastKnownLocation = new Location<>(dimension, new BlockPos(posX, posY, posZ));
        this.friendlyName = ByteBufUtils.readUTF8String(buf);
        if(buf.readBoolean()){
            resourceLocation = new ResourceLocation(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
        }else{
            resourceLocation = null;
        }
    }


    public static class Analyzer implements Runnable {
        private final ReferencedTickable.Reference reference;
        private final TickTime times;
        private final Set<AnalyzedComponent> set;

        public Analyzer(ReferencedTickable.Reference reference, TickTime times, Set<AnalyzedComponent> set) {
            this.reference = reference;
            this.times = times;
            this.set = set;
        }

        public AnalyzedComponent analyze(){
            try {
                ITextComponent friendlyName = reference.getName();
                Location<Integer, BlockPos> lastKnownLocation = reference.currentPos();
                Class clazz = reference.getReferencedClass();
                return new AnalyzedComponent(clazz.toString().substring(6), times, reference.getId(), lastKnownLocation, friendlyName.getUnformattedText(), reference.getResourceLocation());
            }catch (Throwable t){
                ITextComponent friendlyName = new TextComponentString("Error").setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(t.toString()))));
                return new AnalyzedComponent("Error", times, reference.getId(), null, friendlyName.getFormattedText(), null);
            }
        }

        @Override
        public void run() {
            set.add(analyze());
        }
    }
}
