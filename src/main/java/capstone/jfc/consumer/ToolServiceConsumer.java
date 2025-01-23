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
    private final Random random = new Random();

    @Value("${jfc.topics.status}")
    private String commonStatusTopic;

    public ToolServiceConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = {
            "${jfc.topics.toolA}",
            "${jfc.topics.toolB}",
            "${jfc.topics.toolC}"
    }, groupId = "simulated-tools-group")
    public void onToolMessage(ConsumerRecord<String, Object> record) {
        Object value = record.value();
        if (!(value instanceof Map)) {
            LOGGER.warn("Expected batch message (Map), got: {}", value.getClass());
            return;
        }

        Map<?, ?> batchMessage = (Map<?, ?>) value;
        String toolId = (String) batchMessage.get("toolId");
        if (toolId == null) {
            LOGGER.warn("No toolId in batch message.");
            return;
        }

        // Extract the array of jobs
        Object jobsObj = batchMessage.get("jobs");
        if (!(jobsObj instanceof List)) {
            LOGGER.warn("No 'jobs' field or not a list in batch message for tool {}", toolId);
            return;
        }

        List<Map<String, Object>> jobs = (List<Map<String, Object>>) jobsObj;
        LOGGER.info("ToolServiceConsumer received a batch of {} jobs for tool {}", jobs.size(), toolId);

        // We can simulate "processing" the entire batch at once (sleep)
        try {
            int sleepMs = 1000 + random.nextInt(4000);
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // For each job in the batch, send SUCCESS or FAIL
        for (Map<String, Object> jobData : jobs) {
            String jobId = (String) jobData.get("jobId");
            // 75% success, 25% fail
            boolean success = random.nextInt(100) < 75;
            String status = success ? "SUCCESS" : "FAIL";

            Map<String, Object> statusMessage = Map.of(
                    "jobId", jobId,
                    "toolId", toolId,
                    "status", status
            );

            kafkaTemplate.send(commonStatusTopic, statusMessage);
            LOGGER.info("Tool {} completed job {} with status {}", toolId, jobId, status);
        }
    }
}
