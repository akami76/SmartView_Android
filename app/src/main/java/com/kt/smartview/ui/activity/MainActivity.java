package com.kt.smartview.ui.activity;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kt.smartview.R;
import com.kt.smartview.common.Constants;
import com.kt.smartview.db.DomaDBHandler;
import com.kt.smartview.network.HttpCallback;
import com.kt.smartview.network.HttpEnginCallback;
import com.kt.smartview.network.HttpService;
import com.kt.smartview.support.log.YWMLog;
import com.kt.smartview.support.preference.SmartViewPreference;
import com.kt.smartview.support.task.AsyncTaskResult;
import com.kt.smartview.support.task.TokAsyncTask;
import com.kt.smartview.ui.fragment.LoginFragment;
import com.kt.smartview.utils.BackPressCloseHandler;
import com.kt.smartview.utils.CommonUtil;
import com.kt.smartview.utils.DialogUtils;
import com.kt.smartview.utils.YWMTools;
import com.kt.smartview.utils.rsa.RSACipher;
import com.kt.smartview.vo.AlarmSettingVo;
import com.kt.smartview.vo.LoginVo;
import com.kt.smartview.vo.ResultVo;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.List;

import retrofit2.Call;

import static com.kt.smartview.common.Constants.SITE_MAIN_ACTION_URL;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class MainActivity extends BaseCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SwipeRefreshLayout.OnRefreshListener, LoginFragment.LoginActionListener, View.OnClickListener {

    private static final YWMLog logger = new YWMLog(MainActivity.class);
    public static final String EXTRA_KEY_URL = "EXTRA_KEY_URL";
    private Toolbar mToolbar;
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private DomaDBHandler dbHandler;
    private TokAsyncTask badgeCounterTask;
    private String initUrl = SITE_MAIN_ACTION_URL;
    private boolean isWillClearHistory;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private final String jsHandlerName = "callbackHandler";
    private LoginFragment mLoginFragment;
    private FrameLayout mLoginLayout;
    private BackPressCloseHandler backPressCloseHandler;
    private LoginVo lastLoginVo;
    private RSACipher rsaCipher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHandler = new DomaDBHandler(this);
        backPressCloseHandler = new BackPressCloseHandler(getActivity());
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mLoginLayout = (FrameLayout)findViewById(R.id.layout_login);
        mLoginLayout.setOnClickListener(this);
        mLoginFragment = (LoginFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_login);
        mLoginFragment.setLoginActionListener(this);

        mWebView = (WebView) findViewById(R.id.web_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        initSwipeToRefresh();

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        mDrawerLayout.addDrawerListener(drawerListener);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        initNavigationView();

        mWebView.setWebChromeClient(new MyWebChromeClient());
        mWebView.setWebViewClient(new MyWebClient());
        WebSettings setting = mWebView.getSettings();
        setting.setJavaScriptEnabled(true); //자바스크립트 연동을 설정함.
        setting.setBuiltInZoomControls(true);
        setting.setSupportMultipleWindows(false);
        mWebView.addJavascriptInterface(new WebViewInterface(), jsHandlerName);

        applyAppVersionInfo();

        mWebView.loadUrl(initUrl);
    }

    @Override
    public void onRefresh() {
        mWebView.reload();
    }

    @Override
    public void onClick(View v) {

    }
    private final byte CMD_LOGIN = 0x01;
    private final byte CMD_LOGOUT = 0x02;
    private final byte CMD_ERROR = 0x03;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String json = null;
            ResultVo resultVo = null;
            switch (msg.what){
                case CMD_LOGIN:
                    json = (String)msg.obj;
                    if(TextUtils.isEmpty(json) == false){
                        if(msg.obj != null && msg.obj instanceof String){
                            json = (String)msg.obj;
                            resultVo = getResultDataFromJson(json);
                            if(resultVo != null){
                                if("0".equals(resultVo.getErrorCode())){
                                    if(lastLoginVo != null){
                                        SmartViewPreference.setUserId(getContext(), lastLoginVo.getUserId());
                                        SmartViewPreference.setUserName(getContext(), null);
                                    }
                                    SmartViewPreference.setLogin(getContext(), true);
                                    loginSuccess();
                                }else{
                                    releaseLogin(resultVo.getErrorDescription());
                                }
                            }else{
                                logger.e("Unable to parse json string...");
                                releaseLogin(getString(R.string.message_service_failed_login));
                            }
                        }else{
                            releaseLogin(getString(R.string.message_service_failed_login));
                        }
                    }else{
                        releaseLogin(getString(R.string.message_service_failed_login));
                    }
                    break;
                case CMD_LOGOUT:
                    forceLogOut();
                    break;
                case CMD_ERROR:
                    if(msg.obj != null && msg.obj instanceof String){
                        json = (String)msg.obj;
                        resultVo = getResultDataFromJson(json);
                        if(resultVo != null){
                            DialogUtils.showDialog(getContext(), getSupportFragmentManager(), String.format("%s [%s]",resultVo.getErrorDescription(), resultVo.getErrorCode()), null);
                        }
                    }
                    break;
            }
        }
    };
    /*
     * 자바스크립트에서 호출할 Interface 정의
     */
    private class WebViewInterface {

        // 웹에서 로그아웃이 발생될때 호출됨.
        @JavascriptInterface
        public void mobileLogoutProcess() {
            handler.sendEmptyMessage(CMD_LOGOUT);
        }

        // Request Action에 대한 결과값 CallBack Interface.
        @JavascriptInterface
        public void postMessage(final String json) {
            logger.i("postMessage-->" + json);
            Message msg = new Message();
            msg.what = CMD_LOGIN;
            msg.obj = json;
            handler.sendMessage(msg);
        }

        // 웹에서 에러가 발생될때 호출됨.
        @JavascriptInterface
        public void onError(String json) {
            Message msg = new Message();
            msg.what = CMD_ERROR;
            msg.obj = json;
            handler.sendMessage(msg);
        }

    }

    class MyWebChromeClient extends WebChromeClient {

        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            logger.d("message-->" + message);
            new AlertDialog.Builder(MainActivity.this).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setCancelable(false).create().show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            logger.d("message-->" + message);
            new AlertDialog.Builder(MainActivity.this).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            }).setCancelable(false).create().show();
            return true;
        }
        @Override
        public void onProgressChanged(WebView view, int progress) {
            mProgressBar.setProgress(progress);
            if(progress == 100){
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    class MyWebClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            logger.i("onPageFinished URL-->" + url);
            if (Constants.SITE_MAIN_ACTION_URL.equals(url)) { // /main/dashboard 페이지가 로딩이 완료되면 로그인을 시도한다.
                loginSuccess();
            }else if (url.startsWith(Constants.SITE_LOGIN_PAGE_URL)) {
                forceLogOut();
            }

            if(isWillClearHistory){
                view.clearHistory();
                isWillClearHistory = false;
            }
            mProgressBar.setVisibility(View.GONE);
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            logger.i("shouldOverrideUrlLoading URL-->" + request.getUrl().toString());
            view.loadUrl(request.getUrl().toString());
            return false;
        }

        // 아래 코드는 안드로이드 Nougat (7.0) 이상에서 동작하지 않아 위 shouldOverrideUrlLoading 로 대체함.

        //        @TargetApi(Build.VERSION_CODES.N)
        //        @Override
        //        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        //            shouldOverrideUrlLoading(view, request.getUrl().getPath());
        //            return true;
        //        }

        //        @SuppressWarnings("deprecation")
        //        @Override
        //        public boolean shouldOverrideUrlLoading(final WebView view, String url) {
        //            mProgressBar.setVisibility(View.VISIBLE);
        //            logger.i("shouldOverrideUrlLoading URL-->" + url);
        //            return super.shouldOverrideUrlLoading(view, url);
        //        }


        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            int keyCode = event.getKeyCode();
            if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT) && mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            } else if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && mWebView.canGoForward()) {
                mWebView.goForward();
                return true;
            }
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(badgeCounterTask != null){
            badgeCounterTask.stop();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(SmartViewPreference.isLogin(getContext())){
            getMenuInflater().inflate(R.menu.menu_main, menu);
            MenuItem action_settings = menu.findItem(R.id.action_settings);
            action_settings.setIcon(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_settings).sizeDp(24).colorRes(android.R.color.white));
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getContext(), SettingAlarmActivity.class);
            startActivity(intent);
            return true;
        }else if (id == R.id.action_refresh) {
            mWebView.reload();
            return true;
        }else if (id == R.id.action_copy_url) {
            CommonUtil.copyToClipboard(getContext(), mWebView.getUrl(), getString(R.string.message_copy_url));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;
        if (id == R.id.nav_home) {
            goHome();
        } else if (id == R.id.nav_alarm) {
            intent = new Intent(getContext(), AlarmHistoryActivity.class);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(getContext(), SettingAlarmActivity.class);
        } else if (id == R.id.nav_logout) {
            logout();
        } else if (id == R.id.nav_exit) {
            finish();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        if(intent != null){
            startActivity(intent);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if(mLoginLayout.getVisibility() == View.GONE){
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }else{
                    if(mWebView.canGoBack()){
                        mWebView.goBack();
                        return true;
                    }
                }
            }
            backPressCloseHandler.onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void initNavigationView(){
        Menu menu = mNavigationView.getMenu();
        MenuItem item_home = menu.findItem(R.id.nav_home);
        item_home.setIcon(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_home));

        MenuItem item_alarm = menu.findItem(R.id.nav_alarm);
        item_alarm.setIcon(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_alarm));

        MenuItem item_settings = menu.findItem(R.id.nav_settings);
        item_settings.setIcon(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_settings));

        MenuItem item_logout = menu.findItem(R.id.nav_logout);
        item_logout.setIcon(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_assignment_ind));

        MenuItem item_exit = menu.findItem(R.id.nav_exit);
        item_exit.setIcon(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_exit_to_app));
    }

    private void goHome(){
        if(TextUtils.isEmpty(initUrl) == false){
            isWillClearHistory = true;
            mWebView.loadUrl(initUrl);
        }
    }

    private void forceLogOut(){
        SmartViewPreference.logout(getContext());
        mLoginLayout.setVisibility(View.VISIBLE);
        invalidateOptionsMenu();
        mLoginFragment.resetLayout();
        mWebView.clearHistory();
        lastLoginVo = null;
    }

    private void loginSuccess(){
        mLoginFragment.loginSuccess(lastLoginVo);
        lastLoginVo = null;
        mLoginFragment.cancelLogin();
        mLoginLayout.setVisibility(View.GONE);
        loadAlarmSettings();
        invalidateOptionsMenu();
        mLoginFragment.resetLayout();
        mWebView.clearHistory();
    }

    private void releaseLogin(String errorMessage){
        mLoginFragment.cancelLogin();
        DialogUtils.showDialog(getContext(), getSupportFragmentManager(), errorMessage, null);
        forceLogOut();
    }

    public void onNotiBadgeUpdate() {
        if(badgeCounterTask != null){
            badgeCounterTask.stop();
        }
        badgeCounterTask = new TokAsyncTask(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                final MenuItem item_alarm = mNavigationView.getMenu().findItem(R.id.nav_alarm);
                final View layout_alarm_counter = mNavigationView.getMenu().findItem(R.id.nav_alarm).getActionView();
                final TextView textAlarmCounter = (TextView) layout_alarm_counter.findViewById(R.id.text_alarm_counter);
                final ProgressBar progress_bar = (ProgressBar) layout_alarm_counter.findViewById(R.id.progress_bar);

                progress_bar.setVisibility(View.VISIBLE);
            }

            @Override
            protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
                try{
                    final int alarmCount = getUnReadAlarmCount();
                    return new AsyncTaskResult(alarmCount);
                }catch (Exception e){
                    return new AsyncTaskResult(0);
                }
            }

            @Override
            protected void onPostExecute(AsyncTaskResult<Object> result) {
                super.onPostExecute(result);
                if(result != null && result.getResult() != null){
                    int alarmCount = (int)result.getResult();
                    final MenuItem item_alarm = mNavigationView.getMenu().findItem(R.id.nav_alarm);
                    final View layout_alarm_counter = mNavigationView.getMenu().findItem(R.id.nav_alarm).getActionView();
                    final TextView textAlarmCounter = (TextView) layout_alarm_counter.findViewById(R.id.text_alarm_counter);
                    final ProgressBar progress_bar = (ProgressBar) layout_alarm_counter.findViewById(R.id.progress_bar);

                    if(alarmCount > 0){
                        textAlarmCounter.setText(YWMTools.addComma(alarmCount));
                        textAlarmCounter.setVisibility(View.VISIBLE);
                    }else{
                        textAlarmCounter.setText("0");
                        textAlarmCounter.setVisibility(View.GONE);
                    }
                    progress_bar.setVisibility(View.GONE);
                    CommonUtil.applyUnReadAlarmCountToBadge(getContext(), alarmCount);
                }
            }
        };
        badgeCounterTask.execute();

    }

    private int getUnReadAlarmCount(){
        int count = dbHandler.getUnReadAlarmCount();
        return count;
    }

    DrawerLayout.SimpleDrawerListener drawerListener = new DrawerLayout.SimpleDrawerListener() {
        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            onNotiBadgeUpdate();
            displayUserInfo();
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
        }
    };

    private void applyAppVersionInfo(){
        TextView text_version = (TextView) findViewById(R.id.text_version);
        String currentVersionName = null;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        text_version.setText(String.format("%s v%s", getString(R.string.app_name), currentVersionName));
    }

    @Override
    public void startActivityAnimation() {
        overridePendingTransition(R.anim.start_enter, R.anim.start_exit);
    }

    @Override
    public void finishActivityAnimation() {
        overridePendingTransition(R.anim.end_enter, R.anim.end_exit);
    }

    private void initSwipeToRefresh() {
        if(Constants.isEnableSwipeRefresh){
            mSwipeRefreshLayout.setDistanceToTriggerSync(390);
            mSwipeRefreshLayout.setEnabled(true);
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.color_refresh_1, R.color.color_refresh_2,
                    R.color.color_refresh_3, R.color.color_refresh_4);
            mSwipeRefreshLayout.setOnRefreshListener(this);
        }else{
            mSwipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void login(LoginVo loginVo) {
        this.lastLoginVo = loginVo;
        String javascript = String.format("javascript:$.mobileLoginProcess('%s', '%s', '%s', '%s')", encryptText(loginVo.getUserId()), encryptText(loginVo.getPassword()), encryptText(loginVo.getOtpCode()), encryptText(loginVo.getDeviceKey()));
        logger.e("Call Login--> " + javascript);
        mWebView.loadUrl(javascript);
    }

    private void logout(){
        mWebView.loadUrl("javascript:$.requestLogout()");
    }

    private void loadAlarmSettings(){
//        progressDialog = ProgressDialog.show(getContext(), getString(R.string.app_name), getString(R.string.alarm_progress));
        Call<List<AlarmSettingVo>> req = HttpService.getEnginApiService().getAlarmSettings();
        HttpEnginCallback callback = new HttpEnginCallback(new HttpCallback.HttpCallbackListener() {
            @Override
            public void onSuccess(Object response) {
//                progressDialog.dismiss();
                if(response == null){
//                    CommonUtil.toast(getContext(), R.string.message_service_failed_settings);
                }else{
                    List<AlarmSettingVo> settingItems = (List<AlarmSettingVo>) response;
                    if(settingItems != null && settingItems.size() > 0){
                        Gson gson = new Gson();
                        String json = gson.toJson(settingItems);
                        SmartViewPreference.setSettingText(getContext(), json);
                    }
                }
                startPushService();
            }

            @Override
            public void onFail(String failMessage, @Nullable Call call, @Nullable Throwable t) {
                CommonUtil.toast(getContext(), failMessage, Toast.LENGTH_LONG);
//                progressDialog.dismiss();
                if(TextUtils.isEmpty(SmartViewPreference.getSettingText(getContext(), null)) == false){
                    startPushService();
                }
            }
        });
        req.enqueue(callback);
    }

    private void startPushService(){
        if(SmartViewPreference.isPushOn(getContext())){
            CommonUtil.getGlobalApplication(getContext()).startPushManager();
        }
    }

    private void displayUserInfo(){
//        View layout_login_status = findViewById(R.id.layout_login_status);
        String userName = SmartViewPreference.getUserName(getContext());
        String userId = SmartViewPreference.getUserId(getContext());
        TextView text_user_id = (TextView)findViewById(R.id.text_user_id);
        if(TextUtils.isEmpty(userName)){
            text_user_id.setText(YWMTools.getString(userId, ""));
        }else{
            text_user_id.setText(userName);
        }
    }

    private ResultVo getResultDataFromJson(String json){
        if(TextUtils.isEmpty(json)){
            return null;
        }
        try{
            Gson gson = new Gson();
            ResultVo resultVo = gson.fromJson(json, ResultVo.class);
            return resultVo;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String encryptText(String rawText){
        try{
            String descriptAndroidKey = getRsaCipher().encrypt(rawText);
            return descriptAndroidKey;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private RSACipher createRsaCipher() throws Exception{
        return new RSACipher(getContext(), Constants.RSA_PUBLIC_KEY_PATH, null);
    }

    public RSACipher getRsaCipher(){
        try{
            if(rsaCipher == null){
                rsaCipher = createRsaCipher();
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return rsaCipher;
    }
}
