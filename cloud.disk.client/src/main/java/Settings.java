import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Settings {

    @JsonProperty("host")
    private String serverHost;

    @JsonProperty("port")
    private int serverPort;

    @JsonProperty("clientDirectory")
    private String clientDirectory;

    @JsonIgnore
    private String serverDirectory = "";

    @JsonIgnore
    private String serverRoot;

    public Settings() {
    }

    public Settings(String serverHost, int serverPort, String clientDirectory) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.clientDirectory = clientDirectory;
    }

    public static Settings getDefaults() {
        return new Settings("localhost", 41952, System.getProperty("user.dir"));
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getClientDirectory() {
        return clientDirectory;
    }

    public void setClientDirectory(String clientDirectory) {
        this.clientDirectory = clientDirectory;
    }

    public String getServerDirectory() {
        return serverDirectory;
    }

    public void setServerDirectory(String serverDirectory) {
        this.serverDirectory = serverDirectory;
    }

}
