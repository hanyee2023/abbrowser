package com.abbrowser.app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;
    private Button goBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        goBtn = findViewById(R.id.goBtn);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        // 关键修复：处理百度自定义协议跳转
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // 拦截百度APP协议，强制使用网页版
                if (url.startsWith("baiduboxapp://")) {
                    return true;
                }
                view.loadUrl(url);
                return false;
            }
        });

        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlInput.getText().toString().trim();
                if (url.isEmpty()) return;

                // 修复1：优先用百度搜索，国内可访问
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://www.baidu.com/s?wd=" + url;
                }
                webView.loadUrl(url);
            }
        });

        // 默认加载百度首页
        webView.loadUrl("https://www.baidu.com");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}