import 'dart:async';
import 'dart:ui';
import 'dart:io';
import 'dart:collection';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const _kChannel = 'flutter_webview_plugin';

// TODO: more general state for iOS/android
enum WebViewState { shouldStart, startLoad, finishLoad, abortLoad }

///This type represents a callback, added with [addJavaScriptHandler], that listens to post messages sent from JavaScript.
///
///The Android implementation uses [addJavascriptInterface](https://developer.android.com/reference/android/webkit/WebView#addJavascriptInterface(java.lang.Object,%20java.lang.String)).
///The iOS implementation uses [addScriptMessageHandler](https://developer.apple.com/documentation/webkit/wkusercontentcontroller/1537172-addscriptmessagehandler?language=objc)
///
///The JavaScript function that can be used to call the handler is `window.flutter_inappbrowser.callHandler(handlerName <String>, ...args);`, where `args` are [rest parameters](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/rest_parameters).
///The `args` will be stringified automatically using `JSON.stringify(args)` method and then they will be decoded on the Dart side.
///
///Also, a [JavaScriptHandlerCallback] can return json data to the JavaScript side.
///In this case, simply return data that you want to send and it will be automatically json encoded using [jsonEncode] from the `dart:convert` library.
typedef dynamic JavaScriptHandlerCallback(List<dynamic> arguments);


// TODO: use an id by webview to be able to manage multiple webview

/// Singleton class that communicate with a Webview Instance
class FlutterWebviewPlugin {
  Map<String, JavaScriptHandlerCallback> javaScriptHandlersMap = HashMap<String, JavaScriptHandlerCallback>();
  factory FlutterWebviewPlugin() => _instance ??= FlutterWebviewPlugin._();

  FlutterWebviewPlugin._() {
    _channel.setMethodCallHandler(_handleMessages);
  }

  static FlutterWebviewPlugin _instance;

  final _channel = const MethodChannel(_kChannel);

  final _onBack = StreamController<Null>.broadcast();
  final _onSelectText= StreamController<WebController>.broadcast();
  final _onSession = StreamController<WebController>.broadcast();
  final _onTimeLine = StreamController<WebController>.broadcast();
  final _onDestroy = StreamController<Null>.broadcast();
  final _onUrlChanged = StreamController<String>.broadcast();
  final _onStateChanged = StreamController<WebViewStateChanged>.broadcast();
  final _onScrollXChanged = StreamController<double>.broadcast();
  final _onScrollYChanged = StreamController<double>.broadcast();
  final _onProgressChanged = new StreamController<double>.broadcast();
  final _onHttpError = StreamController<WebViewHttpError>.broadcast();

  void addJavaScriptHandler(String handlerName, JavaScriptHandlerCallback callback) {
    this.javaScriptHandlersMap[handlerName] = (callback);
  }

  ///Removes a JavaScript message handler previously added with the [addJavaScriptHandler()] associated to [handlerName] key.
  ///Returns the value associated with [handlerName] before it was removed.
  ///Returns `null` if [handlerName] was not found.
  JavaScriptHandlerCallback removeJavaScriptHandler(String handlerName) {
    return this.javaScriptHandlersMap.remove(handlerName);
  }


  Future<Null> _handleMessages(MethodCall call) async {
    switch (call.method) {
      case 'onBack':
        _onBack.add(null);
        break;

      case 'onSelectText':
        _onSelectText.add(WebController.fromMap(
          Map<String, dynamic>.from(call.arguments),
        ));
        break;

      case 'onSession':
        _onSession.add(WebController.fromMap(
          Map<String, dynamic>.from(call.arguments),
        ));
        break;

      case 'onTimeLine':
        _onTimeLine.add(WebController.fromMap(
          Map<String, dynamic>.from(call.arguments),
        ));
        break;

      case 'onDestroy':
        _onDestroy.add(null);
        break;
      case 'onUrlChanged':
        _onUrlChanged.add(call.arguments['url']);
        break;
      case 'onScrollXChanged':
        _onScrollXChanged.add(call.arguments['xDirection']);
        break;
      case 'onScrollYChanged':
        _onScrollYChanged.add(call.arguments['yDirection']);
        break;
      case "onProgressChanged":
        _onProgressChanged.add(call.arguments["progress"]);
        break;
      case 'onState':
        _onStateChanged.add(
          WebViewStateChanged.fromMap(
            Map<String, dynamic>.from(call.arguments),
          ),
        );
        break;
      case 'onHttpError':
        _onHttpError.add(WebViewHttpError(call.arguments['code'], call.arguments['url']));
        break;

      case "onCallJsHandler":
        print("onCallJsHandler dart");
        String handlerName = call.arguments["handlerName"];
        print(handlerName);
        // decode args to json
        List<dynamic> args = jsonDecode(call.arguments["args"]);
        if (javaScriptHandlersMap.containsKey(handlerName)) {
          // convert result to json
          try {
            return jsonEncode(await javaScriptHandlersMap[handlerName](args));
          } catch (error) {
            print(error);
            return null;
          }
        }
        break;
    }
  }

  /// Listening the OnDestroy LifeCycle Event for Android
  Stream<Null> get onDestroy => _onDestroy.stream;

  /// Listening the back key press Event for Android
  Stream<Null> get onBack => _onBack.stream;

  /// Listening url changed
  Stream<String> get onUrlChanged => _onUrlChanged.stream;

  /// Listening the onState Event for iOS WebView and Android
  /// content is Map for type: {shouldStart(iOS)|startLoad|finishLoad}
  /// more detail than other events
  Stream<WebViewStateChanged> get onStateChanged => _onStateChanged.stream;

  //long click webview show copy and share
  Stream<WebController> get onSelectText => _onSelectText.stream;

  //wechat share to session
  Stream<WebController> get onSession => _onSession.stream;
  Stream<WebController> get onTimeLine => _onTimeLine.stream;

  /// Listening web view loading progress estimation, value between 0.0 and 1.0
  Stream<double> get onProgressChanged => _onProgressChanged.stream;

  /// Listening web view y position scroll change
  Stream<double> get onScrollYChanged => _onScrollYChanged.stream;

  /// Listening web view x position scroll change
  Stream<double> get onScrollXChanged => _onScrollXChanged.stream;

  Stream<WebViewHttpError> get onHttpError => _onHttpError.stream;

  /// Start the Webview with [url]
  /// - [headers] specify additional HTTP headers
  /// - [withJavascript] enable Javascript or not for the Webview
  /// - [clearCache] clear the cache of the Webview
  /// - [clearCookies] clear all cookies of the Webview
  /// - [hidden] not show
  /// - [rect]: show in rect, fullscreen if null
  /// - [enableAppScheme]: false will enable all schemes, true only for httt/https/about
  ///     android: Not implemented yet
  /// - [userAgent]: set the User-Agent of WebView
  /// - [withZoom]: enable zoom on webview
  /// - [withLocalStorage] enable localStorage API on Webview
  ///     Currently Android only.
  ///     It is always enabled in UIWebView of iOS and  can not be disabled.
  /// - [withLocalUrl]: allow url as a local path
  ///     Allow local files on iOs > 9.0
  /// - [localUrlScope]: allowed folder for local paths
  ///     iOS only.
  ///     If null and withLocalUrl is true, then it will use the url as the scope,
  ///     allowing only itself to be read.
  /// - [scrollBar]: enable or disable scrollbar
  /// - [supportMultipleWindows] enable multiple windows support in Android
  /// - [invalidUrlRegex] is the regular expression of URLs that web view shouldn't load.
  /// For example, when webview is redirected to a specific URL, you want to intercept
  /// this process by stopping loading this URL and replacing webview by another screen.
  ///   Android only settings:
  /// - [displayZoomControls]: display zoom controls on webview
  /// - [withOverviewMode]: enable overview mode for Android webview ( setLoadWithOverviewMode )
  /// - [useWideViewPort]: use wide viewport for Android webview ( setUseWideViewPort )
  Future<Null> launch(String url, {
    Map<String, String> headers,
    bool withJavascript,
    bool clearCache,
    bool clearCookies,
    bool hidden,
    bool enableAppScheme,
    Rect rect,
    String userAgent,
    bool withZoom,
    bool displayZoomControls,
    bool withLocalStorage,
    bool withLocalUrl,
    String localUrlScope,
    bool withOverviewMode,
    bool scrollBar,
    bool supportMultipleWindows,
    bool appCacheEnabled,
    bool allowFileURLs,
    bool useWideViewPort,
    String invalidUrlRegex,
    bool geolocationEnabled,
    bool debuggingEnabled,
  }) async {
    final args = <String, dynamic>{
      'url': url,
      'withJavascript': withJavascript ?? true,
      'clearCache': clearCache ?? false,
      'hidden': hidden ?? false,
      'clearCookies': clearCookies ?? false,
      'enableAppScheme': enableAppScheme ?? true,
      'userAgent': userAgent,
      'withZoom': withZoom ?? false,
      'displayZoomControls': displayZoomControls ?? false,
      'withLocalStorage': withLocalStorage ?? true,
      'withLocalUrl': withLocalUrl ?? false,
      'localUrlScope': localUrlScope,
      'scrollBar': scrollBar ?? true,
      'supportMultipleWindows': supportMultipleWindows ?? false,
      'appCacheEnabled': appCacheEnabled ?? false,
      'allowFileURLs': allowFileURLs ?? false,
      'useWideViewPort': useWideViewPort ?? false,
      'invalidUrlRegex': invalidUrlRegex,
      'geolocationEnabled': geolocationEnabled ?? false,
      'withOverviewMode': withOverviewMode ?? false,
      'debuggingEnabled': debuggingEnabled ?? false,
    };

    if (headers != null) {
      args['headers'] = headers;
    }

    if (rect != null) {
      args['rect'] = {
        'left': rect.left,
        'top': rect.top,
        'width': rect.width,
        'height': rect.height,
      };
    }
    await _channel.invokeMethod('launch', args);
  }

  /// Execute Javascript inside webview
  Future<String> evalJavascript(String code) async {
    final res = await _channel.invokeMethod('eval', {'code': code});
    return res;
  }

  Future<String> getTitle() async {
    final res = await _channel.invokeMethod('getTitle');
    return res;
  }

  /// Close the Webview
  /// Will trigger the [onDestroy] event
  Future<Null> close() async => await _channel.invokeMethod('close');

  /// Reloads the WebView.
  Future<Null> reload() async => await _channel.invokeMethod('reload');

  /// Navigates back on the Webview.
  Future<Null> goBack() async => await _channel.invokeMethod('back');

  /// Navigates forward on the Webview.
  Future<Null> goForward() async => await _channel.invokeMethod('forward');

  // Hides the webview
  Future<Null> hide() async => await _channel.invokeMethod('hide');

  // Shows the webview
  Future<Null> show() async => await _channel.invokeMethod('show');

  // show native dialog
  Future<Null> showShareDialog() async => await _channel.invokeMethod('showShareDialog');

  // Reload webview with a url
  Future<Null> reloadUrl(String url, {Map<String, String> headers}) async {
    final args = <String, dynamic>{'url': url};
    if (headers != null) {
      args['headers'] = headers;
    }
    await _channel.invokeMethod('reloadUrl', args);
  }

  //textColor eg: "#ff0000"
  Future<Null> setWebTitle(String title, {String textColor,int textSize,bool isBold}) async {
    Map<String, dynamic> args = <String, dynamic>{};
    if (title != null) {
      args.putIfAbsent('title', () => title);
    }
    if(textColor != null){
      args.putIfAbsent('textColor', () => textColor);
    }
    if(textSize != null){
      args.putIfAbsent('textSize', () => textSize);
    }
    if(isBold != null){
      args.putIfAbsent('isBold', () => isBold);
    }
    await _channel.invokeMethod('setWebTitle', args);
  }

  Future<Null> showShareIcon({bool showShareIcon}) async {
    Map<String, dynamic> args = <String, dynamic>{};
    if(showShareIcon != null){
      args.putIfAbsent('showShareIcon', () => showShareIcon);
    }
    await _channel.invokeMethod('showShareIcon', args);
  }

  // Clean cookies on WebView
  Future<Null> cleanCookies() async => await _channel.invokeMethod('cleanCookies');

  // Stops current loading process
  Future<Null> stopLoading() async => await _channel.invokeMethod('stopLoading');

  /// Close all Streams
  void dispose() {
    _onDestroy.close();
    _onUrlChanged.close();
    _onStateChanged.close();
    _onProgressChanged.close();
    _onScrollXChanged.close();
    _onScrollYChanged.close();
    _onHttpError.close();
    _onSelectText.close();
    _onSession.close();
    _onTimeLine.close();
    _instance = null;
  }

  Future<Map<String, String>> getCookies() async {
    final cookiesString = await evalJavascript('document.cookie');
    final cookies = <String, String>{};

    if (cookiesString?.isNotEmpty == true) {
      cookiesString.split(';').forEach((String cookie) {
        final split = cookie.split('=');
        cookies[split[0]] = split[1];
      });
    }

    return cookies;
  }

  /// resize webview
  Future<Null> resize(Rect rect) async {
    final args = {};
    args['rect'] = {
      'left': rect.left,
      'top': rect.top,
      'width': rect.width,
      'height': rect.height,
    };
    await _channel.invokeMethod('resize', args);
  }
}

class WebController{
  final String url;
  final String text;

  WebController(this.url, this.text);

  factory WebController.fromMap(Map<String, dynamic> map) {
    WebViewState t;
    return WebController(map['url'], map['text']);
  }
}

class WebViewStateChanged {
  WebViewStateChanged(this.type, this.url, this.navigationType);

  factory WebViewStateChanged.fromMap(Map<String, dynamic> map) {
    WebViewState t;
    switch (map['type']) {
      case 'shouldStart':
        t = WebViewState.shouldStart;
        break;
      case 'startLoad':
        t = WebViewState.startLoad;
        break;
      case 'finishLoad':
        t = WebViewState.finishLoad;
        break;
      case 'abortLoad':
        t = WebViewState.abortLoad;
        break;
    }
    return WebViewStateChanged(t, map['url'], map['navigationType']);
  }

  final WebViewState type;
  final String url;
  final int navigationType;
}

class WebViewHttpError {
  WebViewHttpError(this.code, this.url);

  final String url;
  final String code;
}