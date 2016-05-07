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
    List<FileItem> sharedItemList;
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
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    String msg = dis.readUTF();
                    if (msg.equals("profile")) {
                        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                        dos.writeUTF(userPreferences.getString("name", null));
                        dos.writeUTF(String.valueOf(userPreferences.getInt("img_id", 0)));
                    }
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
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    String name = dis.readUTF();
                    String mac = dis.readUTF();
                    String files = dis.readUTF();
                    List<String> items = Arrays.asList(files.split("\\s*,\\s*"));
                    for (int i = 0; i < items.size(); i++)
                        sharedItemList.add(new FileItem(name, mac, items.get(i)));
                    databaseHandler.addSharedFiles(sharedItemList);
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
                    String path = dis.readUTF();
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    File file = new File(path);
                    int size = (int) (file.length());
                    dos.writeInt(size);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    OutputStream outputStream = clientSocket.getOutputStream();
                    int read;
                    byte[] buffer = new byte[1024];
                    while ((read = fileInputStream.read(buffer, 0, 1024)) > 0) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                    outputStream.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
