import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'tentron_dfu_method_channel.dart';

abstract class TentronDfuPlatform extends PlatformInterface {
  /// Constructs a TentronDfuPlatform.
  TentronDfuPlatform() : super(token: _token);

  static final Object _token = Object();

  static TentronDfuPlatform _instance = MethodChannelTentronDfu();

  /// The default instance of [TentronDfuPlatform] to use.
  ///
  /// Defaults to [MethodChannelTentronDfu].
  static TentronDfuPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [TentronDfuPlatform] when
  /// they register themselves.
  static set instance(TentronDfuPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> doDfu(String deviceAddress,String deviceName,String firmwareUri,{
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
  }){
    throw UnimplementedError('doDfu() has not been implemented.');
  }

  Future<String?> endDfu(){
    throw UnimplementedError("ednDfu() not implemented");
  }
}
