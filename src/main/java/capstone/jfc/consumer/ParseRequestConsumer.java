package capstone.jfc.consumer;

import capstone.jfc.event.ParseRequestEvent;
import capstone.jfc.event.ScanRequestEvent;
import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.model.ParseRequestPayload;
import capstone.jfc.model.ScanRequestPayload;
import capstone.jfc.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ParseRequestConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanRequestConsumer.class);

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;
    public ParseRequestConsumer(JobRepository jobRepository,ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper=objectMapper;
    }

    @KafkaListener(
            topics = "#{'${app.kafka.topics.parserjfc}'}",
            groupId = "jfc-ingestion-consumer",
            containerFactory = "parseListenerContainerFactory"
    )
    public void onMessage(ParseRequestEvent event) {
        try {
            handleParseRequestEvent(event);
        } catch (Exception e) {
            LOGGER.error("Error processing ingestion message", e);
        }
    }
    private void handleParseRequestEvent(ParseRequestEvent event) {
        // Extract the payload
        ParseRequestPayload payload = event.getPayload();

        // For example, we have tenantId, a list of scanTypes, etc.
        Integer tenantId = payload.getTenantId();
        // 2) For each scanType, create a job in DB
        String jobId = event.getEventId();
        JobEntity jobEntity = new JobEntity();
        jobEntity.setJobId(jobId);
        jobEntity.setJobCategory(event.getType());
        jobEntity.setTenantId(tenantId);

        String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(event.getPayload());
        } catch (Exception ex) {
            LOGGER.error("Error serializing ScanRequestEvent to JSON", ex);
            eventJson = "SerializationError:"+ex.getMessage();
        }
        jobEntity.setPayload(eventJson);

        jobEntity.setStatus(JobStatus.NEW);
        jobEntity.setTimestampCreated(LocalDateTime.now());

        jobRepository.save(jobEntity);

        LOGGER.info("Created new job {} of type {} for tenant {}, {}/{}");
    }
}
