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
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import android.media.MediaPlayer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private final int PICK_TEXT = 10, PICK_AUDIO = 11;

    private ArrayList<Song> songs;
    private ArrayList<Song> visibleSongs;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> names;

    private ListView list;
    private EditText search;
    private MediaPlayer player;

    private Button playButton;
    private SeekBar seekBar;
    private TextView timeText;

    private int selected = -1;
    private boolean prepared = false;
    private boolean dragging = false;

    private Handler handler = new Handler();

    private final int navy = Color.rgb(16, 27, 53);
    private final int gold = Color.rgb(213, 173, 85);

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && prepared) {
                try {
                    if (!dragging) {
                        seekBar.setProgress(player.getCurrentPosition());
                    }

                    timeText.setText(
                        formatTime(player.getCurrentPosition())
                        + " / " + formatTime(player.getDuration())
                    );
                } catch (Exception ignored) {}
            }

            handler.postDelayed(this, 500);
        }
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        songs = SongStore.load(this);
        visibleSongs = new ArrayList<>();
        names = new ArrayList<>();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(14, 14, 14, 10);
        root.setBackgroundColor(Color.rgb(245, 246, 249));

        TextView brand = new TextView(this);
        brand.setText("TONONKIRA A.P.T.M.");
        brand.setTextSize(25);
        brand.setTextColor(gold);
        brand.setGravity(Gravity.CENTER);
        brand.setPadding(8, 20, 8, 8);
        brand.setBackgroundColor(navy);
        root.addView(brand);

        TextView sub = new TextView(this);
        sub.setText("TARATR’I MARIA • BOKIN-KIRA");
        sub.setTextColor(Color.WHITE);
        sub.setGravity(Gravity.CENTER);
        sub.setBackgroundColor(navy);
        sub.setPadding(8, 0, 8, 18);
        root.addView(sub);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Mitadiava hira...");
        root.addView(search);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        addButton(actions, "+ Hira", v -> newSong());
        addButton(actions, "Import TXT", v -> pickText());
        addButton(actions, "Audio", v -> pickAudio());

        root.addView(actions);

        list = new ListView(this);
        root.addView(list, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        list.setOnItemClickListener((p, v, pos, id) -> {
            Song s = visibleSongs.get(pos);
            selected = songs.indexOf(s);
            showSong(selected);
        });

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(8, 10, 8, 8);
        controls.setBackgroundColor(navy);

        timeText = new TextView(this);
        timeText.setText("00:00 / 00:00");
        timeText.setTextColor(Color.WHITE);
        timeText.setGravity(Gravity.CENTER);
        controls.addView(timeText);

        seekBar = new SeekBar(this);
        controls.addView(seekBar);

        seekBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(
                    SeekBar bar, int progress, boolean fromUser
                ) {}

                @Override
                public void onStartTrackingTouch(SeekBar bar) {
                    dragging = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar bar) {
                    dragging = false;

                    if (player != null && prepared) {
                        try {
                            player.seekTo(bar.getProgress());
                        } catch (Exception ignored) {}
                    }
                }
            }
        );

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        addControl(buttons, "⏮", v -> playAdjacent(-1));

        playButton = new Button(this);
        playButton.setText("▶ Play");
        playButton.setTextColor(navy);
        playButton.setOnClickListener(v -> togglePlay());
        buttons.addView(playButton,
            new LinearLayout.LayoutParams(0, -2, 2));

        addControl(buttons, "⏭", v -> playAdjacent(1));

        controls.addView(buttons);
        root.addView(controls);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after
            ) {}

            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count
            ) {
                refresh();
            }

            @Override
            public void afterTextChanged(Editable e) {}
        });

        refresh();
        setContentView(root);

        handler.post(progressRunnable);
    }

    private void addButton(
        LinearLayout row,
        String label,
        View.OnClickListener listener
    ) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(navy);
        b.setOnClickListener(listener);

        row.addView(b,
            new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void addControl(
        LinearLayout row,
        String label,
        View.OnClickListener listener
    ) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(navy);
        b.setOnClickListener(listener);

        row.addView(b,
            new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void refresh() {
        names.clear();
        visibleSongs.clear();

        String query = search == null
            ? ""
            : search.getText().toString().toLowerCase(Locale.ROOT);

        for (Song s : songs) {
            String title = s.title == null ? "" : s.title;
            String lyrics = s.lyrics == null ? "" : s.lyrics;

            if (title.toLowerCase(Locale.ROOT).contains(query)
                || lyrics.toLowerCase(Locale.ROOT).contains(query)) {

                visibleSongs.add(s);
                names.add(title);
            }
        }

        adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_list_item_1,
            names
        );

        list.setAdapter(adapter);
    }

    private void newSong() {
        editSong("", "", -1);
    }

    private void editSong(
        String title,
        String lyrics,
        int index
    ) {
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
                    Toast.makeText(
                        this,
                        "Ampidiro ny lohateny",
                        Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                if (index >= 0) {
                    songs.get(index).title = n;
                    songs.get(index).lyrics = t;
                } else {
                    songs.add(new Song(n, t, ""));
                }

                SongStore.save(this, songs);
                refresh();
            })
            .show();
    }

    private void showSong(int pos) {
        selected = pos;
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
                        this,
                        ProjectionActivity.class
                    );

                    i.putExtra("title", s.title);
                    i.putExtra("lyrics", s.lyrics);
                    startActivity(i);
                }

                if (which == 2) {
                    pickAudio();
                }

                if (which == 3) {
                    play(s);
                }

                if (which == 4) {
                    new AlertDialog.Builder(this)
                        .setMessage("Fafàna ity hira ity?")
                        .setNegativeButton("Tsia", null)
                        .setPositiveButton("Eny", (x, y) -> {

                            songs.remove(pos);

                            if (selected == pos) {
                                stopPlayer();
                                selected = -1;
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
        if (selected < 0) {
            Toast.makeText(
                this,
                "Safidio aloha ny hira",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");

        startActivityForResult(i, PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(
        int req, int res, Intent data
    ) {
        super.onActivityResult(req, res, data);

        if (res != RESULT_OK || data == null) return;

        Uri u = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                u,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        if (req == PICK_TEXT) {
            try (
                java.io.InputStream in =
                    getContentResolver().openInputStream(u)
            ) {
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

                SongStore.save(this, songs);
                refresh();

                Toast.makeText(
                    this,
                    "Hira nampidirina",
                    Toast.LENGTH_SHORT
                ).show();

            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "Tsy voavaky ilay rakitra",
                    Toast.LENGTH_LONG
                ).show();
            }
        }

        if (req == PICK_AUDIO && selected >= 0) {
            songs.get(selected).audioUri = u.toString();

            SongStore.save(this, songs);

            Toast.makeText(
                this,
                "Audio nampidirina",
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void togglePlay() {
        try {
            if (player != null && prepared) {

                if (player.isPlaying()) {
                    player.pause();
                    playButton.setText("▶ Play");
                } else {
                    player.start();
                    playButton.setText("⏸ Pause");
                }

            } else if (selected >= 0) {
                play(songs.get(selected));
            } else {
                Toast.makeText(
                    this,
                    "Safidio aloha ny hira",
                    Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception ignored) {}
    }

    private void play(Song s) {
        int index = songs.indexOf(s);

        if (index >= 0) selected = index;

        if (s.audioUri == null || s.audioUri.isEmpty()) {
            Toast.makeText(
                this,
                "Tsy mbola misy audio amin'ity hira ity",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        stopPlayer();

        try {
            player = new MediaPlayer();
            prepared = false;

            player.setDataSource(
                this,
                Uri.parse(s.audioUri)
            );

            player.setOnPreparedListener(mp -> {
                prepared = true;

                try {
                    seekBar.setMax(mp.getDuration());
                    seekBar.setProgress(0);
                    mp.start();
                    playButton.setText("⏸ Pause");
                } catch (Exception ignored) {}
            });

            player.setOnCompletionListener(mp -> {
                playAdjacent(1);
            });

            player.setOnErrorListener((mp, what, extra) -> {
                stopPlayer();

                Toast.makeText(
                    this,
                    "Nisy olana tamin'ny audio",
                    Toast.LENGTH_SHORT
                ).show();

                return true;
            });

            player.prepareAsync();

            timeText.setText("Miandry ny audio...");

        } catch (Exception e) {
            stopPlayer();

            Toast.makeText(
                this,
                "Tsy afaka alefa ilay audio",
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void playAdjacent(int direction) {
        if (songs.isEmpty()) return;

        int start = selected;

        if (start < 0) start = 0;

        for (int i = 1; i <= songs.size(); i++) {
            int index = (start + direction * i) % songs.size();

            if (index < 0) index += songs.size();

            Song s = songs.get(index);

            if (s.audioUri != null && !s.audioUri.isEmpty()) {
                selected = index;
                play(s);
                return;
            }
        }

        Toast.makeText(
            this,
            "Tsy misy audio hafa",
            Toast.LENGTH_SHORT
        ).show();
    }

    private void stopPlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {}

            try {
                player.release();
            } catch (Exception ignored) {}

            player = null;
        }

        prepared = false;

        if (playButton != null) {
            playButton.setText("▶ Play");
        }

        if (seekBar != null) {
            seekBar.setProgress(0);
            seekBar.setMax(100);
        }

        if (timeText != null) {
            timeText.setText("00:00 / 00:00");
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

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(progressRunnable);
        stopPlayer();
        super.onDestroy();
    }
    }
