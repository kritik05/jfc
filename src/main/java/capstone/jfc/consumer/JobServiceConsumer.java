package capstone.jfc.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;

@Component
public class JobServiceConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceConsumer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jfc.topics.status}")
    private String commonStatusTopic;

    private final Random random = new Random();

    public JobServiceConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = {
            "${jfc.topics.jobA}",
            "${jfc.topics.jobB}",
            "${jfc.topics.jobC}"
    }, groupId = "simulated-jobs-group")
    public void onJobMessage(ConsumerRecord<String, Object> record) {
        Object value = record.value();
        if (!(value instanceof Map)) {
            LOGGER.warn("Received unexpected message type: {}", value.getClass());
            return;
        }
        Map<?, ?> jobMessage = (Map<?, ?>) value;

        String jobId = (String) jobMessage.get("jobId");
        String jobCategory = (String) jobMessage.get("jobCategory");
        LOGGER.info("Tool consumer: received job {} for tool {}. Processing...", jobId, jobCategory);

        // Random processing time (1–5s)
        try {
            int sleepMs = 1000 + random.nextInt(4000);
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 75% chance success, 25% fail
        boolean success = random.nextInt(100) < 75;
        String status = success ? "SUCCESS" : "FAIL";

        Map<String, Object> statusMessage = Map.of(
                "jobId", jobId,
                "jobCategory", jobCategory,
                "status", status
        );

        kafkaTemplate.send(commonStatusTopic, statusMessage);
        LOGGER.info("Tool consumer for tool {} completed job {} with status {}", jobCategory, jobId, status);
    }
}
