package cf.terminator.tiquality.profiling;

import cf.terminator.tiquality.api.Location;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class ReferencedTickable {

    interface Reference extends IMessage, Comparable {
        @Nonnull ITextComponent getName();
        @Nullable Location<Integer, BlockPos> currentPos();
    }

    public static class BlockReference implements Reference{

        private int dimension;
        private BlockPos pos;

        private BlockReference(){

        }

        public BlockReference(int dimension, BlockPos pos){
            this.dimension = dimension;
            this.pos = pos;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(dimension);
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            dimension = buf.readInt();
            pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        }

        @Nonnull
        @Override
        public ITextComponent getName() {
            WorldServer world = net.minecraftforge.common.DimensionManager.getWorld(dimension, false);
            if (world == null){
                return new TextComponentString(TextFormatting.RED + "Block: Unloaded");
            }
            Block block = world.getBlockState(pos).getBlock();

            ResourceLocation resourceLocation = block.getRegistryName();
            if (resourceLocation == null){
                return new TextComponentString(block.getLocalizedName());
            }
            return new TextComponentString(resourceLocation.toString());
        }

        @Override
        public Location<Integer, BlockPos> currentPos() {
            return new Location<>(dimension, pos);
        }

        @Override
        public int compareTo(@Nonnull Object obj) {
            if(obj instanceof BlockReference){
                int c = this.pos.compareTo(((BlockReference) obj).pos);
                if(c == 0){
                    c = Integer.compare(this.dimension, ((BlockReference) obj).dimension);
                }
                return c;
            }else {
                return -1;
            }
        }

    }


    public static class EntityReference implements Reference{

        private int dimension;
        private UUID uuid;

        private EntityReference(){

        }

        public EntityReference(int dimension, UUID uuid){
            this.dimension = dimension;
            this.uuid = uuid;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(dimension);
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            dimension = buf.readInt();
            uuid = new UUID(buf.readLong(), buf.readLong());
        }

        @Nonnull
        @Override
        public ITextComponent getName() {
            WorldServer world = net.minecraftforge.common.DimensionManager.getWorld(dimension, false);
            if (world == null){
                return new TextComponentString(TextFormatting.RED + "Entity: Unloaded");
            }
            Entity entity = world.getEntityFromUuid(uuid);
            if (entity == null){
                return new TextComponentString(TextFormatting.RED + "Entity: Dead");
            }
            ITextComponent displayName = entity.getDisplayName();
            /*
             *  It's possible a mod author decides to return null.
             */
            //noinspection ConstantConditions
            if(displayName != null){
                return displayName;
            }
            String text = entity.getName();
            if(text != null){
                return new TextComponentString(text);
            }
            /*
             *  If for some reason we still don't get the name, we return the class
             */
            return new TextComponentString(TextFormatting.YELLOW + entity.getClass().getName());
        }

        @Override
        public Location<Integer, BlockPos> currentPos() {
            WorldServer world = net.minecraftforge.common.DimensionManager.getWorld(dimension, false);
            if (world == null){
                return null;
            }
            Entity entity = world.getEntityFromUuid(uuid);
            if (entity == null){
                return null;
            }
            return new Location<>(entity.dimension, entity.getPosition());
        }

        @Override
        public int compareTo(@Nonnull Object obj) {
            if (obj instanceof EntityReference) {
                int c = Integer.compare(this.dimension, ((EntityReference) obj).dimension);
                if(c == 0){
                    c = this.uuid.compareTo(((EntityReference) obj).uuid);
                }
                return c;
            }else if(obj instanceof BlockReference){
                return 1;
            }else{
                return -1;
            }
        }
    }

}
