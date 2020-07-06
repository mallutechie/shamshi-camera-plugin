/**
 */
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
import android.util.Log;
import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Base64;

import org.apache.cordova.BuildHelper;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyCordovaPlugin extends CordovaPlugin {
  private static final String TAG = "MyCordovaPlugin";

  @Override
public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    // your init code here
}
private static final int DATA_URL = 0;              // Return base64 encoded string
    private static final int FILE_URI = 1;              // Return file uri (content://media/external/images/media/2 for Android)
    private static final int NATIVE_URI = 2;                    // On Android, this is the same as FILE_URI

    private static final int PHOTOLIBRARY = 0;          // Choose image from picture library (same as SAVEDPHOTOALBUM for Android)
    private static final int CAMERA = 1;                // Take picture from camera
    private static final int SAVEDPHOTOALBUM = 2;       // Choose image from picture library (same as PHOTOLIBRARY for Android)

    private static final int PICTURE = 0;               // allow selection of still pictures only. DEFAULT. Will return format specified via DestinationType
    private static final int VIDEO = 1;                 // allow selection of video only, ONLY RETURNS URL
    private static final int ALLMEDIA = 2;              // allow selection from all media types

    private static final int JPEG = 0;                  // Take a picture of type JPEG
    private static final int PNG = 1;                   // Take a picture of type PNG
    private static final String JPEG_TYPE = "jpg";
    private static final String PNG_TYPE = "png";
    private static final String JPEG_EXTENSION = "." + JPEG_TYPE;
    private static final String PNG_EXTENSION = "." + PNG_TYPE;
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String GET_PICTURE = "Get Picture";
    private static final String GET_VIDEO = "Get Video";
    private static final String GET_All = "Get All";
    private static final String CROPPED_URI_KEY = "croppedUri";
    private static final String IMAGE_URI_KEY = "imageUri";

    private static final String TAKE_PICTURE_ACTION = "takePicture";

    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final int TAKE_PIC_SEC = 0;
    public static final int SAVE_TO_ALBUM_SEC = 1;

    private static final String LOG_TAG = "CameraLauncher";

    //Where did this come from?
    private static final int CROP_CAMERA = 100;

    private static final String TIME_FORMAT = "yyyyMMdd_HHmmss";

    private int mQuality;                   // Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
    private int targetWidth;                // desired width of the image
    private int targetHeight;               // desired height of the image
    private CordovaUri imageUri;            // Uri of captured image
    private int encodingType;               // Type of encoding to use
    private int mediaType;                  // What type of media to retrieve
    private int destType;                   // Source type (needs to be saved for the permission handling)
    private int srcType;                    // Destination type (needs to be saved for permission handling)
    private boolean saveToPhotoAlbum;       // Should the picture be saved to the device's photo album
    private boolean correctOrientation;     // Should the pictures orientation be corrected
    private boolean orientationCorrected;   // Has the picture's orientation been corrected
    private boolean allowEdit;              // Should we allow the user to crop the image.

    protected final static String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    public CallbackContext callbackContext;
    private int numPics;

    private MediaScannerConnection conn;    // Used to update gallery app with newly-written files
    private Uri scanMe;                     // Uri of image to be added to content store
    private Uri croppedUri;
    private ExifHelper exifData;            // Exif data from source
    private String applicationId;
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Log.d(TAG,action);
    if(action.equals("echo")) {
      String phrase = args.getString(0);
      // Echo back the first argument
      Log.d(TAG, phrase);
    } else if(action.equals("getDate")) {
      // An example of returning data back to the web layer
      final PluginResult result = new PluginResult(PluginResult.Status.OK, (new Date()).toString());
      callbackContext.sendPluginResult(result);
    } else if(action.equals("openCamera")){
      try{
        this.callTakePicture(1,0);
        final PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        callbackContext.sendPluginResult(result);
      }catch(Exception e){
        final PluginResult result = new PluginResult(PluginResult.Status.OK, e.getMessage());
        callbackContext.sendPluginResult(result);
      }
    }
    return true;
  }

  public void callTakePicture(int returnType, int encodingType) {
    boolean saveAlbumPermission = PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            && PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    boolean takePicturePermission = PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);

    // CB-10120: The CAMERA permission does not need to be requested unless it is declared
    // in AndroidManifest.xml. This plugin does not declare it, but others may and so we must
    // check the package info to determine if the permission is present.

    if (!takePicturePermission) {
        takePicturePermission = true;
        try {
            PackageManager packageManager = this.cordova.getActivity().getPackageManager();
            String[] permissionsInPackage = packageManager.getPackageInfo(this.cordova.getActivity().getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (permissionsInPackage != null) {
                for (String permission : permissionsInPackage) {
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        takePicturePermission = false;
                        break;
                    }
                }
            }
        } catch (NameNotFoundException e) {
            // We are requesting the info for our package, so this should
            // never be caught
        }
    }

    if (takePicturePermission && saveAlbumPermission) {
        takePicture(returnType, encodingType);
    } else if (saveAlbumPermission && !takePicturePermission) {
        PermissionHelper.requestPermission(this, TAKE_PIC_SEC, Manifest.permission.CAMERA);
    } else if (!saveAlbumPermission && takePicturePermission) {
        PermissionHelper.requestPermissions(this, TAKE_PIC_SEC,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
    } else {
        PermissionHelper.requestPermissions(this, TAKE_PIC_SEC, permissions);
    }
}

public void takePicture(int returnType, int encodingType)
{
    // Save the number of images currently on disk for later
    this.numPics = queryImgDB(whichContentStore()).getCount();

    // Let's use the intent and see what happens
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    // Specify file so that large image is captured and returned
    File photo = createCaptureFile(encodingType);
    this.imageUri = new CordovaUri(FileProvider.getUriForFile(cordova.getActivity(),
            applicationId + ".provider",
            photo));
    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri.getCorrectUri());
    //We can write to this URI, this will hopefully allow us to write files to get to the next step
    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

    if (this.cordova != null) {
        // Let's check to make sure the camera is actually installed. (Legacy Nexus 7 code)
        PackageManager mPm = this.cordova.getActivity().getPackageManager();
        if(intent.resolveActivity(mPm) != null)
        {
            this.cordova.startActivityForResult((CordovaPlugin) this, intent, (CAMERA + 1) * 16 + returnType + 1);
        }
        else
        {
            LOG.d(LOG_TAG, "Error: You don't have a default camera.  Your device may not be CTS complaint.");
        }
    }
//        else
//            LOG.d(LOG_TAG, "ERROR: You must use the CordovaInterface for this to work correctly. Please implement it in your activity");
}

private Uri whichContentStore() {
  if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
  } else {
      return MediaStore.Images.Media.INTERNAL_CONTENT_URI;
  }
}

/**
     * Creates a cursor that can be used to determine how many images we have.
     *
     * @return a cursor
     */
    private Cursor queryImgDB(Uri contentStore) {
      return this.cordova.getActivity().getContentResolver().query(
              contentStore,
              new String[]{MediaStore.Images.Media._ID},
              null,
              null,
              null);
  }

  private File createCaptureFile(int encodingType) {
    return createCaptureFile(encodingType, "");
}

/**
 * Create a file in the applications temporary directory based upon the supplied encoding.
 *
 * @param encodingType of the image to be taken
 * @param fileName or resultant File object.
 * @return a File object pointing to the temporary picture
 */
private File createCaptureFile(int encodingType, String fileName) {
    if (fileName.isEmpty()) {
        fileName = ".Pic";
    }

    if (encodingType == JPEG) {
        fileName = fileName + JPEG_EXTENSION;
    } else if (encodingType == PNG) {
        fileName = fileName + PNG_EXTENSION;
    } else {
        throw new IllegalArgumentException("Invalid Encoding Type: " + encodingType);
    }

    return new File(getTempDirectoryPath(), fileName);
}

private String getTempDirectoryPath() {
  File cache = cordova.getActivity().getCacheDir();
  // Create the cache directory if it doesn't exist
  cache.mkdirs();
  return cache.getAbsolutePath();
}

}
