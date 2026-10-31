package ua.bashaiev.onlinetv;
import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
public class TvPlayerActivity extends Activity {
 private ExoPlayer player;
 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  String url=getIntent().getStringExtra("url");
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.BLACK);
  TextView title=new TextView(this);
  title.setText("‹  "+getIntent().getStringExtra("name"));
  title.setTextColor(Color.WHITE);
  title.setTextSize(20);
  title.setPadding(25,24,16,24);
  title.setOnClickListener(v->finish());
  root.addView(title);
  PlayerView view=new PlayerView(this);
  root.addView(view,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
  TextView hint=new TextView(this);
  hint.setText("Якщо відео не запускається, джерело може бути недоступним.");
  hint.setTextColor(Color.LTGRAY);
  hint.setGravity(Gravity.CENTER);
  hint.setPadding(16,16,16,32);
  root.addView(hint);
  setContentView(root);
  player=new ExoPlayer.Builder(this).build();
  view.setPlayer(player);
  player.setMediaItem(MediaItem.fromUri(url));
  player.prepare();
  player.play();
 }
 @Override public void onStop(){super.onStop();if(player!=null){player.release();player=null;}}
}
