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

import java.util.ArrayList;
import java.util.List;

import in.codehex.shareipo.app.ItemClickListener;
import in.codehex.shareipo.db.DatabaseHandler;
import in.codehex.shareipo.model.FileItem;

public class SharedFilesActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView recyclerView;
    DatabaseHandler databaseHandler;
    List<FileItem> fileItemList;
    FileAdapter adapter;
    Intent intent;

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
        adapter = new FileAdapter(this, fileItemList);
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

        fileItemList.addAll(databaseHandler.getSharedFileList());
        adapter.notifyDataSetChanged();
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
            holder.file.setText(fileItem.getFile());
            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    //TODO: connect to the device and download the file and then open the file using file manager intent
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
