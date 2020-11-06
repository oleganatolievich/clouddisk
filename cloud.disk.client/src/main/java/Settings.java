import com.fasterxml.jackson.annotation.JsonProperty;

public class Settings {

    @JsonProperty("host")
    private String serverHost;

    @JsonProperty("port")
    private int serverPort;

    @JsonProperty("currentDirectory")
    private String currentUserDirectory;

    public Settings() {
    }

    public Settings(String serverHost, int serverPort, String currentUserDirectory) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.currentUserDirectory = currentUserDirectory;
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

    public String getCurrentUserDirectory() {
        return currentUserDirectory;
    }

    public void setCurrentUserDirectory(String currentUserDirectory) {
        this.currentUserDirectory = currentUserDirectory;
    }

}
