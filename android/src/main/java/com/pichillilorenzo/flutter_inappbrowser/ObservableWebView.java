package com.pichillilorenzo.flutter_inappbrowser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import java.util.HashMap;
import java.util.Map;

public class ObservableWebView extends WebView {
    private static final String TAG = "ObservableWebView";
    private OnScrollChangedCallback mOnScrollChangedCallback;
    private MenuItem.OnMenuItemClickListener menuHandler;

    static final String consoleLogJS = "(function() {" +
            "   var oldLogs = {" +
            "       'log': console.log," +
            "       'debug': console.debug," +
            "       'error': console.error," +
            "       'info': console.info," +
            "       'warn': console.warn" +
            "   };" +
            "   for (var k in oldLogs) {" +
            "       (function(oldLog) {" +
            "           console[oldLog] = function() {" +
            "               var message = '';" +
            "               for (var i in arguments) {" +
            "                   if (message == '') {" +
            "                       message += arguments[i];" +
            "                   }" +
            "                   else {" +
            "                       message += ' ' + arguments[i];" +
            "                   }" +
            "               }" +
            "               oldLogs[oldLog].call(console, message);" +
            "           }" +
            "       })(k);" +
            "   }" +
            "})();";

    static final String platformReadyJS = "window.dispatchEvent(new Event('flutterInAppBrowserPlatformReady'));";

    public ObservableWebView(final Context context)
    {
        super(context);
        init();
    }

    public ObservableWebView(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public ObservableWebView(final Context context, final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        menuHandler = new MenuItem.OnMenuItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        clipData();
                        break;
                    case 2:
                        share();
                        break;
                }
                return true;
            }
        };
    }


    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);
        if(mOnScrollChangedCallback != null) mOnScrollChangedCallback.onScroll(l, t, oldl, oldt);
    }

    public OnScrollChangedCallback getOnScrollChangedCallback()
    {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(final OnScrollChangedCallback onScrollChangedCallback)
    {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }

    /**
     * Impliment in the activity/fragment/view that you want to listen to the webview
     */
    public static interface OnScrollChangedCallback
    {
        public void onScroll(int l, int t, int oldl, int oldt);
    }

    //add by larry start
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        Log.e(TAG,"startActionMode  1111");
        ActionMode actionMode = super.startActionMode(callback);
        return resolveMode(actionMode);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        Log.e(TAG,"startActionMode  2222");
        ActionMode actionMode = super.startActionMode(callback, type);
        return resolveMode(actionMode);
    }

    public ActionMode resolveMode(ActionMode actionMode) {
        if (actionMode != null){
            final Menu menu = actionMode.getMenu();
            menu.clear();
            Log.e(TAG,"resolveMode  2222 " + menu.size());
//            for(int i = 0; i< menu.size(); i++) {
//                MenuItem item = menu.getItem(i);
//                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//                    @Override
//                    public boolean onMenuItemClick(MenuItem item) {
//                        return false;
//                    }
//                });
//                String title = item.toString();
//                if(title.equals("复制") || title.equals("分享") || title.equals("网页搜索") || title.equals("全选")
//                || title.equals("浏览器搜索") ||title.equals("翻译") || title.equals("搜索") ) {
//                    item.setVisible(false);
//                }
//            }
//            menu.add(0, 1, 0, "复制").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//                    Log.e(TAG," onMenuItemClick copy");
//                    clipData();
//                    return true;
//                }
//            });
            menu.add(0, 2, 1, "分享").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Log.e(TAG," onMenuItemClick sahre");
                    share();
                    return true;
                }
            });
        }
        return actionMode;
    }
    //end

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void share(){
        Log.e(TAG,"share......");
        this.evaluateJavascript("window.getSelection().toString();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.e(TAG,"onReceiveValue......" + value);
                Map<String, Object> obj = new HashMap<>();
                obj.put("text", value);
                obj.put("url", getUrl());
                FlutterWebviewPlugin.channel.invokeMethod("onSelectText", obj);
                clearFocus();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void clipData() {
        this.evaluateJavascript("window.getSelection().toString();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                value = value.substring(1, value.length()-1);
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("Label", value);
                cm.setPrimaryClip(mClipData);
                Toast.makeText(getContext(),"复制成功",Toast.LENGTH_SHORT).show();
                clearFocus();
            }
        });

    }
}