package in.codehex.shareipo.model;

/**
 * Created by Bobby on 22-04-2016
 */
public class DeviceItem {

    private String name, mac, ip;
    private boolean isSelected;
    private int imgId;

    public DeviceItem(String name, String mac, String ip,
                      int imgId, boolean isSelected) {
        this.name = name;
        this.mac = mac;
        this.ip = ip;
        this.imgId = imgId;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public int getImgId() {
        return imgId;
    }

    public void setImgId(int imgId) {
        this.imgId = imgId;
    }
}
