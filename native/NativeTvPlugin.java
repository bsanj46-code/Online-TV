package ua.bashaiev.onlinetv;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;
import android.content.Intent;
@CapacitorPlugin(name = "NativeTv")
public class NativeTvPlugin extends Plugin {
 @PluginMethod public void play(PluginCall call) {
  String url=call.getString("url");
  if(url==null || !(url.startsWith("https://") || url.startsWith("http://"))) {call.reject("Invalid URL");return;}
  Intent intent=new Intent(getContext(),TvPlayerActivity.class);
  intent.putExtra("url",url);
  intent.putExtra("name",call.getString("name","Онлайн ТВ"));
  getActivity().startActivity(intent);
  call.resolve();
 }
}
