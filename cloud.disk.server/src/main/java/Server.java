import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class Server extends Application {

    private Settings settings;
    private final String settingsFileName = "server_settings.json";
    private static final Logger logger = LogManager.getLogger();
    private Stage mainStage;
    private ServerManagerController serverManagerController;
    private Connector connector;
    private UserManager userManager;

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public Stage getMainStage() {
        return mainStage;
    }

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        setMainStage(stage);
        mainStage.setOnCloseRequest(e -> {
            saveConfiguration();
            if (connector != null) connector.stopServerNow();
            Platform.exit();
            System.exit(0);
        });
        OperationResult loadResult = loadConfiguration();
        if (!loadResult.isSuccess()) saveConfiguration();
        showServerManagerForm(stage);
    }

    public OperationResult startServer() {
        if (userManager == null) userManager = new UserManager();
        OperationResult initResult = userManager.initDBConnection();
        if (!initResult.isSuccess()) return initResult;
        if (connector == null) connector = new Connector(this);
        return connector.startServer();
    }

    public OperationResult stopServer() {
        OperationResult funcResult = OperationResult.getSuccess(null);
        if (connector != null) {
            funcResult = connector.stopServer();
            if (!funcResult.isSuccess()) return funcResult;
        }
        if (userManager != null) funcResult = userManager.closeDBConnection();
        return funcResult;
    }

    public OperationResult stopServerNow() {
        OperationResult funcResult = OperationResult.getSuccess(null);
        if (connector != null) connector.stopServerNow();
        if (userManager != null) funcResult = userManager.closeDBConnection();
        return funcResult;
    }

    public boolean isRunning() {
        boolean funcResult = false;
        if (connector != null) funcResult = connector.isRunning();
        return  funcResult;
    }

    public OperationResult loadConfiguration() {
        OperationResult funcResult = OperationResult.getSuccess(null);
        ObjectMapper mapper = new ObjectMapper();
        try {
            settings = mapper.readValue(new File(settingsFileName), Settings.class);
        } catch (IOException e) {
            settings = Settings.getDefaults();
            funcResult = OperationResult.getExceptionResult(e);
        }
        return funcResult;
    }

    public OperationResult saveConfiguration() {
        OperationResult funcResult = OperationResult.getSuccess(null);
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(settingsFileName), settings);
        } catch (IOException e) {
            funcResult = OperationResult.getExceptionResult(e);
        }
        return funcResult;
    }

    private void showServerManagerForm(Stage stage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ServerManager.fxml"));
        Parent serverManagerForm = null;
        try {
            serverManagerForm = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("Couldn't load server manager window: %n%s%n", e.getMessage());
            Platform.exit();
            System.exit(1);
        }
        if (serverManagerForm != null) {
            serverManagerController = loader.getController();
            serverManagerController.setServer(this);
            serverManagerController.fillSettings();

            stage.setTitle("Cloud disk server");
            stage.setScene(new Scene(serverManagerForm));
            stage.show();
        }
    }

}
