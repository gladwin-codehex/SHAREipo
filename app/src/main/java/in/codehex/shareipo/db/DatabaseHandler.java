package in.codehex.shareipo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import in.codehex.shareipo.app.Config;
import in.codehex.shareipo.model.FileItem;

/**
 * Created by Bobby on 28-04-2016
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    public DatabaseHandler(Context context) {
        super(context, Config.DB_NAME, null, Config.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createShareDB(db);
        createSharedDB(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String dropTable = "DROP TABLE IF EXISTS ";
        db.execSQL(dropTable + Config.TABLE_SHARE);
        db.execSQL(dropTable + Config.TABLE_SHARED);
    }

    private void createShareDB(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + Config.TABLE_SHARE + "("
                + Config.KEY_ID + " INTEGER PRIMARY KEY, " + Config.KEY_USER + " TEXT, "
                + Config.KEY_MAC_ID + " TEXT, " + Config.KEY_FILE + " TEXT" + ")";
        db.execSQL(createTable);
    }

    private void createSharedDB(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + Config.TABLE_SHARED + "("
                + Config.KEY_ID + " INTEGER PRIMARY KEY, " + Config.KEY_USER + " TEXT, "
                + Config.KEY_MAC_ID + " TEXT, " + Config.KEY_FILE + " TEXT" + ")";
        db.execSQL(createTable);
    }

    public void addShareFiles(List<FileItem> fileItemList) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (int i = 0; i < fileItemList.size(); i++) {
            ContentValues values = new ContentValues();
            values.put(Config.KEY_USER, fileItemList.get(i).getUser());
            values.put(Config.KEY_MAC_ID, fileItemList.get(i).getMacId());
            values.put(Config.KEY_FILE, fileItemList.get(i).getFile());
            db.insert(Config.TABLE_SHARE, null, values);
            db.close();
        }
    }

    public void addSharedFiles(List<FileItem> fileItemList) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (int i = 0; i < fileItemList.size(); i++) {
            ContentValues values = new ContentValues();
            values.put(Config.KEY_USER, fileItemList.get(i).getUser());
            values.put(Config.KEY_MAC_ID, fileItemList.get(i).getMacId());
            values.put(Config.KEY_FILE, fileItemList.get(i).getFile());
            db.insert(Config.TABLE_SHARED, null, values);
            db.close();
        }
    }

    public List<FileItem> getShareFileList() {
        List<FileItem> fileItemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Config.TABLE_SHARE;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                fileItemList.add(new FileItem(cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), false));
            } while (cursor.moveToNext());
            cursor.close();
            db.close();
        }
        return fileItemList;
    }

    public List<FileItem> getSharedFileList() {
        List<FileItem> fileItemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Config.TABLE_SHARED;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                fileItemList.add(new FileItem(cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), false));
            } while (cursor.moveToNext());
            cursor.close();
            db.close();
        }
        return fileItemList;
    }

    public List<FileItem> getShareUserList() {
        List<FileItem> fileItemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Config.TABLE_SHARED
                + " GROUP BY " + Config.KEY_USER;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                fileItemList.add(new FileItem(cursor.getString(1), cursor.getString(2),
                        cursor.getString(3)));
            } while (cursor.moveToNext());
            cursor.close();
            db.close();
        }
        return fileItemList;
    }

    public List<FileItem> getShareUserFileList(String user) {
        List<FileItem> fileItemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Config.TABLE_SHARED
                + " WHERE " + Config.KEY_MAC_ID + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{user});
        if (cursor.moveToFirst()) {
            do {
                fileItemList.add(new FileItem(cursor.getInt(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), false));
            } while (cursor.moveToNext());
            cursor.close();
            db.close();
        }
        return fileItemList;
    }

    public void removeShareFiles(List<Integer> integerList) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (int i = 0; i < integerList.size(); i++)
            db.delete(Config.TABLE_SHARE, Config.KEY_ID + " = ?", new String[]{String.valueOf(i)});
        db.close();
    }

    public void removeSharedFile(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(Config.TABLE_SHARED, Config.KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
