package com.pichillilorenzo.flutter_inappbrowser;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry;

/**
 * FlutterWebviewPlugin
 */
public class FlutterWebviewPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {
    private Activity activity;
    private WebviewManager webViewManager;
    private Context context;
    static MethodChannel channel;
    private static final String CHANNEL_NAME = "flutter_webview_plugin";
    TextView webTitle;

    public static void registerWith(PluginRegistry.Registrar registrar) {
        Log.e("FlutterWebviewPlugin", "registerWith");
        channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
        final FlutterWebviewPlugin instance = new FlutterWebviewPlugin(registrar.activity(),registrar.activeContext());
        registrar.addActivityResultListener(instance);
        channel.setMethodCallHandler(instance);
    }

    private FlutterWebviewPlugin(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        switch (call.method) {
            case "launch":
                openUrl(call, result);
                break;
            case "close":
                close(call, result);
                break;
            case "eval":
                eval(call, result);
                break;
            case "resize":
                resize(call, result);
                break;
            case "reload":
                reload(call, result);
                break;
            case "back":
                back(call, result);
                break;
            case "forward":
                forward(call, result);
                break;
            case "hide":
                hide(call, result);
                break;
            case "show":
                show(call, result);
                break;
            case "reloadUrl":
                reloadUrl(call, result);
                break;
            case "stopLoading":
                stopLoading(call, result);
                break;
            case "cleanCookies":
                cleanCookies(call, result);
                break;

            case "getTitle":
                result.success(webViewManager != null ? webViewManager.getTitle() : null);
                break;

            case "setWebTitle":
                setWebTitle(call, result);
                break;

            case "showShareDialog":
                showShareDialog();
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }


    private void showShareDialog(){
        //1、使用Dialog、设置style
        final Dialog dialog = new Dialog(activity,R.style.DialogTheme);
        //2、设置布局
        View view = View.inflate(activity,R.layout.share_layout,null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        //设置弹出位置
        window.setGravity(Gravity.BOTTOM);
        //设置弹出动画
        window.setWindowAnimations(R.style.main_menu_animStyle);
        //设置对话框大小
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(activity,100));
        dialog.show();

        dialog.findViewById(R.id.session).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlutterWebviewPlugin.channel.invokeMethod("onSession", new HashMap<>());
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.timeline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlutterWebviewPlugin.channel.invokeMethod("onTimeLine", new HashMap<>());
                dialog.dismiss();
            }
        });
    }

    private void openUrl(MethodCall call, MethodChannel.Result result) {
        boolean hidden = call.argument("hidden");
        String url = call.argument("url");
        String userAgent = call.argument("userAgent");
        boolean withJavascript = call.argument("withJavascript");
        boolean clearCache = call.argument("clearCache");
        boolean clearCookies = call.argument("clearCookies");
        boolean withZoom = call.argument("withZoom");
        boolean displayZoomControls = call.argument("displayZoomControls");
        boolean withLocalStorage = call.argument("withLocalStorage");
        boolean withOverviewMode = call.argument("withOverviewMode");
        boolean supportMultipleWindows = call.argument("supportMultipleWindows");
        boolean appCacheEnabled = call.argument("appCacheEnabled");
        Map<String, String> headers = call.argument("headers");
        boolean scrollBar = call.argument("scrollBar");
        boolean allowFileURLs = call.argument("allowFileURLs");
        boolean useWideViewPort = call.argument("useWideViewPort");
        String invalidUrlRegex = call.argument("invalidUrlRegex");
        boolean geolocationEnabled = call.argument("geolocationEnabled");
        boolean debuggingEnabled = call.argument("debuggingEnabled");
        double paddingTop = getStatusBarHeight(activity);

        if (webViewManager == null || webViewManager.closed == true) {
            webViewManager = new WebviewManager(activity, context);
        }

        FrameLayout.LayoutParams params = buildLayoutParams(call);
        View view = activity.getLayoutInflater().inflate(R.layout.web_header, null);
        view.findViewById(R.id.iv_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShareDialog();
            }
        });

        webTitle = view.findViewById(R.id.tv_web_title);
        view.findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //back
                FlutterWebviewPlugin.channel.invokeMethod("onBack", new HashMap<>());
            }
        });
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        Log.e("paddingTop","paddingTop " + paddingTop);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = (int) paddingTop;
        view.setLayoutParams(layoutParams);
        linearLayout.addView(view);
        linearLayout.addView(webViewManager.webView);
        activity.addContentView(linearLayout, params);
        webViewManager.openUrl(withJavascript,
                clearCache,
                hidden,
                clearCookies,
                userAgent,
                url,
                headers,
                withZoom,
                displayZoomControls,
                withLocalStorage,
                withOverviewMode,
                scrollBar,
                supportMultipleWindows,
                appCacheEnabled,
                allowFileURLs,
                useWideViewPort,
                invalidUrlRegex,
                geolocationEnabled,
                debuggingEnabled
        );
        result.success(null);
    }

    private FrameLayout.LayoutParams buildLayoutParams(MethodCall call) {
        Map<String, Number> rc = call.argument("rect");
        FrameLayout.LayoutParams params;
        if (rc != null) {
            params = new FrameLayout.LayoutParams(
                    dp2px(activity, rc.get("width").intValue()), dp2px(activity, rc.get("height").intValue()));
            params.setMargins(dp2px(activity, rc.get("left").intValue()), dp2px(activity, rc.get("top").intValue()),
                    0, 0);
        } else {
            Display display = activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            params = new FrameLayout.LayoutParams(width, height);
        }

        return params;
    }

    private void setWebTitle(MethodCall call, MethodChannel.Result result){
        String title = call.argument("title");
        if(webTitle != null){
            webTitle.setText(" " + title);
        }
        result.success(null);
    }

    private void stopLoading(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.stopLoading(call, result);
        }
        result.success(null);
    }

    private void close(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.close(call, result);
            webViewManager = null;
        }
    }

    /**
     * Navigates back on the Webview.
     */
    private void back(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.back(call, result);
        }
        result.success(null);
    }

    /**
     * Navigates forward on the Webview.
     */
    private void forward(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.forward(call, result);
        }
        result.success(null);
    }

    /**
     * Reloads the Webview.
     */
    private void reload(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.reload(call, result);
        }
        result.success(null);
    }

    private void reloadUrl(MethodCall call, MethodChannel.Result result) {
        if (webViewManager != null) {
            String url = call.argument("url");
            Map<String, String> headers = call.argument("headers");
            if (headers != null) {
                webViewManager.reloadUrl(url, headers);
            } else {
                webViewManager.reloadUrl(url);
            }

        }
        result.success(null);
    }

    private void eval(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.eval(call, result);
        }
    }

    private void resize(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            FrameLayout.LayoutParams params = buildLayoutParams(call);
            webViewManager.resize(params);
        }
        result.success(null);
    }

    private void hide(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.hide(call, result);
        }
        result.success(null);
    }

    private void show(MethodCall call, final MethodChannel.Result result) {
        if (webViewManager != null) {
            webViewManager.show(call, result);
        }
        result.success(null);
    }

    private void cleanCookies(MethodCall call, final MethodChannel.Result result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean aBoolean) {

                }
            });
        } else {
            CookieManager.getInstance().removeAllCookie();
        }
        result.success(null);
    }

    private int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public boolean onActivityResult(int i, int i1, Intent intent) {
        if (webViewManager != null && webViewManager.resultHandler != null) {
            return webViewManager.resultHandler.handleResult(i, i1, intent);
        }
        return false;
    }
}
