package de.cactus_world.SlimFBViewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ClientCertRequest;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;

import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.Preference;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.cactus_world.SlimFBViewer.R;
import de.cactus_world.SlimFBViewer.settings.SettingsActivity;
import de.cactus_world.SlimFBViewer.utility.Dimension;
import de.cactus_world.SlimFBViewer.utility.MyAdvancedWebView;

import static android.graphics.Bitmap.createBitmap;
import static de.cactus_world.SlimFBViewer.R.id.off;
import static de.cactus_world.SlimFBViewer.R.id.webView;

/**
 * SlimFBViewer is an Open Source app realized by Cactus World Android Development <android@cactus-world.de> based on SlimFacebook by Leonardo Rignanese <rignanese.leo@gmail.com>
 * GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
 * GITHUB: https://github.com/cactus_world/SlimFBViewer
 */
public class MainActivity extends AppCompatActivity implements MyAdvancedWebView.Listener {

    private static final int PICTURE_ACTIVITY = 1;
    private SwipeRefreshLayout swipeRefreshLayout;//the layout that allows the swipe refresh
    private ConstraintLayout constraintLayout;//app layout
    private AppBarLayout appBarLayout;
    private Toolbar toolbar;
    private MyAdvancedWebView webViewFacebook;//the main webView where is shown facebook
    private JavaScriptInterface jsInterface;
    private WebViewClient webViewClient;
    private WebViewClient originalWebViewClient;
    private Runnable goBackRunner = new Runnable() {
        @Override
        public void run() {
            onBackPressed(true);
        }
    };
    private boolean goBackRunnerInstalled = false;
    private SharedPreferences savedPreferences;//contains all the values of saved preferences
    private String pageLocation = "";
    private boolean noConnectionError = false;//flag: is true if there is a connection error. It should reload the last useful page

    private boolean isSharer = false;//flag: true if the app is called from sharer
    private String urlSharer = "";//to save the url got from the sharer
    private boolean cssLoaded = false;
    private String currentView = "home";
    private boolean isFirstBookmarksLoad = true;
    private boolean isFirstSearchLoad = true;
    private boolean isFirstFeedLoad = true;
    private boolean isFirstMessagesLoad = true;
    private boolean isFirstNotificationsLoad = true;
    private boolean isFirstFriendRequestsLoad = true;
    private boolean useOwnMessageDisplay = false;
    private boolean inOwnMessageDisplay = false;
    private boolean inSearch = false;
    private boolean inBookmarks = false;
    private boolean softkeyBackPressed = false;
    private int runCounter = 0;
    private int historyIndexOffset = 0;
    // create link handler (long clicked links)
    private final MyHandler linkHandler = new MyHandler(this);

    //full screen video variables
    private FrameLayout mTargetView;
    private WebChromeClient myWebChromeClient;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
    private boolean darkThemeSet = false;
    private String fbDesktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.50 Safari/537.36";
    private String fbMobileUserAgentOld = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
    private String fbMobileUserAgent = "Mozilla/5.0 (Linux; Android 9; G8441) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.50 Mobile Safari/537.36";

    private String userAgent = "";

    private class WebResourceRetrievalResponse {
        private String webResourceRetrievalType;
        private String webResourceContentEncoding;
        private String webResourceRetrievalContent;
        private String webResourceRetrievalStatusReason;
        private int webResourceRetrievalStatusCode;
        private HashMap webResourceRetrievalHeaders;
        private byte[] webResourceRetrievalByteContent;

        public WebResourceRetrievalResponse() {
            this.webResourceRetrievalType = null;
            this.webResourceContentEncoding = null;
            this.webResourceRetrievalContent = null;
            this.webResourceRetrievalHeaders = new HashMap();
            this.webResourceRetrievalByteContent = null;
        }

        public WebResourceRetrievalResponse(String type, String content) {
            this.setWebResourceRetrievalType(type);
            this.setWebResourceContentEncoding(null);
            this.setWebResourceRetrievalContent(content);
            this.webResourceRetrievalHeaders = new HashMap();
            this.webResourceRetrievalByteContent = null;
        }

        public WebResourceRetrievalResponse(String type, int statusCode, String statusReason, Map responseHeaders) {
            this.setWebResourceRetrievalType(type);
            this.setWebResourceContentEncoding(null);
            this.setWebResourceRetrievalStatusCode(statusCode);
            this.setWebResourceRetrievalStatusReason(statusReason);
            this.setWebResourceRetrievalHeaders(responseHeaders);
        }

        public WebResourceRetrievalResponse(String type, String encoding, int statusCode, String statusReason, Map responseHeaders) {
            this.setWebResourceRetrievalType(type);
            this.setWebResourceContentEncoding(encoding);
            this.setWebResourceRetrievalContent(null);
            this.setWebResourceRetrievalStatusCode(statusCode);
            this.setWebResourceRetrievalStatusReason(statusReason);
            this.setWebResourceRetrievalHeaders(responseHeaders);
            this.webResourceRetrievalByteContent = null;
        }

        public WebResourceRetrievalResponse(String type, String encoding, int statusCode, String statusReason, Map responseHeaders, String content) {
            this.setWebResourceRetrievalType(type);
            this.setWebResourceContentEncoding(encoding);
            this.setWebResourceRetrievalContent(content);
            this.setWebResourceRetrievalStatusCode(statusCode);
            this.setWebResourceRetrievalStatusReason(statusReason);
            this.setWebResourceRetrievalHeaders(responseHeaders);
            this.webResourceRetrievalByteContent = null;
        }

        public WebResourceRetrievalResponse(String type, String encoding, int statusCode, String statusReason, Map responseHeaders, byte[] byteContent) {
            this.setWebResourceRetrievalType(type);
            this.setWebResourceContentEncoding(encoding);
            this.setWebResourceRetrievalContent(null);
            this.setWebResourceRetrievalStatusCode(statusCode);
            this.setWebResourceRetrievalStatusReason(statusReason);
            this.setWebResourceRetrievalHeaders(responseHeaders);
            this.setWebResourceRetrievalByteContent(byteContent);
        }

        public void setWebResourceRetrievalStatusCode(int statusCode) {
            this.webResourceRetrievalStatusCode = statusCode;
        }

        public int getWebResourceRetrievalStatusCode() {
            return webResourceRetrievalStatusCode;
        }

        public void setWebResourceRetrievalStatusReason(String statusReason) {
            this.webResourceRetrievalStatusReason = statusReason;
        }

        public String getWebResourceRetrievalStatusReason() {
            return webResourceRetrievalStatusReason;
        }

        public String getWebResourceRetrievalContent() {
            return webResourceRetrievalContent;
        }

        public void setWebResourceRetrievalContent(String getWebResourceRetrievalContent) {
            this.webResourceRetrievalContent = getWebResourceRetrievalContent;

        }

        public String getWebResourceRetrievalType() {
            return webResourceRetrievalType;
        }

        public void setWebResourceRetrievalType(String webResourceRetrievalType) {
            if (!webResourceRetrievalType.contains(";")) {
                this.webResourceRetrievalType = webResourceRetrievalType;
            } else {
                this.webResourceRetrievalType = webResourceRetrievalType.substring(0, webResourceRetrievalType.indexOf(";"));
            }
        }

        public HashMap getWebResourceRetrievalHeaders() {
            return webResourceRetrievalHeaders;
        }

        public void setWebResourceRetrievalHeaders(Map webResourceRetrievalHeaders) {
            this.webResourceRetrievalHeaders = new HashMap<String, List<String>>(webResourceRetrievalHeaders);
        }

        public void setWebResourceContentEncoding(String webResourceContentEncoding) {
            this.webResourceContentEncoding = webResourceContentEncoding;
        }

        public String getWebResourceContentEncoding() {
            return webResourceContentEncoding;
        }

        public byte[] getWebResourceRetrievalByteContent() {
            return this.webResourceRetrievalByteContent;
        }

        public void setWebResourceRetrievalByteContent(byte[] byteContent) {
            this.webResourceRetrievalByteContent = byteContent;
        }
    }

    private View mCustomView;
    private Menu menuBar;
    private HashSet<Integer> collapsableActionItems = new HashSet<Integer>() {{
        add(R.id.refresh);
        add(R.id.top);
    }};
    private Drawable notifications;
    private Drawable feed;
    private Drawable message;
    private Drawable friends;
    private int alphaSelected = 255;
    private int alphaNotSelected = 127;
    private String fbMobileBaseUrl;
    private String fbMobileNewsfeedUrl;
    private String fbMobileFriendsUrl;
    private String fbMobileMessageUrl;
    private String fbDesktopMessageUrl;
    private String fbMobileNotificationsUrl;
    private String fbMobileSearchUrl;
    private String fbMobileBookmarksUrl;
    private ConcurrentHashMap<String, Integer> notificationStates = new ConcurrentHashMap<String, Integer>();
    private int toolBarColor = 0;

    //*********************** ACTIVITY EVENTS ****************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fbMobileBaseUrl = getString(R.string.urlFacebookMobileBase);
        fbMobileNewsfeedUrl = getString(R.string.urlFacebookMobile);
        fbMobileFriendsUrl = getString(R.string.urlFacebookMobileFriends);
        fbMobileMessageUrl = getString(R.string.urlFacebookMobileMessages);
        fbDesktopMessageUrl = getString(R.string.urlFacebookDesktopMessages);
        fbMobileNotificationsUrl = getString(R.string.urlFacebookMobileNotifications);
        fbMobileSearchUrl = getString(R.string.urlFacebookMobileSearch);
        fbMobileBookmarksUrl = getString(R.string.urlFacebookMobileBookmarks);
        userAgent = fbMobileUserAgent;
        savedPreferences = PreferenceManager.getDefaultSharedPreferences(this); // setup the sharedPreferences

        SetTheme();//set the activity theme

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        constraintLayout = findViewById(R.id.rootLayout);
        //SetAppTheme();
        appBarLayout = findViewById(R.id.appBarLayout);
        //SetAppBarLayout();
        toolbar = findViewById(R.id.toolbar);
        //SetToolBarTheme();

        setSupportActionBar(toolbar);
        //findViewById(R.id.toolbar).setBackgroundColor(toolBarColor);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_container);
        int actionBarHeight = getSupportActionBar().getHeight();
        ConstraintLayout.LayoutParams layoutParams = ((ConstraintLayout.LayoutParams) appBarLayout.getLayoutParams());
        layoutParams.bottomToTop = swipeRefreshLayout.getId();
        appBarLayout.setLayoutParams(layoutParams);
        //swipeRefreshLayout.setPadding(0, Dimension.heightForFixedFacebookNavbar(getApplicationContext()),0,0);
        // if the app is being launched for the first time
        //swipeRefreshLayout.setSize(swipeRefreshLayout.getHeight()+88);
        //swipeRefreshLayout.setTranslationY(-88);


        try {
//            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }

        constraintLayout = (ConstraintLayout) findViewById(R.id.rootLayout);
        MyAdvancedWebView myAdvancedWebView = findViewById(webView);
        if (savedPreferences.getBoolean("first_run", true)) {
            savedPreferences.edit().putBoolean("first_run", false).apply();
        }


        SetupRefreshLayout();// setup the refresh layout

        ShareLinkHandler();//handle a link shared (if there is)

        SetupWebView();//setup webview
        SetupWebViewClient();//set WebClient to be able to intercept traffic and make direct injections into css
        SetupFullScreenVideo();

        SetupOnLongClickListener();
        SetupWebViewClient();
        if (isSharer) {//if is a share request
            Log.d("MainActivity.OnCreate", "Loading shared link");
            webViewFacebook.loadUrl(urlSharer);//load the sharer url
            isSharer = false;
        } else if (getIntent() != null && getIntent().getDataString() != null) {
            //if the app is opened by fb link
            webViewFacebook.loadUrl(FromDesktopToMobileUrl(getIntent().getDataString()));
        } else GoHome(this.isFirstFeedLoad);//load homepage
        initNotificationStates();
    }

    private void SetAppTheme() {
        if (darkThemeSet) {
            constraintLayout.setBackgroundColor(getResources().getColor(R.color.blackSlimFBViewerTheme));
        } else {
            constraintLayout.setBackgroundColor(getResources().getColor(R.color.blueSlimFacebookTheme));
        }

    }

    private void SetAppBarLayout() {
        if (darkThemeSet) {
            appBarLayout.getContext().setTheme(R.style.DarkTheme_AppBarOverlay);
        } else {
            appBarLayout.getContext().setTheme(R.style.DefaultTheme_AppBarOverlay);
        }
    }

    private void SetToolBarTheme() {
        if (darkThemeSet) {
            toolbar.setPopupTheme(R.style.DarkTheme_PopupOverlay);
        } else {
            toolbar.setPopupTheme(R.style.DefaultTheme_PopupOverlay);

        }
    }

    private void initNotificationStates() {
        this.notificationStates.put("feed", 0);
        this.notificationStates.put("requests", 0);
        this.notificationStates.put("messages", 0);
        this.notificationStates.put("videos", 0);
        this.notificationStates.put("notifications", 0);
    }


    private WebResourceRetrievalResponse getWebResourceFromServer(Uri url, String method, Map<String, String> params) {
        {
            ByteArrayInputStream byteArrayInputStream = null;

            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;
            this.cookieManager = this.webViewFacebook.getCookieManager();
            //CookieHandler.setDefault(this.cookieManager);

            if (isInternetAvailable()) {
                try {
                    httpURLConnection = (HttpURLConnection) (new URL(url.toString())).openConnection();
                    httpURLConnection.setRequestProperty("user-agent", userAgent);
                    httpURLConnection.setUseCaches(true);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setRequestMethod(method);
                    String cookieString = this.cookieManager.getCookie(url.toString());
                    if (method.equals("POST")) {
                        httpURLConnection.setDoOutput(true);
                        try {
                            BufferedOutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                            Set paramList = params.keySet();
                            Boolean firstArgumentSet = false;
                            for (Object key : paramList) {
                                if (((String) key).equals("X-XSRF-TOKEN")) {
                                    httpURLConnection.addRequestProperty("X-XSRF-TOKEN", (String) params.get(key));
                                }
                                if (firstArgumentSet) {
                                    bufferedWriter.write("&" + (String) key + "=" + (String) params.get(key));
                                } else {
                                    bufferedWriter.write((String) key + "=" + (String) params.get(key));
                                    firstArgumentSet = true;
                                }

                            }
                            if (cookieString != null && !cookieString.isEmpty()) {
                                bufferedWriter.write("&Cookie=" + cookieString);
                            }
                            bufferedWriter.flush();
                            bufferedWriter.close();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        httpURLConnection.addRequestProperty("Cookie", cookieString);
                        Set paramList = params.keySet();
                        for (Object key : paramList) {
                            httpURLConnection.setRequestProperty((String) key, (String) params.get(key));
                        }
                    }
                    httpURLConnection.connect();
                    if (httpURLConnection.getHeaderField("Set-Cookie") != null) {
                        this.cookieManager.setCookie(url.toString(), httpURLConnection.getHeaderField("Set-Cookie"));
                    }
                    List<String> setCookie = httpURLConnection.getHeaderFields().get("set-cookie");
                    int responseCode = httpURLConnection.getResponseCode();
                    String responseMessage = httpURLConnection.getResponseMessage();
                    //StringBuilder test= new StringBuilder();
                    Map<String, List<String>> responseHeaders = httpURLConnection.getHeaderFields();
                /*for (Map.Entry<String, List<String>> responseHeader :responseHeaders.entrySet())
                {
                    if (responseHeader.getKey()==null)
                    {
                        continue;
                    }
                    test.append(responseHeader.getKey()+" : ");
                    List  <String> responseHeaderValues = responseHeader.getValue();
                    Iterator <String> it = responseHeaderValues.iterator();
                    if (it.hasNext())
                    {
                        test.append(it.next());
                        while (it.hasNext())
                        {
                            test.append("; "+it.next());
                        }
                    }
                    test.append("\n");
                }
*/
                    //String test = httpURLConnection.getRequestMethod();
                    InputStream inputStream = httpURLConnection.getInputStream();

                    if (!((String) ((List<String>) responseHeaders.get("Content-Type")).get(0)).contains("image")) {
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuffer buffer = new StringBuffer();
                        String line = "";

                        while ((line = bufferedReader.readLine()) != null) {
                            buffer.append(line + "\n");
                            Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                        }

                        int i = responseHeaders.get("Content-Type").get(0).indexOf("charset=");
                        String contentEncoding = (httpURLConnection.getContentEncoding() != null) ? httpURLConnection.getContentEncoding() : ((String) responseHeaders.get("Content-Type").get(0)).substring(responseHeaders.get("Content-Type").get(0).indexOf("charset=") + 8);
                        contentEncoding = i >= 0 ? contentEncoding : "chunked";
                        return new WebResourceRetrievalResponse(httpURLConnection.getContentType(), contentEncoding, httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage(), responseHeaders, buffer.toString());
                    } else {
                        int i = responseHeaders.get("Content-Type").get(0).indexOf("charset=");
                        String contentEncoding = (httpURLConnection.getContentEncoding() != null) ? httpURLConnection.getContentEncoding() : ((String) responseHeaders.get("Content-Type").get(0)).substring(responseHeaders.get("Content-Type").get(0).indexOf("charset=") + 8);
                        contentEncoding = i >= 0 ? contentEncoding : "chunked";
                        WebResourceRetrievalResponse webResourceRetrievalResponse = new WebResourceRetrievalResponse(httpURLConnection.getContentType(), contentEncoding, httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage(), responseHeaders);
                        byte[] inputBytes = new byte[1000];
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        ArrayList<Byte> byteArrayList = new ArrayList<Byte>();
                        int bytesRead = -1;
                        while ((bytesRead = inputStream.read(inputBytes, 0, inputBytes.length)) != -1) {
                            byteArrayOutputStream.write(inputBytes, 0, bytesRead);
                        }

                        webResourceRetrievalResponse.setWebResourceRetrievalByteContent(byteArrayOutputStream.toByteArray());
                        try {
                            if (byteArrayOutputStream != null)
                                byteArrayOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return webResourceRetrievalResponse;
                    }
                } catch (Exception e) {
                    //Something went wrong
                    e.printStackTrace();
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    try {
                        if (bufferedReader != null)
                            bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    private WebResourceResponse getCssWebResourceResponseFromAsset(String assetName) {
        try {
            return getUtf8EncodedCssWebResourceResponse(getAssets().open("style/" + assetName));
        } catch (IOException e) {
            return null;
        }
    }

    private WebResourceResponse getUtf8EncodedCssWebResourceResponse(InputStream data) {
        return new WebResourceResponse("text/css", "UTF-8", data);
    }

    private WebResourceResponse getUtf8EncodedHtmlWebResourceResponse(InputStream data) {
        return new WebResourceResponse("text/html", "UTF-8", data);
    }

    private WebResourceResponse getUtf8EncodedWebResourceResponse(String type, String encoding, InputStream data) {
        if (encoding != null) {
            return new WebResourceResponse(type, encoding, data);
        } else {
            return new WebResourceResponse(type, "UTF-8", data);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        webViewFacebook.onResume();
        webViewFacebook.resumeTimers();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        webViewFacebook.pauseTimers();
        webViewFacebook.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("Info", "onDestroy()");
        webViewFacebook.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        webViewFacebook.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PICTURE_ACTIVITY) {
            webViewFacebook.goBack();
        }
    }

    // app is already running and gets a new intent (used to share link without open another activity)
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        setIntent(intent);

        // grab an url if opened by clicking a link
        String webViewUrl = getIntent().getDataString();

        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        Log.d("sharedUrl", "onNewIntent() - sharedUrl: " + sharedUrl);
        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                webViewUrl = String.format("https://m.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                // ... and parse it just in case
                webViewUrl = Uri.parse(webViewUrl).toString();
            }
        }

        if (webViewUrl != null)
            webViewFacebook.loadUrl(FromDesktopToMobileUrl(webViewUrl));


        // recreate activity when something important was just changed
        if (getIntent().getBooleanExtra("settingsChanged", false)) {
            finish(); // close this
            Intent restart = new Intent(MainActivity.this, MainActivity.class);
            startActivity(restart);//reopen this
        }
    }

    //*********************** SETUP ****************************

    private void SetupWebView() {

        webViewFacebook = findViewById(webView);
        webViewFacebook.setListener(this, this);

        webViewFacebook.clearPermittedHostnames();
        webViewFacebook.addPermittedHostname("facebook.com");
        webViewFacebook.addPermittedHostname("fbcdn.net");
        webViewFacebook.addPermittedHostname("fb.com");
        webViewFacebook.addPermittedHostname("fb.me");
        webViewFacebook.addPermittedHostname("messenger.com");
        webViewFacebook.addPermittedHostname("youtube.com");
        webViewFacebook.setExternalLinkPrefix("l.facebook.com/l.php?u=");

        /*
        webViewFacebook.addPermittedHostname("m.facebook.com");
        webViewFacebook.addPermittedHostname("h.facebook.com");
        webViewFacebook.addPermittedHostname("touch.facebook.com");
        webViewFacebook.addPermittedHostname("mbasic.facebook.com");
        webViewFacebook.addPermittedHostname("touch.facebook.com");
        webViewFacebook.addPermittedHostname("messenger.com");
*/
        webViewFacebook.requestFocus(View.FOCUS_DOWN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);//remove the keyboard issue

        WebSettings settings = webViewFacebook.getSettings();

        webViewFacebook.setDesktopMode(false);
        settings.setUserAgentString(fbMobileUserAgent);
        this.userAgent = fbMobileUserAgent;
        settings.setJavaScriptEnabled(true);
        jsInterface = new JavaScriptInterface(this);
        webViewFacebook.addJavascriptInterface(jsInterface, "SlimFBViewer");
        settings.setDomStorageEnabled(true);
        //set text zoom
        int zoom = Integer.parseInt(savedPreferences.getString("pref_textSize", "100"));
        settings.setTextZoom(zoom);

        //set Geolocation
        settings.setGeolocationEnabled(savedPreferences.getBoolean("pref_allowGeolocation", true));

        // Use WideViewport and Zoom out if there is no viewport defined
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // better image sizing support
        settings.setSupportZoom(true);
        settings.setDisplayZoomControls(false);
        settings.setBuiltInZoomControls(true);

        // set caching
        settings.setAppCachePath(getCacheDir().getAbsolutePath());
        settings.setAppCacheEnabled(true);
        settings.setLoadsImagesAutomatically(!savedPreferences.getBoolean("pref_doNotDownloadImages", false));//to save data

        settings.setAllowUniversalAccessFromFileURLs(true);
        useOwnMessageDisplay = savedPreferences.getBoolean("pref_useAlternativeMessagesDisplay", false);
        /*this.cookieManager = new CookieManager(new CookieStore() {
            @Override
            public void add(URI uri, HttpCookie cookie) {
                String url = uri.toString();
            }

            @Override
            public List<HttpCookie> get(URI uri) {
                return null;
            }

            @Override
            public List<HttpCookie> getCookies() {
                return null;
            }

            @Override
            public List<URI> getURIs() {
                return null;
            }

            @Override
            public boolean remove(URI uri, HttpCookie cookie) {
                return false;
            }

            @Override
            public boolean removeAll() {
                return false;
            }
        },CookiePolicy.ACCEPT_ALL);

        */
        webViewFacebook.setCookieManager(this.cookieManager);

        //cookieManager.setAcceptThirdPartyCookies(webViewFacebook,true);

        //webViewFacebook.setCookiesEnabled(true);

    }

    private void SetupWebViewClient() {

        //CookieHandler.setDefault(this.cookieManager);
        this.cookieManager = webViewFacebook.getCookieManager();
        this.cookieManager.setAcceptCookie(true);
        this.cookieManager.setAcceptThirdPartyCookies(this.webViewFacebook, true);

        webViewFacebook.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);

            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                //onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
                super.onReceivedError(view, req, rerr);
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest webResourceRequest) {

                return super.shouldOverrideUrlLoading(view, webResourceRequest);


            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {

                String tmp = view.getOriginalUrl();
                //webViewFacebook.loadUrl(view.getOriginalUrl());
                return false;
                //super.shouldOverrideUrlLoading(view, view.getOriginalUrl());


            }

            @Override
            public void onLoadResource(WebView view, String url) {

                if (url.contains(getString(R.string.urlFacebookMobileMessages))) {
                    super.onLoadResource(view, url);
                    //WebResourceRetrievalResponse webResourceRetrievalResponse = getWebResourceFromServer(Uri.parse(url), "GET", new HashMap<String, String>());
                    /*if (webResourceRetrievalResponse != null) {
                        view.loadData(webResourceRetrievalResponse.webResourceRetrievalContent, webResourceRetrievalResponse.webResourceRetrievalType, webResourceRetrievalResponse.webResourceContentEncoding);
                    } else {
                        super.onLoadResource(view, url);
                    }*/

                } else if (url.contains(getString(R.string.urlFacebookMobileSearch))) {
                    super.onLoadResource(view, url);

                    // view.loadUrl("javascript:$(\"#search-jewel\").click()");

                    /*WebResourceRetrievalResponse webResourceRetrievalResponse = getWebResourceFromServer(Uri.parse(url), "GET", new HashMap<String, String>());
                    if (webResourceRetrievalResponse != null && webResourceRetrievalResponse.getWebResourceRetrievalType().contains("text")) {
                        //Pattern pattern = Pattern.compile("\\._52z5\\{([^}]*)\\}");
                        Pattern pattern = Pattern.compile("(<div.*?data-sigil=\\\"MTopBlueBarHeader\\\"[^>]*)>");
                        Matcher matcher = pattern.matcher(webResourceRetrievalResponse.getWebResourceRetrievalContent());
                        StringBuffer newWebResponse = new StringBuffer();
                        while (matcher.find()) {
                            //matcher.appendReplacement(newCSSResponse, "._52z5{display: none;" + matcher.group(1) + "}");
                            matcher.appendReplacement(newWebResponse, matcher.group(1) + " style=\"display:none;\"" + ">");
                            //matcher.appendReplacement(newWebResponse,matcher.group(1)+">");
                        }
                        matcher.appendTail(newWebResponse);
                        webResourceRetrievalResponse.webResourceRetrievalContent = newWebResponse.toString();
                        view.loadData(webResourceRetrievalResponse.webResourceRetrievalContent, webResourceRetrievalResponse.webResourceRetrievalType, webResourceRetrievalResponse.webResourceContentEncoding);
                    }*/
                } else {
                    //WebResourceRetrievalResponse webResourceRetrievalResponse = getWebResourceFromServer(Uri.parse(url), "GET", new HashMap<String, String>());
                    /*if (webResourceRetrievalResponse != null && webResourceRetrievalResponse.webResourceRetrievalContent != null && webResourceRetrievalResponse.webResourceRetrievalContent.contains("MTopBlueBarHeader")) {
                        webResourceRetrievalResponse.webResourceRetrievalContent = webResourceRetrievalResponse.webResourceRetrievalContent.replace("function Y(){!D&&E&&(D=b(\"DOM\").scry(E,\"*\",\"MTopBlueBarHeader\")[0]);return D||null}", "function Y(){!D&&E&&(D=b(\"DOM\").scry(E,\"*\",\"MTopBlueBarHeader\")[0]);return null}");
                        view.loadData(webResourceRetrievalResponse.webResourceRetrievalContent, webResourceRetrievalResponse.webResourceRetrievalType, webResourceRetrievalResponse.webResourceContentEncoding);
                    } else {
                    */
                    super.onLoadResource(view, url);
                    //webViewFacebook.loadUrl(url);


                }
            }


            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (Build.VERSION.SDK_INT >= 11) {
                    {
                        return super.shouldInterceptRequest(view, url);
                    }
                } else {
                    return null;
                }
            }

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                {
                    super.onFormResubmission(view, dontResend, resend);
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                {
                    String origin = view.getOriginalUrl();
                    String request_url = view.getUrl();
                    if (origin.equals(request_url)) {
                        super.doUpdateVisitedHistory(view, url, isReload);
                    }

                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                {
                    super.onReceivedSslError(view, handler, error);
                }
            }

            @Override
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                {
                    super.onReceivedClientCertRequest(view, request);
                }

            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm);
                }
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                {
                    return super.shouldOverrideKeyEvent(view, event);
                }
            }

            @Override
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                {
                    super.onUnhandledKeyEvent(view, event);
                }
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                {
                    super.onScaleChanged(view, oldScale, newScale);
                }
            }

            public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
                {
                    super.onReceivedLoginRequest(view, realm, account, args);

                }
            }


            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
                return super.shouldInterceptRequest(webView, webResourceRequest);
            }

            public WebResourceResponse shouldInterceptRequest2(WebView webView, WebResourceRequest webResourceRequest) {
                String url = webResourceRequest.getUrl().toString();
                if (webResourceRequest.getMethod().equals("POST")) {
                    return super.shouldInterceptRequest(webView, webResourceRequest);
                }
                if (savedPreferences.getBoolean("pref_noBar", true)) {
                    //if (url.contains("home.php") && !url.contains(".css") && !url.contains(".js")) {
                    if (url.contains(getString(R.string.urlFacebookMobileMessagesComposer))) {
                        //doUpdateVisitedHistory(webView,getString(R.string.urlFacebookMobileMessages),false);
                    }
                    if (webResourceRequest.getUrl().toString().contains("login") && !savedPreferences.getBoolean("hasRun", false)) {
                        SharedPreferences.Editor sharedPreferencesEditor = savedPreferences.edit();
                        sharedPreferencesEditor.putBoolean("hasRun", true);
                        sharedPreferencesEditor.commit();
                        return super.shouldInterceptRequest(webView, webResourceRequest);
                    }
                    if (inOwnMessageDisplay) {
                        //return super.shouldInterceptRequest(webView,webResourceRequest);
                    }
                    if (webResourceRequest.getMethod().equals("GET") && savedPreferences.getBoolean("hasRun", false)) {
                        //if (webResourceRequest.isForMainFrame()) {

                        WebResourceRetrievalResponse webResourceRetrievalResponse = null;
                        //= getWebResourceFromServer(webResourceRequest.getUrl(), webResourceRequest.getMethod(), webResourceRequest.getRequestHeaders());
                        //ByteArrayInputStream css = new ByteArrayInputStream(getString(R.string.jT1iNd9vJ_t_css).getBytes());
                        {
                            String method = webResourceRequest.getMethod().toString();
                            ByteArrayInputStream byteArrayInputStream = null;
                            Map<String, String> params = webResourceRequest.getRequestHeaders();

                            HttpURLConnection httpURLConnection = null;
                            BufferedReader bufferedReader = null;
                            //CookieHandler.setDefault(this.cookieManager);

                            if (isInternetAvailable()) {
                                try {
                                    //String userAgent = "";
                                    if (userAgent.equals(fbDesktopUserAgent)) {
                                        url = url.replace("m.facebook", "facebook");
                                        url = url.replace("touch.facebook", "facebook");
                                    }
                                    /*if (url.toString().startsWith("https://facebook.com/messages/"))
                                    {
                                        userAgent = fbDesktopUserAgent;
                                    }
                                    else
                                    {
                                        userAgent = fbMobileUserAgent;
                                    }
                                    */
                                    httpURLConnection = (HttpURLConnection) (new URL(url.toString())).openConnection();
                                    httpURLConnection.setRequestProperty("user-agent", userAgent);
                                    httpURLConnection.setUseCaches(true);
                                    //httpURLConnection.setDoInput(true);
                                    httpURLConnection.setConnectTimeout(0);
                                    httpURLConnection.setRequestMethod(method);
                                    String cookieString = cookieManager.getCookie(url.toString());
                                    if (method.equals("POST")) {
                                        httpURLConnection.setDoOutput(true);
                                        try {
                                            BufferedOutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
                                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                                            Set paramList = params.keySet();
                                            Boolean firstArgumentSet = false;
                                            for (Object key : paramList) {
                                                if (((String) key).equals("X-XSRF-TOKEN")) {
                                                    httpURLConnection.addRequestProperty("X-XSRF-TOKEN", (String) params.get(key));
                                                }
                                                if (firstArgumentSet) {
                                                    bufferedWriter.write("&" + (String) key + "=" + (String) params.get(key));
                                                } else {
                                                    bufferedWriter.write((String) key + "=" + (String) params.get(key));
                                                    firstArgumentSet = true;
                                                }

                                            }
                                            if (cookieString != null && !cookieString.isEmpty()) {
                                                bufferedWriter.write("&Cookie=" + cookieString);
                                            }
                                            bufferedWriter.flush();
                                            bufferedWriter.close();
                                            outputStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        httpURLConnection.addRequestProperty("Cookie", cookieString);
                                        Set paramList = params.keySet();
                                        for (Object key : paramList) {
                                            httpURLConnection.setRequestProperty((String) key, (String) params.get(key));
                                        }
                                    }
                                    httpURLConnection.connect();
                                    if (httpURLConnection.getHeaderField("Set-Cookie") != null) {
                                        cookieManager.setCookie(url.toString(), httpURLConnection.getHeaderField("Set-Cookie"));
                                    }
                                    List<String> setCookie = httpURLConnection.getHeaderFields().get("set-cookie");
                                    int responseCode = httpURLConnection.getResponseCode();
                                    String responseMessage = httpURLConnection.getResponseMessage();
                                    //StringBuilder test= new StringBuilder();
                                    Map<String, List<String>> responseHeaders = httpURLConnection.getHeaderFields();
                /*for (Map.Entry<String, List<String>> responseHeader :responseHeaders.entrySet())
                {
                    if (responseHeader.getKey()==null)
                    {
                        continue;
                    }
                    test.append(responseHeader.getKey()+" : ");
                    List  <String> responseHeaderValues = responseHeader.getValue();
                    Iterator <String> it = responseHeaderValues.iterator();
                    if (it.hasNext())
                    {
                        test.append(it.next());
                        while (it.hasNext())
                        {
                            test.append("; "+it.next());
                        }
                    }
                    test.append("\n");
                }
*/
                                    //String test = httpURLConnection.getRequestMethod();
                                    if (responseCode == 302) {
                                        return super.shouldInterceptRequest(webView, webResourceRequest);
                                    }
                                    InputStream inputStream = httpURLConnection.getInputStream();

                                    if (!((String) ((List<String>) responseHeaders.get("Content-Type")).get(0)).contains("image") && !((String) ((List<String>) responseHeaders.get("Content-Type")).get(0)).contains("video") && !((String) ((List<String>) responseHeaders.get("Content-Type")).get(0)).contains("audio")) {
                                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                                        StringBuffer buffer = new StringBuffer();
                                        String line = "";

                                        while ((line = bufferedReader.readLine()) != null) {
                                            buffer.append(line + "\n");
                                            Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                                        }

                                        int i = responseHeaders.get("Content-Type").get(0).indexOf("charset=");
                                        String contentEncoding = (httpURLConnection.getContentEncoding() != null) ? httpURLConnection.getContentEncoding() : ((String) responseHeaders.get("Content-Type").get(0)).substring(responseHeaders.get("Content-Type").get(0).indexOf("charset=") + 8);
                                        contentEncoding = i >= 0 ? contentEncoding : "chunked";
                                        webResourceRetrievalResponse = new WebResourceRetrievalResponse(httpURLConnection.getContentType(), contentEncoding, httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage(), responseHeaders, buffer.toString());
                                    } else if (!((String) ((List<String>) responseHeaders.get("Content-Type")).get(0)).contains("video") && !((String) ((List<String>) responseHeaders.get("Content-Type")).get(0)).contains("audio")) {
                                        int i = responseHeaders.get("Content-Type").get(0).indexOf("charset=");
                                        String contentEncoding = (httpURLConnection.getContentEncoding() != null) ? httpURLConnection.getContentEncoding() : ((String) responseHeaders.get("Content-Type").get(0)).substring(responseHeaders.get("Content-Type").get(0).indexOf("charset=") + 8);
                                        contentEncoding = i >= 0 ? contentEncoding : "chunked";
                                        webResourceRetrievalResponse = new WebResourceRetrievalResponse(httpURLConnection.getContentType(), contentEncoding, httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage(), responseHeaders);
                                        byte[] inputBytes = new byte[1000];
                                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                        ArrayList<Byte> byteArrayList = new ArrayList<Byte>();
                                        int bytesRead = -1;
                                        while ((bytesRead = inputStream.read(inputBytes, 0, inputBytes.length)) != -1) {
                                            byteArrayOutputStream.write(inputBytes, 0, bytesRead);
                                        }

                                        webResourceRetrievalResponse.setWebResourceRetrievalByteContent(byteArrayOutputStream.toByteArray());
                                        try {
                                            if (byteArrayOutputStream != null)
                                                byteArrayOutputStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        //return webResourceRetrievalResponse;
                                    } else {//video or audio stream detected
                                        return super.shouldInterceptRequest(webView, webResourceRequest);
                                    }

                                } catch (Exception e) {
                                    //Something went wrong
                                    e.printStackTrace();
                                } finally {
                                    if (httpURLConnection != null) {
                                        httpURLConnection.disconnect();
                                    }
                                    try {
                                        if (bufferedReader != null)
                                            bufferedReader.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            //webResourceRetrievalResponse = null;
                        }


                        Object webResourceRRequest;
                        ByteArrayInputStream webResponse = null;
                        Boolean isTextType = false;
                        if (webResourceRetrievalResponse != null) {
                            String webResourceRetrievalType = webResourceRetrievalResponse.getWebResourceRetrievalType();
                            if (webResourceRetrievalType != null) {
                                switch (webResourceRetrievalType) {
                                    case "text/html":
                                    case "text/javascript":
                                    case "application/x-javascript": {
                                        isTextType = true;
                                        break;
                                    }
                                    default: {
                                        isTextType = false;
                                        break;
                                    }


                                }
                            }
                        }
                        /*if (webResourceRetrievalResponse != null && isTextType) {
                            Pattern pattern = Pattern.compile("(<div.*?role=\\\"banner\\\"[^>]*)>");
                            Matcher matcher = pattern.matcher(webResourceRetrievalResponse.getWebResourceRetrievalContent());
                            StringBuffer newWebResponse = new StringBuffer();
                            while (matcher.find()) {
                                //matcher.appendReplacement(newCSSResponse, "._52z5{display: none;" + matcher.group(1) + "}");
                                matcher.appendReplacement(newWebResponse, matcher.group(1) + " style=\"display:none;\"" + ">");
                                //matcher.appendReplacement(newWebResponse,matcher.group(1)+">");
                            }
                            matcher.appendTail(newWebResponse);
                            webResourceRetrievalResponse.setWebResourceRetrievalContent(newWebResponse.toString());
                        }*/
                        if (webResourceRetrievalResponse != null && url.contains(getString(R.string.urlFacebookMobileMessages))) {
                            Pattern pattern = Pattern.compile("(<div.*?data-sigil=\\\"MTopBlueBarHeader\\\"[^>]*)>");
                            Matcher matcher = pattern.matcher(webResourceRetrievalResponse.getWebResourceRetrievalContent());
                            StringBuffer newWebResponse = new StringBuffer();
                            while (matcher.find()) {
                                //matcher.appendReplacement(newCSSResponse, "._52z5{display: none;" + matcher.group(1) + "}");
                                matcher.appendReplacement(newWebResponse, matcher.group(1) + " style=\"display:none;\"" + ">");
                                //matcher.appendReplacement(newWebResponse,matcher.group(1)+">");
                            }
                            matcher.appendTail(newWebResponse);
                            webResourceRetrievalResponse.setWebResourceRetrievalContent(newWebResponse.toString());
                        }
                        if (webResourceRetrievalResponse != null && webResourceRetrievalResponse.webResourceRetrievalContent != null && webResourceRetrievalResponse.webResourceRetrievalContent.contains("this._updatePosition")) {
                            Pattern pattern = Pattern.compile("(this\\._updatePosition\\s*=\\s*function\\s*\\(\\).*?h\\s*=\\s*b\\(\\\"Vector\\\"\\)\\.getElementPosition\\(e\\);)");
                            Matcher matcher = pattern.matcher(webResourceRetrievalResponse.getWebResourceRetrievalContent());
                            StringBuffer newWebResponse = new StringBuffer();
                            while (matcher.find()) {
                                matcher.appendReplacement(newWebResponse, matcher.group(1));
                                newWebResponse.append("h = {y:(e.getBoundingClientRect().top + pageYOffset),x : (e.getBoundingClientRect().left + pageXOffset)};");
                            }
                            matcher.appendTail(newWebResponse);

                            webResourceRetrievalResponse.setWebResourceRetrievalContent(newWebResponse.toString());
                            //webResourceRetrievalContent = webResourceRetrievalResponse.webResourceRetrievalContent.replace("_6j_d.show{display:block}", "_6j_d.show{display:none}");
                        }

                        if (webResourceRetrievalResponse != null && (webResourceRetrievalResponse.getWebResourceRetrievalType().contains("text") || webResourceRetrievalResponse.getWebResourceRetrievalType().contains("css"))) {
                            //Pattern pattern = Pattern.compile("\\._52z5\\{([^}]*)\\}");

                            // webResourceRetrievalResponse.webResourceRetrievalContent = webResourceRetrievalResponse.webResourceRetrievalContent.replace("_6j_d.show{display:block}", "_6j_d.show{display:none}");
                            /*if (webResourceRetrievalResponse.webResourceRetrievalContent.contains("._52z5{"))
                            {
                                webResourceRetrievalResponse.webResourceRetrievalContent = webResourceRetrievalResponse.webResourceRetrievalContent.replace("._52z5{", "._52z5{visibility:hidden; ");

                            }
                            */
                            if (webResourceRetrievalResponse.webResourceRetrievalContent.contains(getString(R.string.messageHistorySelector))) {
                                if (savedPreferences.getBoolean("pref_noMessageHistoryEntry", false)) {
                                    webResourceRetrievalResponse.webResourceRetrievalContent = webResourceRetrievalResponse.webResourceRetrievalContent.replace(getString(R.string.messageHistorySelector) + "0", getString(R.string.messageHistorySelector) + "1");
                                } else {
                                    webResourceRetrievalResponse.webResourceRetrievalContent = webResourceRetrievalResponse.webResourceRetrievalContent.replace(getString(R.string.messageHistorySelector) + "1", getString(R.string.messageHistorySelector) + "0");
                                }
                            }
                            Pattern pattern = Pattern.compile("(<div.*?data-sigil=\\\"MTopBlueBarHeader\\\"[^>]*)>");
                            Matcher matcher = pattern.matcher(webResourceRetrievalResponse.getWebResourceRetrievalContent());
                            StringBuffer newWebResponse = new StringBuffer();
                            while (matcher.find()) {
                                //matcher.appendReplacement(newCSSResponse, "._52z5{display: none;" + matcher.group(1) + "}");
                                matcher.appendReplacement(newWebResponse, matcher.group(1) + " style=\"display:none;\"" + ">");
                                //matcher.appendReplacement(newWebResponse,matcher.group(1)+">");
                            }
                            matcher.appendTail(newWebResponse);
                            if (newWebResponse.toString().contains("cookie")) {
                                boolean b = true;
                                b = false;
                            }


                            //WebResourceResponse webResourceResponse = getCssWebResourceResponseFromAsset();//new WebResourceResponse("text/css", "utf-8", css);
                            try {
                                webResponse = new ByteArrayInputStream(newWebResponse.toString().getBytes("UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        } else if (webResourceRetrievalResponse != null && !webResourceRetrievalResponse.getWebResourceRetrievalType().contains("video")) {

                            if (webResourceRetrievalResponse.getWebResourceRetrievalByteContent() != null) {
                                int size = webResourceRetrievalResponse.getWebResourceRetrievalByteContent().length;
                                byte[] bytes = webResourceRetrievalResponse.getWebResourceRetrievalByteContent();
                                webResponse = new ByteArrayInputStream(bytes);
                            } else {
                                try {
                                    webResponse = new ByteArrayInputStream(webResourceRetrievalResponse.getWebResourceRetrievalContent().getBytes("UTF-8"));
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }

                        } else if (webResourceRetrievalResponse != null && webResourceRetrievalResponse.getWebResourceRetrievalType().contains("video")) {
                            return super.shouldInterceptRequest(webView, webResourceRequest);
                        }

                        WebResourceResponse webResourceResponse = null;

                        HashMap<String, String> responseHeaders = new HashMap<String, String>();

                        if (webResourceRetrievalResponse != null && webResourceRetrievalResponse.getWebResourceRetrievalHeaders() != null) {
                            HashMap.Entry<String, List<String>> webResourceRetrievalHeader;
                            Iterator it = webResourceRetrievalResponse.getWebResourceRetrievalHeaders().entrySet().iterator();
                            while (it.hasNext()) {
                                webResourceRetrievalHeader = (HashMap.Entry<String, List<String>>) it.next();
                                StringBuilder webResourceRetrievalHeaderString = new StringBuilder();
                                List<String> webResourceRetrievalHeaderList = webResourceRetrievalHeader.getValue();
                                Iterator<String> webResourceRetrievalHeaderListIterator = webResourceRetrievalHeaderList.listIterator();
                                if (webResourceRetrievalHeaderListIterator.hasNext()) {
                                    webResourceRetrievalHeaderString.append(webResourceRetrievalHeaderListIterator.next());
                                }
                                while (webResourceRetrievalHeaderListIterator.hasNext()) {
                                    webResourceRetrievalHeaderString.append(";" + webResourceRetrievalHeaderListIterator.next());
                                }
                                if (webResourceRetrievalHeader.getKey() != null) {
                                    responseHeaders.put(webResourceRetrievalHeader.getKey(), webResourceRetrievalHeaderString.toString());
                                }
                            }

                        }
                            /*if (url.contains(".css"))
                            {
                                responseHeaders.put("access-control-allow-origin", "*");
                                responseHeaders.put("timing-allow-origin", "*");
                            }*/
                        if (webResponse != null) {

                            webResourceResponse = getUtf8EncodedWebResourceResponse(webResourceRetrievalResponse.getWebResourceRetrievalType(), webResourceRetrievalResponse.getWebResourceContentEncoding(), webResponse);
                        }
                        if (webResourceResponse != null) {
                            webResourceResponse.setResponseHeaders(responseHeaders);

                            webResourceResponse.setStatusCodeAndReasonPhrase(webResourceRetrievalResponse.getWebResourceRetrievalStatusCode(), webResourceRetrievalResponse.getWebResourceRetrievalStatusReason());
                            /*webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
                            webView.getSettings().setJavaScriptEnabled(true);
                            */
                            if (webResourceRetrievalResponse.getWebResourceRetrievalType().contains("text") && webResourceRetrievalResponse.getWebResourceContentEncoding() == null) {
                                webResourceResponse.setEncoding("UTF-8");
                            } else {
                                webResourceResponse.setEncoding(webResourceRetrievalResponse.getWebResourceContentEncoding());
                            }

                            webResourceResponse.setMimeType(webResourceRetrievalResponse.getWebResourceRetrievalType());

                            return webResourceResponse;
                        } else {
                            return null;
                        }
                    } else {
                        return super.shouldInterceptRequest(webView, webResourceRequest);
                    }
                    //return getCssWebResourceResponseFromAsset();
                } else {
                    return super.shouldInterceptRequest(webView, webResourceRequest);
                    //return null;
                }

            }

            /*@Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }*/


        });
        webViewFacebook.setCookieManager(this.cookieManager);
    }


    private void SetupOnLongClickListener() {
        // OnLongClickListener for detecting long clicks on links and images
        webViewFacebook.setOnLongClickListener(v -> {

            WebView.HitTestResult result = webViewFacebook.getHitTestResult();
            int type = result.getType();
            if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                    || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    || type == WebView.HitTestResult.IMAGE_TYPE) {
                Message msg = linkHandler.obtainMessage();
                webViewFacebook.requestFocusNodeHref(msg);
            }
            return false;
        });

        webViewFacebook.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    if (!v.hasFocus()) {
                        v.requestFocus();
                    }
                    break;
            }
            return false;
        });
    }

    private void SetupFullScreenVideo() {
        //full screen video
        mTargetView = findViewById(R.id.target_view);
        myWebChromeClient = new WebChromeClient() {
            //this custom WebChromeClient allow to show video on full screen
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomViewCallback = callback;
                mTargetView.addView(view);
                mCustomView = view;
                swipeRefreshLayout.setVisibility(View.GONE);
                toolbar.setVisibility(View.GONE);
                // constraintLayout.setVisibility(View.GONE);
                mTargetView.setVisibility(View.VISIBLE);
                mTargetView.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null)
                    return;

                mCustomView.setVisibility(View.GONE);
                mTargetView.removeView(mCustomView);
                mCustomView = null;
                mTargetView.setVisibility(View.GONE);
                mCustomViewCallback.onCustomViewHidden();
                swipeRefreshLayout.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.VISIBLE);
                //constraintLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        };
        webViewFacebook.setWebChromeClient(myWebChromeClient);
    }

    private void ShareLinkHandler() {
        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        Log.d("sharedUrl", "ShareLinkHandler() - sharedUrl: " + sharedUrl);

        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                urlSharer = String.format("https://touch.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                // ... and parse it just in case
                urlSharer = Uri.parse(urlSharer).toString();
                isSharer = true;
            }
        }

    }

    private void SetTheme() {
        switch (savedPreferences.getString("pref_theme", "default")) {
            case "DarkTheme": {
                setTheme(R.style.DarkTheme_NoActionBar);
                toolBarColor = R.color.blackSlimFBViewerTheme;
                this.darkThemeSet = true;
                break;
            }
            default: {
                setTheme(R.style.DefaultTheme_NoActionBar);
                toolBarColor = R.color.blueSlimFacebookTheme;
                this.darkThemeSet = false;
                break;
            }
        }
    }

    private void SetupRefreshLayout() {
        swipeRefreshLayout = findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.officialBlueFacebook, R.color.darkBlueSlimFacebookTheme);// set the colors
        //reload the page
        swipeRefreshLayout.setOnRefreshListener(this::RefreshPage);
    }


    //*********************** WEBVIEW FACILITIES ****************************
    private void GoHome(boolean loadFromWeb) {
        this.inSearch = false;
        this.inBookmarks = false;
        if (this.inOwnMessageDisplay) {
            /*this.userAgent = fbMobileUserAgent;
            webViewFacebook.getSettings().setUserAgentString(userAgent);
            */
            webViewFacebook.getSettings().setUserAgentString(fbMobileUserAgent);
            this.userAgent = fbMobileUserAgent;
            this.swipeRefreshLayout.setEnabled(true);
            this.inOwnMessageDisplay = false;
            loadFromWeb = true;
        }
        if (loadFromWeb || this.inSearch || this.inBookmarks) {
            if (savedPreferences.getBoolean("pref_recentNewsFirst", false)) {
                webViewFacebook.loadUrl(getString(R.string.urlFacebookMobile) + "?sk=h_chr");
            } else {
                webViewFacebook.loadUrl(getString(R.string.urlFacebookMobile) + "?sk=h_nor");
            }

        } else {
            //webViewFacebook.loadUrl("javascript:feed_jewel.firstChild.click()");
            webViewFacebook.loadUrl(getString(R.string.navigateTo));
            webViewFacebook.loadUrl("javascript:navigateTo(\'home\',\'" + String.valueOf((boolean) (this.inBookmarks || this.inSearch)) + "\',\'" + currentView + "\')");
        }
        if (savedPreferences.getBoolean("pref_noBar", true)) {
            webViewFacebook.loadUrl(getString(R.string.removeBlueBarNewsFeed2));
        }
        this.currentView = "home";
        this.inSearch = false;
        this.inBookmarks = false;
    }

    private void GoNotifications() {
        if (this.inOwnMessageDisplay) {
            /*this.userAgent = fbMobileUserAgent;
            webViewFacebook.getSettings().setUserAgentString(userAgent);
            */
            webViewFacebook.getSettings().setUserAgentString(fbMobileUserAgent);
            this.userAgent = fbMobileUserAgent;
            this.swipeRefreshLayout.setEnabled(true);
            this.inOwnMessageDisplay = false;
            //webViewFacebook.loadUrl("javascript:navigateTo(\'home\',\'"+ String.valueOf((boolean) (this.inBookmarks || this.inSearch))+"\',\'"+currentView+"\')");
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileNotifications));
        } else if (isFirstNotificationsLoad || this.inSearch || this.inBookmarks) {
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileNotifications));
            //isFirstNotificationsLoad = false;
        } else {
            //webViewFacebook.loadUrl("javascript:notifications_jewel.firstChild.click()");
            webViewFacebook.loadUrl(getString(R.string.navigateTo));
            webViewFacebook.loadUrl("javascript:navigateTo(\'notifications\',\'" + String.valueOf((boolean) (this.inBookmarks || this.inSearch)) + "\',\'" + currentView + "\')");
        }
        if (savedPreferences.getBoolean("pref_noBar", true)) {
            webViewFacebook.loadUrl(getString(R.string.removeBlueBar3));
        }
        this.inSearch = false;
        this.inBookmarks = false;
        this.currentView = "notifications";
    }

    private void GoMessages() {
        this.inSearch = false;
        this.inBookmarks = false;
        useOwnMessageDisplay = savedPreferences.getBoolean("pref_useAlternativeMessagesDisplay", false);
        if (useOwnMessageDisplay) {
           /*this.userAgent = fbDesktopUserAgent;
           webViewFacebook.setDesktopMode(true);
            webViewFacebook.getSettings().setUserAgentString(userAgent);
            webViewFacebook.setFitsSystemWindows(true);
           webViewFacebook.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
           this.swipeRefreshLayout.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
           this.swipeRefreshLayout.setFitsSystemWindows(true);
           webViewFacebook.getSettings().setBuiltInZoomControls(true);
           webViewFacebook.getSettings().setLoadWithOverviewMode(true);
           webViewFacebook.getSettings().setDomStorageEnabled(true);
           webViewFacebook.getSettings().setUseWideViewPort(true);
           webViewFacebook.getSettings().setJavaScriptEnabled(true);
           */
            webViewFacebook.getSettings().setUserAgentString(fbDesktopUserAgent);
            webViewFacebook.setDesktopMode(true);
            this.userAgent = fbDesktopUserAgent;
            webViewFacebook.loadUrl(fbDesktopMessageUrl);
            this.swipeRefreshLayout.setEnabled(false);
            this.inOwnMessageDisplay = true;
            this.isFirstFeedLoad = true;
            this.isFirstFriendRequestsLoad = true;
            this.isFirstBookmarksLoad = true;
            this.isFirstNotificationsLoad = true;
            this.isFirstMessagesLoad = true;
        } else {
                /*this.userAgent = fbMobileUserAgent;
                webViewFacebook.getSettings().setUserAgentString(userAgent);
                */
            webViewFacebook.getSettings().setUserAgentString(fbMobileUserAgent);
            webViewFacebook.setDesktopMode(false);
            this.userAgent = fbMobileUserAgent;
            this.inOwnMessageDisplay = false;
            if (isFirstMessagesLoad) {
                webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileMessages));
                isFirstMessagesLoad = false;
            } else {
                //webViewFacebook.loadUrl("javascript:messages_jewel.firstChild.click()");
                webViewFacebook.loadUrl(getString(R.string.navigateTo));
                webViewFacebook.loadUrl("javascript:navigateTo(\'messages\',\'" + String.valueOf((boolean) (this.inBookmarks || this.inSearch)) + "\',\'" + currentView + "\')");
            }
        }
        this.currentView = "messages";
    }

    private void GoFriends() {
        if (this.inOwnMessageDisplay) {
            /*this.userAgent = fbMobileUserAgent;
            webViewFacebook.getSettings().setUserAgentString(userAgent);
            */
            webViewFacebook.getSettings().setUserAgentString(fbMobileUserAgent);
            this.userAgent = fbMobileUserAgent;
            this.swipeRefreshLayout.setEnabled(true);
            this.inOwnMessageDisplay = false;
            //webViewFacebook.loadUrl("javascript:navigateTo(\'home\',\'"+ String.valueOf((boolean) (this.inBookmarks || this.inSearch))+"\',\'"+currentView+"\')");
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileFriends));
        }
        if (isFirstFriendRequestsLoad || this.inSearch || this.inBookmarks) {
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileFriends));
            //isFirstFriendRequestsLoad = false;
        } else {
            //webViewFacebook.loadUrl("javascript:requests_jewel.firstChild.click()");
            webViewFacebook.loadUrl(getString(R.string.navigateTo));

            webViewFacebook.loadUrl("javascript:navigateTo(\'friend_requests\',\'" + String.valueOf((boolean) (this.inBookmarks || this.inSearch)) + "\',\'" + currentView + "\')");

        }
        if (savedPreferences.getBoolean("pref_noBar", true)) {
            webViewFacebook.loadUrl(getString(R.string.removeBlueBar3));
        }
        this.currentView = "friend_requests";
        this.inSearch = false;
        this.inBookmarks = false;

    }

    private void GoBookmarks() {
        if (this.inOwnMessageDisplay) {
           /* this.userAgent = fbMobileUserAgent;
            webViewFacebook.getSettings().setUserAgentString(userAgent);
             */
            webViewFacebook.getSettings().setUserAgentString(fbMobileUserAgent);
            this.userAgent = fbMobileUserAgent;
            this.swipeRefreshLayout.setEnabled(true);
            this.inOwnMessageDisplay = false;
            //webViewFacebook.loadUrl("javascript:navigateTo(\'home\',\'"+ String.valueOf((boolean) (this.inBookmarks || this.inSearch))+"\',\'"+currentView+"\')");
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileBookmarks));
        }
        if (isFirstBookmarksLoad || savedPreferences.getBoolean("pref_noBar", false) || this.inSearch || this.inBookmarks) {
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileBookmarks));
            //isFirstBookmarksLoad = false;
        } else {
            //webViewFacebook.loadUrl("javascript:bookmarks_jewel.firstChild.click()");
            webViewFacebook.loadUrl(getString(R.string.navigateTo));
            webViewFacebook.loadUrl("javascript:navigateTo(\'bookmarks\',\'" + String.valueOf((boolean) (this.inBookmarks || this.inSearch)) + "\',\'" + currentView + "\')");
        }
        this.currentView = "bookmarks";
        this.inSearch = false;
        this.inBookmarks = true;
        //webViewFacebook.loadUrl(getString(R.string.removeBackButtons));
        //webViewFacebook.loadUrl("javascript:window.addEventListener(\'load\',removeBackButtons(\'"+this.currentView+"\')");

    }

    private void GoSearch() {
        if (this.inOwnMessageDisplay) {
            /*this.userAgent = fbMobileUserAgent;
            webViewFacebook.getSettings().setUserAgentString(userAgent);
             */
            webViewFacebook.getSettings().setUserAgentString(fbMobileUserAgent);
            this.userAgent = fbMobileUserAgent;
            this.swipeRefreshLayout.setEnabled(true);
            this.inOwnMessageDisplay = false;
            //webViewFacebook.loadUrl("javascript:navigateTo(\'home\',\'"+ String.valueOf((boolean) (this.inBookmarks || this.inSearch))+"\',\'"+currentView+"\')");
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileSearch));
        }
        if (isFirstSearchLoad || this.inSearch || this.inBookmarks) {
            webViewFacebook.loadUrl(getString(R.string.urlFacebookMobileSearch));
        } else {
            webViewFacebook.loadUrl(getString(R.string.navigateTo));
            webViewFacebook.loadUrl("javascript:navigateTo(\'search\',\'" + String.valueOf((boolean) (this.inBookmarks || this.inSearch)) + "\',\'" + currentView + "\')");
            //webViewFacebook.loadUrl(getString(R.string.urlFacebookMobile));
            //webViewFacebook.loadUrl(getString(R.string.openSearch));
            //this.inSearch = false;
            //webViewFacebook.loadUrl("javascript:search_jewel.firstChild.click()");
        }
        this.currentView = "search";
        this.inBookmarks = false;
        this.inSearch = true;
        //webViewFacebook.loadUrl(getString(R.string.removeBackButtons));
    }


    private void RefreshPage() {
        if (noConnectionError) {
            //webViewFacebook.goBack();
            noConnectionError = false;
            webViewFacebook.onBackPressed();
        } else {
            String url = webViewFacebook.getOriginalUrl();
            /*if (url.contains(fbMobileNewsfeedUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.home));
            } else if (url.contains(fbMobileFriendsUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.friends));
            } else if (url.contains(fbMobileMessageUrl) || url.contains(fbDesktopMessageUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.message));
            } else if (url.contains(fbMobileNotificationsUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.notifications));
            } else if (url.contains(fbMobileSearchUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.search));
            } else if (url.contains(fbMobileBookmarksUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.bookmarks));
            }*/
            //webViewFacebook.reload();
            webViewFacebook.loadUrl("javascript:window.location.replace(\"" + url + "\")");
            /*if (this.inSearch || this.inBookmarks) {
                //webViewFacebook.loadUrl(getString(R.string.removeBackButtons));
                //webViewFacebook.loadUrl("javascript:window.addEventListener('load',removeBackButtons.bind(null,'" + this.currentView + "'))");
                //this.historyIndexOffset = 2;
            } else*/
            switch (this.currentView) {
                case "notifications":
                case "friend_requests":
                    if (savedPreferences.getBoolean("pref_noBar", true)) {
                        webViewFacebook.loadUrl(getString(R.string.removeBlueBar3));
                    }
                    //webViewFacebook.loadUrl(getString(R.string.goBack));
                    break;
                case "search":
                case "bookmarks":
                    webViewFacebook.loadUrl(getString(R.string.removeBackButtons));
                    //webViewFacebook.loadUrl(getString(R.string.goBack));
                    break;
                case "home":
                default:
                    if (savedPreferences.getBoolean("pref_noBar", true)) {
                        webViewFacebook.loadUrl(getString(R.string.removeBlueBarNewsFeed2));
                    }
                    //webViewFacebook.loadUrl(getString(R.string.removeBlueBar3));
                    //webViewFacebook.loadUrl(getString(R.string.goBack));
                    break;
                //this.historyIndexOffset = 0;
            }
            webViewFacebook.loadUrl(getString(R.string.installNotificationObserver));


        }
    }


    //*********************** WEBVIEW EVENTS ****************************
    @Override
    public boolean shouldLoadUrl(String url) {
        Log.d("MainActivity", "shouldLoadUrl: " + url);
        String host = Uri.parse(url).getHost();
        if (webViewFacebook.getSettings().getUserAgentString().equals(fbDesktopUserAgent) && host != null && !host.contains("messenger")) {
            webViewFacebook.getSettings().setUserAgentString(fbMobileUserAgent);
            this.userAgent = fbMobileUserAgent;
        }
        if (webViewFacebook.getSettings().getUserAgentString().equals(fbMobileUserAgent) && host != null && host.contains("messenger")) {
            webViewFacebook.getSettings().setUserAgentString(fbDesktopUserAgent);
            this.userAgent = fbDesktopUserAgent;
        }
        //Check is it's opening a image
        boolean b = Uri.parse(url).getHost() != null && Uri.parse(url).getHost().endsWith("fbcdn.net");

        if (b) {
            //open the activity to show the pic
            startActivityForResult(new Intent(this, PictureActivity.class).putExtra("URL", url), PICTURE_ACTIVITY);
        }
        /*if (url.contains(getString(R.string.urlFacebookMobileMessages))) {
            webViewFacebook.getWebViewClient().doUpdateVisitedHistory(webViewFacebook, url, false);
        }
        *///webViewFacebook.loadUrl(url);
        //ApplyCustomCss();

        return !b;
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        //swipeRefreshLayout.setVisibility(View.INVISIBLE);
        this.runCounter = 0;
        swipeRefreshLayout.setRefreshing(true);


    }

    @Override
    public void onPageFinished(String url) {
        //this.swipeRefreshLayout.setRefreshing(true);
        if (!(this.inSearch || this.inBookmarks)) {
            setMenuBarNotificationState(this.webViewFacebook, this.menuBar);
        }
        webViewFacebook.loadUrl(getString(R.string.installNotificationObserver));
        ApplyCustomCss();
        webViewFacebook.loadUrl((getString(R.string.goBack)));
        webViewFacebook.loadUrl(getString(R.string.fixMarkPeople));
        WebBackForwardList wbfl = webViewFacebook.copyBackForwardList();
        WebHistoryItem webHistoryItem = wbfl.getCurrentItem();
        boolean isFacebookNewsFeedUrl = webHistoryItem.getOriginalUrl().contains(fbMobileNewsfeedUrl) || webHistoryItem.getOriginalUrl().equals(fbMobileBaseUrl);
        if (!this.inSearch && savedPreferences.getBoolean("pref_noBar", true)) {
            /*if (url.equals(R.string.urlFacebookMobileSearch)){
                webViewFacebook.loadUrl(getString(R.string.addBlueBar));
            }else {*/
            //webViewFacebook.loadUrl(getString(R.string.removeBlueBar));
            if (isFacebookNewsFeedUrl) {
                // webViewFacebook.loadUrl("javascript:removeBlueBar('isFacebookNewsFeedUrl');");
                //webViewFacebook.loadUrl("javascript:window.addEventListener('load',removeBlueBar.bind(null,'isFacebookNewsFeedUrl'))");

                //webViewFacebook.loadUrl(getString(R.string.removeBlueBarNewsFeed)); OLd Facebook
                webViewFacebook.loadUrl(getString(R.string.removeBlueBarNewsFeed2));

            } else if (!(this.inSearch || this.inBookmarks)) {
                //webViewFacebook.loadUrl("javascript:removeBlueBar('isNotFacebookNewsFeedUrl');");
                //webViewFacebook.loadUrl("javascript:window.addEventListener('load',removeBlueBar.bind(null,'isNotFacebookNewsFeedUrl'))");
                webViewFacebook.loadUrl(getString(R.string.removeBlueBar3));
            }


            //}
        }
        if (!this.inSearch && !savedPreferences.getBoolean("pref_noBar", true)) {
            webViewFacebook.loadUrl(getString(R.string.addBlueBar));
        }

        /*if (this.inSearch)
        {
            webViewFacebook.loadUrl(getString(R.string.openSearch));
        }*/
        if (this.inSearch || this.inBookmarks) {
            webViewFacebook.loadUrl(getString(R.string.removeBackButtons));
            this.webViewFacebook.loadUrl("javascript:try{window.removeEvent('load',removeBackButtons);}catch(e){};");
            webViewFacebook.loadUrl("javascript:window.addEventListener('load',removeBackButtons.bind(null,'" + this.currentView + "'))");
        }
        if (this.inOwnMessageDisplay && !savedPreferences.getBoolean("pref_enableInfoOnAlternativeMessagesDisplay", false)) {
            webViewFacebook.loadUrl(getString(R.string.adaptMessengerView));
        } else if (this.inOwnMessageDisplay) {
            webViewFacebook.loadUrl(getString(R.string.adaptMessengerInfoView));
        }
        // MyAdvancedWebView myAdvancedWebView = findViewById(webView);
        //myAdvancedWebView.scrollBy(0,88);
        if (savedPreferences.getBoolean("pref_enableMessagesShortcut", false)) {
            webViewFacebook.loadUrl(getString(R.string.fixMessages));
        }
        this.useOwnMessageDisplay = savedPreferences.getBoolean("pref_enabledAlternativeMessagesDisplay", false);

        this.webViewFacebook.loadUrl("javascript:try{window.removeEvent('click',identifyClick);}catch(e){};");
        this.webViewFacebook.loadUrl("javascript:window.addEventListener('click',identifyClick = function(evt){var id = -1; try{if(typeof evt.srcElement !== null) {id = evt.srcElement.getAttributeNode('data-action-id').value;console.log('Button clicked: '+id+' Event: '+evt);}}catch(exception){id = -1;console.log('No button hit.')}; if (id === '99' || id === '32745'){window.SlimFBViewer.historyBackPressed(id);evt.preventDefault();evt.stopPropagation();}});");
        //this.webViewFacebook.loadUrl(getString(R.string.goBack));
        this.upDateActiveArea();
        //webViewFacebook.loadUrl(("javascript:window.addEventListener( \"load\", (event) => {window.scroll(-1,-1);console.log(\"Page is loaded\");});"));
        //webViewFacebook.loadUrl(("javascript:window.scroll(-1,-1);console.log(\"Page is loaded\");"));
        swipeRefreshLayout.setRefreshing(false);
        //swipeRefreshLayout.setVisibility(View.VISIBLE);
        //webViewFacebook.invalidate();
        //swipeRefreshLayout.invalidate();
        //swipeRefreshLayout.requestLayout();


    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        // refresh on connection error (sometimes there is an error even when there is a network connection)
        if (isInternetAvailable()) {
        }
        //  if (!isInternetAvailable() && !failingUrl.contains("edge-chat") && !failingUrl.contains("akamaihd")
        // && !failingUrl.contains("atdmt") && !noConnectionError)
        else {
            Log.i("onPageError link", failingUrl);
            String summary = "<h1 style='text-align:center; padding-top:15%; font-size:70px;'>" +
                    getString(R.string.titleNoConnection) +
                    "</h1> <h3 style='text-align:center; padding-top:1%; font-style: italic;font-size:50px;'>" +
                    getString(R.string.descriptionNoConnection) +
                    "</h3>  <h5 style='font-size:30px; text-align:center; padding-top:80%; opacity: 0.3;'>" +
                    getString(R.string.awards) +
                    "</h5>";
            webViewFacebook.loadData(summary, "text/html; charset=utf-8", "utf-8");//load a custom html page
            //to allow to return at the last visited page
            noConnectionError = true;
        }
    }

    @Override
    public void onLoadResource(WebView webView, String URL) {
        if (URL.matches("bookmarks")) {

        }


    }

    public boolean isInternetAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType,
                                    long contentLength, String contentDisposition, String userAgent) {

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

    }


    @Override
    public void onExternalPageRequest(String url) {//if the link doesn't contain 'facebook.com', open it using the browser

        if (this.savedPreferences.getBoolean("pref_openExternalPagesInSlimFBViewer", false)) {
            webViewFacebook.loadUrl(url);
        } else {
            if (Uri.parse(url).getHost() != null && Uri.parse(url).getHost().endsWith("slimsocial.leo")) {
                //he clicked on messages
                startActivity(new Intent(this, MessagesActivity.class));
            } else if (url.contains("/m.me/")) {
                String newUrl = url.replace("m.me", "www.messenger.com/t");
                webViewFacebook.loadUrl(newUrl);

            } else {
                try {
                    webViewFacebook.pauseTimers();
                    //webViewFacebook.loadData(getString(R.string.externalPageRequest), "text/html; charset=utf-8", "utf-8");
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException e) {//this prevents the crash
                    Log.e("shouldOverrideUrlLoad", "" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    //*********************** BUTTON ****************************
    // handling the back button and softkey "back" pressed
    public void softkeyBackPressed(String key) {
        this.softkeyBackPressed = true;
        if (key.equals("99") || (key.equals("32745") && this.inBookmarks)) {
            //this.webViewFacebook.removeCallbacks(goBackRunner);
            if (!this.goBackRunnerInstalled) {
                this.webViewFacebook.post(goBackRunner);
                this.goBackRunnerInstalled = true;
            }
        } else if (key.equals("99") || (key.equals("32745")) && this.goBackRunnerInstalled) {
            this.webViewFacebook.removeCallbacks(this.goBackRunner);

        }

    }

    public void onBackPressed(boolean isSoftKey) {

        if (isSoftKey && this.runCounter == 0) {
            this.softkeyBackPressed = true;
            this.upDateActiveArea();
            //this.onBackPressed();

        } else if (!isSoftKey) {
            this.softkeyBackPressed = false;
            this.onBackPressed();
        }
        this.runCounter++;
        if (this.runCounter == 4) {
            this.runCounter = 0;
        }
    }

    public void goBackOrForward(int steps) {
        int back = steps;
        webViewFacebook.goBackOrForward(steps);
    }

    @Override
    public void onBackPressed() {
        boolean goBackFromSearch = this.inSearch;
        boolean goBackFromBookmarks = this.inBookmarks;
        this.softkeyBackPressed = false;
        if (mCustomView != null) {
            myWebChromeClient.onHideCustomView();//hide video player
        } else {
            if (webViewFacebook.canGoBack()) {
                WebBackForwardList wbfl = webViewFacebook.copyBackForwardList();
       /*  here       WebHistoryItem currentWebHistoryItem = wbfl.getCurrentItem();
                int index = wbfl.getCurrentIndex();
                int offset = 1;
                if ((currentWebHistoryItem.getOriginalUrl().equals(fbMobileBaseUrl))){
                        offset++;
                }
                if (!this.softkeyBackPressed && currentWebHistoryItem.getUrl().contains(fbMobileSearchUrl) || currentWebHistoryItem.getUrl().contains(fbMobileBookmarksUrl) || ((currentWebHistoryItem.getUrl().equals(fbMobileBaseUrl))))
                {
                    offset+=2;
                  /*if (currentWebHistoryItem.getUrl().contains(fbMobileBookmarksUrl)){
                        offset+=2;
                        //this.historyIndexOffset = 2;
                    }else{
                        //this.historyIndexOffset = 0;
                    }*/

       /* here         }

 /*               else{
                    this.historyIndexOffset = 0;
                }
                while (!wbfl.getItemAtIndex(index).getUrl().equals(wbfl.getItemAtIndex(index).getOriginalUrl()))
                {
                    offset++;
                    if (index < 0){
                        break;
                    }else{
                        index--;
                    }
                }

*/
                //int backReferenceId = wbfl.getCurrentIndex() - offset;// - this.historyIndexOffset;
 /*               if (backReferenceId == -1) {
                    backReferenceId++;
                }
                if (backReferenceId > -1) {
                    WebHistoryItem webHistoryItem = wbfl.getItemAtIndex(backReferenceId);
                    String url = webHistoryItem.getUrl();
                    String irl = webHistoryItem.getOriginalUrl();
                    while (!(url.equals(webHistoryItem.getOriginalUrl())))
                    {
                        backReferenceId--;
                        webHistoryItem = wbfl.getItemAtIndex(backReferenceId);
                        url = webHistoryItem.getUrl();
                    }
                    url = webHistoryItem.getUrl();
                    if (url.contains(fbMobileBookmarksUrl) || url.contains(fbMobileSearchUrl)){
                        this.historyIndexOffset = 2;
                    }else if (url.equals(fbMobileBaseUrl)) {
                        this.historyIndexOffset += 1;
                        backReferenceId = wbfl.getCurrentIndex() - offset - this.historyIndexOffset;
                        if (backReferenceId <= -1) {
                            backReferenceId = 0;
                        }

                        webHistoryItem = wbfl.getItemAtIndex(backReferenceId);
                        url = webHistoryItem.getUrl();
                        while (!(url.equals(webHistoryItem.getOriginalUrl())) && backReferenceId > 0 ){
                            backReferenceId -= 1;
                            webHistoryItem = wbfl.getItemAtIndex(backReferenceId);
                        }
                        url = webHistoryItem.getUrl();

                    }else{
                        this.historyIndexOffset = 0;
                    }
 */
 /* here                   wbfl = webViewFacebook.copyBackForwardList();
                   /*if (!this.softkeyBackPressed)
                   {
                       currentWebHistoryItem = wbfl.getItemAtIndex(wbfl.getCurrentIndex()-offset);

                   }else{*/
 /* here                      if (wbfl.getCurrentIndex()>1) {
                           offset = 2;
                       }else{
                           offset = 1 ;
                       }
                   /*}*/
 /* here               index=wbfl.getCurrentIndex()-offset;
                   if (index < 0)
                   {
                       index = 0;
                   }

                while (wbfl.getItemAtIndex(index)!= null && !wbfl.getItemAtIndex(index).getUrl().equals(wbfl.getItemAtIndex(index).getOriginalUrl()))
                    {
                        if (index < 0){
                         break;
                        }else{
                            offset++;
                            index--;
                        }
                    }

                currentWebHistoryItem = wbfl.getItemAtIndex(index);
   here */
                /*webViewFacebook.evaluateJavascript("function goBack() {var evt = new MouseEvent('click',{bubbles: true});document.getElementById('MBackNavBar').dispatchEvent(evt);document.getElementById('MBackNavBar').click();}goBack();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {

                    }
                });*/
                // webViewFacebook.loadUrl(getString(R.string.goBack));
                if (!this.softkeyBackPressed) {
                    //webViewFacebook.goBack();
                    webViewFacebook.loadUrl(getString(R.string.goBack));
                    webViewFacebook.loadUrl("javascript:goBack();");

                } else {
                    this.softkeyBackPressed = false;
                }
                this.upDateActiveArea();

                //wbfl = webViewFacebook.copyBackForwardList();
                //backReferenceId = wbfl.getCurrentIndex();
                //String original_url = webViewFacebook.getOriginalUrl();
                //url = webViewFacebook.getUrl();
                //for (int i = this.historyIndexOffset+2;i>0;i--) {
                //index = backReferenceId - wbfl.getCurrentIndex();
                //                        webViewFacebook.goBackOrForward(index);
                //}

                if (!this.softkeyBackPressed) {
                    //webViewFacebook.goBack();
                    // here webViewFacebook.goBackOrForward(offset * -1);

                } else {
                    this.softkeyBackPressed = false;
                    //if (goBackFromSearch || goBackFromBookmarks)
//                        {
//                            webViewFacebook.loadUrl(currentWebHistoryItem.getUrl());
//                            /*webViewFacebook.goBackOrForward(-2);
//                            if (goBackFromBookmarks)
//                            {
//                                //this.inBookmarks = false;
//                                webViewFacebook.goBackOrForward(-1);
//                            }*/
//                            //this.inSearch = false;
//                        }
                }
                //webViewFacebook.goBack();
                //}
                //webViewFacebook.loadUrl(url);
                //wbfl = webViewFacebook.copyBackForwardList();
                //backReferenceId = wbfl.getCurrentIndex();

                //url = webViewFacebook.getUrl();
                //original_url = webViewFacebook.getOriginalUrl();
                String url = webViewFacebook.getOriginalUrl();
                if (this.inSearch || this.inBookmarks) {
                    //webViewFacebook.loadUrl(getString(R.string.removeBackButtons));
                    //webViewFacebook.loadUrl("javascript:window.addEventListener('load',removeBackButtons.bind(null,'" + this.currentView + "'))");
                    //this.historyIndexOffset = 2;

                } else {
                    boolean isFacebookNewsFeedUrl = url.contains(fbMobileNewsfeedUrl);
                    //webViewFacebook.loadUrl(getString(R.string.removeBlueBar));

                    if (isFacebookNewsFeedUrl && savedPreferences.getBoolean("pref_noBar", true)) {
                        //webViewFacebook.loadUrl("javascript:removeBlueBar('isFacebookNewsFeedUrl');");
                        //webViewFacebook.loadUrl("javascript:window.addEventListener('load',removeBlueBar.bind(null,'isFacebookNewsFeedUrl'))");
                        this.webViewFacebook.loadUrl(getString(R.string.removeBlueBarNewsFeed2));
                        //this.webViewFacebook.loadUrl(getString(R.string.goBack));
                    } else if (savedPreferences.getBoolean("pref_noBar", true)){
                        //webViewFacebook.loadUrl("javascript:removeBlueBar('isNotFacebookNewsFeedUrl');");
                        //webViewFacebook.loadUrl("javascript:window.addEventListener('load',removeBlueBar.bind(null,'isNotFacebookNewsFeedUrl'))");
                        this.webViewFacebook.loadUrl(getString(R.string.removeBlueBar3));
                        //this.webViewFacebook.loadUrl(getString(R.string.goBack));
                    }
                    this.historyIndexOffset = 0;
                }/*

                /*}else {
                    finish();
                }
*/

            } else {
                if (!this.softkeyBackPressed) {
                    finish();// close app
                }
            }

        }
    }

    private void upDateActiveArea() {
        WebHistoryItem currentWebHistoryItem = this.webViewFacebook.copyBackForwardList().getCurrentItem();
        String url = "";
        if (currentWebHistoryItem != null) {
            url = currentWebHistoryItem.getOriginalUrl();

            if (url.contains(fbMobileNewsfeedUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.home));
                this.inSearch = false;
                this.inBookmarks = false;
                this.inOwnMessageDisplay = false;
                this.currentView = "home";
            } else if (url.contains(fbMobileFriendsUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.friends));
                this.inSearch = false;
                this.inBookmarks = false;
                this.inOwnMessageDisplay = false;
                this.currentView = "friend_requests";
            } else if (url.contains(fbMobileMessageUrl) || url.contains(fbDesktopMessageUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.message));
                this.inSearch = false;
                this.inBookmarks = false;
                this.inOwnMessageDisplay = true;
                this.currentView = "messages";
            } else if (url.contains(fbMobileNotificationsUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.notifications));
                this.inSearch = false;
                this.inBookmarks = false;
                this.inOwnMessageDisplay = false;
                this.currentView = "notifications";
            } else if (url.contains(fbMobileSearchUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.search));
                this.inSearch = true;
                this.inBookmarks = false;
                this.inOwnMessageDisplay = false;
                this.currentView = "search";
            } else if (url.contains(fbMobileBookmarksUrl)) {
                setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.bookmarks));
                this.inSearch = false;
                this.inBookmarks = true;
                this.inOwnMessageDisplay = false;
                this.currentView = "bookmarks";
            }
        }
    }

    public void setMenuItemsActive(String url) {
        if (url.contains(fbMobileNewsfeedUrl)) {
            setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.home));
            this.inSearch = false;
            this.inBookmarks = false;
            this.inOwnMessageDisplay = false;
            this.currentView = "home";
        } else if (url.contains(fbMobileFriendsUrl)) {
            setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.friends));
            this.inSearch = false;
            this.inBookmarks = false;
            this.inOwnMessageDisplay = false;
            this.currentView = "friend_requests";
        } else if (url.contains(fbMobileMessageUrl) || url.contains(fbDesktopMessageUrl)) {
            setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.message));
            this.inSearch = false;
            this.inBookmarks = false;
            this.inOwnMessageDisplay = true;
            this.currentView = "messages";
        } else if (url.contains(fbMobileNotificationsUrl)) {
            setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.notifications));
            this.inSearch = false;
            this.inBookmarks = false;
            this.inOwnMessageDisplay = false;
            this.currentView = "notifications";
        } else if (url.contains(fbMobileSearchUrl)) {
            setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.search));
            this.inSearch = true;
            this.inBookmarks = false;
            this.inOwnMessageDisplay = false;
            this.currentView = "search";
        } else if (url.contains(fbMobileBookmarksUrl)) {
            setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.bookmarks));
            this.inSearch = false;
            this.inBookmarks = true;
            this.inOwnMessageDisplay = false;
            this.currentView = "bookmarks";
        }
    }

    //*********************** MENU ****************************
    //add my menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        this.menuBar = menu;
        this.feed = menu.findItem(R.id.home).getIcon();
        this.friends = menu.findItem(R.id.friends).getIcon();
        this.notifications = menu.findItem(R.id.notifications).getIcon();
        this.message = menu.findItem(R.id.message).getIcon();

        setMenuItemActive(this.menuBar, this.menuBar.findItem(R.id.home));
        return super.onCreateOptionsMenu(menu);
    }

    /* @Override
     public boolean onPrepareOptionsMenu(Menu menu)
     {
         menu.findItem(R.id.home).getIcon().setAlpha(alphaSelected);
         return super.onPrepareOptionsMenu(menu);

     }*/
    public void setMenuItemActive(Menu menu, MenuItem item) {
        ColorStateList colorStateList = null;
        if (item.getIcon() != null && !collapsableActionItems.contains(item.getItemId())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                colorStateList = item.getIconTintList();
            }

            if (colorStateList != null) {
                colorStateList = colorStateList.withAlpha(alphaNotSelected);
                setMenuItemColorState(menu, R.id.home, colorStateList);
                setMenuItemColorState(menu, R.id.friends, colorStateList);
                setMenuItemColorState(menu, R.id.message, colorStateList);
                setMenuItemColorState(menu, R.id.notifications, colorStateList);
                setMenuItemColorState(menu, R.id.search, colorStateList);
                setMenuItemColorState(menu, R.id.bookmarks, colorStateList);
                colorStateList = colorStateList.withAlpha(alphaSelected);
                setMenuItemColorState(menu, item.getItemId(), colorStateList);
            } else {
                menu.findItem(R.id.home).getIcon().getCurrent().setAlpha(alphaNotSelected);
                menu.findItem(R.id.friends).getIcon().getCurrent().setAlpha(alphaNotSelected);
                menu.findItem(R.id.message).getIcon().getCurrent().setAlpha(alphaNotSelected);
                menu.findItem(R.id.notifications).getIcon().getCurrent().setAlpha(alphaNotSelected);
                menu.findItem(R.id.search).getIcon().getCurrent().setAlpha(alphaNotSelected);
                menu.findItem(R.id.bookmarks).getIcon().getCurrent().setAlpha(alphaNotSelected);
                item.getIcon().getCurrent().setAlpha(alphaSelected);
            }
        }
    }

    public void setMenuItemColorState(Menu menu, int menuId, ColorStateList colorStateList) {
        MenuItem menuItem = menu.findItem(menuId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menuItem.setIconTintList(colorStateList);
        }
    }

    //handling the tap on the menu's items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.top: {//scroll on the top of the page
                webViewFacebook.scrollTo(0, 0);
                break;
            }
            case R.id.openInBrowser: {//open the actual page into using the browser
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webViewFacebook.getUrl())));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Turn on data please", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            /*case R.id.messages: {//open messages
                startActivity(new Intent(this, MessagesActivity.class));
                break;
            }*/
            case R.id.refresh: {//refresh the page
                RefreshPage();
                break;
            }
            case R.id.home: {//go to the home
                GoHome(this.isFirstFeedLoad);
                break;
            }
            case R.id.shareLink: {//share this page
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, MyHandler.cleanUrl(webViewFacebook.getUrl()));
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.shareThisLink)));

                break;
            }
            case R.id.share: {//share this app
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.downloadThisApp));
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.shareThisApp)));

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.thanks),
                        Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.settings: {//open settings
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }

            case R.id.exit: {//open settings
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                return true;
            }

            case R.id.message: {
                GoMessages();
                break;
            }
            case R.id.notifications: {
                GoNotifications();
                break;
            }
            case R.id.bookmarks: {
                GoBookmarks();
                break;
            }
            case R.id.search: {
                GoSearch();
                break;
            }
            case R.id.friends: {
                GoFriends();
                break;
            }
            default:
                break;
        }
        ContextMenu.ContextMenuInfo contextMenuInfo = ((ContextMenu.ContextMenuInfo) item.getMenuInfo());

        setMenuItemActive(this.menuBar, item);
        return true;
        //return super.onOptionsItemSelected(item);
    }

    public void setMenuBarNotificationState(WebView wv, Menu menu) {
        wv.evaluateJavascript( //"(function (){var screen = document.getElementById('screen-root');while (typeof screen === 'undefined'){screen = document.getElementById('screen-root');}var buttons = screen.getElementsByClassName('m bg-s3');while (typeof buttons === 'undefined'){buttons = screen.getElementsByClassName('m bg-s3');}console.log('IN SCRIPT:'+buttons);var string = 'feed:';var tmp = buttons[1].getElementsByClassName('f6');if (tmp.length > 0){console.log('Element 1:' + tmp.item(0).textContent);string += tmp.item(0).textContent;}else{tmp = buttons[0].getElementsByClassName('f6');if (tmp.length > 0){console.log('Element 1:' + tmp.item(0).textContent);string += tmp.item(0).textContent;}else {string += '0';}};string += ';requests:';tmp = buttons[3].getElementsByClassName('f6');if (tmp.length > 0){console.log('Element 2:' + tmp.item(0).textContent);string += tmp.item(0).textContent;}else{tmp = buttons[2].getElementsByClassName('f6');if (tmp.length > 0){console.log('Element 2:' + tmp.item(0).textContent);string += tmp.item(0).textContent;}else {string += '0';}}string += ';notifications:';tmp = buttons[9].getElementsByClassName('f6');	if (tmp.length > 0){console.log('Element 3:' + tmp.item(0).textContent);string += tmp.item(0).textContent;}else{tmp = buttons[8].getElementsByClassName('f6');if (tmp.length > 0){console.log('Element 3:' + tmp.item(0).textContent);string += tmp.item(0).textContent;}else {string += '0';}}string += ';';return (string);})();"
                getString(R.string.getNotificationsStates),
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String html) {
                        Log.d("HTML", html);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                            notificationStates.forEach((key, value) -> {
                                if (html.contains(key)) {
                                    int count_offset = html.indexOf(key) + key.length() + 1;
                                    try {
                                        notificationStates.put(key, Integer.parseInt(html.substring(count_offset, html.indexOf(";", count_offset))));
                                    } catch (NumberFormatException numberFormatException) {
                                        notificationStates.put(key, 0);
                                    }

                                }
                            });

                        } else {
                            for (String key : notificationStates.keySet()) {

                                if (html.contains(key)) {
                                    int count_offset = html.indexOf(key) + key.length() + 1;
                                    try {
                                        notificationStates.put(key, Integer.parseInt(html.substring(count_offset, html.indexOf(";", count_offset))));
                                    } catch (NumberFormatException numberFormatException) {
                                        notificationStates.put(key, 0);
                                    }
                                }
                            }
                        }
                        if (notificationStates.get("feed") > 0) {
                            MenuItem menuItem = menu.findItem(R.id.home);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.home).getIcon().getCurrent()), alpha, notificationStates.get("feed"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("feed") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.home);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(feed);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }
                        if (notificationStates.get("requests") > 0) {

                            MenuItem menuItem = menu.findItem(R.id.friends);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.friends).getIcon().getCurrent()), alpha, notificationStates.get("requests"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("requests") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.friends);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(friends);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }
                        if (notificationStates.get("messages") > 0) {
                            MenuItem menuItem = menu.findItem(R.id.message);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.message).getIcon().getCurrent()), alpha, notificationStates.get("messages"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("messages") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.message);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(message);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }
                        if (notificationStates.get("notifications") > 0) {
                            MenuItem menuItem = menu.findItem(R.id.notifications);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.notifications).getIcon().getCurrent()), alpha, notificationStates.get("notifications"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("notifications") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.notifications);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(notifications);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }


                    }

                });

    }

    public void setMenuBarNotificationStateOld(WebView wv, Menu menu) {
        wv.evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String html) {
                        Log.d("HTML", html);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                            notificationStates.forEach((key, value) -> {
                                if (html.contains(key)) {
                                    Pattern pattern = Pattern.compile("\\\\u003Cdiv\\s+class=\\\\\"_59te\\s+jewel\\s+_hzb.*?\\s+([A-Za-z]*?)Count\\\\\".*?id=\\\\\"([^_]*)_jewel\\\\\"[^>]*>\\\\u003Ca[^>]*>\\\\u003Cspan[^>]*>[^>]*\\\\u003C/span>\\\\u003Cdiv[^>]*>\\\\u003Cdiv[^>]*>\\\\u003Cspan class=\\\\\"_59tg\\\\\" data-sigil=\\\\\"count\\\\\">(\\d+)\\\\u003C/span>\\\\u003C/div>\\\\u003C/div>\\\\u003C/a>\\\\u003C/div>", Pattern.DOTALL);
                                    //Pattern pattern = Pattern.compile("\\\\u003Cdiv class=\\\"_59te jewel _hzb.*?\\s+([A-Za-z]*)Count\\\".*?id=\\\"([^_]*)_jewel\\\"[^>]*>\\\\u003Ca[^>]*>\\\\u003Cspan[^>]*>[^>]*\\\\u003C/span>\\\\u003Cdiv[^>]*>\\\\u003Cdiv[^>]*>\\\\u003Cspan class=\\\"_59tg\\\" data-sigil=\\\"count\\\">(\\d+)\\\\u003C/span>\\\\u003C/div>\\\\u003C/div>\\\\u003C/a>\\\\u003C/div>");
                                    //Pattern pattern = Pattern.compile("div\\s+class=\\\\\"");

                                    Matcher matcher = pattern.matcher(html);
                                    while (matcher.find()) {
                                        String type = matcher.group(2);
                                        String notificationAvailable = matcher.group(1);
                                        int count = Integer.parseInt(matcher.group(3));
                                        notificationStates.put(type, count);
                                    }
                                }
                            });

                        } else {
                            for (String key : notificationStates.keySet()) {

                                if (html.contains(key)) {
//                                    Pattern pattern = Pattern.compile("\\u003Cdiv class=\"_59te jewel _hzb _2cnm\\s(.*)Count\".*id=\"([^_]*)_jewel\"[^>]*>\\u003Ca[^>]*>\\u003Cspan[^>]*>[^>]*\\u003C/span>\\u003Cdiv[^>]*>\\u003Cdiv[^>]*>\\u003Cspan class=\"_59tg\" data-sigil=\"count\">(\\d+)\\u003C/span>\\u003C/div>\\u003C/div>\\u003C/a>\\u003C/div>");
                                    Pattern pattern = Pattern.compile("\\\\u003Cdiv\\s+class=\\\\\"_59te\\s+jewel\\s+_hzb.*?\\s+([A-Za-z]*?)Count\\\\\".*?id=\\\\\"([^_]*)_jewel\\\\\"[^>]*>\\\\u003Ca[^>]*>\\\\u003Cspan[^>]*>[^>]*\\\\u003C/span>\\\\u003Cdiv[^>]*>\\\\u003Cdiv[^>]*>\\\\u003Cspan class=\\\\\"_59tg\\\\\" data-sigil=\\\\\"count\\\\\">(\\d+)\\\\u003C/span>\\\\u003C/div>\\\\u003C/div>\\\\u003C/a>\\\\u003C/div>", Pattern.DOTALL);

                                    Matcher matcher = pattern.matcher(html);
                                    while (matcher.find()) {
                                        String type = matcher.group(2);
                                        String notificationAvailable = matcher.group(1);
                                        int count = Integer.parseInt(matcher.group(3));
                                        notificationStates.put(type, count);
                                    }
                                }
                            }
                        }
                        if (notificationStates.get("feed") > 0) {
                            MenuItem menuItem = menu.findItem(R.id.home);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.home).getIcon().getCurrent()), alpha, notificationStates.get("feed"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("feed") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.home);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(feed);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }
                        if (notificationStates.get("requests") > 0) {

                            MenuItem menuItem = menu.findItem(R.id.friends);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.friends).getIcon().getCurrent()), alpha, notificationStates.get("requests"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("requests") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.friends);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(friends);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }
                        if (notificationStates.get("messages") > 0) {
                            MenuItem menuItem = menu.findItem(R.id.message);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.message).getIcon().getCurrent()), alpha, notificationStates.get("messages"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("messages") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.message);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(message);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }
                        if (notificationStates.get("notifications") > 0) {
                            MenuItem menuItem = menu.findItem(R.id.notifications);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(new BitmapDrawable(getResources(), setNotificationRect((menu.findItem(R.id.notifications).getIcon().getCurrent()), alpha, notificationStates.get("notifications"))));
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        } else if (notificationStates.get("notifications") == 0) {
                            MenuItem menuItem = menu.findItem(R.id.notifications);
                            int alpha = menuItem.getIcon().getCurrent().getAlpha();
                            menuItem.setIcon(notifications);
                            menuItem.getIcon().getCurrent().setAlpha(alpha);
                        }


                    }

                });

    }

    public Bitmap setNotificationRect(Drawable backgroundIcon, int alphaState, Integer count) {
        float scaleFactor = 1.4f;
        Bitmap icon = null;
        float[] transform =
                {1.0f, 0, 0, 0, 0,
                        0, 1.0f, 0, 0, 0,
                        0, 0, 1.0f, 0, 0,
                        0, 0, 0, 1 - 0f, 0

                };
        float strokeWidth = 1.0f;
        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha(255);
        if (!(backgroundIcon instanceof BitmapDrawable)) {
            if (backgroundIcon instanceof VectorDrawable) {
                icon = Bitmap.createBitmap(backgroundIcon.getIntrinsicWidth(), backgroundIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                //alphaPaint.setAlpha(255);
                //alphaPaint.setColor(Color.WHITE);
                Canvas canvas = new Canvas(icon);

                backgroundIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                backgroundIcon.setAlpha(255);
                backgroundIcon.draw(canvas);

            } else //test to eliminate crash on API23 devices
            {
                icon = Bitmap.createBitmap(backgroundIcon.getIntrinsicWidth(), backgroundIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                //alphaPaint.setAlpha(255);
                //alphaPaint.setColor(Color.WHITE);
                Canvas canvas = new Canvas(icon);

                backgroundIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                backgroundIcon.setAlpha(255);
                backgroundIcon.draw(canvas);

            }
        } else {
            icon = ((BitmapDrawable) backgroundIcon).getBitmap();
            Bitmap bitmap = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawARGB(0, 0, 0, 0);
            canvas.drawBitmap(icon, 0, 0, alphaPaint);
            bitmap.setHasAlpha(true);
            icon = bitmap;
        }

        ColorFilter colorFilter = new ColorMatrixColorFilter(transform);
        int height = icon.getHeight();
        int width = icon.getWidth();
        RectF rectF = new RectF(width - scaleFactor * (width / 3.0f), 0f, width, scaleFactor * (height / 3.0f));
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fillPaint.setColor(Color.RED);
        //fillPaint.setColorFilter(colorFilter);
        Bitmap iconWithNotification = createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas combinedImage = new Canvas(iconWithNotification);
        combinedImage.drawBitmap(icon, new Matrix(), alphaPaint);
        Paint paint = new Paint();
        paint.setColorFilter(colorFilter);
        combinedImage.drawRoundRect(rectF, 5, 5, fillPaint);
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeWidth(strokeWidth);
        combinedImage.drawRoundRect(rectF, 5, 5, fillPaint);
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.WHITE);
        fillPaint.setTextSize(scaleFactor * 0.8f * height / 3.0f);
        fillPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = fillPaint.getFontMetrics();
        Rect textRect = new Rect();
        fillPaint.getTextBounds(count.toString(), 0, count.toString().length(), textRect);
        //float fontHeight = (fontMetrics.top + fontMetrics.bottom);
        float fontHeight = textRect.height();
        //combinedImage.drawText(count.toString(),((width - scaleFactor*(width / 3.0f))+(width - (scaleFactor*(width / 3.0f))/2)-(height/3.0f)), (height - (scaleFactor*(((height/3.0f))))), fillPaint);
        combinedImage.drawText(count.toString(), (width - ((scaleFactor * (width / 3.0f)) / 2)), ((scaleFactor * (height / 3.0f)) / 2.0f) + (fontHeight / 2.0f), fillPaint);
        iconWithNotification.setHasAlpha(true);
        return iconWithNotification;
    }
    //*********************** OTHER ****************************

    String FromDesktopToMobileUrl(String url) {
        if (Uri.parse(url).getHost() != null && Uri.parse(url).getHost().endsWith("facebook.com")) {
            url = url.replace("mbasic.facebook.com", "touch.facebook.com");
            url = url.replace("www.facebook.com", "touch.facebook.com");
            url = url.replace("m.facebook.com", "touch.facebook.com");
        }
        return url;
    }

    private void ApplyCustomCss() {

        String css = "";
        if (savedPreferences.getBoolean("pref_centerTextPosts", false)) {
            css += getString(R.string.centerTextPosts);
        }
        if (savedPreferences.getBoolean("pref_addSpaceBetweenPosts", false)) {
            css += getString(R.string.addSpaceBetweenPosts);
        }
        if (savedPreferences.getBoolean("pref_hideSponsoredPosts", false)) {
            css += getString(R.string.hideAdsAndPeopleYouMayKnow);
        }
        if (savedPreferences.getBoolean("pref_fixedBar", false)) {//without add the barHeight doesn't scroll
            css += (getString(R.string.fixedBar).replace("$s", ""
                    + Dimension.heightForFixedFacebookNavbar(getApplicationContext())));
        }
        /*if (savedPreferences.getBoolean("pref_noBar", true)) {
            css += getString(R.string.noBar);
        }*/
        if (savedPreferences.getBoolean("pref_removeMessengerDownload", true)) {
            css += getString(R.string.removeMessengerDownload);
        }


        switch (savedPreferences.getString("pref_theme", "standard")) {
            case "DarkTheme":
            case "DarkNoBar": {
                css += getString(R.string.blackTheme);
                break;
            }
            default:
                break;
        }

        //apply the customizations
        webViewFacebook.loadUrl(getString(R.string.editCss).replace("$css", css));
    }

        public class JavaScriptInterface {
            private MainActivity activity;
            private WebView webview;

            public JavaScriptInterface(MainActivity activity) {
                this.activity = activity;
            }

            @JavascriptInterface
            public void historyBackPressed(String jsValue) {
                this.activity.softkeyBackPressed(jsValue);
            }
            @JavascriptInterface
            public void noBackKey(String jsValue) {
                this.activity.noBackKey();
            }
            @JavascriptInterface
            public void setPageLocation(String location) {
                this.activity.pageLocation = location;
            }
            @JavascriptInterface
            public void echoElement(String element) {
                this.activity.echoElement(element);
            }
            @JavascriptInterface
            public void newNotificationState()
            {
                this.activity.webViewFacebook.post(new Runnable() {
                @Override
                public void run() {
                    activity.setMenuBarNotificationState(activity.webViewFacebook, activity.menuBar);
                }
            });
            }
        }

    private void echoElement(String element) {
        String el = element;
    }

    private void noBackKey() {
        if (this.webViewFacebook.canGoBack())
        {
            this.webViewFacebook.goBack();
        }
    }
// handle long clicks on links, an awesome way to avoid memory leaks

        private static class MyHandler extends Handler {
            MainActivity activity;
            //thanks to FaceSlim
            private final WeakReference<MainActivity> mActivity;

            public MyHandler(MainActivity activity) {
                this.activity = activity;
                mActivity = new WeakReference<>(activity);
            }

            @Override
            public void handleMessage(Message msg) {
                SharedPreferences savedPreferences = PreferenceManager.getDefaultSharedPreferences(activity); // setup the sharedPreferences
                if (savedPreferences.getBoolean("pref_enableFastShare", true)) {
                    MainActivity activity = mActivity.get();
                    if (activity != null) {

                        // get url to share
                        String url = (String) msg.getData().get("url");

                        if (url != null) {
                    /* "clean" an url to remove Facebook tracking redirection while sharing
                    and recreate all the special characters */
                            url = decodeUrl(cleanUrl(url));

                            // create share intent for long clicked url
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_TEXT, url);
                            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.shareThisLink)));
                        }
                    }
                }
            }

            // "clean" an url and remove Facebook tracking redirection
            private static String cleanUrl(String url) {
                return url.replace("http://lm.facebook.com/l.php?u=", "")
                        .replace("https://m.facebook.com/l.php?u=", "")
                        .replace("http://0.facebook.com/l.php?u=", "")
                        .replaceAll("&h=.*", "").replaceAll("\\?acontext=.*", "");
            }

            // url decoder, recreate all the special characters
            private static String decodeUrl(String url) {
                return url.replace("%3C", "<").replace("%3E", ">")
                        .replace("%23", "#").replace("%25", "%")
                        .replace("%7B", "{").replace("%7D", "}")
                        .replace("%7C", "|").replace("%5C", "\\")
                        .replace("%5E", "^").replace("%7E", "~")
                        .replace("%5B", "[").replace("%5D", "]")
                        .replace("%60", "`").replace("%3B", ";")
                        .replace("%2F", "/").replace("%3F", "?")
                        .replace("%3A", ":").replace("%40", "@")
                        .replace("%3D", "=").replace("%26", "&")
                        .replace("%24", "$").replace("%2B", "+")
                        .replace("%22", "\"").replace("%2C", ",")
                        .replace("%20", " ");
            }
        }


    }


