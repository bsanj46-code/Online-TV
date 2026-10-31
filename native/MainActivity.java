package ua.bashaiev.onlinetv;
import com.getcapacitor.BridgeActivity;
import android.os.Bundle;
public class MainActivity extends BridgeActivity {
 @Override public void onCreate(Bundle savedInstanceState) {
  registerPlugin(NativeTvPlugin.class);
  super.onCreate(savedInstanceState);
 }
}
