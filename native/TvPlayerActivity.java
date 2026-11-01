package ua.bashaiev.onlinetv;
import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
public class TvPlayerActivity extends Activity {
 private ExoPlayer player;
 private java.util.ArrayList<String> sources;
 private int sourceIndex=0;
 private final android.os.Handler stallHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private Runnable stallCheck;
 private boolean played=false;
 private TextView statusHint;
 private void tryNext(String reason){
  stallHandler.removeCallbacks(stallCheck);
  if(player==null)return;
  if(sourceIndex+1<sources.size()){
   sourceIndex++;played=false;
   statusHint.setText("Резервне джерело "+(sourceIndex+1)+"/"+sources.size());
   player.setMediaItem(MediaItem.fromUri(sources.get(sourceIndex)));
   player.prepare();player.play();scheduleStallCheck();
  }else{statusHint.setText("Немає робочого джерела: "+reason);}
 }
 private void scheduleStallCheck(){
  stallHandler.removeCallbacks(stallCheck);
  stallCheck=()->{if(player!=null&&!played&&player.getPlaybackState()!=Player.STATE_READY)tryNext("час очікування вичерпано");};
  stallHandler.postDelayed(stallCheck,12000);
 }
 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  String url=getIntent().getStringExtra("url");
  sources=getIntent().getStringArrayListExtra("urls");
  if(sources==null || sources.isEmpty()){sources=new java.util.ArrayList<>();sources.add(url);}
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
  statusHint=hint;
  setContentView(root);
  player=new ExoPlayer.Builder(this).build();
  view.setPlayer(player);
  player.addListener(new Player.Listener() {
   @Override public void onPlayerError(PlaybackException error) {
    runOnUiThread(() -> tryNext("код "+error.errorCode));
   }
   @Override public void onPlaybackStateChanged(int state) {
    if(state == Player.STATE_READY) runOnUiThread(() -> {played=true;stallHandler.removeCallbacks(stallCheck);hint.setText("Відтворення запущено");});
    if(state == Player.STATE_BUFFERING && !played) runOnUiThread(() -> scheduleStallCheck());
   }
  });
  player.setMediaItem(MediaItem.fromUri(sources.get(0)));
  player.prepare();
  player.play();
  scheduleStallCheck();
 }
 @Override public void onStop(){stallHandler.removeCallbacksAndMessages(null);super.onStop();if(player!=null){player.release();player=null;}}
}
