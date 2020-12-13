import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class FileListResponse {

    @JsonProperty("fileList")
    private ArrayList<FileDescription> filesList;

    public FileListResponse() {
        filesList = new ArrayList<>();
    }

    public FileListResponse(ArrayList<FileDescription> filesList) {
        this.filesList = filesList;
    }

    public ArrayList<FileDescription> getFilesList() {
        return filesList;
    }

    public void setFilesList(ArrayList<FileDescription> filesList) {
        this.filesList = filesList;
    }

}
