package cn.edu.nottingham.sam20031862.musicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.Objects;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity
{
    // Init the global val
    DBHelper mDbHelper;
    SQLiteDatabase mDb;
    Music selectedMusic;
    ListView mMusicListView;
    SimpleCursorAdapter mDataAdapter;
    MusicPlayerHelper myMusicHelper;
    SeekBar mySeekBar;
    TextView maxDuration;
    TextView currentDuration;

    String nameToSend = "MusicList";;
    // Default PlayList
    String currentPlayList = "MusicList";
    // Default Position
    int currentPos = -1;
    // Default PlayList ID list
    int[] currentPlayListID;
    // PlayList_REQUEST
    public static final int PlayList_REQUEST = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Using the Dexter library to get the permission dynamically
        // Get Storage Permission
        // If the Permission is deny, quit the app
        // Else start the app
        // Reference: https://github.com/Karumi/Dexter
        Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    // If the permission accept
                    @Override public void onPermissionGranted(PermissionGrantedResponse response) { // Init the Database
                        initDB();
                        // Build the Adapter for the PlayList
                        buildAdapter(currentPlayList);
                        // Find the SeekBar View
                        mySeekBar = findViewById(R.id.seekBar);
                        // Forbidden move the seekBar
                        mySeekBar.setOnTouchListener((v, event) -> true);
                        // Find the Duration View
                        maxDuration = findViewById(R.id.maxDuration);
                        currentDuration = findViewById(R.id.currentDuration);
                        // Find the Music List View
                        mMusicListView = findViewById(R.id.musicList);

                        // Init the Music and MusicPlayerHelper
                        selectedMusic = new Music();
                        myMusicHelper = new MusicPlayerHelper();

                        Intent musicIntent = getIntent();
                        playMusicFromOther(musicIntent);

                        // Send the component to the Music Helper
                        myMusicHelper.initCom(mySeekBar,maxDuration,currentDuration);
                        // Set up the listener of ListView
                        mMusicListView.setOnItemClickListener((parent, view, position, id) -> selectMusic(position+1));}
                    // If the Permission is deny
                    @Override public void onPermissionDenied(PermissionDeniedResponse response) {System.exit(0);}
                    // Retry to get the permission
                    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {token.continuePermissionRequest();}
                }).check();
    }

    // Launch to the PlayList Activity
    public void launchPlayListActivity(View view) {
        // Create new intent
        Intent intent = new Intent(MainActivity.this, PlayListActivity.class);
        intent.putExtra("whatPlaying", nameToSend);
        // Start Activity for the result
        startActivityForResult(intent, PlayList_REQUEST);
    }


    @SuppressLint("SetTextI18n")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // If the current play List has been deleted
        // Switch to the Default List
        @SuppressLint("Recycle") Cursor cursor = mDb.query("PlayList", new String[]{"_id", "name"},
                "name = '" + nameToSend +"'", null, null, null, null);
        if (cursor.getCount() == 0){
            currentPlayList = "MusicList";
            TextView playListName = findViewById(R.id.PlayListName);
            playListName.setText("Default PlayList");
        }
        // Set the Current to the Init Pos
        currentPos = -1;
        buildAdapter(currentPlayList);


    }

    // Press the selectPlayList Button to select the current playlist
    @SuppressLint("SetTextI18n")
    public void selectPlayList(View view){
        // Set up the Dialog builder
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Select the Playlist");
        // Query all the PlayList name
        @SuppressLint("Recycle") Cursor cursor = mDb.query("PlayList", new String[]{"_id", "name"},
                null, null, null, null, null);
        // Storage all the name to the items list
        String[] items = new String[cursor.getCount() + 1];
        // Set the default Playlist
        items[0] = "Default PlayList";
        int pos = 1;
        while (cursor.moveToNext()) {
            items[pos] = cursor.getString(cursor.getColumnIndex("name"));
            pos++;
        }
        // Build the Dialog
        final int[] myChoice = {0};
        builder = new AlertDialog.Builder(this).setTitle("Select a playlist to play")
                .setSingleChoiceItems(items, 0, (dialogInterface, count) -> myChoice[0] = count).setPositiveButton("Confirm", (dialogInterface, count) -> {
                    TextView playListName = findViewById(R.id.PlayListName);
                    // If do not select the playlist select the default playlist
                    if (myChoice[0] == 0){
                        playListName.setText("Default PlayList");
                        currentPlayList = "MusicList";
                        nameToSend = "MusicList";
                    }
                    // else set up the current PlayList as the selected one
                    else {
                        playListName.setText(items[myChoice[0]]);
                        currentPlayList = items[myChoice[0]] + "_table";
                        nameToSend = items[myChoice[0]];
                    }
                    // Build the adapter
                    buildAdapter(currentPlayList);
                });
        // Show the Dialog
        builder.create().show();
        // Set up the current playlist pos to the first one
        currentPos = -1;
    }

    // Press the music in the list view to select and play the music
    public void selectMusic(int position){
        // Query the musics in the current playlist
        @SuppressLint("Recycle") Cursor cursor = mDb.query(currentPlayList, new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);

        // Set up the ids in database as a list
        int [] ids = new int [cursor.getCount()];
        int temp_pos = 0;
        while (cursor.moveToNext()) {
            ids[temp_pos] = cursor.getInt(cursor.getColumnIndex("_id"));
            temp_pos++;
        }

        // Query all the musics in the current playlist
        @SuppressLint("Recycle") Cursor myCursor =  mDb.rawQuery("SELECT * FROM " + currentPlayList +" WHERE _id = "  + ids[position-1],null, null);

        // Save the music information to the selected music
        if (myCursor.moveToFirst()) {
            do {
                selectedMusic.setName(myCursor.getString(myCursor.getColumnIndex("name")));
                selectedMusic.setSinger(myCursor.getString(myCursor.getColumnIndex("singer")));
                selectedMusic.setPath(myCursor.getString(myCursor.getColumnIndex("path")));
                selectedMusic.setDuration(myCursor.getInt(myCursor.getColumnIndex("duration")));
            } while (myCursor.moveToNext());
        }

        // Change the Current music textView
        TextView currentName = findViewById(R.id.Name);
        TextView currentSinger = findViewById(R.id.Singer);
        currentName.setText(selectedMusic.getName());
        currentSinger.setText(selectedMusic.getSinger());

        // Change the current Position and the ID list
        this.currentPos = position - 1;
        this.currentPlayListID = ids;

        // Build the Notification
        doNotification();
        // Call the Music Player Helper to switch and play the Music
        myMusicHelper.switchMusic(selectedMusic);
    }

    // Play the next Music
    public void next(View view){
        // If this is the first play in the PlayList
        if (currentPos == -1){
            @SuppressLint("Recycle") Cursor cursor = mDb.query(currentPlayList, new String[]{"_id", "name", "singer","path","duration"},
                    null, null, null, null, null);

            int [] ids = new int [cursor.getCount()];

            int temp_pos = 0;
            while (cursor.moveToNext()) {
                ids[temp_pos] = cursor.getInt(cursor.getColumnIndex("_id"));
                temp_pos++;
            }
            currentPlayListID = ids;
        }
        // Then Plus the Current Position
        currentPos += 1;
        // If The Position is oversize, Reset the position
        if (currentPos >= currentPlayListID.length){
            currentPos -= currentPlayListID.length;
        }
        @SuppressLint("Recycle") Cursor cursor = mDb.query(currentPlayList, new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);
        if(cursor.getCount() == 0){
            return;
        }
        // Select the Music according to the currentPlayListID List
        @SuppressLint("Recycle") Cursor myCursor =  mDb.rawQuery("SELECT * FROM " + currentPlayList +" WHERE _id = "  + currentPlayListID[currentPos],null, null);
        if (myCursor.moveToFirst()) {
            do {
                selectedMusic.setName(myCursor.getString(myCursor.getColumnIndex("name")));
                selectedMusic.setSinger(myCursor.getString(myCursor.getColumnIndex("singer")));
                selectedMusic.setPath(myCursor.getString(myCursor.getColumnIndex("path")));
                selectedMusic.setDuration(myCursor.getInt(myCursor.getColumnIndex("duration")));
            } while (myCursor.moveToNext());
        }
        if(selectedMusic.getPath() == null){
            Toast.makeText(this.getApplicationContext(),"Please Add the Music",Toast.LENGTH_LONG).show();
        }else {
            // Set up the TextView of Musics
            TextView currentName = findViewById(R.id.Name);
            TextView currentSinger = findViewById(R.id.Singer);
            currentName.setText(selectedMusic.getName());
            currentSinger.setText(selectedMusic.getSinger());
            doNotification();
            // Use the myMusicHelper to select the music and play the Music
            myMusicHelper.switchMusic(selectedMusic);
        }
    }

    // Play the next Music
    public void last(View view){
        // If this is the first play in the PlayList
        if (currentPos == -1){
            @SuppressLint("Recycle") Cursor cursor = mDb.query(currentPlayList, new String[]{"_id", "name", "singer","path","duration"},
                    null, null, null, null, null);

            int [] ids = new int [cursor.getCount()];

            int temp_pos = 0;
            while (cursor.moveToNext()) {
                ids[temp_pos] = cursor.getInt(cursor.getColumnIndex("_id"));
                temp_pos++;
            }
            currentPlayListID = ids;
            currentPos = 1;
        }
        // Then Minus the Current Position
        currentPos -= 1;
        // If The Position is oversize, Reset the position
        if (currentPos < 0){
            currentPos += currentPlayListID.length;
        }
        @SuppressLint("Recycle") Cursor cursor = mDb.query(currentPlayList, new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);
        if(cursor.getCount() == 0){
            return;
        }
        // Select the Music according to the currentPlayListID List
        @SuppressLint("Recycle") Cursor myCursor =  mDb.rawQuery("SELECT * FROM " + currentPlayList +" WHERE _id = "  + currentPlayListID[currentPos],null, null);
        if (myCursor.moveToFirst()) {
            do {
                selectedMusic.setName(myCursor.getString(myCursor.getColumnIndex("name")));
                selectedMusic.setSinger(myCursor.getString(myCursor.getColumnIndex("singer")));
                selectedMusic.setPath(myCursor.getString(myCursor.getColumnIndex("path")));
                selectedMusic.setDuration(myCursor.getInt(myCursor.getColumnIndex("duration")));
            } while (myCursor.moveToNext());
        }
        if(selectedMusic.getPath() == null){
            Toast.makeText(this.getApplicationContext(),"Please Add the Music",Toast.LENGTH_LONG).show();
        }else{
            // Set up the TextView of Musics
            TextView currentName = findViewById(R.id.Name);
            TextView currentSinger = findViewById(R.id.Singer);
            currentName.setText(selectedMusic.getName());
            currentSinger.setText(selectedMusic.getSinger());

            doNotification();
            // Use the myMusicHelper to select the music and play the Music
            myMusicHelper.switchMusic(selectedMusic);
        }

    }

    // Play the Music
    public void play(View view){
        // Should select the music
        if(selectedMusic.getPath() == null){
            Toast.makeText(this.getApplicationContext(),"Please select the music",Toast.LENGTH_LONG).show();
        }
        else{
            // Make the Notification
            doNotification();
            myMusicHelper.playMusic();
        }
    }

    // Pause the Music
    public void pause(View view){
        myMusicHelper.pauseMusic();
    }

    // Stop the Music
    @SuppressLint("SetTextI18n")
    public void stop(View view){
        // Use the Music Player Helper to stop the Music
        myMusicHelper.stopMusic();
        // Reset the TextView and all the
        TextView currentName = findViewById(R.id.Name);
        TextView currentSinger = findViewById(R.id.Singer);
        currentName.setText("");
        currentSinger.setText("");
        // Reset the assembly
        mySeekBar.setProgress(0);
        maxDuration.setText("00:00");
        currentDuration.setText("00:00");
        selectedMusic = new Music();
    }

    // Make the Notification
    // By pressing the Notification could return to the Main activity
    private void doNotification() {
        // Set up the Notification
        String id = "01";
        String name = "name";
        Notification notification;
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        // Using the PendingIntent to return the current activity
        Intent mIntent=new Intent(this, MainActivity.class);
        PendingIntent mPendingIntent=PendingIntent.getActivity(this, 0, mIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        // If the Build the Notification in different version of SDK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId(id)
                    .setContentTitle(selectedMusic.getName())
                    .setContentText(selectedMusic.getSinger())
                    .setContentIntent(mPendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher).build();
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle(selectedMusic.getName())
                    .setContentText(selectedMusic.getSinger())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(mPendingIntent)
                    .setOngoing(true)
                    .setChannelId(id);
            notification = notificationBuilder.build();
        }
        // Make the Notification
        notificationManager.notify(1, notification);
    }

    // Init the whole Database
    private void initDB(){
        // Init the variable
        String name;
        String singer;
        String path;
        int duration;

        // In each opening, Rebuild the whole MusicList Database
        mDbHelper = new DBHelper(this);
        mDb = mDbHelper.getWritableDatabase();
        mDb.execSQL ("delete from MusicList");
        mDb.execSQL("update sqlite_sequence set seq=0 where name='MusicList'");
        // Build the contentResolver
        ContentResolver contentResolver = getContentResolver();
        Cursor myCursor=contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,MediaStore.Audio.AudioColumns.IS_MUSIC);

        // Read the Music in the MediaStore
        if (myCursor != null) {
            while (myCursor.moveToNext()) {
                name = myCursor.getString(myCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                singer = myCursor.getString(myCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                path = myCursor.getString(myCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                duration = myCursor.getInt(myCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                mDb.execSQL("INSERT INTO MusicList (name, singer, path, duration) " + "VALUES " +"('" + name
                        + "','" + singer + "','" + path + "','" + duration +
                        "');");
            }
        }

        // Close the Cursor
        assert myCursor != null;
    }

    // Build the Adapter for the ListView
    private void buildAdapter(String tableName){
        // Find the Music List View
        mMusicListView = findViewById(R.id.musicList);
        mDbHelper = new DBHelper(this);
        mDb = mDbHelper.getWritableDatabase();
        // Search the musics in the current Playlist
        Cursor cursor = mDb.query(tableName, new String[]{"_id", "name", "singer","path","duration"},
                null, null, null, null, null);
        String[] columns = new String[]{
                "name",
                "singer",
        };

        // Map the ListView
        int[] uiMapping = new int[]{
                R.id.nameView,
                R.id.singerView,
        };

        // Build the Adapter
        mDataAdapter = new SimpleCursorAdapter(
                this, R.layout.db_item_view,
                cursor,
                columns,
                uiMapping,
                0);
        // Set the Adapter
        mMusicListView.setAdapter(mDataAdapter);
    }

    // Override the onNewIntent for get the intent from other app or other activity
    @Override
    protected void onNewIntent(Intent intent) {
        // Switch and play music through other app (Only sd card)
        super.onNewIntent(intent);
        setIntent(intent);
        Intent musicIntent = getIntent();
        playMusicFromOther(musicIntent);

        // If the Intent is not from other app
        if (!Objects.equals(musicIntent.getAction(), "android.intent.action.VIEW")) {
            Bundle bundle = this.getIntent().getExtras();
            if (bundle != null) {
                Music returnMusic = (Music) bundle.getSerializable("currentMusic");
                if (returnMusic.getPath() != null) {
                    // Set up the TextView
                    TextView currentName = findViewById(R.id.Name);
                    TextView currentSinger = findViewById(R.id.Singer);
                    currentName.setText(returnMusic.getName());
                    currentSinger.setText(returnMusic.getSinger());
                    // Play the music by MusicHelper and Do the notification
                    selectedMusic = returnMusic;
                    doNotification();
                    myMusicHelper.switchMusic(returnMusic);
                }
            }
        }
    }

    // Deal with the Music from different Music
    private void playMusicFromOther(Intent musicIntent){
        // If got the view from other app, load the image by url
        if (Objects.equals(musicIntent.getAction(), "android.intent.action.VIEW")){
            // Transform the Uri to the Music class
            Music inputMusic = new Music();
            String path = musicIntent.getData().getPath();
            String[] nameSplit = path.split("/");
            String name = nameSplit[nameSplit.length-1];
            // Search the Music in the MusicList
            @SuppressLint("Recycle") Cursor cursor = mDb.query("MusicList", new String[]{"_id", "name", "singer","path","duration"},
                    "name=?",  new String[]{name}, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    inputMusic.setName(cursor.getString(cursor.getColumnIndex("name")));
                    inputMusic.setSinger(cursor.getString(cursor.getColumnIndex("singer")));
                    inputMusic.setPath(cursor.getString(cursor.getColumnIndex("path")));
                    inputMusic.setDuration(cursor.getInt(cursor.getColumnIndex("duration")));
                }
            }
            else {
                Toast.makeText(MainActivity.this, "Wrong at play music from other music ", Toast.LENGTH_SHORT).show();
                return;
            }
            // Set up the TextView
            TextView currentName = findViewById(R.id.Name);
            TextView currentSinger = findViewById(R.id.Singer);
            currentName.setText(inputMusic.getName());
            currentSinger.setText(inputMusic.getSinger());
            // Play the music by MusicHelper
            selectedMusic = inputMusic;
            doNotification();
            myMusicHelper.switchMusic(inputMusic);
        }
    }
}