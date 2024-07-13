package com.tentrontechnologies.tentron_dfu;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import kotlin.TuplesKt;
import kotlin.collections.MapsKt;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * TentronDfuPlugin
 */
public class TentronDfuPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private static final int kForceNumberOfPacketsReceiptNotificationsValue = 4;
    private DfuServiceController mDfuServiceController;
    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink sink;
    private MethodChannel.Result pendingResult;
    private WeakReference<Context> tWeakContext;
    //private String url = "https://raw.githubusercontent.com/Masinjaka/device_firmware_releases/main/v0.1/bleuart_me.ino.zip";

    private DownloadTask downloadTask;

    private Context tContext;
    private final DfuProgressListener progressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(@NonNull String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDeviceConnecting",deviceAddress)));
            }
        }
        @Override
        public void onDeviceConnected(@NonNull String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDeviceConnected",deviceAddress)));
            }
        }

        @Override
        public void onDfuProcessStarting(@NonNull String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDfuProcessStarting",deviceAddress)));
            }
        }

        @Override
        public void onDfuProcessStarted(@NonNull String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDfuProcessStarted",deviceAddress)));
            }
        }

        @Override
        public void onEnablingDfuMode(@NonNull String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onEnablingDfuMode",deviceAddress)));
            }
        }

        @Override
        public void onProgressChanged(@NonNull String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {

            System.out.println(percent+ "% vita ho an'ny "+deviceAddress);

            Map parameters = new LinkedHashMap();
            parameters.put("deviceAddress",deviceAddress);
            parameters.put("percent",percent);
            parameters.put("speed",speed);
            parameters.put("avgSpeed",avgSpeed);
            parameters.put("currentPart",currentPart);
            parameters.put("totalParts",partsTotal);

            EventChannel.EventSink eventSink = TentronDfuPlugin.this.sink;
            if(eventSink!=null){
                eventSink.success(MapsKt.mapOf(TuplesKt.to("onProgressChanged",parameters)));
            }
        }

        @Override
        public void onFirmwareValidating(@NonNull String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onFirmwareValidating",deviceAddress)));
            }
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDeviceDisconnecting",deviceAddress)));
            }
        }

        @Override
        public void onDeviceDisconnected(@NonNull String deviceAddress) {
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDeviceDisconnected",deviceAddress)));
            }
        }

        @Override
        public void onDfuCompleted(@NonNull String deviceAddress) {
            // Cancel notification because the dfu process is done
            TentronDfuPlugin.this.cancelNotification();
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDfuCompleted",deviceAddress)));
            }

            MethodChannel.Result res = TentronDfuPlugin.this.pendingResult;
            if(res!=null){
                res.success(deviceAddress);
            }
            TentronDfuPlugin.this.pendingResult = null;
        }

        @Override
        public void onDfuAborted(@NonNull String deviceAddress) {
            TentronDfuPlugin.this.cancelNotification();
            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onDfuAborted",deviceAddress)));
            }

            MethodChannel.Result res = TentronDfuPlugin.this.pendingResult;
            if(res!=null){
                // Send error message
                res.error("DFU_ABORTED","Dfu aborted by user","Device adress: "+deviceAddress);
            }

            TentronDfuPlugin.this.pendingResult = null;
        }

        @Override
        public void onError(@NonNull String deviceAddress, int error, int errorType, String message) {
            // Cancel notification due to error
            Log.d(String.valueOf(error),message);
            TentronDfuPlugin.this.cancelNotification();
            Map parameters = new LinkedHashMap();
            parameters.put("deviceAddress",deviceAddress);
            parameters.put("error",error);
            parameters.put("errorType",errorType);
            parameters.put("message",message);

            EventChannel.EventSink eveSink = TentronDfuPlugin.this.sink;
            if(eveSink!=null){
                eveSink.success(MapsKt.mapOf(TuplesKt.to("onError",parameters)));
            }

            if(TentronDfuPlugin.this.pendingResult!=null){
                MethodChannel.Result res  = TentronDfuPlugin.this.pendingResult;
                res.error(String.valueOf(error),"DFU FAILED: "+message,"Address: "+deviceAddress+", Error type: "+errorType);
                TentronDfuPlugin.this.pendingResult = null;
            }
        }
    };
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

        tContext = flutterPluginBinding.getApplicationContext();
        tWeakContext = new WeakReference<>(tContext.getApplicationContext());

        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "tentron_dfu");
        channel.setMethodCallHandler(this);

        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "tentron_event");
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {

            result.success("Android " + android.os.Build.VERSION.RELEASE);

        } else if (call.method.equals("doDfu")) {

            startDeviceFirmwareUpdate(call, result);

        } else if (call.method.equals("endDfu")){

            endDfu();

        }else{
            result.notImplemented();
        }
    }

    private void endDfu() {
        if(mDfuServiceController != null){
            mDfuServiceController.abort();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        this.tContext = null;
        channel = null;
        eventChannel = null;
    }

    public void startDeviceFirmwareUpdate(@NonNull MethodCall call, Result result) {

        String hexUriString = (String) call.argument("hexUri");
        //String iniUriString = (String) call.argument("iniUri");
        String deviceAddress = (String) call.argument("address");
        String deviceName = (String) call.argument("name");

        /// Maka ny uri string ho lasa vrai uri
        Uri uriHex = Uri.parse(hexUriString);
        //Uri uriIni = Uri.parse(iniUriString);
        Log.i("ACTION PERFORM DFU:", "Uri String transformed into Dfu");

        if(downloadTask != null){
            downloadTask.cancel(true);
            downloadTask.setListener(null);
        }

        downloadTask = new DownloadTask(tWeakContext, new DownloadTask.Listener() {
            @Override
            public void onProgress(int progress) {
                System.out.println("Download progress " + progress);
            }

            @Override
            public void onCompleted(@NonNull Uri uri, @Nullable ByteArrayOutputStream byteArrayOutputStream) {
                if(byteArrayOutputStream != null){
                    System.out.println("DOWNLOAD DONE !!!");
                    File file = writeSoftwareDownload(tContext,byteArrayOutputStream,"filename.zip");
                    if(file!=null){
                        String absolutePath = file.getAbsolutePath();
                        install(tContext,deviceAddress,deviceName,absolutePath,result);
                        //result.success("Device updated successfully");
                        downloadTask = null;
                    }
                }else{
                    result.error("File not downloaded","The file is not downloaded",null);
                    downloadTask = null;
                }
            }
        });
        downloadTask.execute(uriHex);



        //String hexMessage = getByteArrayOutputStreamOfHexFile(uriHex,"firmware.zip");
        //String iniMessage = getByteArrayOutputStreamOfHexFile(uriIni,"firmware.ini");

        /*if(hexMessage != null){
            install(tContext,deviceAddress,deviceName,hexMessage);
            result.success("HEX MESSAGE: " + hexMessage);
        }else{
            result.error("One file is null", "That's it, one file is null", null);
        }*/


    }

    /*private String getByteArrayOutputStreamOfHexFile(Uri uri,@NonNull String filename) {
        InputStream input = null;
        ByteArrayOutputStream output = null;
        String message = null;
        try {

            String uriSheme = uri.getScheme();
            boolean shouldBeConsideredAsInputStream = uriSheme != null && (uriSheme.equalsIgnoreCase("file") || uriSheme.equalsIgnoreCase("content"));

            if (shouldBeConsideredAsInputStream) {
                input = tWeakContext.get().getContentResolver().openInputStream(uri);

                if (input != null) {
                    output = new ByteArrayOutputStream();

                    byte[] data = new byte[4096];
                    long total = 0;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                } else {
                    return "The inputStream is null";
                }
            } else {
                return "The file is not considered as InputStream";
            }

        } catch (Exception e) {
            Log.w("Error", "Erreur de télechargement" + e);
            return "We got an error while downloading the file ";
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
            }
        }

        if (output != null) {

            // Copy the byte array outputstream to a file object
            File file = writeSoftwareDownload(tContext,output,filename);
            if(file!=null){
                return file.getAbsolutePath();
            }

        }

        return message;
    }*/

    private File writeSoftwareDownload(@NonNull Context context, @NonNull ByteArrayOutputStream baos, @NonNull String filename) {
        File resultFile;
        // Creating the file directory
        File file = new File(context.getCacheDir(), filename);
        BufferedOutputStream bos;
        boolean success = true;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(baos.toByteArray());
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            success = false;
        } catch (IOException e) {
            success = false;
        }

        resultFile = success ? file : null;

        return resultFile;
    }

    private void install (@NonNull Context context, @NonNull String address, @Nullable String deviceName, @NonNull String localHexPath,@NonNull MethodChannel.Result result){
        final DfuServiceInitiator starter = new DfuServiceInitiator(address);
        if(deviceName!=null){
            starter.setDeviceName(deviceName);
        }
        starter.setMtu(23);
        starter.setForceScanningForNewAddressInLegacyDfu(true);
        //starter.setRestoreBond(true);

        if(kForceNumberOfPacketsReceiptNotificationsValue != 0){
            starter.setPacketsReceiptNotificationsEnabled(true);
            starter.setPacketsReceiptNotificationsValue(kForceNumberOfPacketsReceiptNotificationsValue);
        }
        starter.setZip(localHexPath);

        this.pendingResult = result;
        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        //starter.setBinOrHex(DfuService.TYPE_APPLICATION,null,localHexPath);
        /*if(localIniFile != null){
            starter.setInitFile(null,localIniFile);
        }*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(context);
        }

        mDfuServiceController = starter.start(context,DfuService.class);
    }

    private void cancelNotification(){
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Object obj = TentronDfuPlugin.this.tContext.getSystemService(Context.NOTIFICATION_SERVICE);
                if(obj==null){
                    throw new NullPointerException("getting notification service returned null, that's bad fix that...");
                }else{
                    NotificationManager manager  = (NotificationManager) obj;
                    manager.cancel(DfuService.NOTIFICATION_ID);
                }
            }
        }, 200);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        if(this.tContext != null){
            DfuServiceListenerHelper.registerProgressListener(tContext,this.progressListener);
        }
        this.sink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        this.sink = null;
    }
}
