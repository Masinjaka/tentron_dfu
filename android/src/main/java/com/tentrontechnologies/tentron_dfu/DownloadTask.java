package com.tentrontechnologies.tentron_dfu;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadTask extends AsyncTask<Uri, Integer, ByteArrayOutputStream> {


    private final WeakReference<Context> tWeakReference;
    private Listener listener;
    private PowerManager.WakeLock twakeLock;
    private Uri tUri;

    DownloadTask(@NonNull WeakReference<Context> weakReference,@NonNull Listener listener){
        this.tWeakReference = weakReference;
        this.listener = listener;
    }

    public void setListener(@Nullable Listener listener){
        this.listener = listener;
    }

    @Override
    protected ByteArrayOutputStream doInBackground(Uri... uris) {
        InputStream input = null;
        ByteArrayOutputStream output = null;
        HttpURLConnection connection = null;

        tUri = uris[0];
        int fileLength = 0;
        try {
            URL url = new URL(tUri.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                return null;
            }

            fileLength = connection.getContentLength();

            // Here's where the download process is happening
            input = connection.getInputStream();

            if(input != null){
                output = new ByteArrayOutputStream();

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while((count = input.read(data)) != -1){
                    if(isCancelled()){
                        input.close();
                        return null;
                    }

                    total += count;

                    if(fileLength > 0){
                        publishProgress((int) (total*100/fileLength));
                    }

                    output.write(data,0,count);
                }
            }else{
                System.out.println("Null input in downloadTask");
            }

        } catch (Exception e) {
            return null;
        } finally {
            try {
                if(output != null){
                    output.close();
                }
                if(input != null){
                    input.close();
                }

            } catch (IOException e) { }

            if(connection != null){
                connection.disconnect();
            }
        }

        return output;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        PowerManager powerManager = (PowerManager) tWeakReference.get().getSystemService(Context.POWER_SERVICE);
        if(powerManager != null){
            twakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,getClass().getName());
            twakeLock.acquire();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if(listener != null){
            listener.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(ByteArrayOutputStream byteArrayOutputStream) {
        super.onPostExecute(byteArrayOutputStream);
        if(twakeLock != null){
            twakeLock.release();
            twakeLock = null;
        }

        if(listener != null){
            listener.onCompleted(tUri,byteArrayOutputStream);
            listener = null;
        }
    }

    interface Listener {
        void onProgress(int progress);

        void onCompleted(@NonNull Uri uri, @Nullable ByteArrayOutputStream result);
    }
}
