package capstone.jfc.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class ToolServiceConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolServiceConsumer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jfc.topics.status}")
    private String commonStatusTopic;

    private final Random random = new Random();

    public ToolServiceConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Single method that listens to multiple tool topics:
     *   - toolA-destination
     *   - toolB-destination
     *   - toolC-destination
     */
    @KafkaListener(topics = {
            "${jfc.topics.toolA}",
            "${jfc.topics.toolB}",
            "${jfc.topics.toolC}"
    },
            groupId = "simulated-tools-group")
    public void onToolMessage(ConsumerRecord<String, Object> record) {
        Object value = record.value();
        if (!(value instanceof Map)) {
            LOGGER.warn("Received unexpected message type: {}", value.getClass());
            return;
        }

        // The "batch" message map
        Map<?, ?> batchMessage = (Map<?, ?>) value;

        // Extract toolId
        String toolId = (String) batchMessage.get("toolId");
        LOGGER.info("Received a BATCH for tool {}. Simulating processing...", toolId);

        // Extract the list of jobs
        List<Map<String, Object>> jobsInBatch;
        try {
            jobsInBatch = (List<Map<String, Object>>) batchMessage.get("jobs");
        } catch (ClassCastException e) {
            LOGGER.error("Unexpected 'jobs' format in batch message", e);
            return;
        }

        // 1) Immediately send PROGRESS status for each job
        for (Map<String, Object> jobData : jobsInBatch) {
            String jobId = (String) jobData.get("jobId");
            Map<String, Object> progressMessage = Map.of(
                    "jobId", jobId,
                    "toolId", toolId,
                    "status", "PROGRESS"
            );
            kafkaTemplate.send(commonStatusTopic, progressMessage);
            LOGGER.info("Marked job {} as PROGRESS", jobId);
        }

        // 2) Simulate random processing time (1–5 seconds) for the entire batch
        try {
            int sleepMs = 1000 + random.nextInt(4000);
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3) For each job in the batch, produce final SUCCESS/FAIL
        for (Map<String, Object> jobData : jobsInBatch) {
            String jobId = (String) jobData.get("jobId");

            // 75% chance success, 25% fail
            boolean success = random.nextInt(100) < 75;
            String status = success ? "SUCCESS" : "FAIL";

            Map<String, Object> statusMessage = Map.of(
                    "jobId", jobId,
                    "toolId", toolId,
                    "status", status
            );

            kafkaTemplate.send(commonStatusTopic, statusMessage);

            LOGGER.info("Tool consumer for tool {} completed job {} with status {}", toolId, jobId, status);
        }
    }
}
