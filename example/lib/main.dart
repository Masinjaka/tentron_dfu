import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_reactive_ble/flutter_reactive_ble.dart';
import 'package:location/location.dart';
import 'package:tentron_dfu/tentron_dfu.dart';
import 'package:pick_or_save/pick_or_save.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _tentronDfuPlugin = TentronDfu();

  final _ble = FlutterReactiveBle();

  // Variables for scanning stream
  bool isScanning = false;
  late StreamSubscription<DiscoveredDevice> scanStream;

  //For listening to blestatusStream
  late StreamSubscription<BleStatus> bleStatusStream;

  // Connection streams
  late Stream<ConnectionStateUpdate> _connectionStream;
  // ignore: unused_field
  late StreamSubscription<ConnectionStateUpdate> _connectionSubscription;

  //To check if the app is conencted to something
  bool connectedToSomething = false;
  // ignore: prefer_typing_uninitialized_variables
  var identity;

  //Device firmware update
  bool dfuRunning = false;

  // controller for the pageView
  PageController pageController = PageController(initialPage: 0);

  //Location
  Location location = Location();
  late bool _locationEnabled;
  late PermissionStatus _permissionGranted;

  late String connectedDeviceName;

  // Function to ask for location permission
  Future<void> askLocationPermission() async {
    _locationEnabled = await location.serviceEnabled();
    if (!_locationEnabled) {
      _locationEnabled = await location.requestService();
      if (!_locationEnabled) {
        return;
      }
    }

    _permissionGranted = await location.hasPermission();
    if (_permissionGranted == PermissionStatus.denied) {
      _permissionGranted = await location.requestPermission();
      if (_permissionGranted != PermissionStatus.granted) {
        return;
      }
    }
  }

  // Connect to a device
  void connectTodevice(String id, String deviceName) {
    scanStream.cancel();
    isScanning = false;

    _connectionStream = _ble.connectToDevice(id: id);
    _connectionSubscription = _connectionStream.listen((event) {
      switch (event.connectionState) {
        case DeviceConnectionState.connected:
          setState(() {
            // the deviec is connected to something
            connectedToSomething = true;
            identity = id;
            connectedDeviceName = deviceName;
            pageController.animateToPage(1,
                duration: const Duration(seconds: 1),
                curve: Curves.fastOutSlowIn);
          });
          break;
        default:
      }
    }, onError: (error) {
      setState(() {
        _log = error.toString();
      });
    });
  }

  //log message
  String _log = "";

  // List of discovered device
  List<DiscoveredDevice> myList = [];

  // Start scanning for device
  Future<void> scanForDevice() async {
    setState(() {
      _log = "Scanning started";
    });

    //askLocationPermission();

    // Toggle to scan mode
    isScanning = true;
    //Clear the list
    myList.clear();

    //Start the scanning process
    scanStream = _ble.scanForDevices(
      withServices: [],
    ).listen((device) {
      setState(() {
        _log = "Found ${device.name}";
      });
      if (myList.every((deviceItem) => deviceItem.id != device.id)) {
        setState(() {
          myList.add(device);
        });
      }
    }, onError: (error) {
      setState(() {
        _log = error.toString();
      });
    });
  }

  // Stop scanning for device
  void stopScanning() {
    scanStream.cancel();
    isScanning = false;
  }

  @override
  void initState() {
    super.initState();
    askLocationPermission();
    initPlatformState();

    bleStatusStream = _ble.statusStream.listen((event) {
      switch (event) {
        case BleStatus.poweredOff:
          setState(() {
            _log = "Bluetooth is turned off";
          });
          break;
        case BleStatus.locationServicesDisabled:
          setState(() {
            _log = "Location service is disabled";
          });
          break;
        case BleStatus.ready:
          setState(() {
            _log = "Bluetooth is ready";
          });
        default:
      }
    });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    //String? test = await _tentronDfuPlugin.doDfu("Mbola ho avy","e", "ny ronon-delony e");
    try {
      platformVersion = await _tentronDfuPlugin.getPlatformVersion() ??
          'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  String? hexuri = 'Mbola tsisy', iniuri = 'Mbola tsisy';
  String? _scheme = "Mbola tsisy";

  Future<String?> chooseFile() async {
    List<String>? result = await PickOrSave()
        .filePicker(params: FilePickerParams(getCachedFilePath: false));

    String path = result![0];
    return path;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(useMaterial3: true),
      themeMode: ThemeMode.dark,
      home: Scaffold(
        appBar: AppBar(
          title: Text('Running on: $_platformVersion\n'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Hex uri is ${hexuri!}'),
              Text('Ini uri is ${iniuri!}'),
              Text('$_scheme'),
              Expanded(
                child: ElevatedButton(
                  onPressed: () async {
                    String? got = await chooseFile();
                    setState(() {
                      hexuri = got;
                    });
                  },
                  child: const Text('Choose hex file'),
                ),
              ),
              Expanded(
                child: ElevatedButton(
                  onPressed: () async {
                    String? got = await chooseFile();
                    setState(() {
                      iniuri = got;
                    });
                  },
                  child: const Text('Choose ini file'),
                ),
              ),
              Expanded(
                child: ElevatedButton(
                  onPressed: () async {
                    String? scheme;
                    if (!(hexuri! == "Mbola tsisy") &&
                        !(iniuri! == "Mbola tsisy")) {
                      //
                      //scheme = await _tentronDfuPlugin.doDfu('No address',hexuri!,iniuri!);
                    } else {
                      if (hexuri! == "Mbola tsisy") {
                        scheme = "Choisit d'abord l'url de fichier hex";
                      } else if (iniuri! == "Mbola tsisy") {
                        scheme = "Choisit d'abord l'url de fichier ini";
                      } else {
                        scheme =
                            "Choisit d'abord l'url des fichiers hex et ini";
                      }
                    }

                    setState(() {
                      _scheme = scheme;
                    });
                  },
                  child: const Text("what do you see? Geh content"),
                ),
              ),
              Expanded(
                flex: 2,
                child: OInformation(
                  title: "Log",
                  child: Center(child: Text(_log)),
                ),
              ),
              Expanded(
                flex: 3,
                child: OInformation(
                  title: "Devices",
                  child: SizedBox(
                    child: PageView(
                      controller: pageController,
                      children: [
                        Center(
                          child: ListView.separated(
                            itemBuilder: (context, index) {
                              return ListTile(
                                leading: const Icon(Icons.bluetooth),
                                title: Text(myList[index].name),
                                subtitle: Text(myList[index].id),
                                trailing: ElevatedButton(
                                  onPressed: () {
                                    //Connect to the selected device
                                    connectTodevice(
                                        myList[index].id, myList[index].name);
                                  },
                                  child: const Text(
                                    "Connect",
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 16,
                                    ),
                                  ),
                                ),
                              );
                            },
                            separatorBuilder: (context, index) =>
                                const Divider(color: Colors.black),
                            itemCount: myList.length,
                          ),
                        ),
                        connectedToSomething
                            ? Column(
                                children: [
                                  Text(
                                    connectedDeviceName,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 25),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(identity),
                                  const SizedBox(height: 2),
                                  ElevatedButton(
                                    onPressed: () async {
                                      stopScanning();
                                      setState(() {
                                        dfuRunning = true;
                                        _log = "Dfu about to run";
                                      });
                                      String? scheme;
                                      if (!(hexuri! == "Mbola tsisy") &&
                                          !(iniuri! == "Mbola tsisy")) {
                                        //
                                        //scheme = await _tentronDfuPlugin.doDfu('No address',hexuri!,iniuri!);
                                        try {
                                          scheme = await _tentronDfuPlugin.doDfu(
                                              identity,
                                              connectedDeviceName,
                                              "https://raw.githubusercontent.com/Masinjaka/device_firmware_releases/main/v0.1/bleuart_me.ino.zip");
                                        } on PlatformException {
                                          _log = scheme!;
                                        }
                                      } else {
                                        if (hexuri! == "Mbola tsisy") {
                                          scheme =
                                              "Choisit d'abord l'url de fichier hex";
                                        } else if (iniuri! == "Mbola tsisy") {
                                          scheme =
                                              "Choisit d'abord l'url de fichier ini";
                                        } else {
                                          scheme =
                                              "Choisit d'abord l'url des fichiers hex et ini";
                                        }
                                      }

                                      setState(() {
                                        _scheme = scheme;
                                      });

                                      // Function that perform the beloved OTA update that i hope will perfectly work for the love of god
                                    },
                                    child: Text(dfuRunning
                                        ? "Abord firmware update"
                                        : "Update firmware"),
                                  )
                                ],
                              )
                            : const Center(
                                child: Text("Connect to a device"),
                              )
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(isScanning ? Icons.stop : Icons.refresh),
          onPressed: () {
            setState(() {
              isScanning ? stopScanning() : scanForDevice();
            });
          },
        ),
      ),
    );
  }
}

class OInformation extends StatefulWidget {
  const OInformation({
    super.key,
    required this.title,
    required this.child,
  });

  final String title;
  final Widget child;

  @override
  State<OInformation> createState() => _OInformationState();
}

class _OInformationState extends State<OInformation> {
  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: const BorderRadius.all(
          Radius.circular(20),
        ),
        border: Border.all(
          color: Colors.black38,
        ),
      ),
      child: Stack(
        children: [
          Positioned(
            top: 20,
            left: 20,
            child: Text(
              widget.title,
              style: const TextStyle(
                color: Colors.black,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          Positioned(
            top: 50,
            bottom: 5,
            left: 0,
            right: 0,
            child: widget.child,
          ),
        ],
      ),
    );
  }
}
