package capstone.jfc.consumer;

import capstone.jfc.event.*;
import capstone.jfc.model.*;
import capstone.jfc.repository.JobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class JfcCommonTopicListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JfcCommonTopicListener.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @KafkaListener(
            topics = "#{'${app.kafka.topics.jobunified}'}", // e.g. "jfc.jobs"
            groupId = "jfc-ingestion-consumer",
            containerFactory = "unifiedListenerContainerFactory"
    )
    public void onMessage(String message) throws Exception {
        // 1) Read the raw JSON
        System.out.println(message);
        JsonNode root = objectMapper.readTree(message);
        // 2) Check for the "type" field
        String typeString = root.get("type").asText();
        // 3) Based on event type, deserialize into the correct DTO
        switch (typeString) {
            case "scanCODESCAN":
            case "scanDEPENDABOT":
            case "scanSECRETSCAN": handleScanRequestEvent(message);
            break;
            case "update":handleUpdateRequest(message);
            break;
            case "parseCODESCAN":
            case "parseDEPENDABOT":
            case "parseSECRETSCAN":handleParseRequestEvent(message);
            break;
            case "ticketCreate":handleTicketCreateRequest(message);
            break;
            case "ticketTransition":handleTicketTransitionRequest(message);
            break;
            default : {
                // ignore or log error
                System.out.println("Unknown event type:");
            }
        }
    }

    private void handleTicketTransitionRequest(String message) throws JsonProcessingException {

        TicketTransitionRequestEvent event= objectMapper.readValue(message, TicketTransitionRequestEvent.class);
        TicketTransitionRequestPayload payload = event.getPayload();
        Integer tenantId = payload.getTenantId();
        String jobId = UUID.randomUUID().toString();
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

    private void handleTicketCreateRequest(String message) throws JsonProcessingException {

        TicketCreateRequestEvent event= objectMapper.readValue(message, TicketCreateRequestEvent.class);
        TicketCreateRequestPayload payload = event.getPayload();
        Integer tenantId = payload.getTenantId();
        String jobId = UUID.randomUUID().toString();
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


    private void handleScanRequestEvent(String ev) throws JsonProcessingException {
        ScanRequestEvent event= objectMapper.readValue(ev, ScanRequestEvent.class);
        ScanRequestPayload payload = event.getPayload();
        Integer tenantId = payload.getTenantId();
        String jobId = UUID.randomUUID().toString();
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

    private void handleParseRequestEvent(String ev) throws JsonProcessingException {
        ParseRequestEvent event= objectMapper.readValue(ev, ParseRequestEvent.class);
        // Extract the payload
        ParseRequestPayload payload = event.getPayload();

        // For example, we have tenantId, a list of scanTypes, etc.
        Integer tenantId = payload.getTenantId();
        // 2) For each scanType, create a job in DB
        String jobId = UUID.randomUUID().toString();
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
    private void handleUpdateRequest(String ev) throws JsonProcessingException {
        UpdateRequestEvent event= objectMapper.readValue(ev, UpdateRequestEvent.class);
        // Extract the payload
        UpdateRequestPayload payload = event.getPayload();


        // For example, we have tenantId, a list of scanTypes, etc.
        Integer tenantId = payload.getTenantId();
        // 2) For each scanType, create a job in DB
        String jobId = UUID.randomUUID().toString();
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
