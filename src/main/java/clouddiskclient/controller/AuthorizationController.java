package clouddiskclient.controller;

import clouddiskclient.model.FormController;
import clouddiskclient.util.FXMLContainer;
import common.OperationResult;
import common.OperationType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class AuthorizationController implements FormController {

	private FXMLContainer container;

	@FXML
	private TextField user;

	@FXML
	private TextField password;

	@FXML
	private Button loginButton;

	public AuthorizationController() {
	}

	@FXML
	private void initialize() {
	}

	@FXML
	private void handleOnActionLogin(ActionEvent event) {
		String userValue = user.getText();
		String passwordValue = password.getText();

		if (userValue.trim().isEmpty()) {
			showMessageBox(Alert.AlertType.ERROR, "Error", "Please enter user name");
			return;
		}

		if (passwordValue.trim().isEmpty()) {
			showMessageBox(Alert.AlertType.ERROR, "Error", "Please enter password");
			return;
		}

		OperationResult authResult = container.getClient().authorizeUser(userValue, passwordValue, future -> {
			boolean conSuccessFull = future.isSuccess();
			if (conSuccessFull) System.out.println("Connecting...");
			else showMessageBox(Alert.AlertType.ERROR, "Error", future.cause().getMessage());
		});
		if (!authResult.isSuccess()) {
			showMessageBox(Alert.AlertType.ERROR, "Error", authResult.getShortMessage());
			return;
		}
	}

	private void showMessageBox(Alert.AlertType alertType, String title, String content) {
		Alert alert = new Alert(alertType);

		alert.setTitle(title);
		alert.setContentText(content);
		alert.showAndWait();
	}

	@Override
	public void processOperation(OperationType ot, OperationResult or) {
		System.out.println("Getting result is implemented through the colon (толстая кишка)");
		System.out.println(or.isSuccess());
	}

	@FXML
	private void handleOnActionPreferences(ActionEvent event) {
		//containter.getStage().setScene(CloudDiskClient.getScenes().get(SceneName.PREFERENCES).getScene());
	}

	@Override
	public void setContainer(FXMLContainer container) {
		this.container = container;
	}

}
