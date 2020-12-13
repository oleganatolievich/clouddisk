import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.DirectoryChooser;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;

public class ServerManagerController {

    private Server server;

    @FXML
    private TextField storagePath;

    @FXML
    private Button selectDirectory;

    @FXML
    private TextField port;

    @FXML
    private Button startServer;

    DirectoryChooser directoryChooser = new DirectoryChooser();

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @FXML
    public void initialize() {
        storagePath.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    Settings serverSettings = server.getSettings();
                    String curDir = storagePath.getText();
                    if (!Files.exists(Paths.get(curDir))) {
                        storagePath.setText(serverSettings.getStoragePath());
                        CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", "Folder doesn't exist");
                    } else serverSettings.setStoragePath(curDir);
                }
            }
        });

        UnaryOperator<TextFormatter.Change> portFilter = change -> {
            String newText = change.getControlNewText();
            return (newText.matches("([1-9][0-9]{0,4})")) ? change : null;
        };
        port.setTextFormatter(new TextFormatter<Integer>(new IntegerStringConverter(), 0, portFilter));
        port.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                int portValue;
                boolean wrongValue = false;
                try {
                    portValue = Integer.parseInt(port.getText());
                    if (portValue < 0 || portValue > 65535) wrongValue = true;
                } catch (NumberFormatException e) {
                    portValue = Settings.getDefaults().getServerPort();
                    wrongValue = true;
                }
                Settings serverSettings = server.getSettings();
                if (wrongValue) {
                    port.setText(Integer.toString(serverSettings.getServerPort()));
                    CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", "Enter value between 1 and 65535");
                } else serverSettings.setPort(portValue);
            }
        });
    }

    @FXML
    private void handleSelectDirectory(ActionEvent event) {
        directoryChooser.setInitialDirectory(new File(storagePath.getText()));
        File selectedDirectory = directoryChooser.showDialog(server.getMainStage());
        if (selectedDirectory != null) {
            String absPath = selectedDirectory.getAbsolutePath();
            storagePath.setText(absPath);
            server.getSettings().setStoragePath(absPath);
        }
    }

    @FXML
    private void handleStartServer(ActionEvent event) {
        OperationResult operationResult;
        String newTitle;
        if (server.isRunning()) {
            operationResult = server.stopServerNow();
            newTitle = "Start";
        } else {
            operationResult = server.startServer();
            newTitle = "Stop";
        }
        if (!operationResult.isSuccess()) {
            CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", operationResult.getShortMessage());
        } else {
            boolean serverIsRunning = server.isRunning();
            storagePath.setDisable(serverIsRunning);
            selectDirectory.setDisable(serverIsRunning);
            port.setDisable(serverIsRunning);

            startServer.setText(newTitle);
        }
    }

    public void fillSettings() {
        Settings serverSettings = server.getSettings();
        storagePath.setText(serverSettings.getStoragePath());
        port.setText(Integer.toString(serverSettings.getServerPort()));
    }

}
