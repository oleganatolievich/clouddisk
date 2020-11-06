import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import javafx.util.Callback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataStorageController {

    private Client client;
    private String currentDirectory;

    @FXML
    private HBox clientCommandMenu;

    @FXML
    private Button clientUpdateFileList;

    @FXML
    private Button clientRenameFile;

    @FXML
    private Button clientDeleteFiles;

    @FXML
    private TableView<FileDescription> clientFileView;

	@FXML
	private TableColumn<FileDescription, String> clientFileName;

	@FXML
	private TableColumn<FileDescription, String> clientFileExtension;

	@FXML
	private TableColumn<FileDescription, Long> clientFileSize;

	@FXML
	private TableColumn<FileDescription, LocalDateTime> clientFileDate;

    @FXML
    private Button clientUploadFiles;

    @FXML
    private Button clientDownloadFiles;

    @FXML
    private HBox serverCommandMenu;

    @FXML
    private Button serverUpdateFileList;

    @FXML
    private Button serverRenameFile;

    @FXML
    private Button serverDeleteFiles;

    @FXML
    private TableView<FileDescription> serverFileView;

	@FXML
	private TableColumn<FileDescription, String> serverFileName;

	@FXML
	private TableColumn<FileDescription, String> serverFileExtension;

	@FXML
	private TableColumn<FileDescription, Long> serverFileSize;

	@FXML
	private TableColumn<FileDescription, LocalDateTime> serverFileDate;

    private final DateTimeFormatter defaultLDTFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy kk:mm");

	public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @FXML
    private void initialize() {

        clientFileView.setRowFactory(new Callback<TableView<FileDescription>, TableRow<FileDescription>>() {
            @Override
            public TableRow<FileDescription> call(TableView<FileDescription> param) {
                final TableRow<FileDescription> row = new TableRow<FileDescription>() {
                    @Override
                    protected void updateItem(FileDescription row, boolean empty) {
                        super.updateItem(row, empty);
                        if (!empty && row != null && row.isDirectory()) setStyle("-fx-font-weight: bold;");
                        else setStyle("");
                    }
                };
                return row;
            }
        });

		clientFileName.setCellValueFactory(new PropertyValueFactory<>("name"));
		clientFileExtension.setCellValueFactory(new PropertyValueFactory<>("extension"));
		clientFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        clientFileSize.setCellFactory(column -> new TableCell<FileDescription, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                FileDescription fileDesc = (FileDescription) getTableRow().getItem();
                if (fileDesc != null && fileDesc.isDirectory()) {
                    setText("<Folder>");
                } else {
                    if (empty || item == null || item == 0) setText(null);
                    else setText(item.toString());
                }
            }
        });
		clientFileDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        clientFileDate.setCellFactory(column -> new TableCell<FileDescription, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format(item.format(defaultLDTFormatter)));
            }
        });
	}

    @FXML
    private void handleClientUpdateFileList(ActionEvent event) {
        updateFileList();
    }

    @FXML
    private void handleClientRenameFile(ActionEvent event) {
    }

    @FXML
    private void handleClientDeleteFiles(ActionEvent event) {
    }

    @FXML
    void handleClientFileViewOnMouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            FileDescription fileDesc = clientFileView.getSelectionModel().getSelectedItem();
            if (fileDesc != null && fileDesc.isDirectory()) {
                Path file = fileDesc.getFileLink();
                setCurrentDirectory(file.toString());
                updateFileList();
            }
        }
    }

    public void updateFileList() {
        Path currentPath = Paths.get(currentDirectory);
        if (!Files.exists(currentPath)) currentDirectory = System.getProperty("user.dir");
		clientFileView.setItems(getFilesFromCurrentDirectory());
    }

    private ObservableList<FileDescription> getFilesFromCurrentDirectory() {
		ObservableList<FileDescription> funcResult = FXCollections.observableArrayList();
        Path currentPath = Paths.get(currentDirectory);
        List<Path> directories = null;
		try (Stream<Path> stream = Files.walk(currentPath, 1)) {
            directories = stream
                    .filter(file -> Files.isDirectory(file))
                    .collect(Collectors.toList());
		} catch (Exception e) {
		    e.printStackTrace();
        }
		if (directories != null) {
		    directories.forEach((dir) -> {
		        FileDescription fileDesc = null;
		        if (currentPath.equals(dir)) {
                    Path parentDir = dir.getParent();
                    if (parentDir != null) {
                        fileDesc = new FileDescription(parentDir);
                        fileDesc.setName("..");
                    }
                } else fileDesc = new FileDescription(dir);
		        if (fileDesc != null) funcResult.add(fileDesc);
            });
        }
        List<Path> files = null;
        try (Stream<Path> stream = Files.walk(currentPath, 1)) {
            files = stream
                    .filter(file -> !Files.isDirectory(file))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (files != null) {
            files.forEach((file) -> {
                funcResult.add(new FileDescription(file));
            });
        }
		return funcResult;
	}

}