package com.pichillilorenzo.flutter_inappbrowser;
import android.util.Log;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import java.util.HashMap;
import java.util.Map;
import io.flutter.plugin.common.MethodChannel;

public class JSBridgeInterface {
    private static final String LOG_TAG = "JSBridgeInterface";
    public static final String name = "flutter_inappbrowser";
    private ObservableWebView flutterWebView;

  public static final String flutterInAppBroserJSClass = "window." + name + ".callHandler = function() {" +
          "var _callHandlerID = setTimeout(function(){});" +
          "window." + name + "._callHandler(arguments[0], _callHandlerID, JSON.stringify(Array.prototype.slice.call(arguments, 1)));" +
          "return new Promise(function(resolve, reject) {" +
          "  window." + name + "[_callHandlerID] = resolve;" +
          "});" +
          "}";

  public JSBridgeInterface(Object obj) {
      this.flutterWebView = (ObservableWebView) obj;
  }

  @JavascriptInterface
  public void _callHandler(String handlerName, final String _callHandlerID, String args) {
    final Map<String, Object> obj = new HashMap<>();
    obj.put("handlerName", handlerName);
    obj.put("args", args);

    // java.lang.RuntimeException: Methods marked with @UiThread must be executed on the main thread.
    // https://github.com/pichillilorenzo/flutter_inappbrowser/issues/98
    final Handler handler = new Handler(Looper.getMainLooper());
    handler.post(new Runnable() {
      @Override
      public void run() {
        FlutterWebviewPlugin.channel.invokeMethod("onCallJsHandler",obj, new MethodChannel.Result() {
          @Override
          public void success(Object json) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
              flutterWebView.evaluateJavascript("window." + name + "[" + _callHandlerID + "](" + json + "); delete window." + name + "[" + _callHandlerID + "];", null);
            }
            else {
              flutterWebView.loadUrl("javascript:window." + name + "[" + _callHandlerID + "](" + json + "); delete window." + name + "[" + _callHandlerID + "];");
            }
          }

          @Override
          public void error(String s, String s1, Object o) {
            Log.d(LOG_TAG, "ERROR: " + s + " " + s1);
          }

          @Override
          public void notImplemented() {

          }
        });
      }
    });

  }

}
