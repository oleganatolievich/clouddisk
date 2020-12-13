import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Paths;

public class Settings {

    @JsonProperty("port")
    private int serverPort;

    @JsonProperty("storage")
    private String storagePath;

    public Settings() {
    }

    public Settings(int serverPort, String storagePath) {
        this.serverPort = serverPort;
        this.storagePath = storagePath;
    }

    public static Settings getDefaults() {
        return new Settings(41952, Paths.get(System.getProperty("user.dir")).toString());
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

}
