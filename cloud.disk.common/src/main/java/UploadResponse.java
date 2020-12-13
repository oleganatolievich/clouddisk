import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class UploadResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private FileStatus status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("received")
    private long received;

    @JsonProperty("size")
    private long size;

    public UploadResponse() {
    }

    public UploadResponse(String id, FileStatus status, String message, long received, long size) {
        this.id = id;
        this.status = status;
        this.message = message;
        this.received = received;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getReceived() {
        return received;
    }

    public void setReceived(long received) {
        this.received = received;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
