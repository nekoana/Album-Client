package com.codinlog.album.entity;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "albumTB",indices = {@Index(value = {"albumName"},unique = true)})
public class AlbumEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "albumId")
    private int id;

    @NonNull
    @ColumnInfo(name = "albumName")
    private String albumName;

    @ColumnInfo(name = "displayPhotoPath")
    private String displayPhotoPath;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getDisplayPhotoPath() {
        return displayPhotoPath;
    }

    public void setDisplayPhotoPath(String displayPhotoPath) {
        this.displayPhotoPath = displayPhotoPath;
    }

    @NonNull
    @Override
    public String toString() {
        return "albumName:" + albumName + ",id:" + id + "\n";
    }
}