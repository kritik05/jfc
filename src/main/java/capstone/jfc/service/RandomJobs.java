package capstone.jfc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RandomJobs implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomJobs.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ingestionTopic;

    private static final List<String> JOBCATEGORY = Arrays.asList("JobA", "JobB", "JobC");

    public RandomJobs(KafkaTemplate<String, Object> kafkaTemplate, org.springframework.core.env.Environment env) {
        this.kafkaTemplate = kafkaTemplate;
        this.ingestionTopic = env.getProperty("jfc.topics.ingestion");
    }

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("Generating 50 random jobs to ingestion topic...");
        for (int i = 1; i <= 50; i++) {
            String jobId = "job-" + i;
            String jobCategory = JOBCATEGORY.get(new Random().nextInt(JOBCATEGORY.size()));
            String payload = "{\"data\":\"Random payload " + i + "\"}";
            int priority = 1;

            Map<String, Object> message = new HashMap<>();
            message.put("jobId", jobId);
            message.put("jobCategory", jobCategory);
            message.put("payload", payload);
            message.put("priority", priority);

            kafkaTemplate.send(ingestionTopic, message);
            LOGGER.info("Sent random job {} for tool {}", jobId, jobCategory);
        }
    }
}