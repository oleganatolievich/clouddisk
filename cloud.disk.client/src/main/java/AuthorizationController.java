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
import javafx.stage.Stage;

import java.io.IOException;

public class AuthorizationController {

	private Client client;
	private DataStorageController dsController;

	@FXML
	private TextField user;

	@FXML
	private TextField password;

	@FXML
	private Button loginButton;

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	@FXML
	private void initialize() {
	}

	@FXML
	private void handleOnActionLogin(ActionEvent event) {
		String userValue = user.getText().trim();
		String passwordValue = password.getText().trim();

		if (userValue.isEmpty()) {
			showMessageBox(AlertType.INFORMATION, "Error", "Please enter user name");
			return;
		}

		if (passwordValue.isEmpty()) {
			showMessageBox(AlertType.INFORMATION, "Error", "Please enter password");
			return;
		}

		OperationResult authResult = client.authorizeUser(userValue, passwordValue, future -> {
			boolean conSuccessFull = future.isSuccess();
			if (!conSuccessFull) showMessageBox(AlertType.ERROR, "Error", future.cause().getMessage());
		});

		if (!authResult.isSuccess()) {
			showMessageBox(AlertType.ERROR, "Error", authResult.getShortMessage());
			return;
		}
	}

	public void handleAuthorizationResult(OperationResult authResult) {
		if (authResult.isSuccess()) openDataStorageWindow();
		else showMessageBox(AlertType.ERROR, "Error", authResult.getShortMessage());
	}

	private void openDataStorageWindow() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/datastorage.fxml"));
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
			dsController = loader.<DataStorageController>getController();
			dsController.setClient(client);
			dsController.setCurrentDirectory(client.getSettings().getCurrentUserDirectory());

			Stage mainStage = client.getMainStage();
			final Scene dsScene = new Scene(dsForm);
			Platform.runLater(() -> {
				mainStage.setScene(dsScene);
			});
		}
	}

	private void showMessageBox(AlertType alertType, String title, String content) {
		Platform.runLater(() -> {
			Alert alert = new Alert(alertType);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.setContentText(content);
			alert.showAndWait();
		});
	}

}
