package com.musicplayer;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.musicplayer.databinding.ItemSongBinding;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewholder> {
    private final List<Song> songs;
    private final OnItemClickListener listener;

    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public SongAdapter(List<Song> songs, OnItemClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongAdapter.SongViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSongBinding binding=ItemSongBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new SongViewholder(binding,listener);
    }

    @Override
    public void onBindViewHolder(@NonNull SongAdapter.SongViewholder holder, int position) {
        Song song=songs.get(position);
        holder.binding.songTitleTxt.setText(song.title);
        holder.binding.artistTxt.setText(song.artist);

        Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),song.albumId);

        Glide.with(holder.binding.getRoot().getContext())
                .load(albumArtUri)
                .circleCrop()
                .placeholder(R.drawable.ic_music)
                .error(R.drawable.ic_music)
                .into(holder.binding.imageAlbumArt);

    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class SongViewholder extends RecyclerView.ViewHolder {
        final ItemSongBinding binding;
        final OnItemClickListener listener;
        public SongViewholder(ItemSongBinding binding,OnItemClickListener listener) {
            super(binding.getRoot());
            this.binding=binding;
            this.listener=listener;
            binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    int pos= getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION){
                        listener.onItemClick(pos);
                    }
                }
            });

        }
    }
}
