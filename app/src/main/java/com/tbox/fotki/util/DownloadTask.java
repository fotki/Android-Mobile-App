package com.tbox.fotki.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.tbox.fotki.BuildConfig;
import com.tbox.fotki.refactoring.service.PhotoUploader;
import com.tbox.fotki.util.compression.Compressor;
import com.tbox.fotki.util.sync_files.PreferenceHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Junaid on 8/31/17.
 */

public class DownloadTask {
    private static DownloadTask sDownload;
    public Context mContext;
    public String url;
    public String originalUrl = "";
    public boolean isCompression = false;
    public String mFiletype = "";
    public String mFolder = "";
    private String filename = "";
    private int threshold = 1400;
    public boolean isCompressionFailed = false;
    private DownloadAsychTask downloadAsychTask = null;

    public ActionCall finishAction = null;

    public interface ActionCall{
        void call(String type);
    }

    public static DownloadTask getInstance() {
        if (sDownload == null) {
            sDownload = getSession();
        }
        return sDownload;
    }

    private static DownloadTask getSession() {
        return new DownloadTask();
    }

    public void startDownloadTask() {
        //if(downloadAsychTask==null){
        downloadAsychTask = new DownloadAsychTask(mContext);
        //}
        downloadAsychTask.execute(url,originalUrl);
        Log.d("shareError","DownloadTask startDownloadTask");
    }

    public void stopDownloadTask(){
        if(downloadAsychTask!=null){
            downloadAsychTask.cancel(true);
        }
    }

    private class DownloadAsychTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadAsychTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            L.INSTANCE.print(this,"doInBackground");
            try {
                URL url = new URL(sUrl[0]);

                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                String contentType[] = url.getFile().split("\\.");
                String urlExtension = contentType[contentType.length - 1];

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                if(mFolder.equals("FotkiShare")) {
                    removeOldFiles();
                }
                File file = null;
                // download the file

                if(sUrl[1].equals("")){
                    filename = url.getFile().split("/")[contentType.length - 1];
                } else {
                    filename = sUrl[1];
                    L.INSTANCE.print(this,"title - "+filename);
                }

                if (urlExtension.equals("gif")) {
                    mFiletype = "gif";
                    filename = filename + ".gif";
                } else if (urlExtension.equals("mp4")) {
                    mFiletype = "mp4";
                    filename = filename + ".mp4";
                } else {
                    mFiletype = "jpg";
                    filename = filename + ".jpg";
                }

                /*if(mFolder.equals("")) {
                    File fotkiDir = null;
                    fotkiDir = new File(android.os.Environment.getExternalStorageDirectory()
                            + java.io.File.separator + "Fotki");
                    fotkiDir.mkdirs();
                    file = new File(fotkiDir, filename);
                } else {
                    file = new File(mFolder, filename);
                }*/

                File fotkiDir = null;
                fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS ),
                        java.io.File.separator + mFolder
                );
                fotkiDir.mkdirs();
                file = new File(fotkiDir, filename);

                L.INSTANCE.print(this,"Final path - "+file.getAbsolutePath());

                new PreferenceHelper(context).getSp().edit()
                        .putString("shared_file_name",file.getAbsolutePath()).apply();
                input = connection.getInputStream();
                output = new FileOutputStream(file);

                byte data[] = new byte[4096];
                //byte data[] = new byte[16096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1 && !isCancelled()) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                    {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }

            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return "";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            Intent intent = new Intent("sharing_progress");
            intent.putExtra("downloading_progress", progress[0]);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }



        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            L.INSTANCE.print(this,"onPostExecute");
            if(mFolder.equals("FotkiShare")) {
                if (result == null) {
                    Intent intent = new Intent("sharing_file_downloading_error");
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                    Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
                } else {
                    if (isCompression) {
                        if (mFiletype.equals("jpg")) {
                            removeOldResizeFile();
                            File fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS ),
                                            java.io.File.separator + "FotkiResize"
                            );
//                            File fotkiDir = new File(android.os.Environment.getExternalStorageDirectory()
//                                    + java.io.File.separator + "FotkiResize");
                            fotkiDir.mkdirs();
                            File file = new File(fotkiDir, filename);
                            new PreferenceHelper(context).getSp().edit()
                                    .putString("shared_file_name",file.getAbsolutePath()).apply();
                            imageCompression();;
                            PhotoUploader.Companion.refreshGallery(new File(fotkiDir, filename),context);
                            Intent intent = new Intent(Constants.SHARING_FILE_DOWNLOADED_SHARE);
                            finishAction.call(PhotoUploader.TYPE_FOTKI_RESIZE);
                            //LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                        }
                    } else {
                        Intent intent = new Intent(Constants.SHARING_FILE_DOWNLOADED_SHARE);
                        File fotkiDir = null;
                        fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS ),
                                java.io.File.separator + "FotkiShare"
                        );
//                        fotkiDir = new File(android.os.Environment.getExternalStorageDirectory()
//                                + java.io.File.separator + "FotkiShare");
                        PhotoUploader.Companion.refreshGallery(new File(fotkiDir, filename),context);
                        //LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                        finishAction.call(PhotoUploader.TYPE_FOTKI_USUAL);
                    }
                }
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent("sharing_file_downloading_error");
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                L.INSTANCE.print(this,"Downloaded - "+filename);
                File fotkiDir = null;
                fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS ),
                        java.io.File.separator + "Fotki"
                );
//                fotkiDir = new File(android.os.Environment.getExternalStorageDirectory()
//                        + java.io.File.separator + "Fotki");
                PhotoUploader.Companion.refreshGallery(new File(fotkiDir, filename),context);
                mFolder = "";
            }
        }

        private void removeOldFiles() {
            File fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS ),
                    java.io.File.separator + "FotkiShare"
            );
//            File fotkiDir = new File(android.os.Environment.getExternalStorageDirectory()
//                    + java.io.File.separator + "FotkiShare");
            if (fotkiDir.isDirectory()) {
                String[] children = fotkiDir.list();
                for (String aChildren : children) new File(fotkiDir, aChildren).delete();
            }

        }

        private void removeOldResizeFile() {

            File fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS ),
                    java.io.File.separator + "FotkiResize"
            );
//            File fotkiDir = new File(android.os.Environment.getExternalStorageDirectory()
//                    + java.io.File.separator + "FotkiResize");
            if (fotkiDir.isDirectory()) {
                String[] children = fotkiDir.list();
                for (String aChildren : children) new File(fotkiDir, aChildren).delete();
            }
        }
    }

    private void imageCompression() {
        File fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS ),
                java.io.File.separator + "FotkiShare"
        );
//        File fotkiDir = new File(android.os.Environment.getExternalStorageDirectory()
//                + java.io.File.separator + "FotkiShare");
        for (File sourceFile : fotkiDir.listFiles()) {
            if (sourceFile.isFile()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath());
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int max = width;
                    if (max > height) {
                        max = width;
                    } else {
                        max = height;
                    }
                    if (max > threshold) {
                        float proportion = getProportion(max);
                        proportion = proportion / 100;
                        float percentWidth = proportion * width;
                        float percentHeight = proportion * height;
                        width = width - (int) percentWidth;
                        height = height - (int) percentHeight;
                        // Compress image in main thread using custom Compressor
//                        File fotkiDir = new File(Environment.getExternalStoragePublicDirectory(
//                                Environment.DIRECTORY_DOWNLOADS ),
//                                java.io.File.separator + "FotkiResize"
//                        );
                        new Compressor(mContext)
                                .setMaxWidth(width)
                                .setMaxHeight(height)
                                .setQuality(75)
                                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS )+
                                        java.io.File.separator + "FotkiResize"
                                )
                                .compressToFile(sourceFile);
                        isCompressionFailed = false;
                    }else{
                        isCompressionFailed = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    isCompressionFailed = true;
                }
            }
        }
    }

    private float getProportion(int max) {
        float proportion = 1400.0f / max;
        proportion = proportion * 100;
        proportion = 100 - proportion;
        return proportion;
    }

    @Override
    public String toString() {
        return "DownloadTask{" +
                "url='" + url + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", isCompression=" + isCompression +
                ", mFiletype='" + mFiletype + '\'' +
                ", mFolder='" + mFolder + '\'' +
                ", filename='" + filename + '\'' +
                ", threshold=" + threshold +
                ", isCompressionFailed=" + isCompressionFailed +
                '}';
    }
}

