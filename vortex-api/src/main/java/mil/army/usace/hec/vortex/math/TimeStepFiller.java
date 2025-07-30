package mil.army.usace.hec.vortex.math;

import mil.army.usace.hec.vortex.MessageStore;
import mil.army.usace.hec.vortex.VortexGrid;
import mil.army.usace.hec.vortex.VortexProperty;
import mil.army.usace.hec.vortex.io.DataReader;
import mil.army.usace.hec.vortex.io.DataWriter;
import mil.army.usace.hec.vortex.io.TemporalDataReader;
import mil.army.usace.hec.vortex.util.Stopwatch;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mil.army.usace.hec.vortex.util.FilenameUtil.isSameFile;

class TimeStepFiller extends BatchGapFiller {
    private static final Logger LOGGER = Logger.getLogger(TimeStepFiller.class.getName());

    TimeStepFiller(Builder builder) {
        super(builder);
    }

    @Override
    public void run() {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        String templateBegin = MessageStore.getInstance().getMessage("time_step_filler_begin");
        String messageBegin = String.format(templateBegin);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageBegin);

        boolean isSourceEqualToDestination = isSameFile(source, destination);

        condenseVariables();

        AtomicInteger processed = new AtomicInteger();

        for (String variable : variables) {
            List<ZonedDateTime> startTimes = new ArrayList<>();
            Set<Duration> intervals = new HashSet<>();

            try (DataReader reader = DataReader.builder()
                    .path(source)
                    .variable(variable)
                    .build()) {

                int dtoCount = reader.getDtoCount();

                for (int i = 0; i < dtoCount; i++) {
                    VortexGrid vortexGrid = (VortexGrid) reader.getDto(i);
                    startTimes.add(vortexGrid.startTime());
                    intervals.add(vortexGrid.interval());

                    if (!isSourceEqualToDestination) {
                        DataWriter dataWriter = DataWriter.builder()
                                .destination(destination)
                                .data(Collections.singletonList(vortexGrid))
                                .build();

                        dataWriter.write();

                        processed.incrementAndGet();
                    }
                }

                if (intervals.size() != 1) {
                    LOGGER.log(Level.SEVERE, "Data interval must be consistent");
                    continue;
                }

                Duration interval = intervals.iterator().next();

                Set<ZonedDateTime> startTimesSet = new HashSet<>(startTimes);
                List<ZonedDateTime> missingStartTimes = new ArrayList<>();

                Duration timeStep = interval.isZero() ? getMostCommonInterval(startTimes) : interval;
                if (timeStep == null) {
                    LOGGER.log(Level.SEVERE, "Could not determine time step");
                    continue;
                }

                ZonedDateTime timeN = startTimes.get(startTimes.size() - 1);
                ZonedDateTime time = startTimes.get(0);
                while (time.isBefore(timeN)) {
                    if (!startTimesSet.contains(time)) {
                        missingStartTimes.add(time);
                    }
                    time = time.plus(timeStep);
                }

                DataReader copy = DataReader.copy(reader);
                try (TemporalDataReader temporalDataReader = TemporalDataReader.create(copy)) {
                    for (ZonedDateTime missingStartTime : missingStartTimes) {
                        ZonedDateTime missingEndTime = missingStartTime.plus(interval);
                        temporalDataReader.read(missingStartTime, missingEndTime).ifPresent(vortexGrid -> {
                            DataWriter dataWriter = DataWriter.builder()
                                    .destination(destination)
                                    .data(Collections.singletonList(vortexGrid))
                                    .build();

                            dataWriter.write();

                            processed.incrementAndGet();
                        });
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e, e::getMessage);
            }
        }

        stopwatch.end();
        String timeMessage = "Batch time-step-fill time: " + stopwatch;
        LOGGER.info(timeMessage);

        String templateEnd = MessageStore.getInstance().getMessage("time_step_filler_end");
        String messageEnd = String.format(templateEnd, processed, destination);
        support.firePropertyChange(VortexProperty.COMPLETE.toString(), null, messageEnd);

        String templateTime = MessageStore.getInstance().getMessage("time_step_filler_time");
        String messageTime = String.format(templateTime, stopwatch);
        support.firePropertyChange(VortexProperty.STATUS.toString(), null, messageTime);
    }

    private static Duration getMostCommonInterval(List<ZonedDateTime> times) {
        Map<Duration, Integer> durationCounts = new HashMap<>();
        for (int i = 1; i < times.size(); i++) {
            Duration interval = Duration.between(times.get(0), times.get(1));
            durationCounts.putIfAbsent(interval, 0);
            durationCounts.computeIfPresent(interval, (k, count) -> count + 1);
        }

        Map<Duration, Integer> sorted = durationCounts.entrySet().stream()
                .sorted(Map.Entry.<Duration, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // Keeps the order
                ));

        List<Integer> counts = new ArrayList<>(sorted.values());

        if (counts.size() > 1 && counts.get(0) <= counts.get(1)) {
            return null;
        }

        return sorted.keySet().iterator().next();
    }
}
