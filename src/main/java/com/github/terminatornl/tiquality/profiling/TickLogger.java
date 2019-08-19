package com.github.terminatornl.tiquality.profiling;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

public class TickLogger implements IMessage {

    private final TreeMap<ReferencedTickable.ReferenceId, TickTime> TIMES = new TreeMap<>();
    private int serverTickCount = 0;
    private int trackerTickCount = 0;
    private long grantedNanos = 0L;

    /**
     * Log the times
     *
     * @param reference the reference, nulls are ignored.
     * @param nanos     nanoseconds consumed
     */
    public void addNanosAndIncrementCalls(@Nullable ReferencedTickable.Reference reference, long nanos) {
        if (reference == null) {
            return;
        }
        ReferencedTickable.ReferenceId id = reference.getId();
        TickTime existingTimes = TIMES.get(id);
        if (existingTimes == null) {
            existingTimes = new TickTime();
            TIMES.put(id, existingTimes);
        }
        existingTimes.consumeNanosIncrementCalls(nanos);
    }

    public void addServerTick(long granted) {
        serverTickCount++;
        grantedNanos += granted;
    }

    public void addTrackerTick() {
        trackerTickCount++;
    }

    public TreeMap<ReferencedTickable.ReferenceId, TickTime> getTimes() {
        return TIMES;
    }

    public int getServerTicks() {
        return serverTickCount;
    }

    public int getTrackerTicks() {
        return trackerTickCount;
    }

    public long getGrantedNanos() {
        return grantedNanos;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(serverTickCount);
        buf.writeLong(grantedNanos);
        buf.writeInt(TIMES.size());
        for (Map.Entry<ReferencedTickable.ReferenceId, TickTime> entry : TIMES.entrySet()) {
            entry.getKey().toBytes(buf);
            entry.getValue().toBytes(buf);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        serverTickCount = buf.readInt();
        grantedNanos = buf.readLong();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            TIMES.put(new ReferencedTickable.ReferenceId(buf), new TickTime(buf));
        }
    }

}
