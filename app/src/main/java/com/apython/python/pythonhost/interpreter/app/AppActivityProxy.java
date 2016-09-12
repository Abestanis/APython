package com.apython.python.pythonhost.interpreter.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.app.SharedElementCallback;
import android.app.TaskStackBuilder;
import android.app.VoiceInteractor;
import android.app.assist.AssistContent;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toolbar;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * This Activity proxies most calls to the provided real activity,
 * but intercepts calls related to the context and the classloader.
 * 
 * Created by Sebastian on 09.09.2016.
 */
@SuppressLint("Registered")
@SuppressWarnings("deprecation")
public class AppActivityProxy extends Activity {

    private final Activity wrappedActivity;
    private final Context  context;

    public AppActivityProxy(Activity wrappedActivity, Context context) {
        super();
        this.wrappedActivity = wrappedActivity;
        this.context = context;
    }

    @Override
    public ClassLoader getClassLoader() {
        return context.getClassLoader();
    }

    @Override
    public Context getBaseContext() {
        return context;
    }

    @Override
    public Context getApplicationContext() {
        return context;
    }

    @Override
    public Intent getIntent() {
        return wrappedActivity.getIntent();
    }

    @Override
    public void setIntent(Intent newIntent) {
        wrappedActivity.setIntent(newIntent);
    }

    @Override
    public WindowManager getWindowManager() {
        return wrappedActivity.getWindowManager();
    }

    @Override
    public Window getWindow() {
        return wrappedActivity.getWindow();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public LoaderManager getLoaderManager() {
        return wrappedActivity.getLoaderManager();
    }

    @Nullable
    @Override
    public View getCurrentFocus() {
        return wrappedActivity.getCurrentFocus();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        wrappedActivity.onCreate(savedInstanceState, persistentState);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        wrappedActivity.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        wrappedActivity.onPostCreate(savedInstanceState, persistentState);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onStateNotSaved() {
        wrappedActivity.onStateNotSaved();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean isVoiceInteraction() {
        return wrappedActivity.isVoiceInteraction();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean isVoiceInteractionRoot() {
        return wrappedActivity.isVoiceInteractionRoot();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public VoiceInteractor getVoiceInteractor() {
        return wrappedActivity.getVoiceInteractor();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        wrappedActivity.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
        return wrappedActivity.onCreateThumbnail(outBitmap, canvas);
    }

    @Nullable
    @Override
    public CharSequence onCreateDescription() {
        return wrappedActivity.onCreateDescription();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onProvideAssistData(Bundle data) {
        wrappedActivity.onProvideAssistData(data);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onProvideAssistContent(AssistContent outContent) {
        wrappedActivity.onProvideAssistContent(outContent);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean showAssist(Bundle args) {
        return wrappedActivity.showAssist(args);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void reportFullyDrawn() {
        wrappedActivity.reportFullyDrawn();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        wrappedActivity.onConfigurationChanged(newConfig);
    }

    @Override
    public int getChangingConfigurations() {
        return wrappedActivity.getChangingConfigurations();
    }

    @Nullable
    @Override
    public Object getLastNonConfigurationInstance() {
        return wrappedActivity.getLastNonConfigurationInstance();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return wrappedActivity.onRetainNonConfigurationInstance();
    }

    @Override
    public void onLowMemory() {
        wrappedActivity.onLowMemory();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        wrappedActivity.onTrimMemory(level);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public FragmentManager getFragmentManager() {
        return wrappedActivity.getFragmentManager();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onAttachFragment(Fragment fragment) {
        wrappedActivity.onAttachFragment(fragment);
    }

    @Override
    public void startManagingCursor(Cursor c) {
        wrappedActivity.startManagingCursor(c);
    }

    @Override
    public void stopManagingCursor(Cursor c) {
        wrappedActivity.stopManagingCursor(c);
    }

    @Override
    public View findViewById(int id) {
        return wrappedActivity.findViewById(id);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Nullable
    @Override
    public ActionBar getActionBar() {
        return wrappedActivity.getActionBar();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setActionBar(Toolbar toolbar) {
        wrappedActivity.setActionBar(toolbar);
    }

    @Override
    public void setContentView(int layoutResID) {
        wrappedActivity.setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        wrappedActivity.setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        wrappedActivity.setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        wrappedActivity.addContentView(view, params);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public TransitionManager getContentTransitionManager() {
        return wrappedActivity.getContentTransitionManager();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setContentTransitionManager(TransitionManager tm) {
        wrappedActivity.setContentTransitionManager(tm);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Scene getContentScene() {
        return wrappedActivity.getContentScene();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setFinishOnTouchOutside(boolean finish) {
        wrappedActivity.setFinishOnTouchOutside(finish);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return wrappedActivity.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return wrappedActivity.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return wrappedActivity.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return wrappedActivity.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public void onBackPressed() {
        wrappedActivity.onBackPressed();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return wrappedActivity.onKeyShortcut(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return wrappedActivity.onTouchEvent(event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        return wrappedActivity.onTrackballEvent(event);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return wrappedActivity.onGenericMotionEvent(event);
    }

    @Override
    public void onUserInteraction() {
        wrappedActivity.onUserInteraction();
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
        wrappedActivity.onWindowAttributesChanged(params);
    }

    @Override
    public void onContentChanged() {
        wrappedActivity.onContentChanged();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        wrappedActivity.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onAttachedToWindow() {
        wrappedActivity.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        wrappedActivity.onDetachedFromWindow();
    }

    @Override
    public boolean hasWindowFocus() {
        return wrappedActivity.hasWindowFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return wrappedActivity.dispatchKeyEvent(event);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return wrappedActivity.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return wrappedActivity.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return wrappedActivity.dispatchTrackballEvent(ev);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return wrappedActivity.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return wrappedActivity.dispatchPopulateAccessibilityEvent(event);
    }

    @Nullable
    @Override
    public View onCreatePanelView(int featureId) {
        return wrappedActivity.onCreatePanelView(featureId);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return wrappedActivity.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return wrappedActivity.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return wrappedActivity.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return wrappedActivity.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        wrappedActivity.onPanelClosed(featureId, menu);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void invalidateOptionsMenu() {
        wrappedActivity.invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return wrappedActivity.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return wrappedActivity.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return wrappedActivity.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onNavigateUp() {
        return wrappedActivity.onNavigateUp();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onNavigateUpFromChild(Activity child) {
        return wrappedActivity.onNavigateUpFromChild(child);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreateNavigateUpTaskStack(TaskStackBuilder builder) {
        wrappedActivity.onCreateNavigateUpTaskStack(builder);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onPrepareNavigateUpTaskStack(TaskStackBuilder builder) {
        wrappedActivity.onPrepareNavigateUpTaskStack(builder);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        wrappedActivity.onOptionsMenuClosed(menu);
    }

    @Override
    public void openOptionsMenu() {
        wrappedActivity.openOptionsMenu();
    }

    @Override
    public void closeOptionsMenu() {
        wrappedActivity.closeOptionsMenu();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        wrappedActivity.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void registerForContextMenu(View view) {
        wrappedActivity.registerForContextMenu(view);
    }

    @Override
    public void unregisterForContextMenu(View view) {
        wrappedActivity.unregisterForContextMenu(view);
    }

    @Override
    public void openContextMenu(View view) {
        wrappedActivity.openContextMenu(view);
    }

    @Override
    public void closeContextMenu() {
        wrappedActivity.closeContextMenu();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return wrappedActivity.onContextItemSelected(item);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        wrappedActivity.onContextMenuClosed(menu);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean onSearchRequested(SearchEvent searchEvent) {
        return wrappedActivity.onSearchRequested(searchEvent);
    }

    @Override
    public boolean onSearchRequested() {
        return wrappedActivity.onSearchRequested();
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        wrappedActivity.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
    }

    @Override
    public void triggerSearch(String query, Bundle appSearchData) {
        wrappedActivity.triggerSearch(query, appSearchData);
    }

    @Override
    public void takeKeyEvents(boolean get) {
        wrappedActivity.takeKeyEvents(get);
    }

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        return wrappedActivity.getLayoutInflater();
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return wrappedActivity.getMenuInflater();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        wrappedActivity.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        return wrappedActivity.shouldShowRequestPermissionRationale(permission);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        wrappedActivity.startActivityForResult(intent, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        wrappedActivity.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        wrappedActivity.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        wrappedActivity.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @Override
    public void startActivity(Intent intent) {
        wrappedActivity.startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivity(Intent intent, Bundle options) {
        wrappedActivity.startActivity(intent, options);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void startActivities(Intent[] intents) {
        wrappedActivity.startActivities(intents);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        wrappedActivity.startActivities(intents, options);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        wrappedActivity.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        wrappedActivity.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @Override
    public boolean startActivityIfNeeded(@NonNull Intent intent, int requestCode) {
        return wrappedActivity.startActivityIfNeeded(intent, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean startActivityIfNeeded(@NonNull Intent intent, int requestCode, Bundle options) {
        return wrappedActivity.startActivityIfNeeded(intent, requestCode, options);
    }

    @Override
    public boolean startNextMatchingActivity(@NonNull Intent intent) {
        return wrappedActivity.startNextMatchingActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean startNextMatchingActivity(@NonNull Intent intent, Bundle options) {
        return wrappedActivity.startNextMatchingActivity(intent, options);
    }

    @Override
    public void startActivityFromChild(@NonNull Activity child, Intent intent, int requestCode) {
        wrappedActivity.startActivityFromChild(child, intent, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivityFromChild(@NonNull Activity child, Intent intent, int requestCode, Bundle options) {
        wrappedActivity.startActivityFromChild(child, intent, requestCode, options);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void startActivityFromFragment(@NonNull Fragment fragment, Intent intent, int requestCode) {
        wrappedActivity.startActivityFromFragment(fragment, intent, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivityFromFragment(@NonNull Fragment fragment, Intent intent, int requestCode, Bundle options) {
        wrappedActivity.startActivityFromFragment(fragment, intent, requestCode, options);
    }

    @Override
    public void startIntentSenderFromChild(Activity child, IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        wrappedActivity.startIntentSenderFromChild(child, intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startIntentSenderFromChild(Activity child, IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        wrappedActivity.startIntentSenderFromChild(child, intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @Override
    public void overridePendingTransition(int enterAnim, int exitAnim) {
        wrappedActivity.overridePendingTransition(enterAnim, exitAnim);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Nullable
    @Override
    public Uri getReferrer() {
        return wrappedActivity.getReferrer();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public Uri onProvideReferrer() {
        return wrappedActivity.onProvideReferrer();
    }

    @Nullable
    @Override
    public String getCallingPackage() {
        return wrappedActivity.getCallingPackage();
    }

    @Nullable
    @Override
    public ComponentName getCallingActivity() {
        return wrappedActivity.getCallingActivity();
    }

    @Override
    public void setVisible(boolean visible) {
        wrappedActivity.setVisible(visible);
    }

    @Override
    public boolean isFinishing() {
        return wrappedActivity.isFinishing();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public boolean isDestroyed() {
        return wrappedActivity.isDestroyed();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean isChangingConfigurations() {
        return wrappedActivity.isChangingConfigurations();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void recreate() {
        wrappedActivity.recreate();
    }

    @Override
    public void finish() {
        wrappedActivity.finish();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void finishAffinity() {
        wrappedActivity.finishAffinity();
    }

    @Override
    public void finishFromChild(Activity child) {
        wrappedActivity.finishFromChild(child);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void finishAfterTransition() {
        wrappedActivity.finishAfterTransition();
    }

    @Override
    public void finishActivity(int requestCode) {
        wrappedActivity.finishActivity(requestCode);
    }

    @Override
    public void finishActivityFromChild(@NonNull Activity child, int requestCode) {
        wrappedActivity.finishActivityFromChild(child, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void finishAndRemoveTask() {
        wrappedActivity.finishAndRemoveTask();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean releaseInstance() {
        return wrappedActivity.releaseInstance();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        wrappedActivity.onActivityReenter(resultCode, data);
    }

    @Override
    public PendingIntent createPendingResult(int requestCode, @NonNull Intent data, int flags) {
        return wrappedActivity.createPendingResult(requestCode, data, flags);
    }

    @Override
    public void setRequestedOrientation(int requestedOrientation) {
        wrappedActivity.setRequestedOrientation(requestedOrientation);
    }

    @Override
    public int getRequestedOrientation() {
        return wrappedActivity.getRequestedOrientation();
    }

    @Override
    public int getTaskId() {
        return wrappedActivity.getTaskId();
    }

    @Override
    public boolean isTaskRoot() {
        return wrappedActivity.isTaskRoot();
    }

    @Override
    public boolean moveTaskToBack(boolean nonRoot) {
        return wrappedActivity.moveTaskToBack(nonRoot);
    }

    @NonNull
    @Override
    public String getLocalClassName() {
        return wrappedActivity.getLocalClassName();
    }

    @Override
    public ComponentName getComponentName() {
        return wrappedActivity.getComponentName();
    }

    @Override
    public SharedPreferences getPreferences(int mode) {
        return wrappedActivity.getPreferences(mode);
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        return wrappedActivity.getSystemService(name);
    }

    @Override
    public void setTitle(CharSequence title) {
        wrappedActivity.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        wrappedActivity.setTitle(titleId);
    }

    @Override
    public void setTitleColor(int textColor) {
        wrappedActivity.setTitleColor(textColor);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        wrappedActivity.setTaskDescription(taskDescription);
    }

    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return wrappedActivity.onCreateView(name, context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return wrappedActivity.onCreateView(parent, name, context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        wrappedActivity.dump(prefix, fd, writer, args);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean isImmersive() {
        return wrappedActivity.isImmersive();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean requestVisibleBehind(boolean visible) {
        return wrappedActivity.requestVisibleBehind(visible);
    }

    @SuppressLint("MissingSuperCall")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onVisibleBehindCanceled() {
        wrappedActivity.onVisibleBehindCanceled();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onEnterAnimationComplete() {
        wrappedActivity.onEnterAnimationComplete();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void setImmersive(boolean i) {
        wrappedActivity.setImmersive(i);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Nullable
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return wrappedActivity.startActionMode(callback);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Nullable
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        return wrappedActivity.startActionMode(callback, type);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return wrappedActivity.onWindowStartingActionMode(callback);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
        return wrappedActivity.onWindowStartingActionMode(callback, type);
    }

    @SuppressLint("MissingSuperCall")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onActionModeStarted(ActionMode mode) {
        wrappedActivity.onActionModeStarted(mode);
    }

    @SuppressLint("MissingSuperCall")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onActionModeFinished(ActionMode mode) {
        wrappedActivity.onActionModeFinished(mode);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return wrappedActivity.shouldUpRecreateTask(targetIntent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean navigateUpTo(Intent upIntent) {
        return wrappedActivity.navigateUpTo(upIntent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean navigateUpToFromChild(Activity child, Intent upIntent) {
        return wrappedActivity.navigateUpToFromChild(child, upIntent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Nullable
    @Override
    public Intent getParentActivityIntent() {
        return wrappedActivity.getParentActivityIntent();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setEnterSharedElementCallback(SharedElementCallback callback) {
        wrappedActivity.setEnterSharedElementCallback(callback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setExitSharedElementCallback(SharedElementCallback callback) {
        wrappedActivity.setExitSharedElementCallback(callback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void postponeEnterTransition() {
        wrappedActivity.postponeEnterTransition();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void startPostponedEnterTransition() {
        wrappedActivity.startPostponedEnterTransition();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void startLockTask() {
        wrappedActivity.startLockTask();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stopLockTask() {
        wrappedActivity.stopLockTask();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void showLockTaskEscapeMessage() {
        wrappedActivity.showLockTaskEscapeMessage();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        wrappedActivity.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    public Resources getResources() {
        return wrappedActivity.getResources();
    }

    @Override
    public void setTheme(int resId) {
        wrappedActivity.setTheme(resId);
    }

    @Override
    public Resources.Theme getTheme() {
        return wrappedActivity.getTheme();
    }

    @Override
    public AssetManager getAssets() {
        return wrappedActivity.getAssets();
    }

    @Override
    public PackageManager getPackageManager() {
        return wrappedActivity.getPackageManager();
    }

    @Override
    public ContentResolver getContentResolver() {
        return wrappedActivity.getContentResolver();
    }

    @Override
    public Looper getMainLooper() {
        return wrappedActivity.getMainLooper();
    }

    @Override
    public String getPackageName() {
        return wrappedActivity.getPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return wrappedActivity.getApplicationInfo();
    }

    @Override
    public String getPackageResourcePath() {
        return wrappedActivity.getPackageResourcePath();
    }

    @Override
    public String getPackageCodePath() {
        return wrappedActivity.getPackageCodePath();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return wrappedActivity.getSharedPreferences(name, mode);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return wrappedActivity.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return wrappedActivity.openFileOutput(name, mode);
    }

    @Override
    public boolean deleteFile(String name) {
        return wrappedActivity.deleteFile(name);
    }

    @Override
    public File getFileStreamPath(String name) {
        return wrappedActivity.getFileStreamPath(name);
    }

    @Override
    public String[] fileList() {
        return wrappedActivity.fileList();
    }

    @Override
    public File getFilesDir() {
        return wrappedActivity.getFilesDir();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public File getNoBackupFilesDir() {
        return wrappedActivity.getNoBackupFilesDir();
    }

    @Override
    public File getExternalFilesDir(String type) {
        return wrappedActivity.getExternalFilesDir(type);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public File[] getExternalFilesDirs(String type) {
        return wrappedActivity.getExternalFilesDirs(type);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public File getObbDir() {
        return wrappedActivity.getObbDir();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public File[] getObbDirs() {
        return wrappedActivity.getObbDirs();
    }

    @Override
    public File getCacheDir() {
        return wrappedActivity.getCacheDir();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public File getCodeCacheDir() {
        return wrappedActivity.getCodeCacheDir();
    }

    @Override
    public File getExternalCacheDir() {
        return wrappedActivity.getExternalCacheDir();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public File[] getExternalCacheDirs() {
        return wrappedActivity.getExternalCacheDirs();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public File[] getExternalMediaDirs() {
        return wrappedActivity.getExternalMediaDirs();
    }

    @Override
    public File getDir(String name, int mode) {
        return wrappedActivity.getDir(name, mode);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return wrappedActivity.openOrCreateDatabase(name, mode, factory);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return wrappedActivity.openOrCreateDatabase(name, mode, factory, errorHandler);
    }

    @Override
    public boolean deleteDatabase(String name) {
        return wrappedActivity.deleteDatabase(name);
    }

    @Override
    public File getDatabasePath(String name) {
        return wrappedActivity.getDatabasePath(name);
    }

    @Override
    public String[] databaseList() {
        return wrappedActivity.databaseList();
    }

    @Override
    public Drawable getWallpaper() {
        return wrappedActivity.getWallpaper();
    }

    @Override
    public Drawable peekWallpaper() {
        return wrappedActivity.peekWallpaper();
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return wrappedActivity.getWallpaperDesiredMinimumWidth();
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return wrappedActivity.getWallpaperDesiredMinimumHeight();
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws IOException {
        wrappedActivity.setWallpaper(bitmap);
    }

    @Override
    public void setWallpaper(InputStream data) throws IOException {
        wrappedActivity.setWallpaper(data);
    }

    @Override
    public void clearWallpaper() throws IOException {
        wrappedActivity.clearWallpaper();
    }

    @Override
    public void sendBroadcast(Intent intent) {
        wrappedActivity.sendBroadcast(intent);
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        wrappedActivity.sendBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        wrappedActivity.sendOrderedBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        wrappedActivity.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        wrappedActivity.sendBroadcastAsUser(intent, user);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
        wrappedActivity.sendBroadcastAsUser(intent, user, receiverPermission);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        wrappedActivity.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        wrappedActivity.sendStickyBroadcast(intent);
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        wrappedActivity.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        wrappedActivity.removeStickyBroadcast(intent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        wrappedActivity.sendStickyBroadcastAsUser(intent, user);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        wrappedActivity.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        wrappedActivity.removeStickyBroadcastAsUser(intent, user);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return wrappedActivity.registerReceiver(receiver, filter);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return wrappedActivity.registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        wrappedActivity.unregisterReceiver(receiver);
    }

    @Override
    public ComponentName startService(Intent service) {
        return wrappedActivity.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        return wrappedActivity.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return wrappedActivity.bindService(service, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        wrappedActivity.unbindService(conn);
    }

    @Override
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        return wrappedActivity.startInstrumentation(className, profileFile, arguments);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public String getSystemServiceName(Class<?> serviceClass) {
        return wrappedActivity.getSystemServiceName(serviceClass);
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return wrappedActivity.checkPermission(permission, pid, uid);
    }

    @Override
    public int checkCallingPermission(String permission) {
        return wrappedActivity.checkCallingPermission(permission);
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        return wrappedActivity.checkCallingOrSelfPermission(permission);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public int checkSelfPermission(String permission) {
        return wrappedActivity.checkSelfPermission(permission);
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
        wrappedActivity.enforcePermission(permission, pid, uid, message);
    }

    @Override
    public void enforceCallingPermission(String permission, String message) {
        wrappedActivity.enforceCallingPermission(permission, message);
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
        wrappedActivity.enforceCallingOrSelfPermission(permission, message);
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        wrappedActivity.grantUriPermission(toPackage, uri, modeFlags);
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        wrappedActivity.revokeUriPermission(uri, modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return wrappedActivity.checkUriPermission(uri, pid, uid, modeFlags);
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        return wrappedActivity.checkCallingUriPermission(uri, modeFlags);
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return wrappedActivity.checkCallingOrSelfUriPermission(uri, modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
        return wrappedActivity.checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags);
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        wrappedActivity.enforceUriPermission(uri, pid, uid, modeFlags, message);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        wrappedActivity.enforceCallingUriPermission(uri, modeFlags, message);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        wrappedActivity.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
    }

    @Override
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
        wrappedActivity.enforceUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags, message);
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return wrappedActivity.createPackageContext(packageName, flags);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return wrappedActivity.createConfigurationContext(overrideConfiguration);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public Context createDisplayContext(Display display) {
        return wrappedActivity.createDisplayContext(display);
    }

    @Override
    public boolean isRestricted() {
        return wrappedActivity.isRestricted();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void registerComponentCallbacks(ComponentCallbacks callback) {
        wrappedActivity.registerComponentCallbacks(callback);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        wrappedActivity.unregisterComponentCallbacks(callback);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return wrappedActivity.equals(o);
    }

    @Override
    public int hashCode() {
        return wrappedActivity.hashCode();
    }

    @Override
    public String toString() {
        return wrappedActivity.toString();
    }
}
