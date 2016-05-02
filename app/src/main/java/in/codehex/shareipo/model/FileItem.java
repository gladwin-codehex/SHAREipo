package in.codehex.shareipo.model;

/**
 * Created by Bobby on 28-04-2016
 */
public class FileItem {

    private int id;
    private String user, macId, file;
    private boolean isSelected;

    public FileItem(String user, String macId, String file) {
        this.user = user;
        this.macId = macId;
        this.file = file;
    }

    public FileItem(String user, String macId, String file, boolean isSelected) {
        this.user = user;
        this.macId = macId;
        this.file = file;
        this.isSelected = isSelected;
    }

    public FileItem(int id, String user, String macId, String file, boolean isSelected) {
        this.id = id;
        this.user = user;
        this.macId = macId;
        this.file = file;
        this.isSelected = isSelected;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMacId() {
        return macId;
    }

    public void setMacId(String macId) {
        this.macId = macId;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
