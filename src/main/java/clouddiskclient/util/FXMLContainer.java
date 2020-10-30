package clouddiskclient.util;

import clouddiskclient.CloudDiskClient;
import clouddiskclient.model.FormController;
import clouddiskclient.model.SceneName;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FXMLContainer {
	
	private static Logger logger = LogManager.getLogger();

	private CloudDiskClient client;
	private Stage stage;
	private SceneName sceneName;
	private String resourceName;
	private Scene scene;
	private FormController controller;

	public FormController getController() {
		return controller;
	}

	public void setController(FormController controller) {
		this.controller = controller;
	}

	public FXMLContainer(CloudDiskClient client, Stage stage, SceneName sceneName) {
		this.client = client;
		this.stage = stage;
		this.sceneName = sceneName;
		this.resourceName = sceneName.getResourceName();
	}

	public CloudDiskClient getClient() {
		return client;
	}

	public void setClient(CloudDiskClient client) {
		this.client = client;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public SceneName getSceneName() {
		return sceneName;
	}

	public String getResourceName() {
		return resourceName;
	}

	public boolean hasScene() {
		return scene != null;
	}

	public Scene getScene() {
		if (scene == null) {
			scene = new FXMLUnpack().load(this);
			if (logger.isInfoEnabled()) {
				logger.info("{} has been built", sceneName);
			}
		}
		return scene;
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

}
