import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class AuthorizationController {

	private Client client;
	private DataStorageController dsController;

	@FXML
	private TextField user;

	@FXML
	private TextField password;

	@FXML
	private TextField host;

	@FXML
	private TextField port;

	@FXML
	private Button loginButton;

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	@FXML
	void initialize() {
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
				Settings clientSettings = client.getSettings();
				if (wrongValue) {
					port.setText(Integer.toString(clientSettings.getServerPort()));
					CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", "Enter value between 1 and 65535");
				} else clientSettings.setServerPort(portValue);
			}
		});
	}

	public void fillSettings() {
		Settings clientSettings = client.getSettings();
		host.setText(clientSettings.getServerHost());
		port.setText(Integer.toString(clientSettings.getServerPort()));
	}

	@FXML
	void handleOnHostChange(ActionEvent event) {
		Settings clientSettings = client.getSettings();
		clientSettings.setServerHost(host.getText());
	}

	@FXML
	void handleOnPortChange(ActionEvent event) {
		Settings clientSettings = client.getSettings();
		try {
			clientSettings.setServerPort(Integer.parseInt(port.getText()));
		} catch (NumberFormatException e) {
		}
	}

	@FXML
	private void handleLogin(ActionEvent event) {
		String userValue = user.getText().trim();
		String passwordValue = password.getText().trim();
		String hostValue = host.getText().trim();
		String portValue = port.getText().trim();

		if (userValue.isEmpty()) {
			CommonUtils.showMessageBox(AlertType.INFORMATION, "Error", "Please enter user name");
			return;
		}

		if (passwordValue.isEmpty()) {
			CommonUtils.showMessageBox(AlertType.INFORMATION, "Error", "Please enter password");
			return;
		}

		if (hostValue.isEmpty()) {
			CommonUtils.showMessageBox(AlertType.INFORMATION, "Error", "Please server host");
			return;
		}

		if (portValue.isEmpty()) {
			CommonUtils.showMessageBox(AlertType.INFORMATION, "Error", "Please server port");
			return;
		}

		OperationResult authResult = client.authorizeUser(userValue, passwordValue, future -> {
			boolean conSuccessFull = future.isSuccess();
			if (!conSuccessFull) CommonUtils.showMessageBox(AlertType.ERROR, "Error", future.cause().getMessage());
		});

		if (!authResult.isSuccess()) {
			CommonUtils.showMessageBox(AlertType.ERROR, "Error", authResult.getShortMessage());
			return;
		}
	}

	public void handleAuthorizationResponse(OperationResult authResult) {
		if (authResult.isSuccess()) openDataStorageWindow();
		else CommonUtils.showMessageBox(AlertType.ERROR, "Error", authResult.getShortMessage());
	}

	private void openDataStorageWindow() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DataStorage.fxml"));
		Parent dsForm = null;
		try {
			dsForm = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.printf("Couldn't load data storage window: %s%n", e.getMessage());
			Platform.exit();
			System.exit(1);
		}
		if (dsForm != null) {
			dsController = loader.getController();
			dsController.setClient(client);
			client.setDSController(dsController);

			Stage mainStage = client.getMainStage();
			final Scene dsScene = new Scene(dsForm);
			Platform.runLater(() -> {
				mainStage.setScene(dsScene);
				dsController.updateClientFileList();
				dsController.updateServerFileList();
			});
		}
	}

}
