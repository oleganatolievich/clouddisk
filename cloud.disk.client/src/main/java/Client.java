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
import java.util.ArrayList;

public class Client extends Application {

	private Settings settings;
	private final String settingsFileName = "client_settings.json";
	private Stage mainStage;
	private AuthorizationController authController;
	private DataStorageController dsController;
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

	public DataStorageController getDSController() {
		return dsController;
	}

	public void setDSController(DataStorageController dsController) {
		this.dsController = dsController;
	}

	public static void main(String[] args) {
		launch(args);
	}

	public OperationResult authorizeUser(String login, String password, ChannelFutureListener finishListener) {
		if (connector == null) connector = new Connector(this);
		OperationResult startingNetResult = connector.startClient();
		if (!startingNetResult.isSuccess()) return startingNetResult;
		OperationResult authResult = connector.authorizeUser(login, password, finishListener);
		return authResult;
	}

	public OperationResult registerUser(String login, String password, ChannelFutureListener finishListener) {
		OperationResult<String> hashingResult = PasswordHasher.createHash(password);
		if (!hashingResult.isSuccess()) return hashingResult;
		String passwordHash = hashingResult.getProduct();
		if (connector == null) connector = new Connector(this);
		OperationResult startingNetResult = connector.startClient();
		if (!startingNetResult.isSuccess()) return startingNetResult;
		OperationResult authResult = connector.authorizeUser(login, passwordHash, finishListener);
		return authResult;
	}

	public OperationResult uploadFile(UploadRequest ur, ChannelFutureListener finishListener) {
		OperationResult funcResult;
		if (connector == null || !connector.isRunning()) funcResult = OperationResult.getFailure("Server is down");
		else funcResult = connector.uploadFile(ur, finishListener);
		return funcResult;
	}

	public OperationResult downloadFile(DownloadRequest dr, ChannelFutureListener finishListener) {
		OperationResult funcResult;
		if (connector == null || !connector.isRunning()) funcResult = OperationResult.getFailure("Server is down");
		else funcResult = connector.downloadFile(dr, finishListener);
		return funcResult;
	}

	public OperationResult getServerFilesList(String serverPath, ChannelFutureListener finishListener) {
		OperationResult funcResult;
		if (connector == null || !connector.isRunning()) funcResult = OperationResult.getFailure("Server is down");
		else funcResult = connector.getServerFilesList(serverPath, finishListener);
		return funcResult;
	}

	public OperationResult requestFilesAtServer(ArrayList<FileDescription> fdList, ChannelFutureListener finishListener) {
		OperationResult funcResult;
		if (connector == null || !connector.isRunning()) funcResult = OperationResult.getFailure("Server is down");
		else funcResult = connector.requestFilesAtServer(fdList, finishListener);
		return funcResult;
	}

	public void handleAuthorizationResponse(OperationResult commandResult) {
		if (commandResult.isSuccess()) {
			AuthorizationResponse ar = (AuthorizationResponse)commandResult.getProduct();
			settings.setServerDirectory(ar.getServerRoot());
		}
		if (authController != null) authController.handleAuthorizationResponse(commandResult);
	}

	public void handleFileListResponse(FileListResponse flResponse) {
		if (dsController != null) dsController.handleFileListResponse(flResponse);
	}

	public void handleDirectoryResponse(DirectoryResponse directoryResponse) {
		if (dsController != null) dsController.handleDirectoryResponse(directoryResponse);
	}

	public void handleUploadResponse(UploadResponse uploadResponse) {
		if (dsController != null) dsController.handleUploadResponse(uploadResponse);
	}

	public void updateDownloadStatus(DownloadResponse dr, FileStatus fs) {
		if (dsController != null) dsController.updateDownloadStatus(dr, fs);
	}

	public void handleServerError(ServerError error) {
		System.out.println("Oops: " + error.getShortMessage());
	}

	@Override
	public void start(Stage stage) {
		setMainStage(stage);
		mainStage.setOnCloseRequest(e -> {
			saveConfiguration();
			if (connector != null) connector.stopClientNow();
			Platform.exit();
			System.exit(0);
		});
		OperationResult loadResult = loadConfiguration();
		if (!loadResult.isSuccess()) saveConfiguration();
		showLoginForm(stage);
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

	private void showLoginForm(Stage stage) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Authorization.fxml"));
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
			authController = loader.getController();
			authController.setClient(this);
			authController.fillSettings();

			stage.setTitle("Cloud disk client");
			stage.setScene(new Scene(loginForm));
			stage.show();
		}
	}

}
