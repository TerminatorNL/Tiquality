package cf.terminator.tiquality.util;

import cf.terminator.tiquality.api.DataProcessing;
import cf.terminator.tiquality.tracking.TickLogger;
import cf.terminator.tiquality.tracking.TrackerBase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.*;

public class SimpleProfiler {

    private final TrackerBase[] trackers;
    private final HashMap<TrackerBase, TickLogger> loggers = new HashMap<>();
    private double ticks = 0;

    public SimpleProfiler(TrackerBase ... tracker){
        this.trackers = tracker;
    }

    public void start(){
        for(TrackerBase tracker : trackers){
            tracker.setProfileEnabled(true);
        }
    }

    public void stop(){
        for(TrackerBase tracker : trackers){
            TickLogger logger = tracker.stopProfiler();
            if(logger != null) {
                ticks += logger.getTicks();
                loggers.put(tracker, logger);
            }
        }
        /* Attempts to take the average of all ticks, not really needed, but hey. */
        ticks = Math.max(ticks / loggers.size(),1);
    }

    public List<TextComponentString> createReport(){
        if(ticks < 2){
            List<TextComponentString> list = new ArrayList<>();
            list.add(new TextComponentString(TextFormatting.RED + "Not enough samples, must run at least 2 ticks."));
            return list;
        }

        HashMap<TickLogger.Location, TickLogger.Metrics> data = new HashMap<>();

        /* Merge results from all trackers */
        for(Map.Entry<TrackerBase, TickLogger> e1 : loggers.entrySet()){
            for(Map.Entry<TickLogger.Location, TickLogger.Metrics> e2 : e1.getValue().getMetrics().entrySet()){
                TickLogger.Metrics oldMetrics = data.put(e2.getKey(), e2.getValue());
                if(oldMetrics != null){
                    e2.getValue().add(oldMetrics);
                }
            }
        }

        Collection<Map.Entry<TickLogger.Location, TickLogger.Metrics>> collection = data.entrySet();

        ArrayList<Entry3<Block, TickLogger.Location, TickLogger.Metrics>> blocks = DataProcessing.findBlocks(collection);
        ArrayList<Entry3<Entity, TickLogger.Location, TickLogger.Metrics>> entities = DataProcessing.findEntities(collection);

        List<Entry3<Identifier, TickLogger.Location, TickLogger.Metrics>> summary = new ArrayList<>();
        for(Entry3<Block, TickLogger.Location, TickLogger.Metrics> b : blocks){
            summary.add(new Entry3<>(new Identifier(b.getFirst()), b.getSecond(), b.getThird()));
        }
        for(Entry3<Entity, TickLogger.Location, TickLogger.Metrics> e : entities){
            summary.add(new Entry3<>(new Identifier(e.getFirst()), e.getSecond(), e.getThird()));
        }
        summary.sort(Comparator.comparing(Entry3::getThird));
        summary = summary.subList(Math.max(summary.size()-100,0),summary.size());


        /* BLOCKS */
        LinkedList<TextComponentString> result = new LinkedList<>();
        for(Entry3<Identifier, TickLogger.Location, TickLogger.Metrics> entry : summary){
            int micros_tick = (int) Math.floor((entry.getThird().getNanoseconds()/1000D)/ticks);
            if(micros_tick == 0){
                continue;
            }

            String time = micros_tick + TextFormatting.DARK_GRAY.toString() + "µs/t";
            String block_name = entry.getFirst().toString();
            String location = entry.getSecond().toString();

            result.add(
                new TextComponentString(TextFormatting.WHITE + time + " " + TextFormatting.WHITE + " " + location + TextFormatting.WHITE + " " + block_name)
            );
        }


        return result;
    }


    private static class Identifier{

        private final String text;

        private Identifier(Entity e) {
            if(e != null){
                text = e.getName();
            }else{
                text = "Unknown";
            }
        }

        private Identifier(Block b) {
            text = Block.REGISTRY.getNameForObject(b).toString();
        }

        @Override
        public String toString(){
            return text;
        }
    }
}
