package cf.terminator.tiquality.api;

import cf.terminator.tiquality.tracking.TickLogger;
import cf.terminator.tiquality.util.Entry3;
import cf.terminator.tiquality.util.SynchronizedAction;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

public class DataProcessing {

    public static ArrayList<Map.Entry<TickLogger.Location, TickLogger.Metrics>> sortByTime(Collection<Map.Entry<TickLogger.Location, TickLogger.Metrics>> data){
        ArrayList<Map.Entry<TickLogger.Location, TickLogger.Metrics>> list = new ArrayList<>(data);
        list.sort(Comparator.comparing(Map.Entry::getValue));
        return list;
    }

    public static ArrayList<Map.Entry<TickLogger.Location, TickLogger.Metrics>> sortByLocation(Collection<Map.Entry<TickLogger.Location, TickLogger.Metrics>> data){
        ArrayList<Map.Entry<TickLogger.Location, TickLogger.Metrics>> list = new ArrayList<>(data);
        list.sort(Comparator.comparing(Map.Entry::getKey));
        return list;
    }

    /**
     * Finds the associated blocks belonging to block positions in a safe way.
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     *
     * @return an ArrayList containing additional info
     */
    public static ArrayList<Entry3<Block, TickLogger.Location, TickLogger.Metrics>> findBlocks(Collection<Map.Entry<TickLogger.Location, TickLogger.Metrics>> data){
        return SynchronizedAction.run(new SynchronizedAction.Action<ArrayList<Entry3<Block, TickLogger.Location, TickLogger.Metrics>>>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<ArrayList<Entry3<Block, TickLogger.Location, TickLogger.Metrics>>> variable) {
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                ArrayList<Entry3<Block, TickLogger.Location, TickLogger.Metrics>> list = new ArrayList<>();
                for(Map.Entry<TickLogger.Location, TickLogger.Metrics> s : data){
                    if (s.getKey().getType() == TickLogger.Location.Type.BLOCK){
                        list.add(new Entry3<>(s.getKey().getBlock(server), s.getKey(), s.getValue()));
                    }
                }
                variable.set(list);
            }
        });
    }


    /**
     * Finds the associated entities belonging to block positions in a safe way.
     *
     * BE WARNED: If you're in another thread, AND the server thread is WAITING (blocked) on your current thread,
     * this will cause a deadlock!
     *
     * @return an ArrayList containing additional info
     */
    public static ArrayList<Entry3<Entity, TickLogger.Location, TickLogger.Metrics>> findEntities(Collection<Map.Entry<TickLogger.Location, TickLogger.Metrics>> data){
        return SynchronizedAction.run(new SynchronizedAction.Action<ArrayList<Entry3<Entity, TickLogger.Location, TickLogger.Metrics>>>() {
            @Override
            public void run(SynchronizedAction.DynamicVar<ArrayList<Entry3<Entity, TickLogger.Location, TickLogger.Metrics>>> variable) {
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                ArrayList<Entry3<Entity, TickLogger.Location, TickLogger.Metrics>> list = new ArrayList<>();
                for(Map.Entry<TickLogger.Location, TickLogger.Metrics> s : data){
                    if (s.getKey().getType() == TickLogger.Location.Type.ENTITY){
                        list.add(new Entry3<>(s.getKey().getEntity(server), s.getKey(), s.getValue()));
                    }
                }
                variable.set(list);
            }
        });
    }


    /**
     * Gets the last items in a list.
     * @return an ArrayList containing the last 'X' items
     */
    public static <V> List<V> getLast(ArrayList<V> data, int amount){
        return data.subList(Math.max(0,data.size()-amount),data.size());
    }

    /**
     * Gets the total amount of nanoseconds consumed.
     * @param logger the tick logger
     * @return the total amount of nanoseconds consumed, note: this is NOT an average!
     */
    public static long getTotalNanos(TickLogger logger){
        long totalCounted = 0L;
        for(Map.Entry<TickLogger.Location, TickLogger.Metrics> s : logger.getMetrics().entrySet()){
            totalCounted += s.getValue().getNanoseconds();
        }
        return totalCounted;
    }

    /**
     * Gets the average amount of nanoseconds consumed.
     * @param logger the tick logger
     * @return the total amount of nanoseconds consumed, note: this IS an average!
     */
    public static long getAverageNanos(TickLogger logger){
        return getTotalNanos(logger) / logger.getTicks();
    }

}
