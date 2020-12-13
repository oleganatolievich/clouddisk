import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class DirectoryRequest {

    @JsonProperty("fileList")
    private ArrayList<FileDescription> filesList;

    public DirectoryRequest() {
        filesList = new ArrayList<>();
    }

    public DirectoryRequest(ArrayList<FileDescription> filesList) {
        this.filesList = filesList;
    }

    public ArrayList<FileDescription> getFilesList() {
        return filesList;
    }

    public void setFilesList(ArrayList<FileDescription> filesList) {
        this.filesList = filesList;
    }

}
