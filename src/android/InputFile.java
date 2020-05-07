package org.apache.cordova.inputFile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.Exception;

import org.apache.cordova.BuildHelper;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import android.support.v4.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageManager;
import android.content.Context;
import android.util.Log;
import java.io.File;


public class InputFile extends CordovaPlugin {
	private static final String ACTION_OPEN = "getFile";
	private static final int PICK_FILE_REQUEST = 1;
	private static final String TAG = "InputFile";
	private String captureFileName;
	private Uri captureUri = null;

	public static byte[] getBytesFromInputStream (InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[0xFFFF];

		for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
			os.write(buffer, 0, len);
		}

		return os.toByteArray();
	}


	public static String getDisplayName (ContentResolver contentResolver, Uri uri) {
		String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
		Cursor metaCursor = contentResolver.query(uri, projection, null, null, null);

		if (metaCursor != null) {
			try {
				if (metaCursor.moveToFirst()) {
					return metaCursor.getString(0);
				}
			} finally {
				metaCursor.close();
			}
		}

		return "File";
	}


	private CallbackContext callback;

	public void chooseFile (CallbackContext callbackContext, String accept, Boolean includeData) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/* application/pdf");
		if(accept != null && !(accept.trim().equals("") || accept.trim().equals("*/*")))
			intent.putExtra(Intent.EXTRA_MIME_TYPES, accept.split(","));

		Intent chooser = Intent.createChooser(intent, null); 	
		
		// Image from camera intent
		Uri tempUri = null;
		Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
			.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		Context context = this.cordova.getActivity().getApplicationContext();
	    	if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) && captureIntent.resolveActivity(context.getPackageManager()) != null) {
			try {
			    captureFileName = System.currentTimeMillis() + ".jpg";
			    File tempFile = new File(context.getFilesDir(), captureFileName);
			    Log.d(TAG, "Temporary photo capture file: " + tempFile);
			    tempUri = FileProvider.getUriForFile(context, context.getPackageName() + ".chooser.provider", tempFile);
			    Log.d(TAG, "Temporary photo capture URI: " + tempUri);
			    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
			} catch (Exception e) {
			    Log.e(TAG, "Unable to create temporary file for photo capture", e);
			    captureIntent = null;
			}
		} else {
			Log.w(TAG, "Device does not support photo capture");
			captureIntent = null;
		}
		
		captureUri = tempUri;

		// Chooser intent
		if (captureIntent != null) {
		    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { captureIntent });
		}
		
		cordova.startActivityForResult(this, chooser, Chooser.PICK_FILE_REQUEST);

		PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
		pluginResult.setKeepCallback(true);
		this.callback = callbackContext;
		callbackContext.sendPluginResult(pluginResult);
	}

	@Override
	public boolean execute (
		String action,
		JSONArray args,
		CallbackContext callbackContext
	) {
		try {
			if (action.equals(InputFile.ACTION_OPEN)) {
				this.chooseFile(callbackContext, args.getString(0), args.getBoolean(1));
				return true;
			}
		}
		catch (JSONException err) {
			this.callback.error("Execute failed: " + err.toString());
		}

		return false;
	}

	@Override
	public void onActivityResult (int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "results");
		try {
			if (requestCode == InputFile.PICK_FILE_REQUEST && this.callback != null) {
				if (resultCode == Activity.RESULT_OK) {
					Uri uri = null;
					
					if(data != null){
						uri = data.getData();
						Log.d(TAG, "FROM FILE MANAGER URI = " + uri);
					}else{
						uri = captureUri;
						Log.d(TAG, "FROM CAMERA URI = " + uri);
					}

					if (uri != null) {
						ContentResolver contentResolver =
							this.cordova.getActivity().getContentResolver()
						;

						String name = InputFile.getDisplayName(contentResolver, uri);
						
						Log.d(TAG, "NAME = " + name);

						String mediaType = contentResolver.getType(uri);
						Log.d(TAG, "MEDIATYPE = " + mediaType);
						if (mediaType == null || mediaType.isEmpty()) {
							mediaType = "application/octet-stream";
						}

						String base64 = "";

						byte[] bytes = InputFile.getBytesFromInputStream(
							contentResolver.openInputStream(uri)
						);

						base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
						
						Context context = this.cordova.getActivity().getApplicationContext();
						File captureFile = new File(context.getFilesDir(), captureFileName);
						if(captureFile.exists())
							captureFile.delete();

						JSONObject result = new JSONObject();

						result.put("data", base64);
						result.put("mediaType", mediaType);
						result.put("name", name);
						result.put("uri", uri.toString());

						this.callback.success(result.toString());
					}
					else {
						this.callback.error("File URI was null.");
					}
				}
				else if (resultCode == Activity.RESULT_CANCELED) {
					this.callback.success("RESULT_CANCELED");
				}
				else {
					this.callback.error(resultCode);
				}
			}
		}
		catch (Exception err) {
			this.callback.error("Failed to read file: " + err.toString());
		}
	}
}
