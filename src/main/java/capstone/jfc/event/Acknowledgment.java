package capstone.jfc.event;

public interface Acknowledgment<T> {
    String getAcknowledgementId();
    T getPayload();
}
