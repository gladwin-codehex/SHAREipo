package in.codehex.shareipo;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import in.codehex.shareipo.app.Config;
import in.codehex.shareipo.db.DatabaseHandler;
import in.codehex.shareipo.model.FileItem;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    Button btnShare, btnSharedFiles, btnUnShare;
    Intent intent;
    SharedPreferences userPreferences;
    ArrayList<String> fileList;
    List<FileItem> sharedItemList, mFileItemList;
    DatabaseHandler databaseHandler;
    WifiManager wifiManager;
    WifiInfo info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initObjects();
        prepareObjects();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_profile:
                intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Config.REQUEST_FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        String path = uri.getPath();
                        fileList.add(path);
                    }
                    intent = new Intent(MainActivity.this, ShareActivity.class);
                    intent.putExtra("file", fileList);
                    startActivity(intent);
                }
            } else {
                String path = data.getData().getPath();
                fileList.add(path);
                intent = new Intent(MainActivity.this, ShareActivity.class);
                intent.putExtra("file", fileList);
                startActivity(intent);
            }
        }
    }

    /**
     * initialize the objects
     */
    private void initObjects() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        btnShare = (Button) findViewById(R.id.share);
        btnSharedFiles = (Button) findViewById(R.id.shared_files);
        btnUnShare = (Button) findViewById(R.id.un_share);

        userPreferences = getSharedPreferences(Config.PREF_USER, MODE_PRIVATE);
        databaseHandler = new DatabaseHandler(this);
        fileList = new ArrayList<>();
        mFileItemList = new ArrayList<>();
        sharedItemList = new ArrayList<>();
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        info = wifiManager.getConnectionInfo();
    }

    /**
     * implement and manipulate the objects
     */
    private void prepareObjects() {
        if (userPreferences.contains("name")) {
            setSupportActionBar(toolbar);

            btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intent = new Intent(MainActivity.this, FilePickerActivity.class);
                    intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                    intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                    intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                    intent.putExtra(FilePickerActivity.EXTRA_START_PATH,
                            Environment.getExternalStorageDirectory().getPath());
                    startActivityForResult(intent, Config.REQUEST_FILE_CODE);
                }
            });

            btnSharedFiles.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intent = new Intent(MainActivity.this, SharedFilesActivity.class);
                    startActivity(intent);
                }
            });

            btnUnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intent = new Intent(MainActivity.this, UnShareActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        startServer();
    }

    /**
     * Start all the background server sockets.
     */
    private void startServer() {
        new Thread(new Profile()).start();
        new Thread(new SharedFiles()).start();
        new Thread(new TransferFiles()).start();
    }

    /**
     * A background thread used to communicate with server to send profile details.
     */
    private class Profile extends Thread {

        @Override
        public void run() {
            try {
                ServerSocket socket = new ServerSocket(8080);
                while (true) {
                    Socket clientSocket = socket.accept();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Profile info has been shared", Toast.LENGTH_SHORT).show();
                        }
                    });
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    dos.writeUTF(userPreferences.getString("name", null));
                    dos.writeUTF(String.valueOf(userPreferences.getInt("img_id", 0)));
                    dos.writeUTF(info.getMacAddress());
                    dos.flush();
                    dos.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A background thread to accept shared files from the server.
     */
    private class SharedFiles extends Thread {

        @Override
        public void run() {
            try {
                ServerSocket socket = new ServerSocket(8081);
                while (true) {
                    Socket clientSocket = socket.accept();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "A file has been shared with you", Toast.LENGTH_SHORT).show();
                        }
                    });
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    String name = dis.readUTF();
                    String mac = dis.readUTF();
                    String files = dis.readUTF();
                    List<String> items = Arrays.asList(files.split("\\s*,\\s*"));
                    for (int i = 0; i < items.size(); i++)
                        sharedItemList.add(new FileItem(name, mac, items.get(i)));
                    databaseHandler.addSharedFiles(sharedItemList);
                    dis.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A background thread to transfer files from server to client.
     */
    private class TransferFiles extends Thread {

        @Override
        public void run() {
            try {
                ServerSocket socket = new ServerSocket(8082);
                while (true) {
                    Socket clientSocket = socket.accept();
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    String mac = dis.readUTF();
                    String path = dis.readUTF();
                    mFileItemList.clear();
                    mFileItemList.addAll(databaseHandler.getShareUserFileList(mac));
                    boolean isAvailable = false;
                    for (int i = 0; i < mFileItemList.size(); i++) {
                        if (mFileItemList.get(i).getFile().equals(path)) {
                            isAvailable = true;
                            break;
                        }
                    }
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    if (isAvailable) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        "File is being transferred..", Toast.LENGTH_SHORT).show();
                            }
                        });
                        File file = new File(path);
                        int size = (int) (file.length());
                        dos.writeInt(0);
                        dos.writeInt(size);
                        FileInputStream fileInputStream = new FileInputStream(file);
                        OutputStream outputStream = clientSocket.getOutputStream();
                        int read;
                        byte[] buffer = new byte[1024];
                        while ((read = fileInputStream.read(buffer, 0, 1024)) > 0) {
                            outputStream.write(buffer, 0, read);
                        }
                        fileInputStream.close();
                        outputStream.flush();
                        outputStream.close();
                    } else {
                        dos.writeInt(1);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        "Someone is trying to access unshared file",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    dis.close();
                    dos.flush();
                    dos.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
