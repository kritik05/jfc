package capstone.jfc.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Value("${jfc.topics.ingestion}")
    private String ingestionTopic;

    @Value("${jfc.topics.status}")
    private String statusTopic;

    @Value("${jfc.topics.jobA}")
    private String jobATopic;

    @Value("${jfc.topics.jobB}")
    private String jobBTopic;

    @Value("${jfc.topics.jobC}")
    private String jobCTopic;

    @Bean
    public NewTopic ingestionTopic() {
        return new NewTopic(ingestionTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic statusTopic() {
        return new NewTopic(statusTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic jobATopic() {
        return new NewTopic(jobATopic, 3, (short) 1);
    }

    @Bean
    public NewTopic jobBTopic() {
        return new NewTopic(jobBTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic jobCTopic() {
        return new NewTopic(jobCTopic, 3, (short) 1);
    }
}
