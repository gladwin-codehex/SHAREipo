package in.codehex.shareipo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.codehex.shareipo.app.Config;
import in.codehex.shareipo.db.DatabaseHandler;
import in.codehex.shareipo.model.DeviceItem;
import in.codehex.shareipo.model.FileItem;

public class ShareActivity extends AppCompatActivity implements View.OnClickListener {

    static boolean isLoaded;
    Toolbar toolbar;
    Button btnSelectAll, btnShare;
    RecyclerView recyclerView;
    SwipeRefreshLayout refreshLayout;
    List<DeviceItem> deviceItemList;
    List<FileItem> shareItemList;
    ArrayList<String> fileList;
    DeviceAdapter adapter;
    DatabaseHandler databaseHandler;
    WifiManager wifiManager;
    WifiInfo info;
    Intent intent;
    SharedPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        initObjects();
        prepareObjects();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.select_all:
                for (int i = 0; i < deviceItemList.size(); i++)
                    deviceItemList.get(i).setSelected(true);
                adapter.notifyDataSetChanged();
                break;
            case R.id.share:
                boolean isShared = false;
                for (int i = 0; i < deviceItemList.size(); i++)
                    if (deviceItemList.get(i).isSelected()) {
                        shareFiles(i);
                        isShared = true;
                    }
                if (isShared) {
                    Toast.makeText(ShareActivity.this,
                            "Files shared successfully", Toast.LENGTH_SHORT).show();
                    intent = new Intent(ShareActivity.this, MainActivity.class);
                    intent.addFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else Toast.makeText(ShareActivity.this,
                        "No file has been selected", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * initialize the objects
     */
    private void initObjects() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        btnSelectAll = (Button) findViewById(R.id.select_all);
        btnShare = (Button) findViewById(R.id.share);
        recyclerView = (RecyclerView) findViewById(R.id.user_list);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);

        deviceItemList = new ArrayList<>();
        shareItemList = new ArrayList<>();
        fileList = new ArrayList<>();
        databaseHandler = new DatabaseHandler(this);
        userPreferences = getSharedPreferences(Config.PREF_USER, MODE_PRIVATE);
        adapter = new DeviceAdapter(this, deviceItemList);
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        info = wifiManager.getConnectionInfo();
    }

    /**
     * implement and manipulate the objects
     */
    private void prepareObjects() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnSelectAll.setOnClickListener(this);
        btnShare.setOnClickListener(this);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        refreshLayout.setColorSchemeResources(R.color.primary, R.color.primary_dark,
                R.color.accent);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.setRefreshing(true);
                scan();
            }
        });
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
                scan();
            }
        });

        fileList.clear();
        fileList = (ArrayList<String>) getIntent().getSerializableExtra("file");
    }

    /**
     * Share the files to the selected user
     */
    private void shareFiles(final int pos) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(deviceItemList.get(pos).getIp(), 8081);
                    DataOutputStream dos = new DataOutputStream(socket
                            .getOutputStream());
                    String name = userPreferences.getString("name", null);
                    String mac = info.getMacAddress();
                    String files = TextUtils.join(",", fileList);
                    dos.writeUTF(name);
                    dos.writeUTF(mac);
                    dos.writeUTF(files);
                    for (int i = 0; i < fileList.size(); i++)
                        shareItemList.add(new FileItem(deviceItemList.get(pos).getName(),
                                deviceItemList.get(pos).getMac(), fileList.get(i)));
                    databaseHandler.addShareFiles(shareItemList);
                    dos.flush();
                    dos.close();
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Scan for the client devices connected in the network
     */
    private void scan() {
        isLoaded = false;
        deviceItemList.clear();
        adapter.notifyDataSetChanged();
        int ipAddress = info.getIpAddress();
        final String ip = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String subnet = getSubnet(ip);
                    for (int i = 1; i <= 254; i++) {
                        String host = subnet + i;
                        if (InetAddress.getByName(host).isReachable(Config.NETWORK_TIMEOUT)
                                && !ip.equals(host))
                            getDeviceDetails(host);
                        if (i == 254)
                            isLoaded = true;
                    }
                    if (deviceItemList.isEmpty())
                        ShareActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ShareActivity.this,
                                        "No device is available to share file",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isLoaded)
                    refreshLayout.setRefreshing(false);
                adapter.notifyDataSetChanged();
                handler.postDelayed(this, 1000);
            }
        });
    }

    /**
     * Get the device details from the ip address.
     *
     * @param host the ip address to be connected
     */
    private void getDeviceDetails(final String host) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(host, 8080);
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String name = dis.readUTF();
                    int dp = Integer.parseInt(dis.readUTF());
                    String mac = dis.readUTF();
                    deviceItemList.add(new DeviceItem(name, mac, host, dp, false));
                    dis.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Get the subnet from the device's ip address.
     *
     * @param currentIP the ip address of the device
     * @return subnet of the ip address
     */
    private String getSubnet(String currentIP) {
        int firstSeparator = currentIP.lastIndexOf("/");
        int lastSeparator = currentIP.lastIndexOf(".");
        return currentIP.substring(firstSeparator + 1, lastSeparator + 1);
    }

    /**
     * Get the drawable resource from the resource id
     *
     * @param resId the resource id of the drawable
     * @return drawable id
     */
    private int getDrawableResource(int resId) {
        switch (resId) {
            case R.id.male1:
                return R.drawable.male1;
            case R.id.male2:
                return R.drawable.male2;
            case R.id.male3:
                return R.drawable.male3;
            case R.id.female1:
                return R.drawable.female1;
            case R.id.female2:
                return R.drawable.female2;
            case R.id.female3:
                return R.drawable.female3;
            default:
                return R.drawable.male1;
        }
    }

    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

        Context context;
        List<DeviceItem> deviceItemList;

        public DeviceAdapter(Context context, List<DeviceItem> deviceItemList) {
            this.context = context;
            this.deviceItemList = deviceItemList;
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device, parent, false);
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final DeviceViewHolder holder, int position) {
            final DeviceItem deviceItem = deviceItemList.get(position);

            holder.name.setText(deviceItem.getName());
            holder.mac.setText(deviceItem.getMac());
            holder.ip.setText(deviceItem.getIp());
            holder.dp.setImageDrawable(ContextCompat.getDrawable(context,
                    getDrawableResource(deviceItem.getImgId())));
            holder.select.setChecked(deviceItem.isSelected());
            holder.select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        deviceItem.setSelected(true);
                    } else deviceItem.setSelected(false);
                    holder.select.setChecked(deviceItem.isSelected());
                }
            });
        }

        @Override
        public int getItemCount() {
            return deviceItemList.size();
        }

        protected class DeviceViewHolder extends RecyclerView.ViewHolder {

            private TextView name, mac, ip;
            private ImageView dp;
            private CheckBox select;

            public DeviceViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(R.id.name);
                mac = (TextView) view.findViewById(R.id.mac);
                ip = (TextView) view.findViewById(R.id.ip);
                dp = (ImageView) view.findViewById(R.id.dp);
                select = (CheckBox) view.findViewById(R.id.select);
            }
        }
    }
}
