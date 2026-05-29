package com.abbrowser.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String SP_NAME = "BrowserPrefs";
    private static final String KEY_HOST = "proxy_host";
    private static final String KEY_PORT = "proxy_port";
    private static final String KEY_USER = "proxy_user";
    private static final String KEY_PWD = "proxy_pwd";
    private static final String KEY_HISTORY = "proxy_history";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_BOOKMARK_NAMES = "bookmark_names";
    private static final int MAX_HISTORY = 10;

    private EditText urlInput;
    private FrameLayout webViewContainer;
    private List<WebView> tabList = new ArrayList<>();
    private int currentTabIndex = 0;
    private AlertDialog tabDialog;
    private TextView tabCountText;

    private boolean adBlockEnabled = true;
    private ImageButton btnAdBlock;
    private ImageButton btnBookmark;

    private String proxyHost = "";
    private int proxyPort = 0;
    private String proxyUser = "", proxyPwd = "";
    private boolean isProxyEnabled = false;
    private List<String> proxyHistoryList = new ArrayList<>();
    private List<String> bookmarkUrls = new ArrayList<>();
    private List<String> bookmarkNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadPrefs();

        urlInput = findViewById(R.id.urlInput);
        urlInput.setHint("输入网址或搜索");
        urlInput.setHintTextColor(0xFFAAAAAA);

        webViewContainer = findViewById(R.id.webview_container);
        btnAdBlock = findViewById(R.id.btnAdBlock);
        btnBookmark = findViewById(R.id.btn_bookmark);
        tabCountText = findViewById(R.id.tabCount);
        ImageButton btnProxy = findViewById(R.id.btn_proxy);
        ImageButton btnNewTab = findViewById(R.id.btn_new_tab);
        ImageButton btnSwitchTab = findViewById(R.id.btn_switch_tab);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnForward = findViewById(R.id.btn_forward);
        ImageButton btnRefresh = findViewById(R.id.btn_refresh);

        btnAdBlock.setOnClickListener(v -> {
            adBlockEnabled = !adBlockEnabled;
            updateAdBlockIcon();
            Toast.makeText(this, adBlockEnabled ? "广告拦截已开启" : "广告拦截已关闭", Toast.LENGTH_SHORT).show();
        });

        updateProxyIconColor(btnProxy);
        btnProxy.setOnClickListener(v -> showProxyDialog());
        btnBookmark.setOnClickListener(v -> showBookmarkDialog());

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String input = urlInput.getText().toString().trim();
                if (TextUtils.isEmpty(input)) return true;
                String targetUrl = isValidUrl(input)
                        ? (input.startsWith("http") ? input : "https://" + input)
                        : "https://cn.bing.com/search?q=" + input;
                getCurrentWebView().loadUrl(targetUrl);
                return true;
            }
            return false;
        });

        btnBack.setOnClickListener(v -> {
            if (getCurrentWebView().canGoBack()) getCurrentWebView().goBack();
        });
        btnForward.setOnClickListener(v -> {
            if (getCurrentWebView().canGoForward()) getCurrentWebView().goForward();
        });
        btnRefresh.setOnClickListener(v -> getCurrentWebView().reload());
        btnNewTab.setOnClickListener(v -> createNewTab());
        btnSwitchTab.setOnClickListener(v -> showTabSwitchDialog());

        createNewTab();
        updateAdBlockIcon();
        updateTabCount();
    }

    private void loadPrefs() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        proxyHost = sp.getString(KEY_HOST, "");
        String portStr = sp.getString(KEY_PORT, "0");
        try {
            proxyPort = Integer.parseInt(portStr);
        } catch (Exception e) {
            proxyPort = 0;
        }
        proxyUser = sp.getString(KEY_USER, "");
        proxyPwd = sp.getString(KEY_PWD, "");
        Set<String> historySet = sp.getStringSet(KEY_HISTORY, new LinkedHashSet<>());
        proxyHistoryList = new ArrayList<>(historySet);
        Set<String> bookmarkUrlSet = sp.getStringSet(KEY_BOOKMARKS, new LinkedHashSet<>());
        bookmarkUrls = new ArrayList<>(bookmarkUrlSet);
        Set<String> bookmarkNameSet = sp.getStringSet(KEY_BOOKMARK_NAMES, new LinkedHashSet<>());
        bookmarkNames = new ArrayList<>(bookmarkNameSet);
    }

    private void savePrefs() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_HOST, proxyHost);
        editor.putString(KEY_PORT, String.valueOf(proxyPort));
        editor.putString(KEY_USER, proxyUser);
        editor.putString(KEY_PWD, proxyPwd);
        editor.putStringSet(KEY_HISTORY, new LinkedHashSet<>(proxyHistoryList));
        editor.putStringSet(KEY_BOOKMARKS, new LinkedHashSet<>(bookmarkUrls));
        editor.putStringSet(KEY_BOOKMARK_NAMES, new LinkedHashSet<>(bookmarkNames));
        editor.apply();
    }

    private boolean isValidUrl(String input) {
        if (TextUtils.isEmpty(input)) return false;
        input = input.toLowerCase();
        return input.startsWith("http://") || input.startsWith("https://")
                || input.matches("^.+\\.[a-z]{2,}.*$");
    }

    private void updateProxyIconColor(ImageButton btnProxy) {
        if (isProxyEnabled) btnProxy.setColorFilter(0xFF00FF00);
        else btnProxy.clearColorFilter();
    }

    private void updateAdBlockIcon() {
        btnAdBlock.setImageResource(adBlockEnabled ? R.drawable.ic_adblock_on : R.drawable.ic_adblock_off);
    }

    private void updateTabCount() {
        tabCountText.setText(String.valueOf(tabList.size()));
        tabCountText.setVisibility(tabList.size() > 1 ? View.VISIBLE : View.GONE);
    }

    private void showBookmarkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bookmark, null);
        ListView listView = dialogView.findViewById(R.id.list_view_bookmarks);
        Button btnAdd = dialogView.findViewById(R.id.btn_add_bookmark);

        BookmarkAdapter adapter = new BookmarkAdapter(this, bookmarkUrls, bookmarkNames);
        listView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            final View addView = LayoutInflater.from(this).inflate(R.layout.dialog_add_bookmark, null);
            final EditText etName = addView.findViewById(R.id.et_bookmark_name);
            final EditText etUrl = addView.findViewById(R.id.et_bookmark_url);
            String currentUrl = getCurrentWebView().getUrl();
            if (currentUrl == null || currentUrl.equals("about:blank")) {
                Toast.makeText(this, "当前页面无法添加书签", Toast.LENGTH_SHORT).show();
                return;
            }
            etUrl.setText(currentUrl);
            etName.setText(currentUrl.replace("https://", "").replace("http://", "").split("/")[0]);

            new AlertDialog.Builder(this)
                    .setTitle("添加书签")
                    .setView(addView)
                    .setPositiveButton("保存", (d, w) -> {
                        String name = etName.getText().toString().trim();
                        String url = etUrl.getText().toString().trim();
                        if (TextUtils.isEmpty(url)) {
                            Toast.makeText(this, "网址不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (bookmarkUrls.contains(url)) {
                            int index = bookmarkUrls.indexOf(url);
                            bookmarkNames.set(index, name);
                        } else {
                            bookmarkUrls.add(url);
                            bookmarkNames.add(name);
                        }
                        savePrefs();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "书签已添加", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        builder.setTitle("书签（共" + bookmarkUrls.size() + "个）")
                .setView(dialogView)
                .setNegativeButton("关闭", null)
                .create()
                .show();
    }

    private void showProxyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_proxy, null);
        final EditText etHost = view.findViewById(R.id.et_host);
        final EditText etPort = view.findViewById(R.id.et_port);
        final EditText etUser = view.findViewById(R.id.et_user);
        final EditText etPass = view.findViewById(R.id.et_pass);
        final TextView tvResult = view.findViewById(R.id.tv_result);
        final Spinner spHistory = view.findViewById(R.id.sp_history);

        etHost.setText(proxyHost);
        etPort.setText(proxyPort > 0 ? String.valueOf(proxyPort) : "");
        etUser.setText(proxyUser);
        etPass.setText(proxyPwd);

        List<String> spinnerData = new ArrayList<>();
        spinnerData.add("选择历史代理");
        spinnerData.addAll(proxyHistoryList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerData);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHistory.setAdapter(adapter);

        spHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return;
                String[] hp = spinnerData.get(position).split(":");
                if (hp.length == 2) {
                    etHost.setText(hp[0]);
                    etPort.setText(hp[1]);
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etHost.getWindowToken(), 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        view.findViewById(R.id.btn_test).setOnClickListener(v -> {
            String host = etHost.getText().toString().trim();
            String portStr = etPort.getText().toString().trim();
            if (TextUtils.isEmpty(host) || TextUtils.isEmpty(portStr)) {
                tvResult.setText("❌ 请填写IP和端口");
                return;
            }
            final int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception e) {
                tvResult.setText("❌ 端口格式错误");
                return;
            }
            tvResult.setText("正在检测...");
            new Thread(() -> {
                boolean ok = false;
                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 2500);
                    ok = true;
                } catch (Exception e) {}
                boolean finalOk = ok;
                runOnUiThread(() -> tvResult.setText(finalOk ? "✅ 连通成功" : "❌ 连通失败"));
            }).start();
        });

        builder.setView(view).setTitle("SOCKS5 代理设置")
                .setPositiveButton("保存配置", (d, w) -> {
                    String h = etHost.getText().toString().trim();
                    String pStr = etPort.getText().toString().trim();
                    String u = etUser.getText().toString().trim();
                    String pwd = etPass.getText().toString().trim();
                    if (TextUtils.isEmpty(h) || TextUtils.isEmpty(pStr)) {
                        Toast.makeText(this, "IP/端口不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        proxyPort = Integer.parseInt(pStr);
                    } catch (Exception e) {
                        Toast.makeText(this, "端口格式错误", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    proxyHost = h;
                    proxyUser = u;
                    proxyPwd = pwd;
                    proxyHistoryList.add(h + ":" + pStr);
                    savePrefs();
                    isProxyEnabled = true;
                    updateProxyIconColor(findViewById(R.id.btn_proxy));
                    Toast.makeText(this, "代理已开启并保存", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("关闭代理", (d, w) -> {
                    isProxyEnabled = false;
                    updateProxyIconColor(findViewById(R.id.btn_proxy));
                    Toast.makeText(this, "代理已关闭", Toast.LENGTH_SHORT).show();
                });
        builder.show();
    }

    private void createNewTab() {
        WebView newWebView = new WebView(this);
        WebSettings settings = newWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 9) AppleWebKit/537.36 Chrome/110.0 Mobile Safari/537.36");

        newWebView.setWebChromeClient(new WebChromeClient());
        newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                try {
                    String url = request.getUrl().toString();
                    if (!url.startsWith("http://") && !url.startsWith("https://")) return true;
                    view.loadUrl(url);
                } catch (Exception e) {}
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isProxyEnabled && !TextUtils.isEmpty(proxyHost) && proxyPort > 0) {
                    try {
                        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
                        URL urlObj = new URL(url);
                        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection(proxy);
                        conn.setRequestMethod(request.getMethod());
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(20000);
                        for (String h : request.getRequestHeaders().keySet()) {
                            conn.setRequestProperty(h, request.getRequestHeaders().get(h));
                        }
                        if (!TextUtils.isEmpty(proxyUser)) {
                            String auth = proxyUser + ":" + proxyPwd;
                            String encode = android.util.Base64.encodeToString(auth.getBytes(), android.util.Base64.NO_WRAP);
                            conn.setRequestProperty("Proxy-Authorization", "Basic " + encode);
                        }
                        conn.connect();
                        return new WebResourceResponse(conn.getContentType(), conn.getContentEncoding(), conn.getInputStream());
                    } catch (Exception e) {}
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && !url.equals("about:blank")) {
                    String displayUrl = url.replace("https://", "").replace("http://", "");
                    if (displayUrl.contains("/")) {
                        displayUrl = displayUrl.split("/")[0];
                    }
                    urlInput.setText(displayUrl);
                } else {
                    urlInput.setText("");
                }
            }
        });

        newWebView.loadUrl("about:blank");
        urlInput.setText("");
        tabList.add(newWebView);
        currentTabIndex = tabList.size() - 1;
        showCurrentTab();
        updateTabCount();
    }

    private void showCurrentTab() {
        webViewContainer.removeAllViews();
        webViewContainer.addView(getCurrentWebView());
    }

    private WebView getCurrentWebView() {
        return tabList.get(currentTabIndex);
    }

    private String getTabDisplayName(int index) {
        WebView webView = tabList.get(index);
        String url = webView.getUrl();
        if (url == null || url.equals("about:blank")) return "标签 " + (index + 1) + " (空白)";
        String shortUrl = url.replace("https://", "").replace("http://", "");
        return shortUrl.split("/")[0];
    }

    private void showTabSwitchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tab_switch, null);
        ListView listView = dialogView.findViewById(R.id.list_view_tabs);
        TabAdapter adapter = new TabAdapter(this, tabList);
        listView.setAdapter(adapter);
        tabDialog = builder.setTitle("标签管理（共" + tabList.size() + "个）")
                .setView(dialogView)
                .create();
        tabDialog.show();
    }

    class BookmarkAdapter extends BaseAdapter {
        private final Context context;
        private final List<String> urls;
        private final List<String> names;

        public BookmarkAdapter(Context context, List<String> urls, List<String> names) {
            this.context = context;
            this.urls = urls;
            this.names = names;
        }

        @Override
        public int getCount() {
            return urls.size();
        }

        @Override
        public Object getItem(int position) {
            return urls.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_bookmark, parent, false);
            }
            TextView tvName = convertView.findViewById(R.id.tv_bookmark_name);
            Button btnDelete = convertView.findViewById(R.id.btn_delete_bookmark);

            tvName.setText(names.get(position));

            convertView.setOnClickListener(v -> {
                getCurrentWebView().loadUrl(urls.get(position));
                Toast.makeText(context, "已打开书签", Toast.LENGTH_SHORT).show();
            });

            btnDelete.setOnClickListener(v -> {
                urls.remove(position);
                names.remove(position);
                savePrefs();
                notifyDataSetChanged();
                Toast.makeText(context, "书签已删除", Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }

    class TabAdapter extends BaseAdapter {
        private final Context context;
        private final List<WebView> tabs;

        public TabAdapter(Context context, List<WebView> tabs) {
            this.context = context;
            this.tabs = tabs;
        }

        @Override
        public int getCount() {
            return tabs.size();
        }

        @Override
        public Object getItem(int position) {
            return tabs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_tab, parent, false);
            }
            TextView tvTitle = convertView.findViewById(R.id.tv_tab_title);
            Button btnDelete = convertView.findViewById(R.id.btn_delete_tab);

            String title = getTabDisplayName(position);
            tvTitle.setText("标签" + (position + 1) + ": " + title);

            convertView.setOnClickListener(v -> {
                currentTabIndex = position;
                showCurrentTab();
                if (tabDialog != null) tabDialog.dismiss();
            });

            btnDelete.setOnClickListener(v -> {
                if (tabs.size() <= 1) {
                    Toast.makeText(context, "不能删除最后一个标签", Toast.LENGTH_SHORT).show();
                    return;
                }
                tabs.remove(position);
                if (position == currentTabIndex) {
                    currentTabIndex = Math.max(0, position - 1);
                    showCurrentTab();
                } else if (position < currentTabIndex) {
                    currentTabIndex--;
                }
                updateTabCount();
                notifyDataSetChanged();
                Toast.makeText(context, "标签已删除", Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }

    @Override
    public void onBackPressed() {
        if (getCurrentWebView().canGoBack()) {
            getCurrentWebView().goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (WebView wv : tabList) {
            wv.stopLoading();
            wv.removeAllViews();
            wv.destroy();
        }
        tabList.clear();
    }
}
