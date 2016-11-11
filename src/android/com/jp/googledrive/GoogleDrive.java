package com.jp.googledrive;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.android.gms.drive.metadata.CustomPropertyKey;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class echoes a string called from JavaScript.
 */
public class GoogleDrive extends CordovaPlugin implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

	private GoogleApiClient mGoogleApiClient;
    private String displayName = null, filePath = "";
    private CallbackContext callbackContext;
    private String[] filePathArray;
    private DriveId mSelectedFileDriveId;
    private String action;
    Activity activity;
	
    public boolean execute(String action, String filePath, CallbackContext callbackContext) throws JSONException {

        this.action = action;
        this.callbackContext = callbackContext;

        if (action.equals("uploadFile")) {

            this.filePathArray = filePath.split(",");
            Log.i("TAG", filePath);
            googleApiClientSet(cordova.getActivity());
            return true;
        }
        else if (action.equals("downloadFile")) {
            googleApiClientSet(cordova.getActivity());
            return true;
        }


        return false;
    }

	
	protected void googleApiClientSet(Activity activity) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
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
        if(action.equals("uploadFile"))
        {
            uploadFile(0);
        }
        else if(action.equals("downloadFile")) {
            openDrive();
        }
        else if(action.equals("driveDownload"))
        {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground( Void... voids ) {
                    DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient,mSelectedFileDriveId);
                    DriveResource.MetadataResult mdRslt = file.getMetadata(mGoogleApiClient).await();
                    if (mdRslt != null && mdRslt.getStatus().isSuccess()) {
                        String link = mdRslt.getMetadata().getWebContentLink();
                        Log.d("LINK", link);

                        try {
                            URL url1 = new URL(link);
                            URLConnection conection = url1.openConnection();
                            conection.connect();


                            File dir = new File(Environment.getExternalStorageDirectory(), "EQWaybill");
                            if(!dir.exists()) {
                                dir.mkdir();
                            }

                            File file1 = new File(dir, mdRslt.getMetadata().getOriginalFilename());

                            BufferedInputStream in = null;
                            FileOutputStream fout = null;
                            try {
                                in = new BufferedInputStream(url1.openStream());
                                fout = new FileOutputStream(file1);


                                byte[] buffer = new byte[1024];
                                int len = 0;
                                while ((len = in.read(buffer)) > 0) {
                                    fout.write(buffer, 0, len );
                                }
                                fout.flush();

                            } finally {
                                if (in != null) {
                                    in.close();
                                }
                                if (fout != null) {
                                    fout.close();
                                }
                            }


                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    onStop();
                }
            }.execute();


        }

    }


    public void uploadFile(int id) {

        try {
            Log.d("TAG", ""+id);

            final int elementID = id;

            if(elementID < filePathArray.length) {
                filePath = filePathArray[elementID];
                filePath = filePath.replace('"', ' ');
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

                                File dir = new File(Environment.getExternalStorageDirectory(), "EQWaybill");
                                if(!dir.exists()) {
                                    dir.mkdir();
                                }
                                File file = new File(dir, displayName);

                                Log.i("TAG", ""+file);
                                try {
                                    byte[] buffer = new byte[1024];
                                    FileInputStream fis = new FileInputStream(file);
                                    int read = 0;
                                    while ((read = fis.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, read);
                                        System.out.println("read " + read + " bytes,");
                                    }

                                    CustomPropertyKey approvalPropertyKey = new CustomPropertyKey("approved", CustomPropertyKey.PUBLIC);
                                    MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                            .setTitle(displayName).setMimeType("application/pdf")
                                            .setCustomProperty(approvalPropertyKey, "yes").build();

                                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                            .createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                                            .setResultCallback(fileCallback);

                                    int val = elementID + 1;
                                    uploadFile(val);
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                    Log.i("TAG", "Unable to write file contents.");
                                    onStop();
                                }

                            }
                        });

            }
            else {
                onStop();
            }

            callbackContext.success("file successfully uploaded");
        }
        catch (Exception e) {
            onStop();
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



    public void openDrive() {
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{ DriveFolder.MIME_TYPE, "application/pdf" })
                .build(mGoogleApiClient);
        try {
            cordova.getActivity().startIntentSenderForResult(intentSender, 1, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w("TAG", "Unable to send intent", e);
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (requestCode == 111) {

        /*} else {
            super.onActivityResult(requestCode, resultCode, data);
        }*/
    }


    public void parseDownloadData(Intent data, Activity activity, String action) {
        try {
            Log.e("aaaaa", "aaaaa");
            this.action = action;
            this.activity = activity;
            if(data!=null) {
                mSelectedFileDriveId = (DriveId) data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                Log.e("file id", mSelectedFileDriveId.getResourceId() + "");
                String url = "https://drive.google.com/open?id="+ mSelectedFileDriveId.getResourceId();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                activity.startActivity(i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }




    /**
     * It invoked when connection suspended
     * @param cause
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i("TAG","GoogleApiClient connection suspended");
    }


}
