package clouddiskclient.controller;

import clouddiskclient.CloudDiskClient;
import clouddiskclient.model.FormController;
import clouddiskclient.model.SceneName;
import clouddiskclient.util.FXMLContainer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class SettingsController implements FormController {

	private FXMLContainer container;
	private Stage stage;

	@FXML
	private void handleOnActionClose(ActionEvent event) {
		stage.close();
	}

	@FXML
	private void handleOnActionBackButton(ActionEvent event) {
		stage.setScene(CloudDiskClient.getScenes().get(SceneName.AUTHORIZATION).getScene());
	}

	@Override
	public void setContainer(FXMLContainer container) {
		this.container = this.container;
	}

}
