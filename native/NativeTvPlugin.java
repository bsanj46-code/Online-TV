package ua.bashaiev.onlinetv;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;
import android.content.Intent;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;
@CapacitorPlugin(name = "NativeTv")
public class NativeTvPlugin extends Plugin {
 @PluginMethod public void playlist(PluginCall call) {
  new Thread(() -> {
   HttpURLConnection connection=null;
   try {
    connection=(HttpURLConnection)new URL("https://iptv.org.ua/iptv/ua.m3u").openConnection();
    connection.setConnectTimeout(12000);
    connection.setReadTimeout(18000);
    connection.setRequestProperty("User-Agent","Mozilla/5.0 OnlineTV/1.0");
    if(connection.getResponseCode()!=200) throw new Exception("HTTP "+connection.getResponseCode());
    JSONArray channels=new JSONArray();
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"))) {
     String line,name=null;
     while((line=reader.readLine())!=null && channels.length()<500) {
      line=line.trim();
      if(line.startsWith("#EXTINF:")) {
       int pos=line.lastIndexOf(',');
       name=pos>=0?line.substring(pos+1).trim():null;
      } else if(name!=null && (line.startsWith("http://")||line.startsWith("https://"))) {
       JSONObject item=new JSONObject();
       item.put("name",name);item.put("url",line);
       channels.put(item);name=null;
      }
     }
    }
    com.getcapacitor.JSObject result=new com.getcapacitor.JSObject();
    result.put("channels",channels);
    call.resolve(result);
   } catch(Exception e) {call.reject("Не вдалося завантажити плейлист: "+e.getMessage());}
   finally {if(connection!=null) connection.disconnect();}
  }).start();
 }
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
