import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Paths;

public class Settings {

    @JsonProperty("port")
    private int clientPort;

    @JsonProperty("storage")
    private String storagePath;

    public Settings() {
    }

    public Settings(int clientPort, String storagePath) {
        this.clientPort = clientPort;
        this.storagePath = storagePath;
    }

    public static Settings getDefaults() {
        return new Settings(41952, Paths.get(System.getProperty("user.dir"), "filestorage").toString());
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

}
