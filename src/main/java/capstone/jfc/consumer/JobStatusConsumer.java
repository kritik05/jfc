package capstone.jfc.consumer;

import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
public class JobStatusConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStatusConsumer.class);
    private final JobRepository jobRepository;

    public JobStatusConsumer(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @KafkaListener(topics = "#{ '${jfc.topics.status}' }", groupId = "jfc-status-consumer")
    public void onStatusMessage(Map<String, Object> statusMessage) {
        try {
            // Example statusMessage structure:
            // { "jobId": "123", "toolId": "ToolA", "status": "SUCCESS" }
            String jobId = (String) statusMessage.get("jobId");
            String statusString = (String) statusMessage.get("status");

            JobStatus newStatus = JobStatus.valueOf(statusString);

            // Fetch job from DB
            JobEntity job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                LOGGER.warn("Received status update for unknown job ID: {}", jobId);
                return;
            }

            job.setStatus(newStatus);
            jobRepository.save(job);

            LOGGER.info("Updated job {} to status {}", jobId, newStatus);
        } catch (Exception e) {
            LOGGER.error("Error processing job status message", e);
            // Potentially push to DLQ
        }
    }
}
