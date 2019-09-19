
import 'package:flutter/material.dart';
import 'package:flutter_inappbrowser/base.dart';
import 'package:flutter_inappbrowser/webview_scaffold.dart';


const kAndroidUserAgent =
    'Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Mobile Safari/537.36';

String selectedUrl = 'https://wxopendev.cmtzz.cn/news/25668?ft=recommend';

class WebScafford extends StatefulWidget {

  @override
  _WebScaffordState createState() => new _WebScaffordState();

}

class _WebScaffordState extends State<WebScafford> {
  final flutterWebViewPlugin = FlutterWebviewPlugin();


  void initState() {
    super.initState();

    flutterWebViewPlugin.onUrlChanged.listen((String url) {

    });


  }

  @override
  Widget build(BuildContext context) {
    return  WebviewScaffold(
        url: selectedUrl,
        withZoom: true,
        withLocalStorage: true,
        withJavascript: true,
        hidden: true,
    );

  }
}