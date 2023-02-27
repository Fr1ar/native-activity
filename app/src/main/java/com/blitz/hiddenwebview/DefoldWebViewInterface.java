package com.blitz.hiddenwebview;

import android.util.Log;

public class DefoldWebViewInterface {
    private static final String TAG = "HiddenWebViewLog";

    public void onScriptFinished(String result, int id)
    {
        Log.d(TAG, "DefoldWebViewInterface.onScriptFinished result = " + result);
    }
    public void onScriptCallback(String type, String payload)
    {
        Log.d(TAG, "DefoldWebViewInterface.onScriptCallback payload = " + payload);
    }
    public void executeScript(String js, int id) {
        Log.d(TAG, "DefoldWebViewInterface.executeScript: " + js);

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.executeScript(js, id);
        });
    }

    public void loadGame(String gamePath, int id) {
        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.loadGame(gamePath, id);
        });
    }

    public void loadWebPage(String gamePath, int id) {
        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.loadWebPage(gamePath, id);
        });
    }

    public void changeVisibility(int visible) {
        Log.d(TAG, "DefoldWebViewInterface.changeVisibility visible = " + visible);

        FakeWebViewActivity.defoldWebViewInterface = this;

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.changeVisibility(visible);
        });
    }

    public void setDebugEnabled(int flag) {
        Log.d(TAG, "DefoldWebViewInterface.setDebugEnabled flag = " + flag);

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.setDebugEnabled(flag);
        });
    }

    public void setTouchInterceptor(double width, double height, double x, double y) {
        Log.d(TAG, "DefoldWebViewInterface.setTouchInterceptor");

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.setTouchInterceptor(height, width, x, y);
        });
    }

    public void setPositionAndSize(double width, double height, double x, double y) {
        Log.d(TAG, "DefoldWebViewInterface.setPositionAndSize width = " + width + 
            ", height = " + height + ", x = " + x + ", y = " + y);

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.setPositionAndSize(height, width, x, y);
        });
    }

    public void acceptTouchEvents(int accept) {
        Log.d(TAG, "DefoldWebViewInterface.acceptTouchEvents");

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(() -> {
            FakeWebViewActivity.acceptTouchEvents(accept);
        });
    }

    public int isInUse() {
        Log.d(TAG, "DefoldWebViewInterface.isInUse");

        return FakeWebViewActivity.webViewActive ? 1 : 0;
    }

    public void centerWebView() {
        Log.d(TAG, "DefoldWebViewInterface.centerWebView");

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(FakeWebViewActivity::centerWebView);
    }

    public void matchScreenSize() {
        Log.d(TAG, "DefoldWebViewInterface.matchScreenSize");

        CurrentActivityAwareApplication.currentlyOpenedActivity.runOnUiThread(FakeWebViewActivity::matchScreenSize);
    }
}