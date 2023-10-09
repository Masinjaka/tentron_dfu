import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'tentron_dfu_platform_interface.dart';

/// An implementation of [TentronDfuPlatform] that uses method channels.
class MethodChannelTentronDfu extends TentronDfuPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('tentron_dfu');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
 

  @override
  Future<String?> doDfu(String deviceAddress,String deviceName,String hexUri, String iniUri) async{
    final state = await methodChannel.invokeMethod('doDfu',<String,String>{
      "address":deviceAddress,
      "hexUri":hexUri,
      "iniUri":iniUri,
      "name":deviceName,
    });
    return state;
  }

}
