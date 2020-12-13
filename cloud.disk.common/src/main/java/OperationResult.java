import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.PrintWriter;
import java.io.StringWriter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class OperationResult<T> {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("shortMessage")
    private String shortMessage;

    @JsonProperty("detailedMessage")
    private String detailedMessage;

    @JsonIgnore
    private Throwable exception;

    @JsonIgnore
    private T product;

    public OperationResult() {
    }

    private OperationResult(boolean success, T product, String shortMessage, String detailedMessage) {
        this.success = success;
        this.product = product;
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

    public T getProduct() {
        return product;
    }

    public void setProduct(T product) {
        this.product = product;
    }

    private static <T> OperationResult<T> getNewInstance(boolean success, T product, String ... messages) {
        OperationResult<T> funcResult = null;
        if (messages.length > 1) funcResult = new OperationResult<T>(success, product, messages[0], messages[1]);
        else if (messages.length > 0) funcResult = new OperationResult<T>(success, product, messages[0], messages[0]);
        else if (messages.length == 0) funcResult = new OperationResult<T>(success, product, "", "");

        return funcResult;
    }

    public static <T> OperationResult<T> getSuccess(T product, String ... messages) {
        return getNewInstance(true, product, messages);
    }

    public static <T> OperationResult<T> getFailure(String ... messages) {
        return getNewInstance(false, null, messages);
    }

    public static <T> OperationResult<T> getExceptionResult(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        OperationResult<T> funcResult = getNewInstance(false, null, e.getMessage(), exceptionAsString);
        funcResult.setException(e);

        return funcResult;
    }

}