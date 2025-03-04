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
            case "runbook":handleRunbook(message);
            break;
            default : {
                // ignore or log error
                System.out.println("Unknown event type:");
            }
        }
    }

    private void processJobEvent(String eventType, Integer tenantId, Object payload) {
        String jobId = UUID.randomUUID().toString();
        JobEntity jobEntity = new JobEntity();
        jobEntity.setJobId(jobId);
        jobEntity.setJobCategory(eventType);
        jobEntity.setTenantId(tenantId);
        String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            LOGGER.error("Error serializing payload for event type {}: {}", eventType, ex.getMessage(), ex);
            eventJson = "SerializationError:" + ex.getMessage();
        }
        jobEntity.setPayload(eventJson);
        jobEntity.setStatus(JobStatus.NEW);
        jobEntity.setTimestampCreated(LocalDateTime.now());
        jobRepository.save(jobEntity);
        LOGGER.info("Created new job {} of type {} for tenant {}", jobId, eventType, tenantId);
    }

    private void handleRunbook(String message) throws JsonProcessingException{
        RunbookRequestEvent event= objectMapper.readValue(message, RunbookRequestEvent.class);
        RunbookPayload payload = event.getPayload();
        processJobEvent(event.getType(), payload.getTenantId(), payload);
    }

    private void handleTicketTransitionRequest(String message) throws JsonProcessingException {

        TicketTransitionRequestEvent event= objectMapper.readValue(message, TicketTransitionRequestEvent.class);
        TicketTransitionRequestPayload payload = event.getPayload();
        processJobEvent(event.getType(), payload.getTenantId(), payload);
    }

    private void handleTicketCreateRequest(String message) throws JsonProcessingException {

        TicketCreateRequestEvent event= objectMapper.readValue(message, TicketCreateRequestEvent.class);
        TicketCreateRequestPayload payload = event.getPayload();
        processJobEvent(event.getType(), payload.getTenantId(), payload);
    }

    private void handleScanRequestEvent(String ev) throws JsonProcessingException {
        ScanRequestEvent event= objectMapper.readValue(ev, ScanRequestEvent.class);
        ScanRequestPayload payload = event.getPayload();
        processJobEvent(event.getType(), payload.getTenantId(), payload);
    }

    private void handleParseRequestEvent(String ev) throws JsonProcessingException {
        ParseRequestEvent event= objectMapper.readValue(ev, ParseRequestEvent.class);
        ParseRequestPayload payload = event.getPayload();
        processJobEvent(event.getType(), payload.getTenantId(), payload);
    }
    private void handleUpdateRequest(String ev) throws JsonProcessingException {
        UpdateRequestEvent event= objectMapper.readValue(ev, UpdateRequestEvent.class);
        UpdateRequestPayload payload = event.getPayload();
        processJobEvent(event.getType(), payload.getTenantId(), payload);
    }

}
