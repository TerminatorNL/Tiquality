package cf.terminator.tiquality.store;

import cf.terminator.tiquality.util.SendableTreeMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public class TickLogger implements IMessage {

    private final SendableTreeMap<Location, Metrics> data = new SendableTreeMap<>();
    private int ticks = 0;

    public TickLogger(){}

    /**
     * Logs tick time and calls.
     * @param location the location of the Block
     * @param nanos nanoseconds the block just consumed.
     */
    public void addNanosAndIncrementCalls(Location location, long nanos){
        Metrics metrics = data.get(location);
        if(metrics == null){
            metrics = new Metrics();
            data.put(location, metrics);
        }
        ++metrics.calls;
        metrics.nanoseconds =+ nanos;
    }

    /**
     * Notifies this TickLogger that a complete tick has ran.
     */
    public void addTick(){
        ++ticks;
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

    public static class Metrics implements IMessage {

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
    }

    public static class Location implements Comparable<Location>, IMessage{

        private int world;
        private int x;
        private int y;
        private int z;

        public Location(){}

        public Location(World world, BlockPos pos) {
            this.world = world.provider.getDimension();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }

        /**
         * Gets the block at this location, slightly faster.
         * @param server a cached MinecraftServer reference
         * @return block
         */
        public Block getBlock(MinecraftServer server){
            return server.getWorld(world).getBlockState(new BlockPos(x,y,z)).getBlock();
        }

        /**
         * Gets the block at this location, slightly slower.
         * @return block
         */
        public Block getBlock(){
            return FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(world).getBlockState(new BlockPos(x,y,z)).getBlock();
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof Location == false){
                return false;
            }
            Location other = ((Location) o);
            return  other.world == this.world &&
                    other.x == this.x &&
                    other.y == this.y &&
                    other.z == this.z;
        }

        @Override
        public int compareTo(@Nonnull Location o) {
            return (world < o.world) ? -1 : ((world == o.world) ?
                    (x < o.x) ? -1 : ((x == o.x) ?
                            (y < o.y) ? -1 : ((y == o.y) ?
                                    Integer.compare(z, o.z) : 1) : 1): 1);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            world = buf.readInt();
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(world);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
        }
    }
}
