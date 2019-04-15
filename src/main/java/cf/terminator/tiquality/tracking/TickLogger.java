package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.util.Copyable;
import cf.terminator.tiquality.util.SendableTreeMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class TickLogger implements IMessage, Copyable<TickLogger> {

    private final SendableTreeMap<Location, Metrics> data;
    private int ticks = 0;
    private long grantedNanos = 0L;
    private long consumedNanos = 0L;

    public TickLogger(){
        data = new SendableTreeMap<>();
    }

    public TickLogger(TickLogger logger) {
        this.ticks = logger.ticks;
        this.grantedNanos = logger.grantedNanos;
        this.data = logger.data.copy();
        this.consumedNanos = logger.consumedNanos;
    }

    /**
     * Logs tick time and calls.
     * @param location the location of the Block
     * @param nanos nanoseconds the block just consumed.
     */
    public void addNanosAndIncrementCalls(Location location, long nanos){
        if(location == null){
            return;
        }
        Metrics metrics = data.get(location);
        if(metrics == null){
            metrics = new Metrics();
            data.put(location, metrics);
        }
        metrics.recordTime(nanos);
        consumedNanos += nanos;
    }

    /**
     * Gets a snapshot of the collected data, ready for processing.
     * @return a copy of the ticklogger.
     */
    public TickLogger copy(){
        return new TickLogger(this);
    }

    /**
     * Notifies this TickLogger that a complete tick has ran.
     */
    public void addTick(long grantedNanos){
        ++ticks;
        this.grantedNanos += grantedNanos;
    }

    /**
     * Gets the total amount of ticks this TickLogger has ran.
     * @return the amount of ticks
     */
    public int getTicks(){
        return ticks;
    }

    /**
     * Gets the total amount of granted nanoseconds this TickLogger has received.
     * @return the amount of nanoseconds this TickLogger has received.
     */
    public long getGrantedNanos(){
        return grantedNanos;
    }

    /**
     * Gets the total amount of nanoseconds this TickLogger has consumed.
     * @return the amount of nanoseconds this TickLogger has consumed.
     */
    public long getConsumedNanos(){
        return consumedNanos;
    }

    /**
     * Gets the collected data from this TickLogger.
     * @return an unmodifiable TreeMap. Read access only.
     */
    public Map<Location, Metrics> getMetrics(){
        return Collections.unmodifiableMap(data);
    }

    /**
     * Resets all data this TickLogger has collected.
     */
    public void reset(){
        data.clear();
        grantedNanos = 0L;
        consumedNanos = 0L;
        ticks = 0;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(ticks);
        data.toBytes(buf);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        ticks = buf.readInt();
        data.fromBytes(buf);
    }

    public static class Metrics implements IMessage, Comparable<Metrics>,Copyable<Metrics> {

        private long nanoseconds = 0L;
        private int calls = 0;

        public Metrics(){}

        @Override
        public void fromBytes(ByteBuf buf) {
            nanoseconds = buf.readLong();
            calls = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(nanoseconds);
            buf.writeInt(calls);
        }

        public int getCalls(){
            return calls;
        }

        public void recordTime(long ns){
            nanoseconds = nanoseconds + ns;
            calls++;
        }

        private long getNanosPerCall(){
            return nanoseconds/calls;
        }

        public long getNanoseconds(){
            return nanoseconds;
        }

        @Override
        public int compareTo(@Nonnull Metrics o) {
            return Long.compare(this.getNanosPerCall(), o.getNanosPerCall());
        }

        @Override
        public Metrics copy() {
            Metrics clone = new Metrics();
            clone.nanoseconds = this.nanoseconds;
            clone.calls = this.calls;
            return clone;
        }

        public void add(Metrics other){
            this.calls += other.calls;
            this.nanoseconds += other.calls;
        }
    }

    public static class Location implements Comparable<Location>, IMessage, Copyable<Location> {

        private int world;
        private int x;
        private int y;
        private int z;
        private Type type;
        private UUID entityUUID;

        public Location(){}

        public Location(World world, BlockPos pos) {
            this.world = world.provider.getDimension();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.type = Type.BLOCK;
        }

        public Location(TiqualityEntity entity) {
            this.world = entity.getWorld().provider.getDimension();
            this.entityUUID = entity.getPersistentID();
            this.type = Type.ENTITY;
        }

        /**
         * Gets the block at this location, slightly faster.
         * @param server a cached MinecraftServer reference
         * @return block
         */
        public Block getBlock(MinecraftServer server){
            if(type != Type.BLOCK){
                throw new IllegalStateException("Tried to access block position on entity location");
            }
            return server.getWorld(world).getBlockState(new BlockPos(x,y,z)).getBlock();
        }

        /**
         * Gets the block at this location, slightly slower.
         * @return block
         */
        public Block getBlock(){
            return getBlock(FMLCommonHandler.instance().getMinecraftServerInstance());
        }

        /**
         * Gets the location type
         * @return the type.
         */
        public Type getType(){
            return type;
        }

        /**
         * Gets the entity, slightly faster
         * @return the entity
         */
        public Entity getEntity(MinecraftServer server){
            if(type != Type.ENTITY){
                throw new IllegalStateException("Tried to access entity on block location");
            }
            return server.getEntityFromUuid(entityUUID);
        }

        /**
         * Gets the entity, slightly slower
         * @return the entity
         */
        public Entity getEntity(){
            return getEntity(FMLCommonHandler.instance().getMinecraftServerInstance());
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof Location == false){
                return false;
            }
            Location other = ((Location) o);
            if(this.type != other.type){
                return false;
            }
            if(this.type == Type.BLOCK) {
                return other.world == this.world &&
                        other.x == this.x &&
                        other.y == this.y &&
                        other.z == this.z;
            }else{
                return this.entityUUID.equals(other.entityUUID);
            }
        }

        @Override
        public int compareTo(@Nonnull Location o) {
            int typeComp = this.type.compareTo(o.type);
            if(typeComp != 0){
                return typeComp;
            }
            if(type == Type.BLOCK) {
                return (world < o.world) ? -1 : ((world == o.world) ?
                        (x < o.x) ? -1 : ((x == o.x) ?
                                (y < o.y) ? -1 : ((y == o.y) ?
                                        Integer.compare(z, o.z) : 1) : 1) : 1);
            }else{
                return entityUUID.compareTo(o.entityUUID);
            }
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            type = Type.values()[buf.readInt()];
            switch(type) {
                case BLOCK:
                    world = buf.readInt();
                    x = buf.readInt();
                    y = buf.readInt();
                    z = buf.readInt();
                    break;
                case ENTITY:
                    entityUUID = new UUID(buf.readLong(), buf.readLong());
                    break;
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(type.ordinal());
            switch (type){
                case BLOCK:
                    buf.writeInt(world);
                    buf.writeInt(x);
                    buf.writeInt(y);
                    buf.writeInt(z);
                    break;
                case ENTITY:
                    buf.writeLong(entityUUID.getMostSignificantBits());
                    buf.writeLong(entityUUID.getLeastSignificantBits());
                    break;
            }
        }

        @Override
        public String toString(){
            switch (type){
                case BLOCK:
                    return TextFormatting.DARK_GRAY + "D" + TextFormatting.WHITE + world +
                            TextFormatting.DARK_GRAY + " X" + TextFormatting.WHITE + x +
                            TextFormatting.DARK_GRAY + " Y" + TextFormatting.WHITE + y +
                            TextFormatting.DARK_GRAY + " Z" + TextFormatting.WHITE + z;
                case ENTITY:
                    Entity e = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(entityUUID);
                    if(e == null){
                        return TextFormatting.RED + "Entity no longer exists";
                    }else {
                        return TextFormatting.DARK_GRAY + "D" + TextFormatting.WHITE + e.world.provider.getDimension() +
                                TextFormatting.DARK_GRAY + " X" + TextFormatting.WHITE + (int) e.posX +
                                TextFormatting.DARK_GRAY + " Y" + TextFormatting.WHITE + (int) e.posY +
                                TextFormatting.DARK_GRAY + " Z" + TextFormatting.WHITE + (int) e.posZ;
                    }
            }
            return TextFormatting.DARK_RED + "Unknown type";
        }

        @Override
        public Location copy() {
            Location clone = new Location();
            clone.type = this.type;
            switch (type){
                case BLOCK:
                    clone.world = this.world;
                    clone.x = this.x;
                    clone.y = this.y;
                    clone.z = this.z;
                    break;
                case ENTITY:
                    clone.entityUUID = new UUID(this.entityUUID.getMostSignificantBits(), this.entityUUID.getLeastSignificantBits());
                    break;
            }
            return clone;
        }

        public enum Type{
            BLOCK,
            ENTITY
        }
    }
}
