import 'dart:async';
import 'dart:developer';
import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  final GlobalKey renderPaintKey = GlobalKey();

  MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: NewWidget(renderPaintKey: renderPaintKey),
    );
  }
}

class NewWidget extends StatelessWidget {
  const NewWidget({
    super.key,
    required this.renderPaintKey,
  });
  static const platform = MethodChannel('saveBitmap/test');

  final GlobalKey<State<StatefulWidget>> renderPaintKey;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        RepaintBoundary(
          key: renderPaintKey,
          child: Image.asset('assets/test.jpg'),
        ),
        TextButton(
          onPressed: () async => await capturePng(context),
          style: TextButton.styleFrom(
            backgroundColor: Colors.blueGrey,
          ),
          child: const Text(
            'Guardar Bitmap',
            style: TextStyle(color: Colors.red),
          ),
        )
      ],
    );
  }

  Future<bool> capturePng(BuildContext context) async {
    try {
      RenderRepaintBoundary boundary = renderPaintKey.currentContext
          ?.findRenderObject() as RenderRepaintBoundary;
      if (boundary.debugNeedsPaint) {
        Timer(const Duration(seconds: 1), () => capturePng(context));
        return false;
      }
      ui.Image image = await boundary.toImage(
          pixelRatio: MediaQuery.of(context).devicePixelRatio);
      ByteData? byteData =
          await image.toByteData(format: ui.ImageByteFormat.png);
      var pngBytes = byteData?.buffer.asUint8List();
      final directory = (await getApplicationDocumentsDirectory()).path;
      var inputFormat = DateFormat('ddMMyyyy_HHmmss').format(DateTime.now());
      File imgFile = File('$directory/cAppV$inputFormat.png')
        ..create(recursive: true);
      if (pngBytes != null) {
        await imgFile.writeAsBytes(pngBytes);
        bool? result;
        try {
          result = await platform.invokeMethod('saveImage', <String, dynamic>{
            'bitmap': pngBytes,
            'album': 'Test'
          });
        } on PlatformException catch (e) {
          log(e.toString());
        }

        return result ?? false;
      } else {
        return false;
      }
    } catch (e) {
      log(e.toString());
      return false;
    }
  }
}
