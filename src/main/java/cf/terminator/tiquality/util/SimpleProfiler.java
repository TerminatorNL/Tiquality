package cf.terminator.tiquality.util;

import cf.terminator.tiquality.api.DataProcessing;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.*;

public class SimpleProfiler {

    private final Tracker[] trackers;
    private final HashMap<Tracker, TickLogger> loggers = new HashMap<>();
    private Long consumedNanos = 0L;
    private Long grantedNanos = 0L;

    public SimpleProfiler(Tracker... tracker){
        this.trackers = tracker;
    }

    public void start(){
        for(Tracker tracker : trackers){
            tracker.setProfileEnabled(true);
        }
    }

    public void stop(){
        for(Tracker tracker : trackers){
            TickLogger logger = tracker.stopProfiler();
            if(logger != null) {
                consumedNanos += logger.getConsumedNanos();
                grantedNanos += logger.getGrantedNanos();
                loggers.put(tracker, logger);
            }
        }
    }

    public List<TextComponentString> createReport(){
        if(consumedNanos == 0 || grantedNanos == 0){
            List<TextComponentString> list = new ArrayList<>();
            list.add(new TextComponentString(TextFormatting.RED + "Not enough samples"));
            return list;
        }

        HashMap<TickLogger.Location, TickLogger.Metrics> data = new HashMap<>();

        /* Merge results from all trackers */
        for(Map.Entry<Tracker, TickLogger> e1 : loggers.entrySet()){
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
            int micros_call = (int) Math.floor((entry.getThird().getNanoseconds()/1000D)/entry.getThird().getCalls());
            if(micros_call == 0){
                continue;
            }

            String time = micros_call + TextFormatting.DARK_GRAY.toString() + "µs/c";
            String block_name = entry.getFirst().toString();
            String location = entry.getSecond().toString();

            result.add(
                new TextComponentString(TextFormatting.WHITE + time + " " + TextFormatting.WHITE + " " + location + TextFormatting.WHITE + " " + block_name)
            );
        }
        result.add(
                new TextComponentString(TextFormatting.ITALIC.toString() + TextFormatting.DARK_GRAY.toString() + "This is a very basic profiler, the times shown above are per call, not per tick!")
        );

        double fractionUsed = consumedNanos.doubleValue() / grantedNanos.doubleValue();
        String percentUsed = Math.round(fractionUsed * 10000D)/100D + "%";
        if(consumedNanos > grantedNanos){
            result.add(
                    new TextComponentString(TextFormatting.RED + "Warning: Used more time than allocated, but server had time to spare!")
            );
            result.add(
                    new TextComponentString("Consumed: " + TextFormatting.WHITE + percentUsed + TextFormatting.RED)
            );
        }else{
            result.add(
                    new TextComponentString(TextFormatting.GREEN + "Consumed time: " + TextFormatting.WHITE + percentUsed)
            );
        }
        if(consumedNanos >= grantedNanos) {
            result.add(
                    new TextComponentString(TextFormatting.GRAY + "Your blocks do not run at full speed.")
            );
        }else{
            result.add(
                    new TextComponentString(TextFormatting.GREEN + "Your blocks run at full speed!")
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
