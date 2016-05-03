package in.codehex.shareipo;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import in.codehex.shareipo.app.Config;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    Button btnShare, btnSharedFiles, btnUnShare;
    Intent intent;
    SharedPreferences userPreferences;

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
                        Toast.makeText(MainActivity.this,
                                path, Toast.LENGTH_SHORT).show();
                    }
                    intent = new Intent(MainActivity.this, ShareActivity.class);
                    startActivity(intent);
                }
            } else {
                String path = data.getData().getPath();
                Toast.makeText(MainActivity.this,
                        path, Toast.LENGTH_SHORT).show();
                intent = new Intent(MainActivity.this, ShareActivity.class);
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
        Thread thread = new Thread(new Profile());
        thread.start();
    }

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
                        String profile = userPreferences.getString("name", null) + "," +
                                userPreferences.getInt("img_id", 0);
                        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                        dos.writeUTF(profile);
                    } else {
                        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                        dos.writeUTF("testing");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
