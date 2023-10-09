
import 'tentron_dfu_platform_interface.dart';

class TentronDfu {
  Future<String?> getPlatformVersion() {
    return TentronDfuPlatform.instance.getPlatformVersion();
  }

  /// Method to perform Dfu on tentron devices.
  /// [hexUri] The Uri of the hex File got from a fileChooser.
  /// [iniUri] The Uri of the ini File got from a fileChooser.
  Future<String?> doDfu(String deviceAddress,String deviceName,String hexUri,String iniUri){
    return TentronDfuPlatform.instance.doDfu(deviceAddress,deviceName,hexUri, iniUri);
  }

}
