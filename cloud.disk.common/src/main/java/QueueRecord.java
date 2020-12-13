import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

public class QueueRecord {

    private String id;
    private final ObjectProperty<FileOperation> operation = new SimpleObjectProperty<>();
    private String name;
    private LocalDateTime date;
    private FileDescription sourceFileDescription;
    private String sourceFileName;
    private String destFileName;
    private final ObjectProperty<FileStatus> status = new SimpleObjectProperty<>();
    private double progress;

    public QueueRecord() {
    }

    public QueueRecord(FileOperation operation, FileDescription sourceFileDescription, String sourceFileName, String destFileName) {
        this.id = UUID.randomUUID().toString();
        this.operation.setValue(operation);
        this.sourceFileDescription = sourceFileDescription;
        this.sourceFileName = sourceFileName;
        this.destFileName = destFileName;
        this.date = LocalDateTime.now();
        this.status.set(FileStatus.PENDING);
        fillName();
    }

    public static QueueRecord getUploadRecord(FileDescription fd, String clientDirectory) {
        String fileName = fd.getAbsolutePath();
        Path clientPath = Paths.get(fileName);
        Path clientParentPath = Paths.get(clientDirectory);
        String destFileName = fileName;
        if (!(clientPath == null || clientParentPath == null)) destFileName = clientParentPath.relativize(clientPath).toString();
        return new QueueRecord(FileOperation.UPLOAD, fd, fd.getAbsolutePath(), destFileName);
    }

    public static QueueRecord getDownloadRecord(FileDescription fd, String clientDir, String serverDir) {
        String fileName = fd.getAbsolutePath();
        Path serverPath = Paths.get(fileName);
        Path serverParentPath = Paths.get(serverDir);
        String sourceFileName = fileName;
        String destFileName = fileName;
        if (!(serverPath == null || serverParentPath == null)) {
            sourceFileName = serverParentPath.relativize(serverPath).toString();
            destFileName = Paths.get(clientDir, sourceFileName).toString();
        }
        return new QueueRecord(FileOperation.DOWNLOAD, fd, sourceFileName, destFileName);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public final FileOperation getOperation() {
        return this.operation.get();
    }

    public final void setOperation(FileOperation operation) {
        this.operation.set(operation);
    }

    public final ObjectProperty<FileOperation> operationProperty() {
        return this.operation;
    }

    public final FileStatus getStatus() {
        return this.status.get();
    }

    public final void setStatus(FileStatus status) {
        this.status.set(status);
    }

    public final ObjectProperty<FileStatus> statusProperty() {
        return this.status;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileDescription getSourceFileDescription() {
        return sourceFileDescription;
    }

    public void setSourceFileDescription(FileDescription sourceFileDescription) {
        this.sourceFileDescription = sourceFileDescription;
    }

    public String getDestFileName() {
        return destFileName;
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    private void fillName() {
        name = String.format("%s -> %s", sourceFileName, destFileName);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof QueueRecord)) return false;
        QueueRecord otherObject = (QueueRecord) obj;
        return  (otherObject.getOperation().equals(this.getOperation())
                && otherObject.sourceFileDescription.equals(this.sourceFileDescription)
                && otherObject.destFileName.equals(this.destFileName)
                && otherObject.getStatus().equals(this.getStatus()));
    }

}
