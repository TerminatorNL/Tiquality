package cf.terminator.tiquality.profiling;

import cf.terminator.tiquality.api.Location;
import cf.terminator.tiquality.util.ForgetFulProgrammerException;
import cf.terminator.tiquality.util.Unloaded;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class ReferencedTickable {

    public static abstract class Reference {
        abstract public ReferenceId getId();
        abstract @Nonnull public ITextComponent getName();
        abstract @Nullable public Location<Integer, BlockPos> currentPos();
        abstract @Nonnull public Class getReferencedClass();
        abstract @Nullable public ResourceLocation getResourceLocation();
    }

    public static class ReferenceId implements Comparable, IMessage{

        public enum Type{
            BLOCK((byte) 0),
            ENTITY((byte) 1);

            private final byte b;

            Type(byte b) {
                this.b = b;
            }

            public byte getByte() {
                return b;
            }

            public boolean isEntity(){
                return this == ENTITY;
            }

            public boolean isBlock(){
                return this == BLOCK;
            }
        }

        Type type;
        int data1;
        int data2;
        int data3;
        int data4;
        int data5;


        public ReferenceId(ByteBuf buf) {
            fromBytes(buf);
        }

        ReferenceId(Type type, int data1, int data2, int data3, int data4, int data5) {
            this.type = type;
            this.data1 = data1;
            this.data2 = data2;
            this.data3 = data3;
            this.data4 = data4;
            this.data5 = data5;
        }

        public static ReferenceId getId(Reference tickable){
            if(tickable instanceof BlockReference){
                BlockReference reference = (BlockReference) tickable;
                return new ReferenceId(Type.BLOCK, reference.dimension, reference.pos.getX(), reference.pos.getY(), reference.pos.getZ(),0);
            }else if(tickable instanceof EntityReference){
                EntityReference reference = (EntityReference) tickable;
                return new ReferenceId(Type.ENTITY, reference.dimension, (int) (reference.uuid.getMostSignificantBits() >> 32), (int) reference.uuid.getMostSignificantBits(),(int) (reference.uuid.getLeastSignificantBits() >> 32), (int) reference.uuid.getLeastSignificantBits());
            }else{
                throw new ForgetFulProgrammerException("unidentified class: " + tickable.getClass());
            }
        }

        public Type getType() {
            return type;
        }

        @Nonnull
        public Reference convert(){
            switch (type){
                case BLOCK:
                    /* Block */
                    return new BlockReference(data1,new BlockPos(data2,data3,data4));
                case ENTITY:
                    /* Entity */
                    long mostSignificantPart1 = ((long) data2) << 32;
                    long mostSignificantPart2 = ((long) data3) & 0xFFFFFFFFL;
                    long mostSignificantBits = mostSignificantPart1 | mostSignificantPart2;

                    long leastSignificantPart1 = ((long) data4) << 32;
                    long leastSignificantPart2 = ((long) data5) & 0xFFFFFFFFL;
                    long leastSignificantBits = leastSignificantPart1 | leastSignificantPart2;

                    return new EntityReference(data1, new UUID(mostSignificantBits, leastSignificantBits));
                default:
                    /* Unknown */
                    throw new ForgetFulProgrammerException("unidentified type: " + type);
            }
        }

        @Override
        public int compareTo(@Nonnull Object o) {
            if (o instanceof ReferenceId) {
                ReferenceId other = (ReferenceId) o;
                int c = Integer.compare(this.type.getByte(), other.type.getByte());
                if(c != 0){
                    return c;
                }
                c = Integer.compare(this.data1, other.data1);
                if(c != 0){
                    return c;
                }
                c = Integer.compare(this.data2, other.data2);
                if(c != 0){
                    return c;
                }
                c = Integer.compare(this.data3, other.data3);
                if(c != 0){
                    return c;
                }
                c = Integer.compare(this.data4, other.data4);
                if(c != 0){
                    return c;
                }
                return Integer.compare(this.data5, other.data5);
            } else {
                return -1;
            }
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            type = Type.values()[buf.readByte()];
            data1 = buf.readInt();
            data2 = buf.readInt();
            data3 = buf.readInt();
            data4 = buf.readInt();
            data5 = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(type.ordinal());
            buf.writeInt(data1);
            buf.writeInt(data2);
            buf.writeInt(data3);
            buf.writeInt(data4);
            buf.writeInt(data5);
        }
    }

    public static class BlockReference extends Reference{

        private int dimension;
        private BlockPos pos;

        public BlockReference(int dimension, BlockPos pos){
            this.dimension = dimension;
            this.pos = pos;
        }

        @Override
        public ReferenceId getId() {
            return new ReferenceId(ReferenceId.Type.BLOCK, dimension, pos.getX(), pos.getY(), pos.getZ(),0);
        }

        @Nonnull
        @Override
        public ITextComponent getName() {
            WorldServer world = DimensionManager.getWorld(dimension, true);
            if(world == null){
                net.minecraftforge.common.DimensionManager.initDimension(dimension);
                world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            }
            if (world == null){
                return new TextComponentString(TextFormatting.RED + "World: Unloaded");
            }
            Block block = world.getBlockState(pos).getBlock();
            return new TextComponentString(block.getLocalizedName());
        }

        @Nonnull
        public Class getReferencedClass(){
            WorldServer world = DimensionManager.getWorld(dimension, true);
            if(world == null){
                net.minecraftforge.common.DimensionManager.initDimension(dimension);
                world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            }
            if (world == null){
                return Unloaded.World.class;
            }
            return world.getBlockState(pos).getBlock().getClass();
        }

        @Nullable
        @Override
        public ResourceLocation getResourceLocation() {
            WorldServer world = DimensionManager.getWorld(dimension, true);
            if(world == null){
                net.minecraftforge.common.DimensionManager.initDimension(dimension);
                world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            }
            if (world == null){
                return null;
            }
            return world.getBlockState(pos).getBlock().getRegistryName();
        }

        @Nullable
        @Override
        public Location<Integer, BlockPos> currentPos() {
            return new Location<>(dimension, pos);
        }


    }


    public static class EntityReference extends Reference{

        private int dimension;
        private UUID uuid;

        public EntityReference(int dimension, UUID uuid){
            this.dimension = dimension;
            this.uuid = uuid;
        }

        @Override
        public ReferenceId getId() {
            return new ReferenceId(ReferenceId.Type.ENTITY, dimension, (int) (uuid.getMostSignificantBits() >> 32), (int) uuid.getMostSignificantBits(),(int) (uuid.getLeastSignificantBits() >> 32), (int) uuid.getLeastSignificantBits());
        }

        @Nonnull
        @Override
        public ITextComponent getName() {
            WorldServer world = DimensionManager.getWorld(dimension, true);
            if(world == null){
                net.minecraftforge.common.DimensionManager.initDimension(dimension);
                world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            }
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

        @Nullable
        @Override
        public Location<Integer, BlockPos> currentPos() {
            WorldServer world = DimensionManager.getWorld(dimension, true);
            if(world == null){
                net.minecraftforge.common.DimensionManager.initDimension(dimension);
                world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            }
            Entity entity = world.getEntityFromUuid(uuid);
            if (entity == null){
                return null;
            }
            return new Location<>(entity.dimension, entity.getPosition());
        }

        @Nonnull
        @Override
        public Class getReferencedClass() {
            WorldServer world = DimensionManager.getWorld(dimension, true);
            if(world == null){
                net.minecraftforge.common.DimensionManager.initDimension(dimension);
                world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            }
            if (world == null){
                return Unloaded.World.class;
            }
            Entity entity = world.getEntityFromUuid(uuid);
            if(entity == null){
                return Unloaded.Entity.class;
            }
            return entity.getClass();
        }

        @Nullable
        @Override
        public ResourceLocation getResourceLocation() {
            WorldServer world = DimensionManager.getWorld(dimension, true);
            if(world == null){
                net.minecraftforge.common.DimensionManager.initDimension(dimension);
                world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            }
            if (world == null){
                return null;
            }
            Entity entity = world.getEntityFromUuid(uuid);
            if(entity == null){
                return null;
            }
            EntityEntry entry = EntityRegistry.getEntry(entity.getClass());
            if(entry == null){
                return null;
            }
            return entry.getRegistryName();
        }
    }

}
