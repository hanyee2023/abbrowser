package com.abbrowser.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private EditText urlInput;
    private FrameLayout webViewContainer;
    private List<WebView> tabList = new ArrayList<>();
    private int currentTabIndex = 0;
    private AlertDialog tabDialog;
    private TextView tabCountText;

    private boolean adBlockEnabled = true;
    private ImageButton btnAdBlock;
    private TextView adBlockTip;
    private Handler tipHandler = new Handler();

    private static final Set<String> AD_HOSTS = new HashSet<>(Arrays.asList(
            "ad.", "ads.", "advert.", "adserver.", "doubleclick.net",
            "googleadservices.com", "tanx.com", "adroll.com", "media.net",
            "taboola.com", "outbrain.com", "zedo.com", "smartadserver.com",
            "baidu.com/ad", "youku.com/ad", "iqiyi.com/ad", "sina.com.cn/ad"
    ));

    private static final String AD_ELEMENTS =
            "div[class*='ad'],div[id*='ad'],div[class*='advert'],div[id*='advert']," +
            ".ads,.advert,.banner,.popup,.ad-box,.ad-container,.ad-banner,.ad-popup,.ad-float";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlInput = findViewById(R.id.urlInput);
        webViewContainer = findViewById(R.id.webview_container);
        adBlockTip = findViewById(R.id.adBlockTip);
        btnAdBlock = findViewById(R.id.btnAdBlock);
        tabCountText = findViewById(R.id.tabCount);

        ImageButton btnBookmark = findViewById(R.id.btn_bookmark);
        ImageButton btnNewTab = findViewById(R.id.btn_new_tab);
        ImageButton btnSwitchTab = findViewById(R.id.btn_switch_tab);

        // 广告拦截开关：只在点击时切换状态，不响应软键盘事件
        btnAdBlock.setOnClickListener(v -> {
            adBlockEnabled = !adBlockEnabled;
            updateAdBlockIcon();
            Toast.makeText(this, adBlockEnabled ? "广告拦截已开启" : "广告拦截已关闭", Toast.LENGTH_SHORT).show();
        });

        // 软键盘搜索监听：只执行搜索，不触发广告拦截
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearchOrGo();
                return true;
            }
            return false;
        });

        createNewTab();

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnForward = findViewById(R.id.btn_forward);
        ImageButton btnRefresh = findViewById(R.id.btn_refresh);

        btnBack.setOnClickListener(v -> {
            if (getCurrentWebView().canGoBack()) getCurrentWebView().goBack();
        });

        btnForward.setOnClickListener(v -> {
            if (getCurrentWebView().canGoForward()) getCurrentWebView().goForward();
        });

        btnRefresh.setOnClickListener(v -> {
            getCurrentWebView().reload();
        });

        btnBookmark.setOnClickListener(v -> showBookmarkDialog());
        btnNewTab.setOnClickListener(v -> createNewTab());
        btnSwitchTab.setOnClickListener(v -> showTabSwitchDialog());

        updateAdBlockIcon();
        updateTabCount();
    }

    private void performSearchOrGo() {
        String text = urlInput.getText().toString().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("http://") || text.startsWith("https://")) {
            getCurrentWebView().loadUrl(text);
        } else {
            getCurrentWebView().loadUrl("https://cn.bing.com/search?q=" + text);
        }
    }

    private void updateAdBlockIcon() {
        btnAdBlock.setImageResource(adBlockEnabled ? R.drawable.ic_adblock_on : R.drawable.ic_adblock_off);
    }

    private void updateTabCount() {
        tabCountText.setText(String.valueOf(tabList.size()));
        tabCountText.setVisibility(tabList.size() > 1 ? View.VISIBLE : View.GONE);
    }

    private void showAdBlockTip(String text) {
        adBlockTip.setText(text);
        adBlockTip.setVisibility(View.VISIBLE);
        tipHandler.removeCallbacksAndMessages(null);
        tipHandler.postDelayed(() -> adBlockTip.setVisibility(View.GONE), 2000);
    }

    private void createNewTab() {
        WebView newWebView = new WebView(this);
        WebSettings settings = newWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("baiduboxapp://")) {
                    String realUrl = url.replace("baiduboxapp://", "https://");
                    view.loadUrl(realUrl);
                    return true;
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!adBlockEnabled) return super.shouldInterceptRequest(view, request);

                String url = request.getUrl().toString().toLowerCase();
                for (String ad : AD_HOSTS) {
                    if (url.contains(ad)) {
                        showAdBlockTip("已拦截广告资源");
                        return new WebResourceResponse(null, null, null);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!url.equals("about:blank")) {
                    urlInput.setText("");
                    urlInput.setHint("已加载完成");
                }
                if (adBlockEnabled) {
                    view.evaluateJavascript("javascript:(function(){" +
                            "var ads=document.querySelectorAll('" + AD_ELEMENTS + "');" +
                            "for(var i=0;i<ads.length;i++)ads[i].remove();" +
                            "})()", null);
                }
            }
        });

        newWebView.loadUrl("about:blank");
        urlInput.setText("");
        urlInput.setHint("输入网址或搜索");

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
        if (url == null || url.equals("about:blank")) {
            return "标签 " + (index + 1) + " (空白页)";
        }
        if (url.startsWith("https://")) url = url.substring(8);
        if (url.startsWith("http://")) url = url.substring(7);
        if (url.contains("/")) url = url.split("/")[0];
        return "标签 " + (index + 1) + ": " + url;
    }

    private void showTabSwitchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle);
        builder.setTitle("标签管理（共" + tabList.size() + "个）");

        ListView listView = new ListView(this);
        TabAdapter tabAdapter = new TabAdapter(this, tabList);
        listView.setAdapter(tabAdapter);
        builder.setView(listView);

        tabDialog = builder.create();
        tabDialog.show();
    }

    class TabAdapter extends ArrayAdapter<WebView> {
        private final Context context;
        private final List<WebView> tabs;

        public TabAdapter(Context context, List<WebView> tabs) {
            super(context, R.layout.dialog_tab_item, tabs);
            this.context = context;
            this.tabs = tabs;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.dialog_tab_item, parent, false);
            }

            TextView tvTitle = convertView.findViewById(R.id.tvTabTitle);
            TextView tvClose = convertView.findViewById(R.id.tvClose);

            tvTitle.setText(getTabDisplayName(position));

            convertView.setOnClickListener(v -> {
                currentTabIndex = position;
                showCurrentTab();
                String url = getCurrentWebView().getUrl();
                if (url != null && !url.equals("about:blank")) {
                    urlInput.setHint("已加载完成");
                } else {
                    urlInput.setHint("输入网址或搜索");
                }
                if (tabDialog != null) {
                    tabDialog.dismiss();
                }
            });

            tvClose.setOnClickListener(v -> {
                if (tabs.size() <= 1) {
                    Toast.makeText(context, "至少保留一个标签", Toast.LENGTH_SHORT).show();
                    return;
                }
                tabs.remove(position);
                if (currentTabIndex >= tabs.size()) {
                    currentTabIndex = tabs.size() - 1;
                }
                showCurrentTab();
                notifyDataSetChanged();
                updateTabCount();
            });

            return convertView;
        }
    }

    private void showBookmarkDialog() {
        List<Map<String, String>> bookmarkData = new ArrayList<>();
        SharedPreferences sp = getSharedPreferences("bookmarks", MODE_PRIVATE);
        String json = sp.getString("list", "");
        if (json != null && !json.isEmpty()) {
            String[] items = json.split("@@");
            for (String item : items) {
                String[] parts = item.split("\\|");
                if (parts.length == 2) {
                    Map<String, String> map = new java.util.HashMap<>();
                    map.put("title", parts[0]);
                    map.put("url", parts[1]);
                    bookmarkData.add(map);
                }
            }
        }

        String[] titles = new String[bookmarkData.size()];
        for (int i = 0; i < bookmarkData.size(); i++) {
            titles[i] = bookmarkData.get(i).get("title");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle);
        builder.setTitle("书签管理");
        if (titles.length > 0) {
            builder.setItems(titles, (dialog, which) -> {
                String url = bookmarkData.get(which).get("url");
                getCurrentWebView().loadUrl(url);
                dialog.dismiss();
            });
        } else {
            builder.setMessage("暂无书签");
        }
        builder.setPositiveButton("添加当前页", (dialog, which) -> {
            addCurrentPageToBookmark();
            dialog.dismiss();
        });
        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    private void addCurrentPageToBookmark() {
        WebView webView = getCurrentWebView();
        String url = webView.getUrl();
        String title = webView.getTitle();
        if (url == null || url.equals("about:blank")) {
            Toast.makeText(this, "空白页无法添加书签", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sp = getSharedPreferences("bookmarks", MODE_PRIVATE);
        String json = sp.getString("list", "");
        String newItem = title + "|" + url;
        if (json.isEmpty()) json = newItem;
        else json += "@@" + newItem;
        sp.edit().putString("list", json).apply();
        Toast.makeText(this, "已添加书签", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (getCurrentWebView().canGoBack()) {
            getCurrentWebView().goBack();
        } else {
            super.onBackPressed();
        }
    }
}
