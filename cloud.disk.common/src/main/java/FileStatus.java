import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public enum FileStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SUCCESS("Success"),
    ERROR("Error");

    @JsonProperty("name")
    private String name;

    FileStatus() {
    }

    FileStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}