package cn.edu.nottingham.sam20031862.musicplayer;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

// Class for helping the user to Play the Music
public class MusicPlayerHelper {

    // Init the Val
    MediaPlayer player;
    Music currentMusic;
    Timer timer;
    SeekBar mySeekBar;
    TextView maxDuration;
    TextView currentDuration;

    // Constructor for the class
    public MusicPlayerHelper() {
        player = new MediaPlayer();
    }

    // Switch the current Music
    public void switchMusic(Music currentMusic) {
        // Set up the Current Music
        this.currentMusic = currentMusic;
        player.reset();
        // Try to Set up the Data Source
        try {
            player.setDataSource(currentMusic.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Try to prepare the Player
        try {
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Start the Player
        player.start();
        // Run a thread to get the current Progress
        getProgress();
    }

    // Init the component from main activity
    public void initCom(SeekBar mySeekBar, TextView maxDuration, TextView currentDuration){
        this.mySeekBar = mySeekBar;
        this.maxDuration = maxDuration;
        this.currentDuration = currentDuration;
    }

    // Get the Progress of current music
    public void getProgress() {
        // Get the duration of the player
        int duration = player.getDuration();
        mySeekBar.setMax(duration);
        maxDuration.setText(getTime(duration));
        // Set up the Timer of getting the Progress
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                // Send the message to change the view in MainACTIVITY
                int progress = player.getCurrentPosition();
                mySeekBar.setProgress(progress);
                Message message = new Message();
                message.what = progress;
                handler.sendMessage(message);
            }
        }, 0, 200);
    }

    // IF IS NOT PLAYING, PLAY THE MUSIC
    public void playMusic() {
        if (!player.isPlaying()){
            player.start();
        }
    }

    // IF IF PLAYING, PAUSE IT
    public void pauseMusic() {
        if (player.isPlaying()){
            player.pause();
        }
    }
    // Stop the Music
    public void stopMusic() {
        // stop the timer
        player.stop();
        player.reset();
        if (timer != null){
            timer.cancel();
        }
    }

    // Get the String of Time by Duration
    private String getTime(int duration){
        int second = duration/1000;
        int min = second/60;
        int currentSecond = second%60;
        String myTime;
        if (currentSecond < 10){
            myTime = min + ":0" + currentSecond;
        }
        else {
            myTime = min + ":" + currentSecond;
        }
        return myTime;
    }

    // Build a handler to handle the message and set the text View
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            currentDuration.setText(getTime(msg.what));
            return false;
        }
    });

}
