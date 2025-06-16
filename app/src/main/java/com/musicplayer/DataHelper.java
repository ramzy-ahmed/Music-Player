package com.musicplayer;

import java.util.List;

public class DataHelper {

    private static DataHelper instance;
    private List<Song> songList;

    private DataHelper() {}

    public static DataHelper getInstance() {
        if (instance == null) {
            instance = new DataHelper();
        }
        return instance;
    }

    public void setSongList(List<Song> songList) {
        this.songList = songList;
    }

    public List<Song> getSongList() {
        return songList;
    }
}
