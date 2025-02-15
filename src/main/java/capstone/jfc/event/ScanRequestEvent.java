package capstone.jfc.event;

import capstone.jfc.model.ScanRequestPayload;

import java.util.UUID;

public final class ScanRequestEvent implements Event<ScanRequestPayload> {
    private  ScanRequestPayload payload;
    private String eventId;

    public ScanRequestEvent(ScanRequestPayload payload) {
        this.payload = payload;
        this.eventId= UUID.randomUUID().toString();
    }
    public ScanRequestEvent(){
        this.eventId = UUID.randomUUID().toString();
    }
    @Override
    public String getType() {
        String toolname=payload.getTypes();
        return "scan"+toolname;
    }

    @Override
    public ScanRequestPayload getPayload() {
        return payload;
    }

    public void setPayload(ScanRequestPayload payload) {
        this.payload = payload;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

}