package in.codehex.shareipo.model;

/**
 * Created by Bobby on 22-04-2016
 */
public class DeviceItem {

    private String deviceName, deviceAddress, deviceIp;
    private boolean isSelected;

    public DeviceItem(String deviceName, String deviceAddress, String deviceIp,
                      boolean isSelected) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.deviceIp = deviceIp;
        this.isSelected = isSelected;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
