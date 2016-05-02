package in.codehex.shareipo;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
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

import com.nononsenseapps.filepicker.FilePickerActivity;

import in.codehex.shareipo.app.Config;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    Button btnShare, btnSharedFiles, btnUnShare;
    Intent intent;

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
    }

    /**
     * implement and manipulate the objects
     */
    private void prepareObjects() {
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
    }
}
