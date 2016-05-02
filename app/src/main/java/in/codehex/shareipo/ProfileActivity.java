package in.codehex.shareipo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import in.codehex.shareipo.app.Config;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {

    static Integer imgId = 0;
    Toolbar toolbar;
    EditText editName;
    ImageButton btnMale1, btnMale2, btnMale3, btnFemale1, btnFemale2, btnFemale3;
    Button btnSave;
    Intent intent;
    SharedPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initObjects();
        prepareObjects();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.save) {
            String name = editName.getText().toString().trim();

            if (name.isEmpty())
                editName.setError("Name cannot be empty");

            if (imgId == 0)
                Toast.makeText(ProfileActivity.this,
                        "Please select your profile picture", Toast.LENGTH_SHORT).show();

            if (!name.isEmpty() && imgId != 0) {
                SharedPreferences.Editor editor = userPreferences.edit();
                editor.putInt("img_id", imgId);
                editor.putString("name", name);
                editor.apply();

                Toast.makeText(ProfileActivity.this,
                        "Profile updated", Toast.LENGTH_SHORT).show();

                intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else {
            resetBackgroundColor();
            setBackgroundColor(id);
        }
    }

    /**
     * initialize the objects
     */
    private void initObjects() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        editName = (EditText) findViewById(R.id.name);
        btnMale1 = (ImageButton) findViewById(R.id.male1);
        btnMale2 = (ImageButton) findViewById(R.id.male2);
        btnMale3 = (ImageButton) findViewById(R.id.male3);
        btnFemale1 = (ImageButton) findViewById(R.id.female1);
        btnFemale2 = (ImageButton) findViewById(R.id.female2);
        btnFemale3 = (ImageButton) findViewById(R.id.female3);
        btnSave = (Button) findViewById(R.id.save);

        userPreferences = getSharedPreferences(Config.PREF_USER, MODE_PRIVATE);
    }

    /**
     * implement and manipulate the objects
     */
    private void prepareObjects() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (userPreferences.contains("name")) {
            imgId = userPreferences.getInt("img_id", 0);
            setBackgroundColor(imgId);

            String name = userPreferences.getString("name", "bobby");
            editName.setText(name);
        }

        btnMale1.setOnClickListener(this);
        btnMale2.setOnClickListener(this);
        btnMale3.setOnClickListener(this);
        btnFemale1.setOnClickListener(this);
        btnFemale2.setOnClickListener(this);
        btnFemale3.setOnClickListener(this);
        btnSave.setOnClickListener(this);
    }

    /**
     * resets the background color of all the image buttons
     */
    private void resetBackgroundColor() {
        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray typedArray = obtainStyledAttributes(attrs);
        int backgroundResource = typedArray.getResourceId(0, 0);

        btnMale1.setBackgroundResource(backgroundResource);
        btnMale2.setBackgroundResource(backgroundResource);
        btnMale3.setBackgroundResource(backgroundResource);
        btnFemale1.setBackgroundResource(backgroundResource);
        btnFemale2.setBackgroundResource(backgroundResource);
        btnFemale3.setBackgroundResource(backgroundResource);

        typedArray.recycle();
    }

    /**
     * set the background color for the selected image button
     *
     * @param id the id of the image button
     */
    private void setBackgroundColor(int id) {
        switch (id) {
            case R.id.male1:
                imgId = id;
                btnMale1.setBackgroundColor(ContextCompat.getColor(this, R.color.accent));
                break;
            case R.id.male2:
                imgId = id;
                btnMale2.setBackgroundColor(ContextCompat.getColor(this, R.color.accent));
                break;
            case R.id.male3:
                imgId = id;
                btnMale3.setBackgroundColor(ContextCompat.getColor(this, R.color.accent));
                break;
            case R.id.female1:
                imgId = id;
                btnFemale1.setBackgroundColor(ContextCompat.getColor(this, R.color.accent));
                break;
            case R.id.female2:
                imgId = id;
                btnFemale2.setBackgroundColor(ContextCompat.getColor(this, R.color.accent));
                break;
            case R.id.female3:
                imgId = id;
                btnFemale3.setBackgroundColor(ContextCompat.getColor(this, R.color.accent));
                break;
        }
    }
}
