package com.blitz.hiddenwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.webkit.JavascriptInterface;
import android.graphics.Point;
import android.view.Display;
import android.util.Log;
import android.os.Build;
import android.content.res.Configuration;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class TouchInterceptorView extends View {
    public boolean isAcceptingTouchEvents = true;

    public TouchInterceptorView(Context context) {
        super(context);
    }

    public TouchInterceptorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchInterceptorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isAcceptingTouchEvents) {
            FakeWebViewActivity.htmlGameView.dispatchTouchEvent(event);
        }
        return false;
    }
}

public class FakeWebViewActivity extends Activity {
    // это WebView живет все время сколько живет приложение
    // поэтому утечки памяти от хранения в статичной переменной не будет
    // так же храним как static, чтобы избежать уничтожения самой view
    @SuppressLint("StaticFieldLeak")
    static WebView htmlGameView;
    FrameLayout mainContainer;
    static TouchInterceptorView touchInterceptorView;
    static WindowManager.LayoutParams touchInterceptorViewParams;
    static DefoldWebViewInterface defoldWebViewInterface;

    static CustomWebChromeClient customWebChromeClient;
    static CustomWebViewClient customWebViewClient;
    static Resources resources;

    public static final String TAG = "HiddenWebViewLog";

    private static final long ACTIVITY_DETECT_DELAY_MS = 10;
    public static boolean webViewActive = false;

    public void onPageLoading(String url, int id) {
        Log.d(TAG, "FakeWebViewActivity.onPageLoading url: " + url);
    }
    public void onPageFinished(String url, int id) {
        Log.d(TAG, "FakeWebViewActivity.onPageFinished url: " + url);
    }
    public void onReceivedError(String url, String errorMessage, int id) {
        Log.d(TAG, "FakeWebViewActivity.onReceivedError url: " + url + ", errorMessage: " + errorMessage);
    }
    public void onScriptFailed(String errorMessage, int id) {
        Log.d(TAG, "FakeWebViewActivity.onScriptFailed errorMessage: " + errorMessage);
    }

    private static class CustomWebChromeClient extends WebChromeClient {
        private final FakeWebViewActivity webviewJNI;
        private int scriptId;

        public CustomWebChromeClient(FakeWebViewActivity webviewJNI) {

            Log.d(FakeWebViewActivity.TAG, "CustomWebChromeClient.CustomWebChromeClient");

            this.webviewJNI = webviewJNI;
            reset(-1);
        }

        public void reset(int scriptId) {
            this.scriptId = scriptId;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public boolean onConsoleMessage(ConsoleMessage msg) {


            Log.d(FakeWebViewActivity.TAG, "CustomWebChromeClient.onConsoleMessage: " + msg.message());

            if (msg.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                webviewJNI.onScriptFailed(String.format("js:%d: %s", msg.lineNumber(), msg.message()), scriptId);
                return true;
            }

            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public static class CustomWebViewClient extends WebViewClient {
        private final FakeWebViewActivity webviewJNI;

        private boolean hasError;
        private int requestId;

        public void reset(int scriptId) {


            Log.d(FakeWebViewActivity.TAG, "CustomWebViewClient.reset");

            this.requestId = scriptId;
            this.hasError = false;
        }

        public CustomWebViewClient(FakeWebViewActivity webviewJNI) {
            super();


            Log.d(FakeWebViewActivity.TAG, "CustomWebViewClient");


            this.webviewJNI = webviewJNI;
            reset();
        }

        public void reset() {

            Log.d(FakeWebViewActivity.TAG, "CustomWebChromeClient.reset");

            this.hasError = false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            Log.d(FakeWebViewActivity.TAG, "CustomWebChromeClient.shouldOverrideUrlLoading");

            webviewJNI.onPageLoading(url, requestId);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            Log.d(FakeWebViewActivity.TAG, "CustomWebChromeClient.onPageFinished");

            webviewJNI.onPageFinished(url, requestId);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            Log.d(FakeWebViewActivity.TAG, "CustomWebChromeClient.onReceivedError");

            if (!this.hasError) {
                this.hasError = true;
                webviewJNI.onReceivedError(failingUrl, description, requestId);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resources = getResources();

        Log.d(TAG, "FakeWebViewActivity.onCreate starts");

        setDefaultContainer();
        openDefoldMainSurfaceActivity();


        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            new Runnable() {
                public void run() {
                    // openWebViewActivity();
                    setTouchInterceptor(3000, 3000, 0, 0);
                    changeVisibility(1);
                }
            },
        2000);


        initWebView();
        updateFullscreenMode();

        Log.d(TAG, "FakeWebViewActivity.onCreate ends");
    }

    private void updateFullscreenMode() {
        Log.d(TAG, "FakeWebViewActivity.updateFullscreenMode");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            Log.d(TAG, "FakeWebViewActivity.updateFullscreenMode KITKAT");

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            Log.d(TAG, "FakeWebViewActivity.updateFullscreenMode VERSION_CODES.P");

            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        Log.d(TAG, "FakeWebViewActivity.onWindowFocusChanged");

        if (hasFocus) {
            updateFullscreenMode();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        Log.d(TAG, "FakeWebViewActivity.onConfigurationChanged");

        super.onConfigurationChanged(newConfig);
        if (newConfig.keyboardHidden == Configuration.KEYBOARDHIDDEN_YES) {
            updateFullscreenMode();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {

        Log.d(TAG, "FakeWebViewActivity.initWebView");

        htmlGameView = new WebView(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            htmlGameView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark, getResources().newTheme()));
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.gravity = Gravity.CENTER;

        htmlGameView.setLayoutParams(params);

        WebSettings webSettings = htmlGameView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        htmlGameView.setFocusable(true);
        htmlGameView.setFocusableInTouchMode(true);
        htmlGameView.setClickable(true);
        htmlGameView.requestFocus(View.FOCUS_DOWN);
        htmlGameView.setHapticFeedbackEnabled(true);
        htmlGameView.requestFocusFromTouch();

        htmlGameView.addJavascriptInterface(
            new JavaScriptInterface(
                this, 
                (type, data) -> {
                        
                    Log.d(TAG, "JavaScriptInterface(type, data)");

                    defoldWebViewInterface.onScriptCallback(type, data);
                }
            ), 
            "defoldHandler"
        );

        customWebChromeClient = new CustomWebChromeClient(this);
        customWebViewClient = new CustomWebViewClient(this);

        htmlGameView.setWebChromeClient(customWebChromeClient);
        htmlGameView.setWebViewClient(customWebViewClient);

        htmlGameView.setVisibility(View.GONE);

        mainContainer.addView(htmlGameView, params);

        loadGame("/GAME_PATH_DUMMY", 1);

        // TODO: REMOVE THIS CODE!!!!
        // openWebViewActivity();
        WebView.setWebContentsDebuggingEnabled(true);
        // TODO: REMOVE THIS CODE!!!!


        Log.d(TAG, "FakeWebViewActivity.initWebView view added");
    }

    public static void acceptTouchEvents(int accept) {
        Log.d(TAG, "FakeWebViewActivity.acceptTouchEvents");

        if (touchInterceptorView == null) {
            return;
        }

        touchInterceptorView.isAcceptingTouchEvents = accept == 1;

        WindowManager wm = CurrentActivityAwareApplication.currentlyOpenedActivity.getWindowManager();

        if (accept == 1) {
            Log.d(TAG, "FakeWebViewActivity.acceptTouchEvents accept == 1");

            wm.removeViewImmediate(touchInterceptorView);
            wm.addView(touchInterceptorView, touchInterceptorViewParams);
        } else {
            Log.d(TAG, "FakeWebViewActivity.acceptTouchEvents accept != 1");

            wm.removeViewImmediate(touchInterceptorView);
        }
    }

    private static float dpToPx(double dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) dp,
                CurrentActivityAwareApplication.currentlyOpenedActivity.getResources().getDisplayMetrics()
        );
    }

    public static void executeScript(String js, int id) {
        
        Log.d(TAG, "FakeWebViewActivity.executeScript: " + js);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            htmlGameView.evaluateJavascript(js, value -> defoldWebViewInterface.onScriptFinished(value, id));
        }
    }

    //load file from apps res/raw folder or Assets folder
    public static String LoadFile(String fileName, boolean loadFromRawFolder) throws IOException
    {
        //Create a InputStream to read the file into
        InputStream iS;

        if (loadFromRawFolder)
        {
            //get the resource id from the file name
            int rID = resources.getIdentifier("com.example.native_activity:raw/"+fileName, null, null);
            //get the file as a stream
            iS = resources.openRawResource(rID);
        }
        else
        {
            //get the file as a stream
            iS = resources.getAssets().open(fileName);
        }

        //create a buffer that has the same size as the InputStream
        byte[] buffer = new byte[iS.available()];
        //read the text file as a stream, into the buffer
        iS.read(buffer);
        //create a output stream to write the buffer into
        ByteArrayOutputStream oS = new ByteArrayOutputStream();
        //write this buffer to the output stream
        oS.write(buffer);
        //Close the Input and Output streams
        oS.close();
        iS.close();

        //return the output stream as a String
        return oS.toString();
    }

    public static void loadGame(String gamePath, int id) {

        Log.d(TAG, "FakeWebViewActivity.loadGame");

        customWebViewClient.reset(id);
        customWebChromeClient.reset(id);

        // htmlGameView.loadUrl("http://localhost:8808/" + gamePath);

        try {
            String strData = LoadFile("sketchpad.html", false);
            // strData = "<html><head></head><body><div style='color:red'>HELLOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO</div></body></html>";

            htmlGameView.loadData(strData, "text/html", "UTF-8");
        }
        catch (IOException e) {
            Log.e(TAG, "FakeWebViewActivity.loadGame: File: not found!");
        }

        Log.d(TAG, "FakeWebViewActivity.loadGame ended");
    }

    public static void loadWebPage(String gamePath, int id) {

        Log.d(TAG, "FakeWebViewActivity.loadWebPage");

        customWebViewClient.reset(id);
        customWebChromeClient.reset(id);

        htmlGameView.loadUrl(gamePath);
    }

    public static void setDebugEnabled(int flag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(flag == 1);
        }
    }

    public static void changeVisibility(int visible) {

        Log.d(TAG, "FakeWebViewActivity.changeVisibility visible = " + visible);

        webViewActive = visible == 1;
        htmlGameView.setVisibility(webViewActive ? View.VISIBLE : View.GONE);

        if (visible == 0) {
            htmlGameView.goBack();

            WindowManager wm = CurrentActivityAwareApplication.currentlyOpenedActivity.getWindowManager();
            wm.removeViewImmediate(touchInterceptorView);
        }
    }

    public static void setTouchInterceptor(double height, double width, double x, double y) {
        Log.d(TAG, "FakeWebViewActivity.setTouchInterceptor");

        Activity activity = CurrentActivityAwareApplication.currentlyOpenedActivity;
        WindowManager wm = activity.getWindowManager();

        if (touchInterceptorView != null) {
            wm.removeViewImmediate(touchInterceptorView);
        }

        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        Log.d(TAG, "FakeWebViewActivity.setTouchInterceptor: " + activity.getLocalClassName());

        touchInterceptorView = new TouchInterceptorView(activity);
        touchInterceptorView.setBackground(new ColorDrawable(Color.GRAY)); // TRANSPARENT

        touchInterceptorViewParams = new WindowManager.LayoutParams();
        touchInterceptorViewParams.gravity = Gravity.CENTER;
        touchInterceptorViewParams.x = (int) dpToPx(screenWidth * x);
        touchInterceptorViewParams.y = (int) dpToPx(screenHeight * y);
        touchInterceptorViewParams.width =  (int) dpToPx(screenWidth * width);
        touchInterceptorViewParams.height = (int) dpToPx(screenHeight * height);
        touchInterceptorViewParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            touchInterceptorViewParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        touchInterceptorViewParams.alpha = 0.0f;
        touchInterceptorViewParams.format = PixelFormat.TRANSLUCENT;

        wm.addView(touchInterceptorView, touchInterceptorViewParams);
    }

    public static void setPositionAndSize(double height, double width, double x, double y) {
        Log.d(TAG, "FakeWebViewActivity.setPositionAndSize height = " + height + ", width = " + width +
            ", x = " + x + ", y = " + y);

        Display display = CurrentActivityAwareApplication.currentlyOpenedActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        Log.d(TAG, "FakeWebViewActivity.setPositionAndSize screenWidth = " + screenWidth + 
            ", screenHeight = " + screenHeight);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) htmlGameView.getLayoutParams();
        params.height = (int)(screenHeight * height);
        params.width = (int)(screenWidth * width);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setMargins((int)(screenWidth * x), (int)(screenHeight * y), 0, 0);

        htmlGameView.setLayoutParams(params);
        htmlGameView.requestLayout();
    }

    public static void centerWebView() {
        Log.d(TAG, "FakeWebViewActivity.centerWebView");

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) htmlGameView.getLayoutParams();
        params.gravity = Gravity.CENTER;
        params.setMargins(0, 0, 0, 0);

        htmlGameView.setLayoutParams(params);
        htmlGameView.requestLayout();
    }

    public static void matchScreenSize() {
        Log.d(TAG, "FakeWebViewActivity.matchScreenSize");

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        params.gravity = Gravity.CENTER;

        htmlGameView.setLayoutParams(params);
        htmlGameView.requestLayout();
    }

    private void setDefaultContainer() {
        Log.d(TAG, "FakeWebViewActivity.setDefaultContainer");

        mainContainer = new FrameLayout(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mainContainer.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright, getResources().newTheme()));
        }
        setContentView(mainContainer);
    }

    private void openDefoldMainSurfaceActivity() {
        Log.d(TAG, "FakeWebViewActivity.openDefoldMainSurfaceActivity");

        try {
            Intent intent = new Intent(this, Class.forName("com.dynamo.android.DefoldActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } catch (Throwable e) {
            e.printStackTrace();

            Log.e(TAG, "FakeWebViewActivity.openDefoldMainSurfaceActivity failed: " + e.getMessage());
        }
    }

    private void openWebViewActivity() {
        Log.d(TAG, "FakeWebViewActivity.openWebViewActivity");

        try {
            Intent intent = new Intent(this, Class.forName("com.blitz.hiddenwebview.FakeWebViewActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } catch (Throwable e) {
            e.printStackTrace();

            Log.e(TAG, "FakeWebViewActivity.openWebViewActivity failed: " + e.getMessage());
        }
    }


    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "FakeWebViewActivity.onNewIntent");

        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "FakeWebViewActivity.onNewIntent action is null");
            super.onNewIntent(intent);
            return;
        }

        if (action.equals(Intent.ACTION_MAIN)) {
            Log.d(TAG, "FakeWebViewActivity.onNewIntent Intent.ACTION_MAIN");
            openDefoldMainSurfaceActivity();
        } else {
            Log.d(TAG, "FakeWebViewActivity.onNewIntent onNewIntent");

            super.onNewIntent(intent);
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onStart() {

        Log.d(FakeWebViewActivity.TAG, "FakeWebViewActivity.ClickableViewAccessibility.onStart");

        super.onStart();

        // не работает с DefoldActivity
        // НО! работает с NativeActivity

        /*
        (new Handler()).postDelayed(() -> {
            try {
                Activity topLevelActivity = CurrentActivityAwareApplication.currentlyOpenedActivity;
                final View viewGroup = topLevelActivity.getWindow().getDecorView();

                viewGroup.setOnTouchListener((View v, MotionEvent event) -> {
                    if (webViewActive) {
                        htmlGameView.onTouchEvent(event);
                    }

                    return false;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ACTIVITY_DETECT_DELAY_MS);
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}

interface JavaScriptHandler {
  void handle(String messageType, String payload);
}

class JavaScriptInterface {
  private Activity сontext;
  private JavaScriptHandler handler;

  JavaScriptInterface(Activity activity, JavaScriptHandler handler) {

    Log.d(FakeWebViewActivity.TAG, "JavaScriptInterface");

    сontext = activity;
    this.handler = handler;
  }

  @JavascriptInterface
  public void handle(String messageType, String payload) {

    Log.d(FakeWebViewActivity.TAG, "JavaScriptInterface.handle");

    this.сontext.runOnUiThread(() -> {
      handler.handle(messageType, payload);
    });
  }
}
