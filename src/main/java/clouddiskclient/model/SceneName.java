package clouddiskclient.model;

public enum SceneName {

	AUTHORIZATION("/fxml/login.fxml"),
	PREFERENCES("/fxml/preferences.fxml"),
	DATASTORAGE("/fxml/datastorage.fxml");

	private final String resourceName;

	SceneName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getResourceName() {
		return resourceName;
	}

}