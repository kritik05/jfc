package capstone.jfc.event;

public interface Event <T>{
    String getType();
    T getPayload();
    String getEventId();
}
