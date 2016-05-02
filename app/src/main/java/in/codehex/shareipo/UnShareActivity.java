package in.codehex.shareipo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class UnShareActivity extends AppCompatActivity {

    Toolbar toolbar;
    Button btnUserBased, btnFileBased;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_un_share);

        initObjects();
        prepareObjects();
    }

    /**
     * initialize the objects
     */
    private void initObjects() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        btnFileBased = (Button) findViewById(R.id.file);
        btnUserBased = (Button) findViewById(R.id.user);
    }

    /**
     * implement and manipulate the objects
     */
    private void prepareObjects() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnUserBased.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(UnShareActivity.this, UnShareUserActivity.class);
                startActivity(intent);
            }
        });

        btnFileBased.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(UnShareActivity.this, UnShareFileActivity.class);
                startActivity(intent);
            }
        });
    }
}
