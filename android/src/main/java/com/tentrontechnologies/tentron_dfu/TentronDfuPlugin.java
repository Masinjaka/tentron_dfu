package com.tentrontechnologies.tentron_dfu;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;

/**
 * TentronDfuPlugin
 */
public class TentronDfuPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private static final int kForceNumberOfPacketsReceiptNotificationsValue = 4;
    private DfuServiceController mDfuServiceController;
    private MethodChannel channel;
    private WeakReference<Context> tWeakContext;

    private DownloadTask downloadTask;

    private Context tContext;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

        tContext = flutterPluginBinding.getApplicationContext();
        tWeakContext = new WeakReference<>(tContext.getApplicationContext());
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "tentron_dfu");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("doDfu")) {

            startDeviceFirmwareUpdate(call, result);

        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    public void startDeviceFirmwareUpdate(MethodCall call, Result result) {

        String hexUriString = (String) call.argument("hexUri");
        String iniUriString = (String) call.argument("iniUri");
        String deviceAddress = (String) call.argument("address");
        String deviceName = (String) call.argument("name");

        /// Maka ny uri string ho lasa vrai uri
        Uri uriHex = Uri.parse(hexUriString);
        Uri uriIni = Uri.parse(iniUriString);
        Log.i("ACTION PERFORM DFU:", "Uri String transformed into Dfu");

        downloadTask = new DownloadTask(tWeakContext, new DownloadTask.Listener() {
            @Override
            public void onProgress(int progress) {
                System.out.println("Download progress" + progress);
            }

            @Override
            public void onCompleted(@NonNull Uri uri, @Nullable ByteArrayOutputStream byteArrayOutputStream) {
                System.out.println("DOWNLOAD DONE !!!");
                result.success("HEX MESSAGE: DOWNLOAD DONE");
            }
        });
        downloadTask.execute(uriHex);



        /*String hexMessage = getByteArrayOutputStreamOfHexFile(uriHex,"firmware.hex");
        String iniMessage = getByteArrayOutputStreamOfHexFile(uriIni,"firmware.ini");

        if(hexMessage != null && iniMessage != null){
            install(tContext,deviceAddress,deviceName,hexMessage,iniMessage);
            result.success("HEX MESSAGE: " + hexMessage + ", INI MESSAGE: " + iniMessage);
        }else{
            result.error("One file is null", "That's it, one file is null", null);
        }*/


    }

    private String getByteArrayOutputStreamOfHexFile(Uri uri,@NonNull String filename) {
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
    }

    private File writeSoftwareDownload(@NonNull Context context, @NonNull ByteArrayOutputStream baos, @NonNull String filename) {
        File resultFile;
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


    private void install (@NonNull Context context, @NonNull String address, @NonNull String deviceName, @NonNull String localHexPath, @NonNull String localIniFile){
        final DfuServiceInitiator starter = new DfuServiceInitiator(address);
        if(deviceName!=null){
            starter.setDeviceName(deviceName);
        }
        starter.setMtu(23);
        starter.setForceScanningForNewAddressInLegacyDfu(true);
        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        if(kForceNumberOfPacketsReceiptNotificationsValue != 0){
            starter.setPacketsReceiptNotificationsEnabled(true);
            starter.setPacketsReceiptNotificationsValue(kForceNumberOfPacketsReceiptNotificationsValue);
        }

        starter.setBinOrHex(DfuService.TYPE_APPLICATION,null,localHexPath);
        if(localIniFile != null){
            starter.setInitFile(null,localIniFile);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(context);
        }

        mDfuServiceController = starter.start(context,DfuService.class);
    }
}
