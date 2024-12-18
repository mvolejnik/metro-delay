package app.metrodelay.server.scheduler;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

public class QuartzInit implements AutoCloseable {

    private static final String CONTEXT_PARAM_DELAY = "schedulerdelay";
    private static final String CONTEXT_PARAM_INTERVAL = "schedulerjobinterval";
    private static final String CONTEXT_PARAM_RND = "schedulerjobintervalrandom";
    private static final String CONTEXT_PARAM_OPERATOR = "operators";
    private static final Map<String, URL> OPERATORS = new HashMap<>();
    private static final String CONTEXT_PARAM_VALUES_SEPARATOR = ";";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final Logger l = LogManager.getLogger(QuartzInit.class);

    public QuartzInit(
        Duration delay,
        Duration interval,
        Duration intervalRandom,
        String operators) {
        l.debug("contextInitialized::");
        app.metrodelay.server.scheduler.JobTimeParameters jobTimeParameteres = 
            new JobTimeParameters(delay, interval, intervalRandom);
        OPERATORS.putAll(initOperatorsResources(operators));
        OPERATORS.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .forEach(e -> initQuartzResourceJob(jobTimeParameteres, e.getKey(), e.getValue()));
    }

    private Map<String, URL> initOperatorsResources(final String operatorResourceList) {
        return Pattern.compile(CONTEXT_PARAM_VALUES_SEPARATOR)
                .splitAsStream(operatorResourceList)
                .map(s -> s.trim().split("="))
                .filter(e -> e[1] != null)
                .collect(Collectors.toMap(c -> c[0], c -> {
                    try {
                        return URI.create(c[1]).toURL();
                    } catch (MalformedURLException e) {
                        l.error("Unable to parse URL.", e);
                        return null;
                    }
                }));
    }

    public void close() {
        l.debug("shutting down::");
        shutdownQuartz();
    }

    private void initQuartzResourceJob(JobTimeParameters jobTimeParameters, String jobId, URL url) {
        Objects.requireNonNull(url, "url cannot be null");
        if (jobId == null) {
            jobId = url.getHost() + "~" + new Random().nextInt();
        }
        try {
            l.info("initQuartz:: Initing Quartz Scheduler with Delay [{}], interval [{}], random offset [{}], jobId [{}], jobUrl [{}]", jobTimeParameters.startDelay(),
                    jobTimeParameters.interval(), jobTimeParameters.maxOffset(), jobId, url.toExternalForm());
            Random rnd = new Random();
            ZonedDateTime startBaseline = ZonedDateTime.now().plusSeconds(jobTimeParameters.startDelay().toSeconds());
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            ZonedDateTime startAt = startBaseline.plusSeconds(rnd.nextInt((int) jobTimeParameters.maxOffset().toSeconds()));
            l.info("initQuartz:: scheduling job [{}] to start at [{}] and every [{}]", jobId,
                    DATE_TIME_FORMATTER.format(startAt), jobTimeParameters.interval());
            JobDetail job = newJob(GetUrlResourceJob.class)
                    .withIdentity(jobId + "~job", "download")
                    .usingJobData(GetUrlResourceJob.DATA_OPERATOR, jobId)
                    .usingJobData(GetUrlResourceJob.DATA_URL, url.toExternalForm())
                    .build();
            Trigger trigger = newTrigger().withIdentity(jobId + "~trigger", "download")
                    .startAt(Date.from(startAt.toInstant()))
                    .withSchedule(simpleSchedule().withIntervalInSeconds((int) jobTimeParameters.interval().toSeconds()).repeatForever())
                    .build();

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException se) {
            l.error("initQuartz:: unable to initialize quartz scheduler", se);
        }
    }

    private void shutdownQuartz() {
        l.info("shutdownQuartz::");
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.shutdown();
        } catch (SchedulerException se) {
            l.error("shutdownQuartz:: unable to shutdown quartz scheduler", se);
        }
    }

}
