package clouddiskclient;

import clouddiskclient.model.FormController;
import clouddiskclient.model.SceneName;
import clouddiskclient.util.FXMLContainer;
import common.OperationResult;
import common.OperationType;
import io.netty.channel.ChannelFutureListener;
import javafx.application.Application;
import javafx.stage.Stage;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class CloudDiskClient extends Application {

	private static Map<SceneName, FXMLContainer> scenes = new HashMap<>();
	private CloudDiskClientConnector connector;
	private String serverHost;
	private int serverPort;

	public static void main(String[] args) {
		launch(args);
	}

	public OperationResult authorizeUser(String login, String password, ChannelFutureListener finishListener) {
		String passwordHash = getHashString(password, "SHA-512");
		serverHost = "localhost";
		serverPort = 49152; //взять из настроек позже
		if (connector == null) connector = new CloudDiskClientConnector(this, serverHost, serverPort);
		OperationResult authResult = connector.startClient();
		if (authResult.isSuccess()) connector.authorizeUser(login, passwordHash, finishListener);
		return authResult;
	}

	public void returnAuthorizationResult(boolean authSuccessful) {
		FormController controller = scenes.get(SceneName.AUTHORIZATION).getController();
		if (controller != null) controller.processOperation(OperationType.AUTHORIZATION, OperationResult.getSuccess());
	}

	public String getHashString(String source, String algorithmName)
	{
		String funcResult = null;
		try {
			MessageDigest coder = MessageDigest.getInstance(algorithmName);
			byte[] messageBytes = coder.digest(source.getBytes());
			funcResult = new BigInteger(1, messageBytes).toString(16);
			while (funcResult.length() < 32) funcResult = "0" + funcResult;
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return funcResult;
	}

	@Override
	public void start(Stage stage) {
		scenes.put(SceneName.AUTHORIZATION, new FXMLContainer(this, stage, SceneName.AUTHORIZATION));
		scenes.put(SceneName.PREFERENCES, new FXMLContainer(this, stage, SceneName.PREFERENCES));
		scenes.put(SceneName.DATASTORAGE, new FXMLContainer(this, stage, SceneName.DATASTORAGE));

		stage.setTitle("User login");
		stage.setScene(scenes.get(SceneName.AUTHORIZATION).getScene());
		stage.show();
	}

	public static Map<SceneName, FXMLContainer> getScenes() {
		return scenes;
	}

}