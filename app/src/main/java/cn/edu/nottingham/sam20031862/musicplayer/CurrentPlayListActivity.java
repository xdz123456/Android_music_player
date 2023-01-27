package cn.edu.nottingham.sam20031862.musicplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class CurrentPlayListActivity extends AppCompatActivity {

    // Init the global val
    DBHelper mDbHelper;
    SQLiteDatabase mDb;
    ListView mCurrentPlayListListView;
    SimpleCursorAdapter mDataAdapter;
    String tableName;

    // Override onCreate
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_playlist);
        // Get the current PlayList from intent
        tableName = getIntent().getStringExtra("table") + "_table";
        TextView ListName = findViewById(R.id.CurrentPlayListText);
        // Set the List View of Music List
        ListName.setText(getIntent().getStringExtra("table") + ": ");
        mDbHelper = new DBHelper(this);
        // Build the Adapter
        mDb = mDbHelper.getWritableDatabase();
        mDataAdapter = buildAdapter();
        mCurrentPlayListListView = findViewById(R.id.CurrentPlayListList);
        mCurrentPlayListListView.setAdapter(mDataAdapter);

        mCurrentPlayListListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(CurrentPlayListActivity.this, MainActivity.class);
            Bundle bundle = new Bundle();
            Music selectedMusic = getMusic(position + 1, tableName);
            bundle.putSerializable("currentMusic", selectedMusic);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        });
    }

    public Music getMusic(int position, String tableName){
        @SuppressLint("Recycle") Cursor cursor = mDb.query(tableName, new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);

        Music selectedMusic = new Music();
        // Set up the ids in database as a list
        int [] ids = new int [cursor.getCount()];
        int temp_pos = 0;
        while (cursor.moveToNext()) {
            ids[temp_pos] = cursor.getInt(cursor.getColumnIndex("_id"));
            temp_pos++;
        }

        // Query all the musics in the current playlist
        @SuppressLint("Recycle") Cursor myCursor =  mDb.rawQuery("SELECT * FROM " + tableName +" WHERE _id = "  + ids[position-1],null, null);

        // Save the music information to the selected music
        if (myCursor.moveToFirst()) {
            do {
                selectedMusic.setName(myCursor.getString(myCursor.getColumnIndex("name")));
                selectedMusic.setSinger(myCursor.getString(myCursor.getColumnIndex("singer")));
                selectedMusic.setPath(myCursor.getString(myCursor.getColumnIndex("path")));
                selectedMusic.setDuration(myCursor.getInt(myCursor.getColumnIndex("duration")));
            } while (myCursor.moveToNext());
        }
        return selectedMusic;
    }
    // Return to the Main Activity
    public void returnToMainActivity(View view){
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    // Delete the Music in the PLayList
    public void deleteCurrentPlayList(View view){
        // Build The DiaLog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Delete the music(s) in Playlist");
        // Get All the music in the current Playlist
        @SuppressLint("Recycle") Cursor cursor = mDb.query(tableName, new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);
        // If it is not empty Build the dialog
        int count = cursor.getCount();
        if (count > 0){
            // Build the String of items list
            String[] items = new String[cursor.getCount()];
            int[] ids = new int[cursor.getCount()];
            boolean[] selected = new boolean[cursor.getCount()];
            int pos = 0;
            while (cursor.moveToNext()) {
                items[pos] = cursor.getString(cursor.getColumnIndex("name"));
                ids[pos] = cursor.getInt(cursor.getColumnIndex("_id"));
                selected[pos] = false;
                pos++;
            }
            // Set Multi Choice
            builder.setMultiChoiceItems(items,selected, (dialog, which, isChecked) -> {
            });
            // Set up the cancel Button
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            // Set up the Confirm Button
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                dialog.dismiss();
                deleteInCurrentDB(ids, selected);
            });
            // Show the Dialog
            builder.create().show();
        }
        else {
            Toast.makeText(CurrentPlayListActivity.this, "There is no playList needed to delete", Toast.LENGTH_SHORT).show();
        }
    }

    // Delete the Music in DataBase
    private void deleteInCurrentDB(int[] finalIDs, boolean[] finalSelected){
        // Delete the Music in the current playList
        int length = finalSelected.length;
        for (int i = 0; i < length; i++){
            if (finalSelected[i]){
                int mID = finalIDs[i];
                mDb.execSQL("delete from " + tableName + " where _id = " + mID);
            }
        }
        // Set up the Adapter of the PlayList
        mDataAdapter = buildAdapter();
        mCurrentPlayListListView.setAdapter(mDataAdapter);
    }

    // Insert the Music to the current Music List
    public void insertCurrentPlayList(View view){
        // Build the builder of dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Insert music to the current Playlist");
        // Get All the music in the current Playlist
        @SuppressLint("Recycle") Cursor cursor = mDb.query("MusicList",  new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);
        // Build the String of items list
        int count = cursor.getCount();
        if (count > 0){
            String[] items = new String[cursor.getCount()];
            int [] ids = new int [cursor.getCount()];
            boolean[] selected = new boolean[cursor.getCount()];
            int pos = 0;
            while (cursor.moveToNext()) {
                items[pos] = cursor.getString(cursor.getColumnIndex("name"));
                ids[pos] = cursor.getInt(cursor.getColumnIndex("_id"));
                selected[pos] = false;
                pos++;
            }
            // Set Multi Choice
            builder.setMultiChoiceItems(items,selected, (dialog, which, isChecked) -> {
            });
            // Set up the cancel Button
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            // Set up the Confirm Button
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                dialog.dismiss();
                insertInCurrentDB(ids, selected);
            });
            builder.create().show();
        }
        else {
            Toast.makeText(CurrentPlayListActivity.this, "Insert Successfully", Toast.LENGTH_SHORT).show();
        }
    }

    // Insert the music in the database
    private void insertInCurrentDB(int[] finalIDs, boolean[] finalSelected){
        // Init the String
        int length = finalSelected.length;
        String name = "";
        String singer = "";
        String path = "";
        int duration = 0;

        // Get the value of the music
        for (int i = 0; i < length; i++){
            if (finalSelected[i]){
                int mID = finalIDs[i];
                @SuppressLint("Recycle") Cursor myCursor =  mDb.rawQuery("SELECT * FROM MusicList WHERE _id = " + mID,null, null);
                if (myCursor.moveToFirst()) {
                    do {
                        name = myCursor.getString(myCursor.getColumnIndex("name"));
                        singer = myCursor.getString(myCursor.getColumnIndex("singer"));
                        path = myCursor.getString(myCursor.getColumnIndex("path"));
                        duration = myCursor.getInt(myCursor.getColumnIndex("duration"));
                    } while (myCursor.moveToNext());
                }
                // Insert the value to the DataBase
                mDb.execSQL("INSERT INTO "+ tableName +" (name, singer, path, duration) " + "VALUES " +"('" + name
                        + "','" + singer + "','" + path + "','" + duration +
                        "');");
            }
        }
        // Build the Adapter
        mDataAdapter = buildAdapter();
        mCurrentPlayListListView.setAdapter(mDataAdapter);
    }

    // Build the Adapter
    private SimpleCursorAdapter buildAdapter(){
        SimpleCursorAdapter mDataAdapter;
        // Search the all the PlayList
        Cursor cursor = mDb.query(tableName, new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);
        // Get a Name List
        String[] columns = new String[]{
                "name",
                "singer",
        };
        // Map the Playlist
        int[] uiMapping = new int[]{
                R.id.currentNameView,
                R.id.currentSingerView,
        };
        // Build the DataAdapter
        mDataAdapter = new SimpleCursorAdapter(
                this, R.layout.current_item_view,
                cursor,
                columns,
                uiMapping,
                0);
        return mDataAdapter;
    }

}
