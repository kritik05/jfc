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

    // A single global batch limit (e.g., 4)
    @Value("${jfc.global-batch-size:4}")
    private int globalBatchSize;

    public BatchDispatcher(JobRepository jobRepository,
                           ToolConfigRepository toolConfigRepository,
                           JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.toolConfigRepository = toolConfigRepository;
        this.jobProducer = jobProducer;
    }

    @Scheduled(fixedRate = 10000)
    public void dispatchJobs() {
        LOGGER.info("Starting batch dispatch with global batch size = {}", globalBatchSize);

        // 1) Find all NEW jobs
        List<JobEntity> newJobs = jobRepository.findByStatus(JobStatus.NEW);
        if (newJobs.isEmpty()) {
            LOGGER.info("No NEW jobs found to dispatch.");
            return;
        }

        // 2) Group NEW jobs by toolId
        Map<String, List<JobEntity>> jobsByTool = newJobs.stream()
                .collect(Collectors.groupingBy(JobEntity::getToolId));

        int totalDispatched = 0;

        // 3) Iterate over each tool's new jobs
        for (String toolId : jobsByTool.keySet()) {
            if (totalDispatched >= globalBatchSize) {
                break; // reached global capacity
            }

            // a) Check how many are already IN_PROGRESS for this tool
            int inProgressCount = jobRepository.countByToolIdAndStatus(toolId, JobStatus.IN_PROGRESS);

            // b) Lookup the tool config
            ToolEntity config = toolConfigRepository.findById(toolId).orElse(null);
            if (config == null) {
                LOGGER.warn("No tool config found for tool: {}", toolId);
                continue;
            }

            int toolLimit = config.getMaxConcurrentJobs();
            int availableForTool = toolLimit - inProgressCount;
            if (availableForTool <= 0) {
                LOGGER.info("Tool {} is already at max concurrency ({})", toolId, toolLimit);
                continue;
            }

            // c) Also ensure we don't exceed the global batch
            int remainingGlobalSlots = globalBatchSize - totalDispatched;
            int numToDispatch = Math.min(availableForTool, remainingGlobalSlots);

            // d) Pick up to numToDispatch from the new jobs for this tool
            List<JobEntity> candidateJobs = jobsByTool.get(toolId).stream()
                    // e.g. sort by priority desc if you want
                     .sorted(Comparator.comparing(JobEntity::getTimestampCreated))
                    .limit(numToDispatch)
                    .collect(Collectors.toList());

            if (candidateJobs.isEmpty()) {
                continue;
            }

            LOGGER.info("Dispatching {} jobs for tool {}. (toolLimit={}, inProgress={}, globalSlotsLeft={})",
                    candidateJobs.size(), toolId, toolLimit, inProgressCount, remainingGlobalSlots);

            // e) Mark them IN_PROGRESS
            for (JobEntity job : candidateJobs) {
                job.setStatus(JobStatus.IN_PROGRESS);
            }
            jobRepository.saveAll(candidateJobs);

            // f) Build a single 'batch' message for the tool
            List<Map<String, Object>> jobsData = candidateJobs.stream().map(job -> {
                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("jobId", job.getJobId());
                jobMap.put("payload", job.getPayload());
                jobMap.put("priority", job.getPriority());
                jobMap.put("toolId", job.getToolId());
                return jobMap;
            }).collect(Collectors.toList());

            // This object represents the entire batch for the tool
            Map<String, Object> batchMessage = new HashMap<>();
            // Optional: a batch ID or timestamp if you like
            batchMessage.put("toolId", toolId);
            batchMessage.put("jobs", jobsData);

            // g) Send the entire batch as a single message
            jobProducer.sendJobToTool(config.getDestinationTopic(), batchMessage);

            // Update how many we've dispatched
            totalDispatched += candidateJobs.size();
            if (totalDispatched >= globalBatchSize) {
                LOGGER.info("Global batch size reached. Dispatched {} total jobs.", totalDispatched);
                break;
            }
        }

        LOGGER.info("Batch dispatch complete. Dispatched {} jobs in total.", totalDispatched);
    }
}