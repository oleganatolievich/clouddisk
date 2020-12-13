import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class FileDescription {

    private class FileName {
        String fileName;
        String fileNameWithoutExtension;
        String fileExtension;

        public FileName(String fileName, boolean isDirectory) {
            this.fileName = fileName;
            if (isDirectory) {
                fileNameWithoutExtension = fileName;
                fileExtension = "";
            } else {
                int dotPosition = fileName.lastIndexOf(".");
                if (dotPosition <= 0) {
                    fileNameWithoutExtension = fileName;
                    fileExtension = "";
                } else {
                    fileNameWithoutExtension = fileName.substring(0, dotPosition);
                    fileExtension = fileName.substring(dotPosition + 1);
                }
            }
        }

        public String getFileNameWithoutExtension() {
            return fileNameWithoutExtension;
        }

        public void setFileNameWithoutExtension(String fileNameWithoutExtension) {
            this.fileNameWithoutExtension = fileNameWithoutExtension;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }
    }

    @JsonProperty("absolutePath")
    private String absolutePath;

    @JsonProperty("isDirectory")
    private boolean isDirectory;

    @JsonProperty("name")
    private String name;

    @JsonProperty("extension")
    private String extension;

    @JsonProperty("size")
    private long size;

    @JsonProperty("date")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    @JsonProperty("isRootDirectory")
    private boolean isRootDirectory;

    public FileDescription() {
        isRootDirectory = false;
        isDirectory = false;
        absolutePath = "";
        name = "";
        extension = "";
        size = 0;
        date = null;
    }

    public FileDescription(Path file) {
        if (file == null) {
            isRootDirectory = true;
            isDirectory = true;
            absolutePath = "";
            name = "";
            extension = "";
            size = 0;
            date = null;
        } else {
            absolutePath = file.toString();
            Path rootPath = file.getRoot();
            String rootPathString = "";
            if (rootPath != null) rootPathString = rootPath.toString();
            isRootDirectory = (file.toString() == rootPathString);
            isDirectory = isRootDirectory || Files.isDirectory(file);
            Path relativePath = file.getFileName();
            String shortName;
            if (relativePath != null) shortName = relativePath.toString();
            else shortName = file.toString();
            FileName fileName = new FileName(shortName, this.isDirectory);

            name = fileName.getFileNameWithoutExtension();
            extension = fileName.getFileExtension();
            if (!isDirectory) {
                try {
                    size = Files.size(file);
                } catch (IOException e) {
                    size = 0;
                }
            } else size = 0;

            FileTime fileTime = null;
            if (!isRootDirectory) {
                try {
                    BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                    fileTime = attr.lastModifiedTime();
                } catch (IOException e) {
                    if (!(e instanceof AccessDeniedException)) {
                        CommonUtils.showMessageBox(Alert.AlertType.ERROR, "Error", e.getMessage());
                    }
                }
            }
            if (fileTime != null) date = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
            else date = null;
        }
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public boolean isRootDirectory() {
        return isRootDirectory;
    }

    public void setRootDirectory(boolean rootDirectory) {
        isRootDirectory = rootDirectory;
    }

}
