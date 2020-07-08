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
import java.io.File;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import java.lang.reflect.InvocationTargetException;
import org.apache.cordova.PluginManager;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import org.apache.cordova.file.FileUtils;
import org.apache.cordova.file.LocalFilesystemURL;

public class MyCordovaPlugin extends CordovaPlugin {
  private static final String TAG = "MyCordovaPlugin";
  private CallbackContext callback = null;
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.d(TAG, "Initializing MyCordovaPlugin");
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if(action.equals("openCamera")) {
      Intent intent= new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
      intent.putExtra("android.intent.extra.durationLimit",args.getInt(0));
      cordova.startActivityForResult((CordovaPlugin)this, intent, 2 * 16 + 1 + 1);
      PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
      pluginResult.setKeepCallback(true);
      callback = callbackContext;
      callbackContext.sendPluginResult(pluginResult);
      return true;
    }
    return false;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) 
  {    
    this.cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        String urlPath="URI:"+data.getData().toString();
        try{
          File fp = webView.getResourceApi().mapUriToFile(data.getData());
          PluginManager pm = null;
          Class webViewClass = webView.getClass();
          try {
              Method gpm = webViewClass.getMethod("getPluginManager");
              pm = (PluginManager) gpm.invoke(webView);
          } catch (NoSuchMethodException e) {
          } catch (IllegalAccessException e) {
          } catch (InvocationTargetException e) {
          }
          if (pm == null) {
            try {
                Field pmf = webViewClass.getField("pluginManager");
                pm = (PluginManager)pmf.get(webView);
            } catch (NoSuchFieldException e) {
            } catch (IllegalAccessException e) {
            }
          }
          try {
            FileUtils filePlugin = (FileUtils) pm.getPlugin("File");
            LocalFilesystemURL url = filePlugin.filesystemURLforLocalPath(fp.getAbsolutePath());
            urlPath=url.toString();
          } catch (Exception e) {
            urlPath=e.getMessage();
          }
        }catch(Exception e){
          urlPath=e.getMessage();
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK, urlPath);
        result.setKeepCallback(true);
        callback.sendPluginResult(result);
      }
    });
  }

}