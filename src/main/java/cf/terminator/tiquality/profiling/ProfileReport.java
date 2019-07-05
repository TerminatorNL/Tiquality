package cf.terminator.tiquality.profiling;

import cf.terminator.tiquality.util.Constants;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.*;

import static cf.terminator.tiquality.util.Utils.TWO_DECIMAL_FORMATTER;

public class ProfileReport implements IMessage {

    private double serverTPS;
    private double trackerTPS;
    private int serverTicks;
    private int trackerTicks;
    private long grantedNanos;
    private long totalNanosUsed = 0L;
    private final long startTimeNanos;
    private final long endTimeNanos;
    private final TreeSet<AnalyzedComponent> analyzedComponents = new TreeSet<>();
    private final TreeMap<String, TickTime> classTimes = new TreeMap<>();
    private NavigableSet<Map.Entry<String, TickTime>> classTimesSorted = null;

    public ProfileReport(long startTimeNanos, long endTimeNanos, double serverTPS, double trackerTPS, int serverTicks, int trackerTicks, long grantedNanos, Collection<AnalyzedComponent> analyzedComponents){
        this.startTimeNanos = startTimeNanos;
        this.endTimeNanos = endTimeNanos;
        this.serverTPS = serverTPS;
        this.trackerTPS = trackerTPS;
        this.serverTicks = serverTicks;
        this.trackerTicks = trackerTicks;
        this.grantedNanos = grantedNanos;
        this.analyzedComponents.addAll(analyzedComponents);

        for(AnalyzedComponent component : this.analyzedComponents){
            /*
                Total nanoseconds used
             */
            totalNanosUsed += component.getTimes().getNanosConsumed();

            /*
                Class times
             */
            TickTime time = classTimes.get(component.getReferencedClass());
            if(time == null){
                classTimes.put(component.getReferencedClass(),new TickTime(component.getTimes()));
            }else{
                time.add(component.getTimes());
            }
        }
    }

    public double getServerTPS(){
        return serverTPS;
    }

    public double getTrackerTPS() {
        return trackerTPS;
    }

    public int getServerTicks() {
        return serverTicks;
    }

    public int getTrackerTicks() {
        return trackerTicks;
    }

    public String getTrackerImpactPercentage(TickTime time){
        double factor = (double) time.getNanosConsumed() / (double) this.grantedNanos;
        return TWO_DECIMAL_FORMATTER.format(Math.round(factor * 10000D) / 100D);
    }

    public String getServerImpactPercentage(TickTime time){
        double nanosPassedOnServer = (Constants.NS_IN_TICK_DOUBLE * (double) serverTicks * serverTPS/20D);

        double factor = (double) time.getNanosConsumed() / nanosPassedOnServer;
        return TWO_DECIMAL_FORMATTER.format(Math.round(factor * 10000D) / 100D);
    }

    public double getMuPerTick(TickTime time){
        return ((double) time.getNanosConsumed() / 1000) / (double) trackerTicks;
    }

    public double getCallsPerTick(TickTime time){
        return ((double) time.getCalls()) / (double) trackerTicks;
    }

    public NavigableSet<AnalyzedComponent> getAnalyzedComponents(){
        return Collections.unmodifiableNavigableSet(analyzedComponents);
    }

    public NavigableMap<String, TickTime> getClassTimes(){
        return Collections.unmodifiableNavigableMap(classTimes);
    }

    public NavigableSet<Map.Entry<String, TickTime>> getClassTimesSorted(){
        if(classTimesSorted != null){
            return classTimesSorted;
        }
        TreeSet<Map.Entry<String, TickTime>> set = new TreeSet<>(Comparator.comparing(Map.Entry::getValue));
        set.addAll(classTimes.entrySet());
        classTimesSorted = Collections.unmodifiableNavigableSet(set);
        return classTimesSorted;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.serverTPS = buf.readDouble();
        this.trackerTPS = buf.readDouble();
        this.serverTicks = buf.readInt();
        this.trackerTicks =  buf.readInt();
        this.grantedNanos = buf.readLong();

        int size = buf.readInt();
        for(int i=0;i<size;i++){
            analyzedComponents.add(new AnalyzedComponent(buf));
        }

        int size2 = buf.readInt();
        for(int i=0;i<size2;i++){
            classTimes.put(ByteBufUtils.readUTF8String(buf), new TickTime(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(serverTPS);
        buf.writeDouble(trackerTPS);
        buf.writeInt(serverTicks);
        buf.writeInt(trackerTicks);
        buf.writeLong(grantedNanos);

        buf.writeInt(analyzedComponents.size());
        for(AnalyzedComponent entry : analyzedComponents){
            entry.toBytes(buf);
        }

        buf.writeInt(classTimes.size());
        for(Map.Entry<String, TickTime> entry : classTimes.entrySet()){
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            entry.getValue().toBytes(buf);
        }
    }

    public long getTotalNanosUsed() {
        return totalNanosUsed;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public long getEndTimeNanos() {
        return endTimeNanos;
    }
}
