package cf.terminator.tiquality.profiling;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TiqualityException;
import cf.terminator.tiquality.interfaces.Tracker;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleProfiler implements Runnable {

    private final Tracker tracker;
    private final long durationInMs;
    private final ProfilePrinter printer;
    private long startTimeNanos;
    private ProfilingKey key;

    public SimpleProfiler(Tracker tracker, long durationInMs, ProfilePrinter printer){
        this.tracker = tracker;
        this.durationInMs = durationInMs;
        this.printer = printer;
    }

    public void start() throws TiqualityException{
        key = tracker.startProfiler(System.currentTimeMillis() + durationInMs);
        startTimeNanos = System.nanoTime();
        new Thread(this, "Tiquality profiler").start();
    }

    public static SortedSet<AnalyzedComponent> analyzeComponents(ProfileMonitor monitor, TickLogger logger) throws InterruptedException {
        TreeMap<ReferencedTickable.ReferenceId, TickTime> times = logger.getTimes();
        Iterator<Map.Entry<ReferencedTickable.ReferenceId, TickTime>> referenceIterator = times.entrySet().iterator();
        SortedSet<AnalyzedComponent> finishedAnalyzers = Collections.synchronizedSortedSet(new TreeSet<>());

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(16, 32, 10L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        Tiquality.SCHEDULER.scheduleWait(new Runnable() {
            @Override
            public void run() {
                monitor.progressUpdate(new TextComponentString("Retrieving results in main thread..."));
                while (referenceIterator.hasNext()) {
                    Map.Entry<ReferencedTickable.ReferenceId, TickTime> entry = referenceIterator.next();
                    referenceIterator.remove(); /* Lots of data can be in here, Free up some sweet sweet RAM. */

                    threadPool.submit(new AnalyzedComponent.Analyzer(entry.getKey().convert(), entry.getValue(), finishedAnalyzers));
                }
            }
        });

        threadPool.shutdown();

        while (threadPool.awaitTermination(5, TimeUnit.SECONDS) == false) {
            long completed = threadPool.getCompletedTaskCount();
            long total = threadPool.getTaskCount();

            String percentage = Math.round((double) completed / (double) total * 100) + "%";
            monitor.progressUpdate(new TextComponentString("Working: "
                    + TextFormatting.WHITE + completed + TextFormatting.GRAY + "/" + TextFormatting.WHITE + total
                    + TextFormatting.GRAY + " (" + TextFormatting.WHITE + percentage + TextFormatting.GRAY + ")"));
        }

        return finishedAnalyzers;
    }

    @Override
    public void run() {
        try {
            if(durationInMs <= 7000) {
                Thread.sleep(durationInMs);
            }else{
                long remainder = durationInMs % 5000;
                Thread.sleep(remainder);
                printer.progressUpdate(new TextComponentString("Profiler finishes in " + TextFormatting.WHITE + ((durationInMs - remainder) / 1000) + TextFormatting.GRAY + " seconds..."));
                long fiveSecondSteps = (durationInMs - remainder) / 5000;
                for(int i=0;i<fiveSecondSteps;i++){
                    Thread.sleep(5000);
                    long timeElapsed = (i + 1) * 5000;
                    long secondsLeft = (durationInMs - remainder - timeElapsed) / 1000;
                    if(secondsLeft > 0) {
                        printer.progressUpdate(new TextComponentString("Profiler finishes in " + TextFormatting.WHITE + secondsLeft + TextFormatting.GRAY + " seconds..."));
                    }
                }
            }
        } catch (InterruptedException e) {
            Tiquality.LOGGER.warn("Failed to sleep for " + durationInMs + " ms. Profiling aborted.");
            e.printStackTrace();
            return;
        }
        final TickLogger logger;
        final long endTimeNanos = System.nanoTime();
        try {
            logger = tracker.stopProfiler(key);
        } catch (TiqualityException.TrackerWasNotProfilingException | TiqualityException.InvalidKeyException e) {
            Tiquality.LOGGER.warn("Tried to stop profiler, but an exception occurred. This probably indicates a collision. Nothing fatal, but this should be looked in to. This means we do not have any results, however.");
            e.printStackTrace();
            return;
        }

        printer.progressUpdate(new TextComponentString("Profiling complete. Analyzing results synchronously..."));

        SortedSet<AnalyzedComponent> components;
        try {
            components = analyzeComponents(printer, logger);
        } catch (InterruptedException e) {
            printer.progressUpdate(new TextComponentString(TextFormatting.RED + "Interrupted!"));
            return;
        }
        printer.progressUpdate(new TextComponentString("Generating report asynchronously..."));


        ProfileReport report = new ProfileReport(startTimeNanos, endTimeNanos, logger, tracker.getInfo(), components);
        printer.progressUpdate(new TextComponentString("Done!"));
        printer.report(report);
    }


    public interface ProfileMonitor {
        /**
         * Progress prompt (Activated once every 5 seconds)
         */
        void progressUpdate(ITextComponent message);
    }

    public interface ProfilePrinter extends ProfileMonitor {
        /**
         * Returned report
         * @param report the report
         */
        void report(ProfileReport report);
    }
}
