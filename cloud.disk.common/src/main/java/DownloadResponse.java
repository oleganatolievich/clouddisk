import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class DownloadResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("fileDescription")
    private FileDescription fileDescription;

    @JsonProperty("destFileName")
    private String destFileName;

    public DownloadResponse() {
        this.id = UUID.randomUUID().toString();
    }

    public DownloadResponse(DownloadRequest dr) {
        this.id = dr.getId();
        this.fileDescription = dr.getFileDescription();
        this.destFileName = dr.getDestFileName();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FileDescription getFileDescription() {
        return fileDescription;
    }

    public void setFileDescription(FileDescription fileDescription) {
        this.fileDescription = fileDescription;
    }

    public String getDestFileName() {
        return destFileName;
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

}
