import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class FileListRequest {

    @JsonProperty("absolutePath")
    private String absolutePath;

    public FileListRequest() {
    }

    public FileListRequest(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

}
