package capstone.jfc.consumer;

import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.service.BatchDispatcher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
public class JobStatusConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStatusConsumer.class);
    private final JobRepository jobRepository;
    private final BatchDispatcher batchDispatcher;

    public JobStatusConsumer(JobRepository jobRepository,BatchDispatcher batchDispatcher) {
        this.batchDispatcher=batchDispatcher;
        this.jobRepository = jobRepository;
    }

    @KafkaListener(topics = "#{ '${jfc.topics.status}' }", groupId = "jfc-status-consumer")
    public void onStatusMessage(Map<String, Object> statusMessage) {
        try {
            String jobId = (String) statusMessage.get("jobId");
            String statusString = (String) statusMessage.get("status");

            JobStatus newStatus = JobStatus.valueOf(statusString);

            JobEntity job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                LOGGER.warn("Received status update for unknown job ID: {}", jobId);
                return;
            }
            JobStatus oldStatus = job.getStatus();
            job.setStatus(newStatus);
            jobRepository.save(job);

            if ((newStatus == JobStatus.SUCCESS || newStatus == JobStatus.FAIL) && oldStatus == JobStatus.IN_PROGRESS) {
                LOGGER.info("A concurrency slot was just freed. Triggering dispatch now...");
                batchDispatcher.dispatchJobs();
            }
            LOGGER.info("Updated job {} to status {}", jobId, newStatus);
        } catch (Exception e) {
            LOGGER.error("Error processing job status message", e);
        }
    }
}
