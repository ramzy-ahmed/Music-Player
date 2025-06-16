package com.musicplayer;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.frolo.waveformseekbar.WaveformSeekBar;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.musicplayer.databinding.ActivityPlayerBinding;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class PlayerActivity extends AppCompatActivity {

    ActivityPlayerBinding binding;
    private ExoPlayer player;
    private Handler handler = new Handler();
    private List<Song> songList = new ArrayList<>();
    private List<Song> shuffledList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private ObjectAnimator rotationAnimator;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();
                if (duration > 0) {
                    float progressPercent = ((float) currentPosition / duration);
                    binding.seekbar.setProgressInPercentage(progressPercent);
                    binding.songElapsedTxt.setText(formatTime((int) (currentPosition / 1000)));
                    binding.songDurationTxt.setText(formatTime((int) (duration / 1000)));
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        songList = DataHelper.getInstance().getSongList();
        currentIndex = getIntent().getIntExtra("position", 0);
        if (songList == null || songList.isEmpty()) {
            Toast.makeText(this, "no songs Found", Toast.LENGTH_SHORT).show();
            finish();
        }
        shuffledList = new ArrayList<>(songList);
        binding.seekbar.setWaveform(createWaveForm(), true);

        initPlayerWithSong(currentIndex);
        setupControls();
        setupRotationAnimator();
    }

    private void setupRotationAnimator() {
        rotationAnimator = ObjectAnimator.ofFloat(binding.pic, "rotation", 0f, 360f);
        rotationAnimator.setDuration(15000);
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnimator.setRepeatMode(ObjectAnimator.RESTART);
        rotationAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
    }

    private void updateAlbumArtRotation() {
        if (player != null && player.isPlaying()) {
            if (rotationAnimator != null && !rotationAnimator.isStarted()) {
                rotationAnimator.start();
            } else if (rotationAnimator != null && rotationAnimator.isPaused()) {
                rotationAnimator.resume();
            }
        } else {
            if (rotationAnimator != null && rotationAnimator.isStarted()) {
                rotationAnimator.pause();
            }
        }
    }

    private void setupControls() {
        binding.playBtn.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                handler.removeCallbacks(updateRunnable);
            } else {
                player.play();
                handler.postDelayed(updateRunnable, 0);
            }
            updatePlayerState();
            updateAlbumArtRotation();
        });
        binding.nextBtn.setOnClickListener(v -> playNext());
        binding.prevBtn.setOnClickListener(v -> playPrevious());
        binding.shuffleBtn.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            if (isShuffle) {
                Collections.shuffle(shuffledList);
                binding.shuffleBtn.setColorFilter(getResources().getColor(R.color.purple));
            } else {
                shuffledList = new ArrayList<>(songList);
                binding.shuffleBtn.clearColorFilter();
            }
        });
        binding.repeatBtn.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            if (isRepeat) {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                binding.repeatBtn.setColorFilter(getResources().getColor(R.color.purple));

            } else {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                binding.repeatBtn.clearColorFilter();
            }
        });
        binding.backBtn.setOnClickListener(v -> finish());


        binding.seekbar.setCallback(new WaveformSeekBar.Callback() {
            @Override
            public void onProgressChanged(WaveformSeekBar seekBar, float percent, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    long seekPosition = (long) (percent * duration);
                    player.seekTo(seekPosition);
                    binding.songElapsedTxt.setText(formatTime((int) (seekPosition / 1000)));
                }
            }

            @Override
            public void onStartTrackingTouch(WaveformSeekBar seekBar) {
                handler.removeCallbacks(updateRunnable);
            }

            @Override
            public void onStopTrackingTouch(WaveformSeekBar seekBar) {
                handler.postDelayed(updateRunnable, 0);
            }
        });

    }

    private void initPlayerWithSong(int index) {
        Song song = isShuffle ? shuffledList.get(index) : songList.get(index);

        if (player != null) player.release();
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(isRepeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updatePlayerState();
                if (playbackState == Player.STATE_READY) {
                    binding.songDurationTxt.setText(formatTime((int) player.getDuration() / 1000));
                    handler.postDelayed(updateRunnable, 0);
                } else if (playbackState == Player.STATE_ENDED) {
                    playNext();
                }
                updateAlbumArtRotation();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this, "Error playing media:" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        player.setMediaItem(MediaItem.fromUri(song.data));
        player.prepare();
        player.play();

        updatePlayerState();
        updateUI(song);
        updateAlbumArtRotation();
    }

    private void updateUI(Song song) {
        binding.songTitleTxt.setText(song.title != null ? song.title : "");
        binding.artistTxt.setText(song.artist != null ? song.artist : "");
        setTitle(song.title);

        Uri albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), song.albumId
        );
        if (hasAlbumArt(albumArtUri)) {
            Glide.with(this)
                    .asBitmap()
                    .load(albumArtUri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_music)
                    .error(R.drawable.ic_music)
                    .into(binding.pic);

            Glide.with(this)
                    .asBitmap()
                    .load(albumArtUri)
                    .apply(bitmapTransform(new BlurTransformation(25, 3)))
                    .placeholder(R.drawable.ic_music)
                    .error(R.drawable.ic_music)
                    .into(binding.bgAlbumArt);

        } else {
            binding.pic.setImageResource(R.drawable.ic_music);
            binding.bgAlbumArt.setImageResource(R.drawable.gradient_bg);
        }
    }

    private boolean hasAlbumArt(Uri albumArtUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(albumArtUri)) {
            return inputStream != null;
        } catch (Exception e) {
            return false;
        }
    }

    private int[] createWaveForm() {
        Random random = new Random(System.currentTimeMillis());
        int[] values = new int[50];
        for (int i = 0; i < values.length; i++) {
            values[i] = 5 + random.nextInt(50);
        }
        return values;
    }

    private void updatePlayerState() {
        binding.playBtn.setImageResource(player != null && player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void playNext() {
        currentIndex = (currentIndex + 1) % (isShuffle ? shuffledList.size() : songList.size());
        initPlayerWithSong(currentIndex);
    }

    private void playPrevious() {
        int listSize = isShuffle ? shuffledList.size() : songList.size();
        currentIndex = (currentIndex - 1 + listSize) % listSize;
        initPlayerWithSong(currentIndex);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        if (player != null) {
            player.release();
            player = null;
        }
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
            rotationAnimator = null;
        }
    }
}