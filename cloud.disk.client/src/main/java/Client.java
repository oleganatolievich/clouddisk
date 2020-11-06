import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client extends Application {

	private Settings settings;
	private final String settingsFileName = "client_settings.json";
	private Stage mainStage;
	private AuthorizationController authController;
	private Connector connector;

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

	public static void main(String[] args) {
		launch(args);
	}

	public OperationResult authorizeUser(String login, String password, ChannelFutureListener finishListener) {
		String passwordHash = getHashString(password, "SHA-512");
		if (connector == null) connector = new Connector(this, settings);
		OperationResult authResult = connector.startClient();
		if (authResult.isSuccess()) connector.authorizeUser(login, passwordHash, finishListener);
		return authResult;
	}

	public void handleAuthorizationResult(OperationResult authResult) {
		if (authController != null) authController.handleAuthorizationResult(authResult);
	}

	public String getHashString(String source, String algorithmName) {
		String funcResult = null;
		try {
			MessageDigest coder = MessageDigest.getInstance(algorithmName);
			byte[] messageBytes = coder.digest(source.getBytes());
			funcResult = new BigInteger(1, messageBytes).toString(16);
			while (funcResult.length() < 32) funcResult = "0" + funcResult;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return funcResult;
	}

	@Override
	public void start(Stage stage) {
		setMainStage(stage);
		mainStage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});
		OperationResult loadResult = loadConfiguration();
		if (!loadResult.isSuccess()) saveConfiguration();
		showLoginForm(stage);
	}

	public OperationResult loadConfiguration() {
		OperationResult funcResult = OperationResult.getSuccess();
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
		OperationResult funcResult = OperationResult.getSuccess();
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File(settingsFileName), settings);
		} catch (IOException e) {
			funcResult = OperationResult.getExceptionResult(e);
		}
		return funcResult;
	}

	private void showLoginForm(Stage stage) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/authorization.fxml"));
		Parent loginForm = null;
		try {
			loginForm = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.printf("Couldn't load authorization window: %n%s%n", e.getMessage());
			Platform.exit();
			System.exit(1);
		}
		if (loginForm != null) {
			authController = loader.<AuthorizationController>getController();
			authController.setClient(this);

			stage.setTitle("Cloud disk");
			stage.setScene(new Scene(loginForm));
			stage.show();
		}
	}

}
