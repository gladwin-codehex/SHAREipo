package in.codehex.shareipo;

import org.apache.commons.io.output.ByteArrayOutputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
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
    ProgressDialog mProgressDialog;
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
        adapter = new FileAdapter(this, fileItemList);
        mProgressDialog = new ProgressDialog(this);
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
     * Hide progress bar if it being displayed.
     */
    private void hideProgressDialog() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Hide progress dialog and then display toast
     */
    private void downloadSuccess() {
        hideProgressDialog();
        Toast.makeText(this, "File download success!", Toast.LENGTH_SHORT).show();
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
                    showProgressDialog();
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Socket socket = new Socket("192.168.43.1", 8082);
                                DataOutputStream dos = new DataOutputStream(socket
                                        .getOutputStream());
                                String path = fileItem.getFile();
                                dos.writeUTF(path);
                                DataInputStream dis = new DataInputStream(socket.getInputStream());
                                int size = dis.readInt();
                                File directory = new File(Environment.getExternalStorageDirectory()
                                        + File.separator + "SHAREipo" + File.separator);
                                File fileData;
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
                                downloadSuccess();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
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
