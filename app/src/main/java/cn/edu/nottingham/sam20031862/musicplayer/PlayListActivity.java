package cn.edu.nottingham.sam20031862.musicplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PlayListActivity extends AppCompatActivity{
    // Init the global val
    DBHelper mDbHelper;
    SQLiteDatabase mDb;
    ListView mPlayListListView;
    SimpleCursorAdapter mDataAdapter;
    String[] itemInList;

    // Init the final String for the Database Build
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SINGER = "singer";
    private static final String KEY_PATH = "path";
    private static final String KEY_DURATION = "duration";

    // OnCreate Method for PlayList Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        // Init the Variable
        mDbHelper = new DBHelper(this);
        mDb = mDbHelper.getWritableDatabase();

        String tableName = getIntent().getStringExtra("whatPlaying");
        if (tableName.equals("MusicList")){
            tableName = "Default List";
        }
        TextView ListName = findViewById(R.id.PlayListText);
        ListName.setText(tableName);
        // Set up the ListView
        mDataAdapter = buildAdapter();
        mPlayListListView = findViewById(R.id.PlayListList);
        mPlayListListView.setAdapter(mDataAdapter);
        // Set the ListView Listener
        // Press to goto the current playlist in the Activity of CurrentPlayListActivity
        mPlayListListView.setOnItemClickListener((parent, view, position, id) -> {
            // Send the Table List to the CurrentPlayListActivity
            String title = itemInList[position];
            Intent intent = new Intent(PlayListActivity.this, CurrentPlayListActivity.class);
            intent.putExtra("table", title);
            // Start the Activity
            startActivity(intent);
            finish();
        });
    }

    // Set the button of the return the MainActivity
    public void returnToMainActivityFromP(View view){
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    // Set the button of the insert the Playlist
    public void inertPlayListButton(View view){
        // Build the Dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        // Set up the Dialog
        builder = new AlertDialog.Builder(this).setTitle("Please input the name of Playlist: ").setView(editText)
                .setPositiveButton("Confirm", (dialogInterface, i) -> {
                    String myPlayListName = editText.getText().toString();
                    // Set the String of the table name
                    String myPlayListTableName = myPlayListName + "_table";
                    // Check the input available or not
                    if (checkAvailable(myPlayListName)){
                        String CREATE_PlayList =
                                "CREATE TABLE if not exists " + myPlayListTableName
                                        + " ("
                                        + KEY_ID + " integer PRIMARY KEY autoincrement,"
                                        + KEY_NAME + ","
                                        + KEY_SINGER + ","
                                        + KEY_PATH + ","
                                        + KEY_DURATION
                                        +  ");";
                        // Try the DataBase execSql
                        try {
                            mDb.execSQL("INSERT INTO PlayList (name) " + "VALUES " +"('" + myPlayListName + "');");
                            mDb.execSQL(CREATE_PlayList);
                        } catch (SQLiteException e) {
                            Toast.makeText(PlayListActivity.this,"Invalid table name. Please try another name",Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Set up the Adapter
                        mDataAdapter = buildAdapter();
                        mPlayListListView = findViewById(R.id.PlayListList);
                        mPlayListListView.setAdapter(mDataAdapter);
                    }
                });
        builder.create().show();
    }

    // Check the input available or not
    private boolean checkAvailable(String input) {
        // Check the input is empty or not
        if(input.isEmpty()){
            Toast.makeText(PlayListActivity.this, "Please input the name of PlayList", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Check the input if contain the Illegal import
        boolean isWord = input.matches("[a-zA-Z0-9]+");
        char fir = input.charAt(0);
        if (isWord && (Character.isLowerCase(fir) || Character.isUpperCase(fir) )){
            // Check the input if is the same table name
            @SuppressLint("Recycle") Cursor cursor = mDb.query("PlayList", new String[]{"_id", "name"},
                    "name=?",  new  String[]{ input }, null, null, null);
            if (cursor.getCount() == 0){
                return true;
            }
            else {
                Toast.makeText(PlayListActivity.this, "The " + input + " PlayList has been created, please try other name.", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        // Check the first char is not the number
        else {
            Toast.makeText(PlayListActivity.this, "PlayList's name should not contain the symbol and the first character could not be the digit. ", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // Set the Button of deleting the PlayList
    public void deletePlayList(View view){
        // Build the Dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Delete the Playlist");
        @SuppressLint("Recycle") Cursor cursor = mDb.query("PlayList", new String[]{"_id", "name"},
                null, null, null, null, null);
        int count = cursor.getCount();
        // If there exist the table
        // Delete the table
        if (count > 0){
            // Get the items lists
            String[] items = new String[cursor.getCount()];
            boolean[] selected = new boolean[cursor.getCount()];
            int pos = 0;
            while (cursor.moveToNext()) {
                items[pos] = cursor.getString(cursor.getColumnIndex("name"));
                selected[pos] = false;
                pos++;
            }
            // Set up the Multi Choice Items
            builder.setMultiChoiceItems(items,selected, (dialog, which, isChecked) -> {

            });
            // Set up the cancel Button
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            // Set up the Confirm Button
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                dialog.dismiss();
                Toast.makeText(PlayListActivity.this, "Delete successfully", Toast.LENGTH_SHORT).show();
                deleteInDB(items, selected);
            });
            // Show the Dialog
            builder.create().show();
        }
        else {
            Toast.makeText(PlayListActivity.this, "There is no playList needed to delete", Toast.LENGTH_SHORT).show();
        }
    }

    // Delete the Table in DataBase
    private void deleteInDB(String[] finalItems, boolean[] finalSelected){
        // Drop the table and delete the name in the play List
        int length = finalSelected.length;
        for (int i = 0; i < length; i++){
            if (finalSelected[i]){
                String mName = finalItems[i];
                mDb.execSQL("drop table " + mName + "_table");
                mDb.delete("PlayList", "name=?",  new String[] {mName});
            }
        }

        // Set up the Adapter of the PlayList
        mDataAdapter = buildAdapter();
        mDataAdapter.notifyDataSetChanged();
        mPlayListListView = findViewById(R.id.PlayListList);
        mPlayListListView.setAdapter(mDataAdapter);

    }
    // Build the Adapter
    private SimpleCursorAdapter buildAdapter(){
        SimpleCursorAdapter mDataAdapter;
        // Search the all the PlayList
        Cursor cursor = mDb.query("PlayList", new String[]{"_id", "name"},
                null, null, null, null, null);
        String[] columns = new String[]{
                "name",
        };
        // Get a Name List
        if (cursor != null) {
            int length = cursor.getCount();
            itemInList = new String[length];
            int i = 0;
            while (cursor.moveToNext()) {
                itemInList[i] = cursor.getString(cursor.getColumnIndex("name"));
                i++;
            }
        }
        // Map the Playlist
        int[] uiMapping = new int[]{
                R.id.playlist_nameView,
        };
        // Build the DataAdapter
        mDataAdapter = new SimpleCursorAdapter(
                this, R.layout.playlist_item_view,
                cursor,
                columns,
                uiMapping,
                0);

        return mDataAdapter;
    }
}
