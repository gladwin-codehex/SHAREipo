package in.codehex.shareipo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import in.codehex.shareipo.app.Config;
import in.codehex.shareipo.db.DatabaseHandler;
import in.codehex.shareipo.hotspot.ClientScanResult;
import in.codehex.shareipo.hotspot.FinishScanListener;
import in.codehex.shareipo.hotspot.WifiApManager;
import in.codehex.shareipo.model.DeviceItem;
import in.codehex.shareipo.model.FileItem;

public class ShareActivity extends AppCompatActivity implements View.OnClickListener {

    Toolbar toolbar;
    Button btnSelectAll, btnShare;
    RecyclerView recyclerView;
    SwipeRefreshLayout refreshLayout;
    List<DeviceItem> deviceItemList;
    List<FileItem> shareItemList;
    ArrayList<String> fileList;
    DeviceAdapter adapter;
    DatabaseHandler databaseHandler;
    WifiApManager wifiApManager;
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
                for (int i = 0; i < deviceItemList.size(); i++)
                    if (deviceItemList.get(i).isSelected()) {
                        shareFiles(i);
                    }
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
        fileList = (ArrayList<String>) getIntent().getSerializableExtra("file");
        databaseHandler = new DatabaseHandler(this);
        userPreferences = getSharedPreferences(Config.PREF_USER, MODE_PRIVATE);
        adapter = new DeviceAdapter(this, deviceItemList);
        wifiApManager = new WifiApManager(this);
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
    }

    /**
     * Share the files to the selected user
     */
    private void shareFiles(final int pos) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(deviceItemList.get(pos).getDeviceIp(), 8081);
                    DataOutputStream dos = new DataOutputStream(socket
                            .getOutputStream());
                    String name = userPreferences.getString("name", null);
                    String mac = info.getMacAddress();
                    String files = TextUtils.join(",", fileList);
                    dos.writeUTF(name);
                    dos.writeUTF(mac);
                    dos.writeUTF(files);
                    socket.close();
                    for (int i = 0; i < fileList.size(); i++)
                        shareItemList.add(new FileItem(deviceItemList.get(pos).getUserName(),
                                deviceItemList.get(pos).getDeviceAddress(), fileList.get(i)));
                    databaseHandler.addShareFiles(shareItemList);
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
        wifiApManager.getClientList(true, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {
                refreshLayout.setRefreshing(false);
                deviceItemList.clear();
                adapter.notifyDataSetChanged();
                for (int i = 0; i < clients.size(); i++) {
                    final int pos = i;
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Socket socket = new Socket(clients.get(pos).getIpAddr(), 8080);
                                DataOutputStream dos = new DataOutputStream(socket
                                        .getOutputStream());
                                dos.writeUTF("profile");
                                DataInputStream dis = new DataInputStream(socket
                                        .getInputStream());
                                String name = dis.readUTF();
                                int dp = Integer.parseInt(dis.readUTF());
                                deviceItemList.add(pos, new DeviceItem(clients.get(pos).getDevice(),
                                        clients.get(pos).getHWAddr(), clients.get(pos).getIpAddr(),
                                        name, dp, false));
                                socket.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    adapter.notifyDataSetChanged();
                }
            }
        });
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

            holder.name.setText(deviceItem.getUserName());
            holder.mac.setText(deviceItem.getDeviceAddress());
            holder.ip.setText(deviceItem.getDeviceIp());
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
