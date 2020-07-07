package com.example;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.Intent; 
import android.provider.MediaStore; 
import android.util.Log;
import org.apache.cordova.CordovaPlugin;
import java.util.Date;

public class MyCordovaPlugin extends CordovaPlugin {
  private static final String TAG = "MyCordovaPlugin";
  private CallbackContext callback = null;
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.d(TAG, "Initializing MyCordovaPlugin");
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if(action.equals("openCamera")) {
      Intent camera_intent= new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
      this.cordova.startActivityForResult((CordovaPlugin) this, camera_intent, 2 * 16 + 1 + 1);
      PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
      pluginResult.setKeepCallback(true);
      this.callback = callbackContext;
      callbackContext.sendPluginResult(pluginResult);
      return true;
    }
    return false;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) 
  {
    PluginResult result = new PluginResult(PluginResult.Status.OK, data.getData().toString());
    result.setKeepCallback(true);
    callback.sendPluginResult(result);
  }

}