package in.codehex.shareipo.hotspot;

import java.util.ArrayList;

/**
 * Created by Bobby on 22-04-2016
 */
public interface FinishScanListener {
    void onFinishScan(ArrayList<ClientScanResult> clients);
}
