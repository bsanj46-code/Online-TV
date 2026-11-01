package ua.bashaiev.onlinetv;
import android.app.Activity;
import android.os.Bundle;
import android.content.res.Configuration;
import android.view.WindowManager;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.drawable.GradientDrawable;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import java.util.ArrayList;
import java.util.Arrays;
public class TvPlayerActivity extends Activity {
 private LibVLC vlc;
 private MediaPlayer player;
 private ArrayList<String> sources;
 private int index=0;
 private TextView hint;
 private TextView title;
 private Button fullscreenButton;
 private boolean manualFullscreen=false;
 private void applyOrientation(){
  boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
  boolean fullscreen=landscape||manualFullscreen;
  if(title!=null)title.setVisibility(fullscreen?View.GONE:View.VISIBLE);
  if(hint!=null)hint.setVisibility(landscape?View.GONE:View.VISIBLE);
  if(fullscreen){getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);}
  else{getWindow().getDecorView().setSystemUiVisibility(0);}
 }
 @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);applyOrientation();}

 private final Handler handler=new Handler(Looper.getMainLooper());
 private Runnable timeout;
 private boolean started=false;
 private boolean closing=false;
 private void scheduleTimeout(){
  if(timeout!=null)handler.removeCallbacks(timeout);
  timeout=()->{if(!started&&!closing)nextSource("час очікування вичерпано");};
  handler.postDelayed(timeout,15000);
 }
 private void nextSource(String reason){
  if(closing||player==null)return;
  if(timeout!=null)handler.removeCallbacks(timeout);
  player.stop();
  if(index+1>=sources.size()){hint.setText("Джерела недоступні: "+reason);return;}
  index++;
  playCurrent();
 }
 private void playCurrent(){
  if(closing||player==null)return;
  started=false;
  hint.setText("Підключення до джерела "+(index+1)+"/"+sources.size()+"…");
  Media media=new Media(vlc,Uri.parse(sources.get(index)));
  media.setHWDecoderEnabled(true,false);
  media.addOption(":network-caching=1500");
  player.setMedia(media);
  media.release();
  player.play();
  scheduleTimeout();
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  sources=getIntent().getStringArrayListExtra("urls");
  if(sources==null)sources=new ArrayList<>();
  String first=getIntent().getStringExtra("url");
  if(sources.isEmpty()&&first!=null)sources.add(first);
  if(sources.isEmpty()){finish();return;}
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.BLACK);
  title=new TextView(this);
  title.setText("‹  "+getIntent().getStringExtra("name"));
  title.setTextColor(Color.WHITE);
  title.setTextSize(20);
  title.setPadding(25,24,16,24);
  title.setOnClickListener(v->finish());
  root.addView(title);
  FrameLayout frame=new FrameLayout(this);
  SurfaceView surface=new SurfaceView(this);
  frame.addView(surface,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
  fullscreenButton=new Button(this);
  fullscreenButton.setText("⛶");
  fullscreenButton.setTextSize(28);
  fullscreenButton.setTextColor(Color.WHITE);
  fullscreenButton.setAllCaps(false);
  GradientDrawable buttonBackground=new GradientDrawable();
  buttonBackground.setColor(0xCC17243B);
  buttonBackground.setCornerRadius(14f*getResources().getDisplayMetrics().density);
  buttonBackground.setStroke((int)(1*getResources().getDisplayMetrics().density),0x99FFFFFF);
  fullscreenButton.setBackground(buttonBackground);
  fullscreenButton.setElevation(12f*getResources().getDisplayMetrics().density);
  fullscreenButton.setContentDescription("На весь екран");
  fullscreenButton.setOnClickListener(v->{manualFullscreen=!manualFullscreen;fullscreenButton.setText(manualFullscreen?"⤢":"⛶");applyOrientation();});
  int dp=(int)getResources().getDisplayMetrics().density;
  FrameLayout.LayoutParams fullParams=new FrameLayout.LayoutParams(56*dp,52*dp,Gravity.RIGHT|Gravity.BOTTOM);
  fullParams.setMargins(0,0,14*dp,18*dp);
  frame.addView(fullscreenButton,fullParams);
  root.addView(frame,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
  hint=new TextView(this);
  hint.setTextColor(Color.LTGRAY);
  hint.setGravity(Gravity.CENTER);
  hint.setPadding(16,16,16,32);
  root.addView(hint);
  setContentView(root);
  applyOrientation();
  vlc=new LibVLC(this,new ArrayList<>(Arrays.asList("--network-caching=1500","--http-user-agent=Mozilla/5.0")));
  player=new MediaPlayer(vlc);
  player.getVLCVout().setVideoView(surface);
  player.getVLCVout().attachViews();
  player.setEventListener(event->runOnUiThread(()->{
   if(closing)return;
   switch(event.type){
    case MediaPlayer.Event.Playing -> {started=true;if(timeout!=null)handler.removeCallbacks(timeout);hint.setText("Прямий ефір");}
    case MediaPlayer.Event.EncounteredError -> nextSource("помилка потоку");
    case MediaPlayer.Event.EndReached -> nextSource("трансляція завершилась");
    case MediaPlayer.Event.Buffering -> {if(!started)hint.setText("Завантаження "+Math.round(event.getBuffering())+"%");}
   }
  }));
  playCurrent();
 }
 @Override protected void onDestroy(){
  closing=true;
  handler.removeCallbacksAndMessages(null);
  if(player!=null){player.setEventListener(null);player.stop();player.getVLCVout().detachViews();player.release();player=null;}
  if(vlc!=null){vlc.release();vlc=null;}
  super.onDestroy();
 }
}