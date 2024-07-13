import 'package:tentron_dfu/tentron_dfu_method_channel.dart';
import 'tentron_dfu_platform_interface.dart';

class TentronDfu {
  Future<String?> getPlatformVersion() {
    return TentronDfuPlatform.instance.getPlatformVersion();
  }

  /// Method to perform Dfu on tentron devices.
  /// [hexUri] The Uri of the hex File got from a fileChooser.
  /// [iniUri] The Uri of the ini File got from a fileChooser.
  Future<String?> doDfu(
    String deviceAddress,
    String deviceName,
    String firmwareUri, {
    DfuCallback? onDeviceConnected,
    DfuCallback? onDeviceConnecting,
    DfuCallback? onDeviceDisconnected,
    DfuCallback? onDeviceDisconnecting,
    DfuCallback? onDfuAborted,
    DfuCallback? onDfuCompleted,
    DfuCallback? onDfuProcessStarted,
    DfuCallback? onDfuProcessStarting,
    DfuCallback? onEnablingDfuMode,
    DfuCallback? onFirmwareValidating,
    DfuErrorCallback? onError,
    DfuProgressCallback? onProgressChanged,
  }) {
    return TentronDfuPlatform.instance.doDfu(
        deviceAddress, deviceName, firmwareUri,
        onDeviceConnected: onDeviceConnected,
        onDeviceConnecting: onDeviceConnecting,
        onDeviceDisconnected: onDeviceDisconnected,
        onDeviceDisconnecting: onDeviceDisconnecting,
        onDfuAborted: onDfuAborted,
        onDfuCompleted: onDfuCompleted,
        onDfuProcessStarted: onDfuProcessStarted,
        onDfuProcessStarting: onDfuProcessStarting,
        onEnablingDfuMode: onEnablingDfuMode,
        onFirmwareValidating: onFirmwareValidating,
        onError: onError,
        onProgressChanged: onProgressChanged);
  }

  Future<String?> endDfu(){
    return TentronDfuPlatform.instance.endDfu();
  }
}
