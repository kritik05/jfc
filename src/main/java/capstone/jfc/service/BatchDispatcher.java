package capstone.jfc.service;
import capstone.jfc.model.JobEntity;
import capstone.jfc.producer.JobProducer;
import capstone.jfc.model.ToolEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.repository.ToolConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class BatchDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchDispatcher.class);
    private final JobRepository jobRepository;
    private final ToolConfigRepository toolConfigRepository;
    private final JobProducer jobProducer;

    // Global concurrency limit (aka "max batch size" overall)
    @Value("${jfc.global-concurrency-limit:5}")
    private int globalConcurrencyLimit;

    public BatchDispatcher(JobRepository jobRepository,
                           ToolConfigRepository toolConfigRepository,
                           JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.toolConfigRepository = toolConfigRepository;
        this.jobProducer = jobProducer;
    }

    @Scheduled(fixedRate = 10000)
    public void dispatchJobs() {
        LOGGER.info("=== Starting dispatch cycle ===");

        // 1) Find how many jobs are already IN_PROGRESS across ALL tools
        int allInProgressCount = jobRepository.countByStatus(JobStatus.IN_PROGRESS);
        LOGGER.info("Total IN_PROGRESS jobs (all tools): {}", allInProgressCount);

        if (allInProgressCount >= globalConcurrencyLimit) {
            LOGGER.info("Global concurrency limit reached. No new jobs can be dispatched.");
            return;
        }

        // 2) Get all NEW jobs
        List<JobEntity> newJobs = jobRepository.findByStatus(JobStatus.NEW);
        if (newJobs.isEmpty()) {
            LOGGER.info("No NEW jobs available.");
            return;
        }

        // Group NEW jobs by tool
        Map<String, List<JobEntity>> jobsByTool = newJobs.stream()
                .collect(Collectors.groupingBy(JobEntity::getToolId));

        int totalDispatchedThisCycle = 0;

        // 3) Iterate over each tool's NEW jobs
        for (String toolId : jobsByTool.keySet()) {

            // If global concurrency is already used up, break out
            if (allInProgressCount >= globalConcurrencyLimit) {
                break;
            }

            // a) Check tool concurrency usage
            int toolInProgress = jobRepository.countByToolIdAndStatus(toolId, JobStatus.IN_PROGRESS);

            // b) Get the tool config
            ToolEntity config = toolConfigRepository.findById(toolId).orElse(null);
            if (config == null) {
                LOGGER.warn("No config found for tool '{}'; skipping...", toolId);
                continue;
            }

            int toolLimit = config.getMaxConcurrentJobs();

            // c) Calculate how many new jobs we can dispatch for this tool
            int toolAvailable = toolLimit - toolInProgress;
            int globalAvailable = globalConcurrencyLimit - allInProgressCount;

            // The actual number of jobs we can pick for this tool this cycle
            int canDispatchForTool = Math.min(toolAvailable, globalAvailable);
            if (canDispatchForTool <= 0) {
                LOGGER.info("Tool {} at concurrency limit: inProgress={} limit={} or global limit reached",
                        toolId, toolInProgress, toolLimit);
                continue;
            }

            // d) Pick up to 'canDispatchForTool' from the tool's NEW jobs
            List<JobEntity> toolJobs = jobsByTool.get(toolId);
            // Sort if needed by priority/time:
             toolJobs.sort(Comparator.comparing(JobEntity::getTimestampCreated));
            List<JobEntity> dispatchable = toolJobs.stream()
                    .limit(canDispatchForTool)
                    .collect(Collectors.toList());

            if (dispatchable.isEmpty()) {
                continue;
            }

            LOGGER.info("Dispatching {} jobs for tool {} (toolLimit={}, toolInProgress={}, globalAvailable={})",
                    dispatchable.size(), toolId, toolLimit, toolInProgress, globalAvailable);

            // e) Mark them IN_PROGRESS, produce them individually
            for (JobEntity job : dispatchable) {
                job.setStatus(JobStatus.IN_PROGRESS);
            }
            jobRepository.saveAll(dispatchable);

            for (JobEntity job : dispatchable) {
                Map<String, Object> message = new HashMap<>();
                message.put("jobId", job.getJobId());
                message.put("toolId", job.getToolId());
                message.put("payload", job.getPayload());
                message.put("priority", job.getPriority());

                jobProducer.sendJobToTool(config.getDestinationTopic(), message);

                // Increase counters
                allInProgressCount++;
                totalDispatchedThisCycle++;

                // If we've reached the global limit, stop sending more
                if (allInProgressCount >= globalConcurrencyLimit) {
                    LOGGER.info("Global concurrency limit reached after dispatching job {}.", job.getJobId());
                    break;
                }
            }

            // If global concurrency is used up, we break out the tool loop
            if (allInProgressCount >= globalConcurrencyLimit) {
                break;
            }
        }

        LOGGER.info("Dispatch cycle complete. Dispatched {} new jobs. Now {} total IN_PROGRESS.",
                totalDispatchedThisCycle, allInProgressCount);
        LOGGER.info("=== End of dispatch cycle ===");
    }
}