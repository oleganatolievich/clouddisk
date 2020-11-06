import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
                if (dotPosition < 0) {
                    fileNameWithoutExtension = fileName;
                    fileExtension = "";
                } else if (dotPosition == 0) {
                    fileNameWithoutExtension = "";
                    fileExtension = fileName;
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

    private Path fileLink;
    private boolean isDirectory;
    private String name;
    private String extension;
    private long size;
    private LocalDateTime date;

    public FileDescription(Path file) {
        fileLink = file;
        isDirectory = Files.isDirectory(fileLink);
        Path relativePath = fileLink.getFileName();
        String shortName;
        if (relativePath != null) shortName = relativePath.toString();
        else shortName = fileLink.toString();
        FileName fileName = new FileName(shortName, this.isDirectory);

        name = fileName.getFileNameWithoutExtension();
        extension = fileName.getFileExtension();
        if (!isDirectory) {
            try {
                size = Files.size(fileLink);
            } catch (IOException e) {
                size = 0;
            }
        } else size = 0;

        FileTime fileTime = null;
        try {
            BasicFileAttributes attr = Files.readAttributes(fileLink, BasicFileAttributes.class);
            fileTime = attr.lastModifiedTime();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileTime != null) date = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        else date = null;
    }

    public Path getFileLink() {
        return fileLink;
    }

    public void setFileLink(Path fileLink) {
        this.fileLink = fileLink;
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

}
