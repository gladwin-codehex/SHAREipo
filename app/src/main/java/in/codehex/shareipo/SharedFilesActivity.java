package in.codehex.shareipo;

import org.apache.commons.io.output.ByteArrayOutputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.codehex.shareipo.app.Config;
import in.codehex.shareipo.app.ItemClickListener;
import in.codehex.shareipo.db.DatabaseHandler;
import in.codehex.shareipo.model.DeviceItem;
import in.codehex.shareipo.model.FileItem;

public class SharedFilesActivity extends AppCompatActivity {

    static String ip;
    boolean isLoaded, isAvailable;
    Toolbar toolbar;
    RecyclerView recyclerView;
    DatabaseHandler databaseHandler;
    List<FileItem> fileItemList;
    List<DeviceItem> deviceItemList;
    FileAdapter adapter;
    ProgressDialog mProgressDialog;
    WifiManager wifiManager;
    WifiInfo info;
    Intent intent;

    static int getChunk(InputStream inputStr, ByteArrayOutputStream bytes, byte[] buffer) throws
            IOException {
        int read = inputStr.read(buffer, 0, 1024);
        while (read < 1024) {
            read += inputStr.read(buffer, read, 1024 - read);
        }
        bytes.write(buffer);
        return read;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_files);

        initObjects();
        prepareObjects();
    }

    /**
     * initialize the objects
     */
    private void initObjects() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.shared_files_list);

        databaseHandler = new DatabaseHandler(this);
        fileItemList = new ArrayList<>();
        deviceItemList = new ArrayList<>();
        adapter = new FileAdapter(this, fileItemList);
        mProgressDialog = new ProgressDialog(this);
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

        mProgressDialog.setMessage("Downloading file..");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        fileItemList.addAll(databaseHandler.getSharedFileList());
        adapter.notifyDataSetChanged();

        if (fileItemList.isEmpty())
            Toast.makeText(SharedFilesActivity.this,
                    "No file has been shared with you", Toast.LENGTH_SHORT).show();

        scan();
    }

    /**
     * Scan for the client devices connected in the network
     */
    private void scan() {
        isLoaded = false;
        int ipAddress = info.getIpAddress();
        fileItemList.clear();
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
     * Display progress bar if it is not being shown.
     */
    private void showProgressDialog() {
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    /**
     * Hide progress bar if it is being displayed.
     */
    private void hideProgressDialog() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Hide progress dialog, display success toast and then start main activity
     */
    private void downloadSuccess(File file) {
        hideProgressDialog();
        Toast.makeText(this, "File download success", Toast.LENGTH_SHORT).show();
        intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(file.getPath());
        Uri fileUri = Uri.fromFile(file);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        intent.setDataAndType(uri, mimeType);
        startActivity(intent);
    }

    /**
     * Hide progress dialog and then display failure toast
     */
    private void downloadFailed() {
        hideProgressDialog();
        Toast.makeText(this, "File download failed", Toast.LENGTH_SHORT).show();
    }

    /**
     * Download the selected file.
     *
     * @param file     the file to be downloaded
     * @param fileItem an object which contains the file details
     */

    private void downloadFile(final File file, final FileItem fileItem) {
        ip = null;
        String fileMac = fileItem.getMacId();
        for (int i = 0; i < deviceItemList.size(); i++) {
            String mac = deviceItemList.get(i).getMac();
            if (mac.equals(fileMac)) {
                ip = deviceItemList.get(i).getIp();
                break;
            }
        }
        if (ip != null)
            new Thread() {
                @Override
                public void run() {
                    try {
                        Socket socket = new Socket(ip, 8082);
                        DataOutputStream dos = new DataOutputStream(socket
                                .getOutputStream());
                        String path = fileItem.getFile();
                        dos.writeUTF(info.getMacAddress());
                        dos.writeUTF(path);
                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        int error = dis.readInt();
                        if (error == 0) {
                            int size = dis.readInt();
                            final File directory = new File(Environment.getExternalStorageDirectory()
                                    + File.separator + "SHAREipo" + File.separator);
                            final File fileData;
                            if (directory.exists() || directory.mkdir())
                                fileData = new File(directory, file.getName());
                            else fileData = new File(Environment.getExternalStorageDirectory()
                                    + File.separator, file.getName());
                            FileOutputStream fileOutputStream =
                                    new FileOutputStream(fileData);
                            InputStream inputStream = socket.getInputStream();
                            byte[] buffer = new byte[1024];
                            ByteArrayOutputStream byteArrayOutputStream = new
                                    ByteArrayOutputStream();
                            while (size >= 1024) {
                                size -= getChunk(inputStream, byteArrayOutputStream, buffer);
                                fileOutputStream.write(byteArrayOutputStream.toByteArray());
                                byteArrayOutputStream.reset();
                            }

                            int data;
                            while ((data = inputStream.read()) != -1)
                                byteArrayOutputStream.write(data);
                            fileOutputStream.write(byteArrayOutputStream.toByteArray());
                            dos.flush();
                            dos.close();
                            dis.close();
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            inputStream.close();
                            byteArrayOutputStream.flush();
                            byteArrayOutputStream.close();
                            socket.close();
                            SharedFilesActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    downloadSuccess(fileData);
                                }
                            });
                        } else if (error == 1) {
                            SharedFilesActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SharedFilesActivity.this,
                                            file.getName() + " is not shared with you anymore",
                                            Toast.LENGTH_SHORT).show();
                                    databaseHandler.removeSharedFile(fileItem.getId());
                                    fileItemList.clear();
                                    fileItemList.addAll(databaseHandler.getSharedFileList());
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    } catch (Exception e) {
                        SharedFilesActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                downloadFailed();
                            }
                        });
                        e.printStackTrace();
                    }
                }
            }.start();
        else Toast.makeText(SharedFilesActivity.this,
                fileItem.getUser() + " is not available", Toast.LENGTH_SHORT).show();
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

        Context context;
        List<FileItem> fileItemList;

        public FileAdapter(Context context, List<FileItem> fileItemList) {
            this.context = context;
            this.fileItemList = fileItemList;
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_shared_files, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            final FileItem fileItem = fileItemList.get(position);
            holder.name.setText(fileItem.getUser());
            final File file = new File(fileItem.getFile());
            holder.file.setText(file.getName());
            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    String fileMac = fileItem.getMacId();
                    for (int i = 0; i < deviceItemList.size(); i++) {
                        String mac = deviceItemList.get(i).getMac();
                        if (mac.equals(fileMac)) {
                            isAvailable = true;
                            break;
                        }
                    }
                    if (isLoaded) {
                        showProgressDialog();
                        downloadFile(file, fileItem);
                    } else if (isAvailable) {
                        isAvailable = false;
                        showProgressDialog();
                        downloadFile(file, fileItem);
                    } else Toast.makeText(SharedFilesActivity.this,
                            "Fetching details.. Please wait..", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return fileItemList.size();
        }

        protected class FileViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView name, file;
            private ItemClickListener itemClickListener;

            public FileViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(R.id.name);
                file = (TextView) view.findViewById(R.id.file);
                view.setOnClickListener(this);
            }

            public void setClickListener(ItemClickListener itemClickListener) {
                this.itemClickListener = itemClickListener;
            }

            @Override
            public void onClick(View view) {
                itemClickListener.onClick(view, getAdapterPosition(), false);
            }
        }
    }
}
