package capstone.jfc.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;


@Component
public class JobProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public JobProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendJobToJObCategory(String topic, Map<String, Object> jobData) {
        kafkaTemplate.send(topic, jobData);
        LOGGER.info("Sent job to topic {} with data: {}", topic, jobData);
    }
}
