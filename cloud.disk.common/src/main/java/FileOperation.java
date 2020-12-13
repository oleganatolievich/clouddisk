public enum FileOperation {
    CLIENT_RENAME("Rename (Client)"),
    CLIENT_DELETE("Delete (Client)"),
    UPLOAD("Upload"),
    DOWNLOAD("Download"),
    SERVER_RENAME("Rename (Server)"),
    SERVER_DELETE("Delete (Server)");

    private String name;

    FileOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}