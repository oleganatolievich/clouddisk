import java.io.PrintWriter;
import java.io.StringWriter;

public class OperationResult {

    private boolean success;
    private String shortMessage;
    private String detailedMessage;
    private Throwable exception;

    private OperationResult(boolean success, String shortMessage, String detailedMessage) {
        this.success = success;
        this.shortMessage = shortMessage;
        this.detailedMessage = detailedMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public boolean isException() {
        return exception != null;
    }

    private static OperationResult getNewInstance(boolean success, String ... messages) {
        OperationResult funcResult = null;
        if (messages.length > 1) funcResult = new OperationResult(success, messages[0], messages[1]);
        else if (messages.length > 0) funcResult = new OperationResult(success, messages[0], messages[0]);
        else if (messages.length == 0) funcResult = new OperationResult(success, "", "");

        return funcResult;
    }

    public static OperationResult getSuccess(String ... messages) {
        return getNewInstance(true, messages);
    }

    public static OperationResult getFailure(String ... messages) {
        return getNewInstance(false, messages);
    }

    public static OperationResult getExceptionResult(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        OperationResult funcResult = getNewInstance(false, e.getMessage(), exceptionAsString);
        funcResult.setException(e);

        return funcResult;
    }

}