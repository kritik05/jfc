package capstone.jfc.service;
import capstone.jfc.model.JobEntity;
import capstone.jfc.producer.JobProducer;
import capstone.jfc.model.ToolConfigEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.repository.ToolConfigRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BatchDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchDispatcher.class);
    private final JobRepository jobRepository;
    private final ToolConfigRepository toolConfigRepository;
    private final JobProducer jobProducer;

    public BatchDispatcher(JobRepository jobRepository,
                           ToolConfigRepository toolConfigRepository,
                           JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.toolConfigRepository = toolConfigRepository;
        this.jobProducer = jobProducer;
    }

    // Runs every 10 seconds (for example)
    @Scheduled(fixedRate = 10000)
    public void dispatchJobs() {

        LOGGER.info("Starting batch dispatch...");
        // Find all NEW jobs
        List<JobEntity> newJobs = jobRepository.findByStatus(JobStatus.NEW);

        // Group jobs by tool_id
        Map<String, List<JobEntity>> jobsByTool = newJobs.stream()
                .collect(java.util.stream.Collectors.groupingBy(JobEntity::getToolId));

        // For each tool, check concurrency limit and dispatch
        for (String toolId : jobsByTool.keySet()) {
            // Retrieve tool config
            ToolConfigEntity config = toolConfigRepository.findById(toolId).orElse(null);
            if (config == null) {
                LOGGER.warn("No tool config found for tool: {}", toolId);
                continue;
            }

            int maxConcurrent = config.getMaxConcurrentJobs();
            String destinationTopic = config.getDestinationTopic();

            List<JobEntity> toolJobs = jobsByTool.get(toolId);

            // Sort by priority desc or by creation time if needed
            // Here we assume default order or priority
            // toolJobs.sort(Comparator.comparing(JobEntity::getPriority).reversed());

            // Take up to maxConcurrent
            List<JobEntity> batch = toolJobs.stream()
                    .limit(maxConcurrent)
                    .toList();

            // Dispatch each job in the batch
            for (JobEntity job : batch) {
                // Construct the message payload to send
                Map<String, Object> message = new HashMap<>();
                message.put("jobId", job.getJobId());
                message.put("toolId", job.getToolId());
                message.put("payload", job.getPayload());
                message.put("priority", job.getPriority());

                jobProducer.sendJobToTool(destinationTopic, message);

                // Update status to PENDING
                job.setStatus(JobStatus.PENDING);
                jobRepository.save(job);
            }
        }

        LOGGER.info("Batch dispatch complete.");
    }
}
