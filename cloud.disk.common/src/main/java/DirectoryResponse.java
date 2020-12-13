import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class DirectoryResponse {

    @JsonProperty("fileList")
    private ArrayList<FileDescription> filesList;

    public DirectoryResponse() {
        filesList = new ArrayList<>();
    }

    public DirectoryResponse(ArrayList<FileDescription> filesList) {
        this.filesList = filesList;
    }

    public ArrayList<FileDescription> getFilesList() {
        return filesList;
    }

    public void setFilesList(ArrayList<FileDescription> filesList) {
        this.filesList = filesList;
    }

}
