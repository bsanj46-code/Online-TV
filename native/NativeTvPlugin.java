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
    JSONArray channels=new JSONArray();
    java.util.HashSet<String> seen=new java.util.HashSet<>();
    String[] sources=new String[]{
      "https://iptv.org.ua/iptv/ua.m3u",
      "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/ua.m3u"
    };
    for(String source:sources){
     HttpURLConnection conn=null;
     try{
      conn=(HttpURLConnection)new URL(source).openConnection();
      conn.setConnectTimeout(12000);conn.setReadTimeout(18000);
      conn.setRequestProperty("User-Agent","Mozilla/5.0 OnlineTV/1.0");
      if(conn.getResponseCode()!=200)continue;
      try(BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))){
       String line,name=null;
       while((line=reader.readLine())!=null&&channels.length()<700){
        line=line.trim();
        if(line.startsWith("#EXTINF:")){
         int pos=line.lastIndexOf(',');
         name=pos>=0?line.substring(pos+1).trim():null;
        }else if(name!=null&&(line.startsWith("http://")||line.startsWith("https://"))){
         if(seen.add(name+"|"+line)){
          JSONObject item=new JSONObject();item.put("name",name);item.put("url",line);channels.put(item);
         }
         name=null;
        }
       }
      }
     }catch(Exception ignored){}finally{if(conn!=null)conn.disconnect();}
    }
    // Add published channel URLs from the Free-TV/IPTV catalogue.
    HttpURLConnection freeTv=null;
    try{
     freeTv=(HttpURLConnection)new URL("https://raw.githubusercontent.com/Free-TV/IPTV/master/lists/ukraine.md").openConnection();
     freeTv.setConnectTimeout(12000);freeTv.setReadTimeout(18000);
     if(freeTv.getResponseCode()==200){
      try(BufferedReader reader=new BufferedReader(new InputStreamReader(freeTv.getInputStream(),"UTF-8"))){
       String line;
       while((line=reader.readLine())!=null&&channels.length()<900){
        if(!line.startsWith("|"))continue;
        String[] cells=line.split(java.util.regex.Pattern.quote("|"));
        if(cells.length<4 || !cells[1].trim().matches("[0-9]+"))continue;
        String name=cells[2].trim().replace("Ⓢ","").trim();
        String cell=cells[3];
        int begin=cell.indexOf("](http");
        if(!name.isEmpty()&&begin>=0){
         int finish=cell.indexOf(")",begin+2);
         if(finish<0)continue;
         String url=cell.substring(begin+2,finish);
         if(!url.contains(".m3u8"))continue;
         if(seen.add(name+"|"+url)){JSONObject item=new JSONObject();item.put("name",name);item.put("url",url);channels.put(item);}
        }
       }
      }
     }
    }catch(Exception ignored){}finally{if(freeTv!=null)freeTv.disconnect();}
    if(channels.length()==0)throw new Exception("Жодне джерело недоступне");
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
  org.json.JSONArray alternatives=call.getArray("urls");
  if(alternatives!=null){java.util.ArrayList<String> candidates=new java.util.ArrayList<>();for(int i=0;i<alternatives.length();i++){String candidate=alternatives.optString(i,"");if(candidate.startsWith("https://")||candidate.startsWith("http://"))candidates.add(candidate);}intent.putStringArrayListExtra("urls",candidates);}
  intent.putExtra("name",call.getString("name","Онлайн ТВ"));
  getActivity().startActivity(intent);
  call.resolve();
 }
}
