package capstone.jfc.consumer;
import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JobIngestionConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobIngestionConsumer.class);
    private final JobRepository jobRepository;

    public JobIngestionConsumer(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @KafkaListener(topics = "#{ '${jfc.topics.ingestion}' }", groupId = "jfc-ingestion-consumer")
    public void onMessage(Map<String, Object> jobMessage) {
        try {
            // Example jobMessage structure:
            // { "jobId": "123", "toolId": "ToolA", "payload": "{}", "priority": 5 }
            String jobId = (String) jobMessage.get("jobId");
            String toolId = (String) jobMessage.get("toolId");
            String payload = (String) jobMessage.get("payload");
            Integer priority = (Integer) jobMessage.getOrDefault("priority", 0);

            JobEntity jobEntity = new JobEntity();
            jobEntity.setJobId(jobId);
            jobEntity.setToolId(toolId);
            jobEntity.setPayload(payload);
            jobEntity.setPriority(priority);
            jobEntity.setStatus(JobStatus.NEW);
            jobEntity.setTimestampCreated(LocalDateTime.now());

            jobRepository.save(jobEntity);

            LOGGER.info("Inserted new job with ID {} for tool {}", jobId, toolId);
        } catch (Exception e) {
            LOGGER.error("Error processing job ingestion message", e);
            // Optionally handle errors, e.g., send to a DLQ
        }
    }
}