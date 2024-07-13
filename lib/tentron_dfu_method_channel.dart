import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'tentron_dfu_platform_interface.dart';

typedef DfuCallback = void Function(String address);

typedef DfuErrorCallback = void Function(
  String address,
  int error,
  int errorType,
  String message,
);

typedef DfuProgressCallback = void Function(
  String address,
  int percent,
  double speed,
  double avgSpeed,
  int currentPart,
  int totalParts,
);

/// An implementation of [TentronDfuPlatform] that uses method channels.
class MethodChannelTentronDfu extends TentronDfuPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('tentron_dfu');
  final eventChannel = const EventChannel('tentron_event');
  StreamSubscription? event;

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
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
  }) async {

    event = eventChannel.receiveBroadcastStream().listen((data) {
      data as Map;
      for (final key in data.keys) {
        switch (key) {
          case 'onDeviceConnected':
            onDeviceConnected?.call(data[key] as String);
            break;
          case 'onDeviceConnecting':
            onDeviceConnecting?.call(data[key] as String);
            break;
          case 'onDeviceDisconnected':
            onDeviceDisconnected?.call(data[key] as String);
            break;
          case 'onDeviceDisconnecting':
            onDeviceDisconnecting?.call(data[key] as String);
            break;
          case 'onDfuAborted':
            onDfuAborted?.call(data[key] as String);
            event?.cancel();
            break;
          case 'onDfuCompleted':
            onDfuCompleted?.call(data[key] as String);
            event?.cancel();
            break;
          case 'onDfuProcessStarted':
            onDfuProcessStarted?.call(data[key] as String);
            break;
          case 'onDfuProcessStarting':
            onDfuProcessStarting?.call(data[key] as String);
            break;
          case 'onEnablingDfuMode':
            onEnablingDfuMode?.call(data[key] as String);
            break;
          case 'onFirmwareValidating':
            onFirmwareValidating?.call(data[key] as String);
            break;
          case 'onError':
            final Map<String, dynamic> result =
            Map<String, dynamic>.from(data[key] as Map);
            onError?.call(
              result['deviceAddress'] as String,
              result['error'] as int,
              result['errorType'] as int,
              result['message'] as String,
            );
            event?.cancel();
            break;
          case 'onProgressChanged':
            final Map<String, dynamic> result =
            Map<String, dynamic>.from(data[key] as Map);
            onProgressChanged?.call(
              result['deviceAddress'] as String,
              result['percent'] as int,
              result['speed'] as double,
              result['avgSpeed'] as double,
              result['currentPart'] as int,
              0,
            );
            break;
        }
      }
    });

    final state = await methodChannel.invokeMethod(
      'doDfu',
      <String, String>{
        "address": deviceAddress,
        "hexUri": firmwareUri,
        "name": deviceName,
      },
    );
    return state;
  }

  @override
  Future<String?> endDfu() async {
    final result = await methodChannel.invokeMethod<String>('endDfu');
    return result;
  }
}
