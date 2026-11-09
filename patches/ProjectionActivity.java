
package mg.aptm.tononkira;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.graphics.Color;

public class ProjectionActivity extends Activity {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        String title = getIntent().getStringExtra("title");
        String lyrics = getIntent().getStringExtra("lyrics");

        TextView t = new TextView(this);
        t.setBackgroundColor(Color.rgb(12, 20, 39));
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(32, 24, 32, 24);
        t.setText((title == null ? "" : title + "\n\n") +
                  (lyrics == null ? "" : lyrics));
        t.setTextSize(30);

        setContentView(t);
    }
}
