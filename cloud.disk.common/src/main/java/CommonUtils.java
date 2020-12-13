import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class CommonUtils {

    public static void showMessageBox(Alert.AlertType alertType, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public static OperationResult<String> serializeToJSON(Object object) {
        OperationResult<String> funcResult;
        ObjectMapper mapper = new ObjectMapper();
        String serializationResult = null;
        try {
            serializationResult = mapper.writeValueAsString(object);
            funcResult = OperationResult.getSuccess(serializationResult);
        } catch (JsonProcessingException e) {
            funcResult = OperationResult.getExceptionResult(e);
        }
        return funcResult;
    }

    public static String serializeToJSONUnsafe(Object object) throws JsonProcessingException {
        return new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(object);
    }

}
