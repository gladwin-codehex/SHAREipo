package in.codehex.shareipo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import in.codehex.shareipo.hotspot.ClientScanResult;
import in.codehex.shareipo.hotspot.FinishScanListener;
import in.codehex.shareipo.hotspot.WifiApManager;
import in.codehex.shareipo.model.DeviceItem;

public class ShareActivity extends AppCompatActivity implements View.OnClickListener {

    Toolbar toolbar;
    Button btnSelectAll, btnShare;
    RecyclerView recyclerView;
    SwipeRefreshLayout refreshLayout;
    List<DeviceItem> deviceItemList;
    DeviceAdapter adapter;
    WifiApManager wifiApManager;

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
                    if (deviceItemList.get(i).isSelected())
                        Toast.makeText(ShareActivity.this,
                                deviceItemList.get(i).getDeviceIp(), Toast.LENGTH_SHORT).show();
                // TODO: send the shared file detail to the selected users and go back to main activity clearing the activity stack
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
        adapter = new DeviceAdapter(this, deviceItemList);
        wifiApManager = new WifiApManager(this);
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
     * Scan for the client devices connected in the network
     */
    private void scan() {
        //TODO: get device name and pic during the scan
        wifiApManager.getClientList(true, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {
                refreshLayout.setRefreshing(false);
                deviceItemList.clear();
                adapter.notifyDataSetChanged();
                for (int i = 0; i < clients.size(); i++) {
                    deviceItemList.add(i, new DeviceItem(clients.get(i).getDevice(),
                            clients.get(i).getHWAddr(), clients.get(i).getIpAddr(), false));
                    adapter.notifyItemInserted(i);
                }
            }
        });
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
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            final DeviceItem deviceItem = deviceItemList.get(position);

            holder.name.setText(deviceItem.getDeviceName());
            holder.mac.setText(deviceItem.getDeviceAddress());
            holder.ip.setText(deviceItem.getDeviceIp());
            holder.select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked)
                        deviceItem.setSelected(true);
                    else deviceItem.setSelected(false);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return deviceItemList.size();
        }

        protected class DeviceViewHolder extends RecyclerView.ViewHolder {

            private TextView name, mac, ip;
            private CheckBox select;

            public DeviceViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(R.id.name);
                mac = (TextView) view.findViewById(R.id.mac);
                ip = (TextView) view.findViewById(R.id.ip);
                select = (CheckBox) view.findViewById(R.id.select);
            }
        }
    }
}
