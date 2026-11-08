
package mg.aptm.tononkira;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.media.MediaPlayer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private final int PICK_TEXT = 10, PICK_AUDIO = 11;

    private ArrayList<Song> songs;
    private ArrayAdapter<String> adapter;
    private ListView list;

    private MediaPlayer player;
    private int selected = -1;
    private boolean prepared = false;
    private boolean isPreparing = false;
    private boolean userSeeking = false;

    private TextView playerTitle, currentTime, totalTime;
    private Button playButton;
    private SeekBar seekBar;

    private final Handler handler = new Handler();

    private final int navy = Color.rgb(16, 27, 53);
    private final int gold = Color.rgb(213, 173, 85);

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && prepared && player.isPlaying()) {
                updateProgress();
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        songs = SongStore.load(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 12, 12, 8);
        root.setBackgroundColor(Color.rgb(245, 246, 249));

        TextView brand = new TextView(this);
        brand.setText("TONONKIRA A.P.T.M.");
        brand.setTextSize(25);
        brand.setTextColor(gold);
        brand.setGravity(Gravity.CENTER);
        brand.setPadding(8, 22, 8, 8);
        brand.setBackgroundColor(navy);
        root.addView(brand);

        TextView sub = new TextView(this);
        sub.setText("BOKIN-KIRA • CHORALE");
        sub.setTextColor(Color.WHITE);
        sub.setGravity(Gravity.CENTER);
        sub.setBackgroundColor(navy);
        sub.setPadding(8, 0, 8, 18);
        root.addView(sub);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        addButton(actions, "+ Hira", v -> newSong());
        addButton(actions, "Import TXT", v -> pickText());
        addButton(actions, "Audio", v -> pickAudio());

        root.addView(actions);

        list = new ListView(this);
        root.addView(list,
            new LinearLayout.LayoutParams(-1, 0, 1));

        list.setOnItemClickListener((p, v, pos, id) -> {
            selected = pos;
            updatePlayerInfo();
            showSong(pos);
        });

        createPlayerPanel(root);

        refresh();
        updatePlayerInfo();

        setContentView(root);
    }

    private void createPlayerPanel(LinearLayout root) {

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(12, 12, 12, 12);
        panel.setBackgroundColor(navy);

        playerTitle = new TextView(this);
        playerTitle.setText("LECTEUR AUDIO");
        playerTitle.setTextColor(gold);
        playerTitle.setTextSize(18);
        playerTitle.setGravity(Gravity.CENTER);
        panel.addView(playerTitle);

        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        panel.addView(seekBar);

        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);

        currentTime = new TextView(this);
        currentTime.setText("00:00");
        currentTime.setTextColor(Color.WHITE);

        totalTime = new TextView(this);
        totalTime.setText("00:00");
        totalTime.setTextColor(Color.WHITE);
        totalTime.setGravity(Gravity.RIGHT);

        times.addView(currentTime,
            new LinearLayout.LayoutParams(0, -2, 1));
        times.addView(totalTime,
            new LinearLayout.LayoutParams(0, -2, 1));

        panel.addView(times);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button previous = new Button(this);
        previous.setText("⏮");
        controls.addView(previous);

        playButton = new Button(this);
        playButton.setText("▶ Play");
        controls.addView(playButton);

        Button next = new Button(this);
        next.setText("⏭");
        controls.addView(next);

        panel.addView(controls);

        previous.setOnClickListener(v -> moveTrack(-1));
        next.setOnClickListener(v -> moveTrack(1));
        playButton.setOnClickListener(v -> pauseResume());

        seekBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStartTrackingTouch(SeekBar bar) {
                    userSeeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar bar) {
                    userSeeking = false;

                    if (player != null && prepared) {
                        int duration = player.getDuration();
                        int position = duration * bar.getProgress() / 1000;
                        player.seekTo(position);
                        updateProgress();
                    }
                }

                @Override
                public void onProgressChanged(
                    SeekBar bar, int progress, boolean fromUser) {
                }
            }
        );

        root.addView(panel,
            new LinearLayout.LayoutParams(-1, -2));
    }

    private void addButton(
        LinearLayout row,
        String label,
        View.OnClickListener listener) {

        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(navy);
        b.setOnClickListener(listener);

        row.addView(b,
            new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void refresh() {
        ArrayList<String> names = new ArrayList<>();

        for (Song s : songs) {
            names.add(s.title);
        }

        adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_list_item_1,
            names
        );

        list.setAdapter(adapter);
        updatePlayerInfo();
    }

    private void updatePlayerInfo() {

        if (playerTitle == null) return;

        if (selected >= 0 && selected < songs.size()) {
            playerTitle.setText("♫ " + songs.get(selected).title);
        } else {
            playerTitle.setText("LECTEUR AUDIO - Misafidiana hira");
        }

        if (player == null || !prepared) {
            currentTime.setText("00:00");
            totalTime.setText("00:00");
            seekBar.setProgress(0);
        }

        if (!isPreparing) {
            playButton.setText(
                player != null && prepared && player.isPlaying()
                    ? "❚❚ Pause" : "▶ Play"
            );
        }
    }

    private void newSong() {
        editSong("", "", -1);
    }

    private void editSong(String title, String lyrics, int index) {

        LinearLayout box = new LinearLayout(this);
        box.setPadding(16, 8, 16, 8);
        box.setOrientation(LinearLayout.VERTICAL);

        EditText name = new EditText(this);
        name.setHint("Lohatenin'ny hira");
        name.setText(title);
        box.addView(name);

        EditText body = new EditText(this);
        body.setHint("Soraty eto ny tononkira");
        body.setText(lyrics);
        body.setMinLines(8);
        body.setGravity(Gravity.TOP);
        box.addView(body);

        new AlertDialog.Builder(this)
            .setTitle("Hira")
            .setView(box)
            .setNegativeButton("Hanafoana", null)
            .setPositiveButton("Tehirizo", (d, w) -> {

                String n = name.getText().toString().trim();
                String t = body.getText().toString().trim();

                if (n.isEmpty()) {
                    Toast.makeText(this,
                        "Ampidiro ny lohateny",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (index >= 0) {
                    songs.get(index).title = n;
                    songs.get(index).lyrics = t;
                } else {
                    songs.add(new Song(n, t, ""));
                    selected = songs.size() - 1;
                }

                SongStore.save(this, songs);
                refresh();
            })
            .show();
    }

    private void showSong(int pos) {

        Song s = songs.get(pos);

        String[] opts = {
            "Vakio / Ovay",
            "Projection",
            "Ampidiro audio",
            "Alefaso audio",
            "Fafao"
        };

        new AlertDialog.Builder(this)
            .setTitle(s.title)
            .setItems(opts, (d, which) -> {

                if (which == 0) {
                    editSong(s.title, s.lyrics, pos);
                }

                if (which == 1) {
                    Intent i = new Intent(
                        this, ProjectionActivity.class);
                    i.putExtra("title", s.title);
                    i.putExtra("lyrics", s.lyrics);
                    startActivity(i);
                }

                if (which == 2) {
                    pickAudio();
                }

                if (which == 3) {
                    playSelected();
                }

                if (which == 4) {
                    new AlertDialog.Builder(this)
                        .setMessage("Fafàna ity hira ity?")
                        .setNegativeButton("Tsia", null)
                        .setPositiveButton("Eny", (x, y) -> {

                            stopPlayer();

                            songs.remove(pos);

                            if (selected == pos) {
                                selected = -1;
                            } else if (selected > pos) {
                                selected--;
                            }

                            SongStore.save(this, songs);
                            refresh();
                        })
                        .show();
                }
            })
            .show();
    }

    private void pickText() {

        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");

        startActivityForResult(i, PICK_TEXT);
    }

    private void pickAudio() {

        if (selected < 0 || selected >= songs.size()) {
            Toast.makeText(this,
                "Safidio aloha ny hira",
                Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");

        startActivityForResult(i, PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(
        int req, int res, Intent data) {

        super.onActivityResult(req, res, data);

        if (res != RESULT_OK || data == null) return;

        Uri u = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }

        if (req == PICK_TEXT) {

            try (java.io.InputStream in =
                getContentResolver().openInputStream(u)) {

                java.io.ByteArrayOutputStream out =
                    new java.io.ByteArrayOutputStream();

                byte[] buf = new byte[4096];
                int n;

                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }

                String text = out.toString("UTF-8");
                String name = u.getLastPathSegment();

                songs.add(new Song(
                    name == null ? "Hira nafarana" : name,
                    text,
                    ""
                ));

                selected = songs.size() - 1;

                SongStore.save(this, songs);
                refresh();

            } catch (Exception e) {
                Toast.makeText(this,
                    "Tsy voavaky ilay rakitra",
                    Toast.LENGTH_LONG).show();
            }
        }

        if (req == PICK_AUDIO && selected >= 0) {

            songs.get(selected).audioUri = u.toString();

            SongStore.save(this, songs);
            updatePlayerInfo();

            Toast.makeText(this,
                "Audio nampidirina",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseResume() {

        if (player != null && prepared) {

            if (player.isPlaying()) {
                player.pause();
                playButton.setText("▶ Play");
            } else {
                player.start();
                playButton.setText("❚❚ Pause");
                handler.removeCallbacks(progressRunnable);
                handler.post(progressRunnable);
            }

        } else {
            playSelected();
        }
    }

    private void playSelected() {

        if (selected < 0 || selected >= songs.size()) {
            Toast.makeText(this,
                "Safidio aloha ny hira",
                Toast.LENGTH_SHORT).show();
            return;
        }

        Song s = songs.get(selected);

        if (s.audioUri == null || s.audioUri.isEmpty()) {
            Toast.makeText(this,
                "Tsy mbola misy audio amin'ity hira ity",
                Toast.LENGTH_SHORT).show();
            return;
        }

        stopPlayer();

        try {

            final MediaPlayer mp = new MediaPlayer();
            player = mp;
            prepared = false;
            isPreparing = true;

            playButton.setText("Miomana...");

            mp.setDataSource(this, Uri.parse(s.audioUri));

            mp.setOnPreparedListener(p -> {

                if (player != mp) {
                    mp.release();
                    return;
                }

                prepared = true;
                isPreparing = false;

                seekBar.setProgress(0);
                totalTime.setText(formatTime(mp.getDuration()));
                currentTime.setText("00:00");

                mp.start();

                playButton.setText("❚❚ Pause");

                handler.removeCallbacks(progressRunnable);
                handler.post(progressRunnable);
            });

            mp.setOnCompletionListener(p -> {

                handler.removeCallbacks(progressRunnable);

                if (player == mp) {
                    mp.release();
                    player = null;
                }

                prepared = false;
                isPreparing = false;

                playButton.setText("▶ Play");
                seekBar.setProgress(0);
                currentTime.setText("00:00");
            });

            mp.prepareAsync();

        } catch (Exception e) {

            stopPlayer();

            Toast.makeText(this,
                "Tsy afaka alefa ilay audio",
                Toast.LENGTH_LONG).show();
        }
    }

    private void moveTrack(int direction) {

        if (songs.isEmpty()) return;

        int next = selected + direction;

        if (next < 0 || next >= songs.size()) {
            Toast.makeText(this,
                "Tsy misy hira intsony amin'io lalana io",
                Toast.LENGTH_SHORT).show();
            return;
        }

        selected = next;
        stopPlayer();
        updatePlayerInfo();

        if (songs.get(selected).audioUri != null
            && !songs.get(selected).audioUri.isEmpty()) {
            playSelected();
        } else {
            Toast.makeText(this,
                "Tsy mbola misy audio amin'ity hira ity",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgress() {

        if (player == null || !prepared) return;

        try {

            int duration = player.getDuration();
            int position = player.getCurrentPosition();

            if (!userSeeking && duration > 0) {
                seekBar.setProgress(position * 1000 / duration);
            }

            currentTime.setText(formatTime(position));
            totalTime.setText(formatTime(duration));

        } catch (Exception ignored) {
        }
    }

    private String formatTime(int milliseconds) {

        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            minutes,
            seconds
        );
    }

    private void stopPlayer() {

        handler.removeCallbacks(progressRunnable);

        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }

            try {
                player.release();
            } catch (Exception ignored) {
            }

            player = null;
        }

        prepared = false;
        isPreparing = false;

        if (playButton != null) {
            playButton.setText("▶ Play");
        }
    }

    @Override
    protected void onDestroy() {
        stopPlayer();
        super.onDestroy();
    }
  }
              
