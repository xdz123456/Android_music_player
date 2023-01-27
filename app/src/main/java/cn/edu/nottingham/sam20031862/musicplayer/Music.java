package cn.edu.nottingham.sam20031862.musicplayer;

import java.io.Serializable;

// Set the Music Class
public class Music implements Serializable {
    private String Name;
    private String Singer;
    private String Path;
    private int Duration;;

    public void setName(String Name){
        this.Name = Name;
    }
    public void setSinger(String Singer){
        this.Singer = Singer;
    }
    public void setPath(String Path){
        this.Path = Path;
    }
    public void setDuration(int Duration){
        this.Duration = Duration;
    }

    public String getName(){
        return this.Name;
    }
    public String getSinger(){
        return this.Singer;
    }
    public String getPath(){
        return this.Path;
    }
    public int getDuration(){
        return this.Duration;
    }
}
