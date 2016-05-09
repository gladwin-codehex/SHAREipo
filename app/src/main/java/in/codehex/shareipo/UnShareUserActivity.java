package in.codehex.shareipo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import in.codehex.shareipo.app.ItemClickListener;
import in.codehex.shareipo.db.DatabaseHandler;
import in.codehex.shareipo.model.FileItem;

public class UnShareUserActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView recyclerView;
    DatabaseHandler databaseHandler;
    List<FileItem> fileItemList;
    UserAdapter adapter;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_un_share_user);

        initObjects();
        prepareObjects();
    }

    /**
     * initialize the objects
     */
    private void initObjects() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.un_share_user_list);

        databaseHandler = new DatabaseHandler(this);
        fileItemList = new ArrayList<>();
        adapter = new UserAdapter(this, fileItemList);
    }

    /**
     * implement and manipulate the objects
     */
    private void prepareObjects() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        fileItemList.addAll(databaseHandler.getShareUserList());
        adapter.notifyDataSetChanged();

        if (fileItemList.isEmpty())
            Toast.makeText(UnShareUserActivity.this,
                    "You haven't shared any file", Toast.LENGTH_SHORT).show();
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        Context context;
        List<FileItem> fileItemList;

        public UserAdapter(Context context, List<FileItem> fileItemList) {
            this.context = context;
            this.fileItemList = fileItemList;
        }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_un_share_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            final FileItem fileItem = fileItemList.get(position);

            holder.name.setText(fileItem.getUser());
            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    intent = new Intent(UnShareUserActivity.this, UnShareFileActivity.class);
                    intent.putExtra("user", fileItem.getMacId());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return fileItemList.size();
        }

        protected class UserViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView name;
            private ItemClickListener itemClickListener;

            public UserViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(R.id.name);
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
