package com.abbrowser.app;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText urlInput;
    private FrameLayout webViewContainer;
    private List<WebView> tabList = new ArrayList<>();
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        urlInput = findViewById(R.id.urlInput);
        webViewContainer = findViewById(R.id.webview_container);
        ImageButton btnBookmark = findViewById(R.id.btn_bookmark);
        ImageButton btnNewTab = findViewById(R.id.btn_new_tab);
        ImageButton btnSwitchTab = findViewById(R.id.btn_switch_tab);

        // 创建第一个标签
        createNewTab();

        // 顶部栏其他按钮
        ImageButton goBtn = findViewById(R.id.goBtn);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnForward = findViewById(R.id.btn_forward);
        ImageButton btnRefresh = findViewById(R.id.btn_refresh);

        goBtn.setOnClickListener(v -> {
            String text = urlInput.getText().toString().trim();
            if (text.isEmpty()) return;
            if (text.startsWith("http://") || text.startsWith("https://")) {
                getCurrentWebView().loadUrl(text);
            } else {
                getCurrentWebView().loadUrl("https://www.baidu.com/s?wd=" + text);
            }
        });

        btnBack.setOnClickListener(v -> {
            if (getCurrentWebView().canGoBack()) getCurrentWebView().goBack();
        });

        btnForward.setOnClickListener(v -> {
            if (getCurrentWebView().canGoForward()) getCurrentWebView().goForward();
        });

        btnRefresh.setOnClickListener(v -> {
            getCurrentWebView().reload();
        });

        // 修复：书签按钮点击事件
        btnBookmark.setOnClickListener(v -> {
            showBookmarkDialog();
        });

        // 新建标签按钮
        btnNewTab.setOnClickListener(v -> {
            createNewTab();
        });

        // 【新增】切换标签按钮点击事件
        btnSwitchTab.setOnClickListener(v -> {
            showTabSwitchDialog();
        });
    }

    // 创建新标签
    private void createNewTab() {
        WebView newWebView = new WebView(this);
        WebSettings settings = newWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("baiduboxapp://")) {
                    String real = url.replace("baiduboxapp://", "https://");
                    view.loadUrl(real);
                    return true;
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!url.equals("about:blank")) {
                    urlInput.setText(url);
                }
            }
        });

        // 加载空白页
        newWebView.loadUrl("about:blank");
        urlInput.setText("");

        // 加入列表并切换到新标签
        tabList.add(newWebView);
        currentTabIndex = tabList.size() - 1;

        // 【关键修复】显示当前标签的WebView
        showCurrentTab();
    }

    // 显示当前标签（解决覆盖问题）
    private void showCurrentTab() {
        webViewContainer.removeAllViews();
        webViewContainer.addView(getCurrentWebView());
    }

    // 获取当前WebView
    private WebView getCurrentWebView() {
        return tabList.get(currentTabIndex);
    }

    // 【新增】标签切换对话框
    private void showTabSwitchDialog() {
        String[] tabTitles = new String[tabList.size()];
        for (int i = 0; i < tabList.size(); i++) {
            String url = tabList.get(i).getUrl();
            if (url == null || url.equals("about:blank")) {
                tabTitles[i] = "标签 " + (i + 1) + " (空白)";
            } else {
                tabTitles[i] = "标签 " + (i + 1) + ": " + url;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("切换标签")
                .setItems(tabTitles, (dialog, which) -> {
                    currentTabIndex = which;
                    showCurrentTab();
                    // 更新地址栏
                    String url = getCurrentWebView().getUrl();
                    if (url != null && !url.equals("about:blank")) {
                        urlInput.setText(url);
                    } else {
                        urlInput.setText("");
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    // 【修复】完整的书签对话框逻辑
    private void showBookmarkDialog() {
        List<Map<String,String>> bookmarkData = new ArrayList<>();
        SharedPreferences sp = getSharedPreferences("bookmarks", MODE_PRIVATE);
        String json = sp.getString("list", "");
        if (json != null && !json.isEmpty()) {
            String[] items = json.split("@@");
            for (String item : items) {
                String[] parts = item.split("\\|");
                if (parts.length == 2) {
                    Map<String,String> map = new HashMap<>();
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    // 添加当前页到书签
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
