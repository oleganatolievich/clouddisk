package clouddiskclient.util;

import clouddiskclient.model.FormController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class FXMLUnpack {
	
	private static Logger logger = LogManager.getLogger();

	public Scene load(FXMLContainer container) {
		
		if (container.hasScene()) {
			return container.getScene();
		}

		URL url = getClass().getResource(container.getResourceName());

		if (url == null) {
			logger.error("The URL for the resource \"{}\" was not found", container.getResourceName());
			getDebugInfo(container.getResourceName());
			Platform.exit();
			return null;
		}

		FXMLLoader loader = new FXMLLoader(url);
		Scene scene;

		try {
			scene = new Scene(loader.load());
		} catch (IOException e) {
			e.printStackTrace();
			Platform.exit();
			return null;
		}
		
		container.setScene(scene);
		FormController controller = loader.getController();
		container.setController(controller);

		if (controller != null) controller.setContainer(container);

		return scene;
	}

	private void getDebugInfo(String resourceName) {
		logger.error("Working Directory = {}", System.getProperty("user.dir"));
		logger.error("Resources for {}", resourceName);
		try {
			Enumeration<URL> urls = ClassLoader.getSystemClassLoader().getResources(resourceName);
			while (urls.hasMoreElements()) {
				logger.error(urls.nextElement());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
