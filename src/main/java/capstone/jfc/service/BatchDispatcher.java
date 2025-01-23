package capstone.jfc.service;
import capstone.jfc.model.JobEntity;
import capstone.jfc.producer.JobProducer;
import capstone.jfc.model.ToolEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.repository.ToolConfigRepository;
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

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public BatchDispatcher(JobRepository jobRepository,
                           ToolConfigRepository toolConfigRepository,
                           JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.toolConfigRepository = toolConfigRepository;
        this.jobProducer = jobProducer;
    }

    @Scheduled(fixedRate = 10000)
    public void dispatchJobs() {
        LOGGER.info("Starting batch dispatch...");

        List<JobEntity> newJobs = jobRepository.findByStatus(JobStatus.NEW);
        if (newJobs.isEmpty()) {
            LOGGER.info("No NEW jobs found. Nothing to dispatch.");
            return;
        }

        Map<String, List<JobEntity>> jobsByTool = newJobs.stream()
                .collect(Collectors.groupingBy(JobEntity::getToolId));

        for (String toolId : jobsByTool.keySet()) {
            executorService.submit(() -> processToolBatch(toolId, jobsByTool.get(toolId)));
        }

        LOGGER.info("Batch dispatch triggered for {} tool(s).", jobsByTool.size());
    }

    private void processToolBatch(String toolId, List<JobEntity> toolJobs) {
        try {
            ToolEntity config = toolConfigRepository.findById(toolId).orElse(null);
            if (config == null) {
                LOGGER.warn("No tool config found for tool: {}", toolId);
                return;
            }

            int maxConcurrent = config.getMaxConcurrentJobs();
            String destinationTopic = config.getDestinationTopic();

            toolJobs.sort(Comparator.comparing(JobEntity::getPriority));

            List<JobEntity> batch = toolJobs.stream()
                    .limit(maxConcurrent)
                    .collect(Collectors.toList());

            if (batch.isEmpty()) {
                LOGGER.info("No jobs to dispatch for tool {}", toolId);
                return;
            }

            LOGGER.info("Dispatching {} jobs for tool {} on thread {}",
                    batch.size(), toolId, Thread.currentThread().getName());

            List<Map<String, Object>> jobsInBatch = new ArrayList<>();
            for (JobEntity job : batch) {
                Map<String, Object> jobData = new HashMap<>();
                jobData.put("jobId", job.getJobId());
                jobData.put("toolId", job.getToolId());
                jobData.put("payload", job.getPayload());
                jobData.put("priority", job.getPriority());
                jobsInBatch.add(jobData);
            }

            Map<String, Object> batchMessage = new HashMap<>();
            batchMessage.put("toolId", toolId);
            batchMessage.put("jobs", jobsInBatch);

            jobProducer.sendJobToTool(destinationTopic, batchMessage);

            for (JobEntity job : batch) {
                job.setStatus(JobStatus.PENDING);
            }
            jobRepository.saveAll(batch);

            LOGGER.info("Finished dispatch for tool {} on thread {}", toolId, Thread.currentThread().getName());

        } catch (Exception e) {
            LOGGER.error("Error dispatching jobs for tool {}", toolId, e);
        }
    }
}