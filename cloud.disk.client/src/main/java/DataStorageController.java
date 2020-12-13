import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataStorageController {

    @FXML
    private SplitPane splitter;

    @FXML
    private HBox clientCommandMenu;

    @FXML
    private Button clientUpdateFileList;

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
    private TableView<FileDescription> serverFileView;

	@FXML
	private TableColumn<FileDescription, String> serverFileName;

	@FXML
	private TableColumn<FileDescription, String> serverFileExtension;

	@FXML
	private TableColumn<FileDescription, Long> serverFileSize;

	@FXML
	private TableColumn<FileDescription, LocalDateTime> serverFileDate;

    @FXML
    private TableView<QueueRecord> queueView;

    @FXML
    private TableColumn<QueueRecord, String> queueOperation;

    @FXML
    private TableColumn<QueueRecord, LocalDateTime> queueOperationDate;

    @FXML
    private TableColumn<QueueRecord, String> queueName;

    @FXML
    private TableColumn<QueueRecord, String> queueStatus;

    private Client client;

    private Settings clientSettings;

    private final DateTimeFormatter fileViewFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy kk:mm");

    private final DateTimeFormatter quequeViewFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy kk:mm:ss");

    private Task<ObservableList<FileDescription>> updateClientFileListTask;

    private Thread updateClientFileListThread;

    private Task<ObservableList<FileDescription>> updateServerFileListTask;

    private Thread updateServerFileListThread;

	public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
        clientSettings = client.getSettings();
    }

    @FXML
    private void initialize() {
        initializeClientFileView();
        initializeServerFileView();
        initializeQueueView();
    }

    private void initializeClientFileView() {
        clientFileView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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

        clientFileView.sortPolicyProperty().set(
                new Callback<TableView<FileDescription>, Boolean>() {

                    @Override
                    public Boolean call(TableView<FileDescription> param) {
                        Comparator<FileDescription> comparator = new Comparator<FileDescription>() {
                            @Override
                            public int compare(FileDescription fd1, FileDescription fd2) {
                                Comparator<FileDescription> paramComparator = param.getComparator();
                                if (fd1.isDirectory() && fd2.isDirectory()) {
                                    String parentPath = getParentClientDirectory();
                                    if (fd1.getAbsolutePath().equals(parentPath)) return -1;
                                    if (fd2.getAbsolutePath().equals(parentPath)) return 1;
                                    else if (paramComparator == null) return 0;
                                    else return paramComparator.compare(fd1, fd2);
                                } else if (fd1.isDirectory() && !fd2.isDirectory()) return -1;
                                else if (!fd1.isDirectory() && fd2.isDirectory()) return 1;
                                else if (paramComparator == null) return 0;
                                else return paramComparator.compare(fd1, fd2);
                            }
                        };
                        FXCollections.sort(clientFileView.getItems(), comparator);
                        return true;
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
                if (fileDesc != null && fileDesc.isDirectory()) setText("<Folder>");
                else {
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
                else setText(String.format(item.format(fileViewFormatter)));
            }
        });
    }

    private void initializeServerFileView() {
        serverFileView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serverFileView.setRowFactory(new Callback<TableView<FileDescription>, TableRow<FileDescription>>() {
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

        serverFileView.sortPolicyProperty().set(
                new Callback<TableView<FileDescription>, Boolean>() {

                    @Override
                    public Boolean call(TableView<FileDescription> param) {
                        Comparator<FileDescription> comparator = new Comparator<FileDescription>() {
                            @Override
                            public int compare(FileDescription fd1, FileDescription fd2) {
                                Comparator<FileDescription> paramComparator = param.getComparator();
                                if (fd1.isDirectory() && fd2.isDirectory()) {
                                    String parentPath = getParentServerDirectory();
                                    if (fd1.getAbsolutePath().equals(parentPath)) return -1;
                                    if (fd2.getAbsolutePath().equals(parentPath)) return 1;
                                    else if (paramComparator == null) return 0;
                                    else return paramComparator.compare(fd1, fd2);
                                } else if (fd1.isDirectory() && !fd2.isDirectory()) return -1;
                                else if (!fd1.isDirectory() && fd2.isDirectory()) return 1;
                                else if (paramComparator == null) return 0;
                                else return paramComparator.compare(fd1, fd2);
                            }
                        };
                        FXCollections.sort(serverFileView.getItems(), comparator);
                        return true;
                    }
                });

        serverFileName.setCellValueFactory(new PropertyValueFactory<>("name"));
        serverFileExtension.setCellValueFactory(new PropertyValueFactory<>("extension"));
        serverFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        serverFileSize.setCellFactory(column -> new TableCell<FileDescription, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                FileDescription fileDesc = (FileDescription) getTableRow().getItem();
                if (fileDesc != null && fileDesc.isDirectory()) setText("<Folder>");
                else {
                    if (empty || item == null || item == 0) setText(null);
                    else setText(item.toString());
                }
            }
        });
        serverFileDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        serverFileDate.setCellFactory(column -> new TableCell<FileDescription, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format(item.format(fileViewFormatter)));
            }
        });
    }

    private void initializeQueueView() {
        queueView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        queueOperation.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<QueueRecord, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<QueueRecord, String> cd) {
                QueueRecord qr = cd.getValue();
                return Bindings.createStringBinding(() -> qr.getOperation().getName(), qr.operationProperty());
            }
        });
        queueName.setCellValueFactory(new PropertyValueFactory<>("name"));
        queueOperationDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        queueOperationDate.setCellFactory(column -> new TableCell<QueueRecord, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format(item.format(quequeViewFormatter)));
            }
        });
        queueStatus.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<QueueRecord, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<QueueRecord, String> cd) {
                QueueRecord qr = cd.getValue();
                return Bindings.createStringBinding(() -> qr.getStatus().getName(), qr.statusProperty());
            }
        });
    }

    @FXML
    private void handleClientUpdateFileList(ActionEvent event) {
        updateClientFileList();
    }

    @FXML
    private void handleServerUpdateFileList(ActionEvent event) {
        updateServerFileList();
    }

    @FXML
    void handleClientFileViewOnMouseClicked(MouseEvent event) {
        if (event.getClickCount() != 2) return;
        FileDescription fileDesc = clientFileView.getSelectionModel().getSelectedItem();
        if (fileDesc == null) return;
        if (fileDesc.isDirectory()) {
            clientSettings.setClientDirectory(fileDesc.getAbsolutePath());
            updateClientFileList();
        } else clientSendFile(fileDesc);
    }

    @FXML
    void handleServerFileViewOnMouseClicked(MouseEvent event) {
        if (event.getClickCount() != 2) return;
        FileDescription fileDesc = serverFileView.getSelectionModel().getSelectedItem();
        if (fileDesc == null) return;
        if (fileDesc.isDirectory()) {
            clientSettings.setServerDirectory(fileDesc.getAbsolutePath());
            updateServerFileList();
        } else serverRequestFiles(Collections.singletonList(fileDesc));
    }

    @FXML
    void handleClientUploadFiles(ActionEvent event) {
        ObservableList<FileDescription> filesDesc = clientFileView.getSelectionModel().getSelectedItems();
        if (filesDesc == null) return;
        for (FileDescription fileDesc : filesDesc) clientSendFile(fileDesc);
    }

    @FXML
    void handleClientDownloadFiles(ActionEvent event) {
        ObservableList<FileDescription> filesDesc = serverFileView.getSelectionModel().getSelectedItems();
        if (filesDesc == null) return;
        serverRequestFiles(filesDesc);
    }

    private void clientSendFile(FileDescription fd) {
        ObservableList<QueueRecord> queueRecords = queueView.getItems();
        if (fd.isDirectory()) {
            ArrayList<QueueRecord> qrList = new ArrayList<>(16);
            qrList.add(QueueRecord.getUploadRecord(fd, clientSettings.getClientDirectory()));
            final Path currentPath = Paths.get(fd.getAbsolutePath());
            List<Path> directories = null;
            try (Stream<Path> stream = Files.walk(currentPath)) {
                directories = stream
                        .filter(file -> Files.isDirectory(file))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", e.getMessage());
                return;
            }
            if (directories != null) {
                directories.forEach((dir) -> {
                    if (!currentPath.equals(dir)) qrList.add(QueueRecord.getUploadRecord(new FileDescription(dir), clientSettings.getClientDirectory()));
                });
            }
            List<Path> files = null;
            try (Stream<Path> stream = Files.walk(currentPath)) {
                files = stream
                        .filter(file -> !Files.isDirectory(file))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", e.getMessage());
                return;
            }
            if (files != null) {
                files.forEach((file) -> {
                    qrList.add(QueueRecord.getUploadRecord(new FileDescription(file), clientSettings.getClientDirectory()));
                });
            }
            for (QueueRecord qr: qrList) if (!queueRecords.contains(qr)) queueRecords.add(qr);
        } else {
            QueueRecord qr = QueueRecord.getUploadRecord(fd, clientSettings.getClientDirectory());
            if (!queueRecords.contains(qr)) queueRecords.add(qr);
        }
        processQueue();
    }

    private void serverRequestFiles(List<FileDescription> fdList) {
        final String clientDir = clientSettings.getClientDirectory();
        final String serverDir = clientSettings.getServerDirectory();
        ObservableList<QueueRecord> queueRecords = queueView.getItems();
        fdList.stream().filter(fd -> !fd.isDirectory()).forEach(fd -> {
            QueueRecord qr = QueueRecord.getDownloadRecord(fd, clientDir, serverDir);
            if (!queueRecords.contains(qr)) queueRecords.add(qr);
        });
        ArrayList<FileDescription> dirsOnly = fdList.stream().filter(fd -> fd.isDirectory()).collect(Collectors.toCollection(ArrayList::new));
        if (dirsOnly.size() > 0) {
            OperationResult sendingCommandResult = client.requestFilesAtServer(dirsOnly, future -> {
                boolean conSuccessFull = future.isSuccess();
                if (!conSuccessFull)
                    CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", future.cause().getMessage());
            });

            if (!sendingCommandResult.isSuccess()) {
                CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", sendingCommandResult.getShortMessage());
            }
        } else processQueue();
    }

    private void processQueue() {
        ObservableList<QueueRecord> queueRecords = queueView.getItems();
        if (queueRecords.size() > 0) {
            ChannelFutureListener errorCallBack = new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    boolean conSuccessFull = future.isSuccess();
                    if (!conSuccessFull)
                        CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", future.cause().getMessage());
                }
            };

            QueueRecord curRecord = queueRecords.get(0);
            FileOperation curOperation = curRecord.getOperation();
            if (curOperation.equals(FileOperation.UPLOAD)) client.uploadFile(new UploadRequest(curRecord), errorCallBack);
            else if (curOperation.equals(FileOperation.DOWNLOAD)) client.downloadFile(new DownloadRequest(curRecord), errorCallBack);
        } else {
            updateClientFileList();
            updateServerFileList();
        }
    }

    public void updateClientFileList() {
	    updateClientFileListTask = new Task<ObservableList<FileDescription>>() {
            @Override
            public ObservableList<FileDescription> call() throws Exception {
                ObservableList<FileDescription> funcResult = FXCollections.observableArrayList();
                String curDirectory = clientSettings.getClientDirectory();
                if (curDirectory == null || curDirectory.isEmpty()) {
                    FileSystem fs = FileSystems.getDefault();
                    for (Path rootPath: fs.getRootDirectories()) {
                        funcResult.add(new FileDescription(rootPath.toAbsolutePath()));
                    }
                } else {
                    Path tempPath = Paths.get(curDirectory);
                    boolean isRootDirectory = tempPath != null && tempPath.equals(tempPath.getRoot());
                    if (!isRootDirectory && !Files.exists(tempPath)) {
                        curDirectory = System.getProperty("user.dir");
                        clientSettings.setClientDirectory(curDirectory);
                    }
                    final Path currentPath = Paths.get(curDirectory);
                    List<Path> directories = null;
                    try (Stream<Path> stream = Files.walk(currentPath, 1)) {
                        directories = stream
                                .filter(file -> Files.isDirectory(file))
                                .collect(Collectors.toList());
                    } catch (IOException e) {
                        throw e;
                    }
                    if (directories != null) {
                        directories.forEach((dir) -> {
                            FileDescription fileDesc = null;
                            if (currentPath.equals(dir)) {
                                Path parentDir = dir.getParent();
                                fileDesc = new FileDescription(parentDir);
                                fileDesc.setName("..");
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
                        throw e;
                    }
                    if (files != null) {
                        files.forEach((file) -> {
                            funcResult.add(new FileDescription(file));
                        });
                    }
                }
                return funcResult;
            }
        };

	    updateClientFileListTask.setOnSucceeded(e -> clientFileView.setItems(updateClientFileListTask.getValue()));
        updateClientFileListTask.setOnFailed(e -> {
            CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error",
                    updateClientFileListTask.getException().getMessage());
            clientSettings.setClientDirectory(getParentClientDirectory());
            updateClientFileList();
        });
        if (updateClientFileListTask.isRunning()) updateClientFileListTask.cancel();
        if (updateClientFileListThread != null && updateClientFileListThread.isAlive()) updateClientFileListThread.interrupt();
        updateClientFileListThread = new Thread(updateClientFileListTask);
        updateClientFileListThread.start();
    }

    public void updateServerFileList() {
        OperationResult sendingCommandResult = client.getServerFilesList(clientSettings.getServerDirectory(), future -> {
            boolean conSuccessFull = future.isSuccess();
            if (!conSuccessFull) CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", future.cause().getMessage());
        });

        if (!sendingCommandResult.isSuccess()) {
            CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", sendingCommandResult.getShortMessage());
        }
    }

    public void handleFileListResponse(final FileListResponse flResponse) {
        updateServerFileListTask = new Task<ObservableList<FileDescription>>() {
            @Override
            public ObservableList<FileDescription> call() {
                return FXCollections.observableArrayList(flResponse.getFilesList());
            }
        };

        updateServerFileListTask.setOnSucceeded(e -> serverFileView.setItems(updateServerFileListTask.getValue()));
        updateServerFileListTask.setOnFailed(e -> {
            CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error",
                    updateServerFileListTask.getException().getMessage());
            clientSettings.setServerDirectory(getParentServerDirectory());
            updateServerFileList();
        });
        if (updateServerFileListTask.isRunning()) updateServerFileListTask.cancel();
        if (updateServerFileListThread != null && updateServerFileListThread.isAlive()) updateServerFileListThread.interrupt();
        updateServerFileListThread = new Thread(updateServerFileListTask);
        updateServerFileListThread.start();
    }

    public void handleUploadResponse(UploadResponse ur) {
        ObservableList<QueueRecord> tableItems = queueView.getItems();
        List<QueueRecord> foundItems = tableItems.stream().filter(qr -> qr.getId().equals(ur.getId())).collect(Collectors.toList());
        foundItems.forEach(qr -> {
            qr.setStatus(ur.getStatus());
        });
        tableItems.removeIf(qr -> (qr.getStatus().equals(FileStatus.SUCCESS)));
        processQueue();
	}

    public void updateDownloadStatus(DownloadResponse dr, FileStatus fs) {
        ObservableList<QueueRecord> tableItems = queueView.getItems();
        List<QueueRecord> foundItems = tableItems.stream().filter(qr -> qr.getId().equals(dr.getId())).collect(Collectors.toList());
        foundItems.forEach(qr -> {
            qr.setStatus(fs);
        });
        tableItems.removeIf(qr -> (qr.getStatus().equals(FileStatus.SUCCESS)));
        processQueue();
    }

    public void handleDirectoryResponse(DirectoryResponse directoryResponse) {
        final String clientDir = clientSettings.getClientDirectory();
        final String serverDir = clientSettings.getServerDirectory();
        ArrayList<FileDescription> filesDesc = directoryResponse.getFilesList();
        ObservableList<QueueRecord> queueRecords = queueView.getItems();
        filesDesc.forEach(fd -> {
            QueueRecord qr = QueueRecord.getDownloadRecord(fd, clientDir, serverDir);
            if (!queueRecords.contains(qr)) queueRecords.add(qr);
        });
        processQueue();
    }

    private String getParentClientDirectory() {
        Path curPath = Paths.get(clientSettings.getClientDirectory());
        Path curParent = curPath.getParent();
        return curParent == null ? "" : curParent.toString();
    }

    private String getParentServerDirectory() {
        Path curPath = Paths.get(clientSettings.getServerDirectory());
        Path curParent = curPath.getParent();
        return curParent == null ? "" : curParent.toString();
    }

}