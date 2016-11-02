package com.jp.googledrive;

import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
/**
 * This class echoes a string called from JavaScript.
 */
public class GoogleDrive extends CordovaPlugin implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

	private GoogleApiClient mGoogleApiClient;
    private String displayName = null, filePath = "";
    private CallbackContext callbackContext;
    private String[] filePathArray;
	
    public boolean execute(String action, String filePath, CallbackContext callbackContext) throws JSONException {

        if (action.equals("uploadFile")) {

            this.filePathArray = filePath.split(",");
            this.callbackContext = callbackContext;

            googleApiClientSet();

            return true;
        }
        return false;
    }

	
	protected void googleApiClientSet() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(cordova.getActivity())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {

        // Called whenever the API client fails to connect.
        Log.i("TAG", "GoogleApiClient connection failed:" + result.toString());

        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(cordova.getActivity(), result.getErrorCode(), 0).show();
            return;
        }

        /**
         *  The failure has a resolution. Resolve it.
         *  Called typically when the app is not yet authorized, and an  authorization
         *  dialog is displayed to the user.
         */

        try {

            result.startResolutionForResult(cordova.getActivity(), 100);

        } catch (IntentSender.SendIntentException e) {

            Log.e("Tag","Exception while starting resolution activity&quot;, e");
        }
    }

    /**
     * It invoked when Google API client connected
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("TAG","GoogleApiClient connected");
        uploadFile();
    }


    public void uploadFile() {

        try {

            for (int i = 0; i < filePathArray.length; i++) {
                filePath = filePathArray[i];

                filePath = filePath.substring(1, filePath.length() - 1);
                Uri uri = Uri.parse(filePath);
                String uriString = uri.toString();

                if (uriString.startsWith("content://")) {
                    Cursor cursor = null;
                    try {
                        cursor = cordova.getActivity().getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            filePath = Uri.parse(cursor.getString(6)).getPath();
                            displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        }
                    } finally {
                        cursor.close();
                    }
                } else {
                    displayName = filePath.substring(filePath.lastIndexOf("/") + 1);
                }

                Drive.DriveApi.newDriveContents(mGoogleApiClient)
                        .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                            @Override
                            public void onResult(DriveApi.DriveContentsResult result) {
                                // If the operation was not successful, we cannot do anything
                                // and must
                                // fail.
                                if (!result.getStatus().isSuccess()) {
                                    Log.i("TAG", "Failed to create new contents.");
                                    return;
                                }

                                // Otherwise, we can write our data to the new contents.
                                Log.i("TAG", "New contents created.");
                                // Get an output stream for the contents.
                                OutputStream outputStream = result.getDriveContents().getOutputStream();
                                // Write the bitmap data from it.
                                File file = new File(filePath);
                                try {
                                    byte[] buffer = new byte[1024];
                                    FileInputStream fis = new FileInputStream(file);
                                    int read = 0;
                                    while ((read = fis.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, read);
                                        System.out.println("read " + read + " bytes,");
                                    }
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                    Log.i("TAG", "Unable to write file contents.");
                                }

                                // Create the initial metadata - MIME type and title.
                                // Note that the user will be able to change the title later.

                                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                        .setTitle(displayName).setMimeType("application/pdf").build();

                                // Create an intent for the file chooser, and start it.
                /*IntentSender intentSender = Drive.DriveApi
					.newCreateFileActivityBuilder()
					.setInitialMetadata(metadataChangeSet)
					.setInitialDriveContents(result.getDriveContents())
					.build(mGoogleApiClient);

				try {
                    cordova.getActivity().startIntentSenderForResult(intentSender, 300, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
					Log.i("TAG", "Failed to launch file chooser.");
				}*/


                                // Create a file in the root folder
                                Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                        .createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                                        .setResultCallback(fileCallback);
                            }
                        });
            }
            callbackContext.success("file successfully uploaded");
        }
        catch (Exception e) {
            callbackContext.error("something went wrong");
            e.printStackTrace();
        }
    }


    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e("upload", "Error while trying to create the file");
                        return;
                    }
                    Log.e("upload", "Created a file with content: " + result.getDriveFile().getDriveId());
                }
            };

    /**
     * It invoked when connection suspended
     * @param cause
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i("TAG","GoogleApiClient connection suspended");
    }


    @Override
    public void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        switch (requestCode) {

            case 100:

               /* if (resultCode == RESULT_OK) {
                    mFileId = (DriveId) data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    Log.e("file id", mFileId.getResourceId() + "");
                    String url = "https://drive.google.com/open?id="+ mFileId.getResourceId();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }*/

                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
