import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.PrintWriter;
import java.io.StringWriter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class ServerError {

    @JsonProperty("connectionClosed")
    private boolean connectionClosed;

    @JsonProperty("shortMessage")
    private String shortMessage;

    @JsonProperty("detailedMessage")
    private String detailedMessage;

    public ServerError() {
    }

    private ServerError(boolean connectionClosed, String shortMessage, String detailedMessage) {
        this.connectionClosed = connectionClosed;
        this.shortMessage = shortMessage;
        this.detailedMessage = detailedMessage;
    }

    public boolean isConnectionClosed() {
        return connectionClosed;
    }

    public void setConnectionClosed(boolean connectionClosed) {
        this.connectionClosed = connectionClosed;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    private static ServerError getNewInstance(boolean connectionClosed, String ... messages) {
        ServerError funcResult = null;
        if (messages.length > 1) funcResult = new ServerError(connectionClosed, messages[0], messages[1]);
        else if (messages.length > 0) funcResult = new ServerError(connectionClosed, messages[0], messages[0]);
        else if (messages.length == 0) funcResult = new ServerError(connectionClosed, "", "");
        return funcResult;
    }

    public static ServerError getExceptionResult(Throwable e, boolean connectionClosed) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        return getNewInstance(connectionClosed, e.getMessage(), exceptionAsString);
    }

}