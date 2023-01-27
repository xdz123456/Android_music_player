package cn.edu.nottingham.sam20031862.musicplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// A Helper for building the MusicList and PlayList database
public class DBHelper extends SQLiteOpenHelper {
    // Init the String for the DataBase Init
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SINGER = "singer";
    private static final String KEY_PATH = "path";
    private static final String KEY_DURATION = "duration";
    private static final String TAG = DBHelper.class.getSimpleName();
    private static final String SQLITE_TABLE_MUSIC = "MusicList";
    private static final String SQLITE_TABLE_PLAYLIST = "PlayList";
    private static final String SQLITE_CREATE_MUSIC =
            "CREATE TABLE if not exists " + SQLITE_TABLE_MUSIC
                    + " ("
                    + KEY_ID + " integer PRIMARY KEY autoincrement,"
                    + KEY_NAME + ","
                    + KEY_SINGER + ","
                    + KEY_PATH + ","
                    + KEY_DURATION
                    +  ");";
    private static final String SQLITE_CREATE_PLAYLIST =
            "CREATE TABLE if not exists " + SQLITE_TABLE_PLAYLIST
                    + " ("
                    + KEY_ID + " integer PRIMARY KEY autoincrement,"
                    + KEY_NAME
                    +  ");";


    public DBHelper(Context context) {
        super(context, context.getString(R.string.app_db_name), null, 1);
    }

    // Build the MusicList and PlayList table
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQLITE_CREATE_PLAYLIST);
        db.execSQL(SQLITE_CREATE_MUSIC);
    }

    // On Upgrade
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.d(TAG, String.format("onUpgrade: Database Version updated (%d -> %d)", oldVersion,
                newVersion));
        db.execSQL("DROP TABLE IF EXISTS MusicList;");
        db.execSQL("DROP TABLE IF EXISTS PlayList;");
        onCreate(db);
    }
}
