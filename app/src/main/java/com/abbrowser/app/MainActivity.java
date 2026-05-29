package com.abbrowser.app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        ImageButton goBtn = findViewById(R.id.goBtn);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnForward = findViewById(R.id.btn_forward);
        ImageButton btnRefresh = findViewById(R.id.btn_refresh);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
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
                urlInput.setText(url);
            }
        });

        webView.loadUrl("https://www.baidu.com");

        // 点击搜索图标，和之前的“前往”按钮逻辑一样
        goBtn.setOnClickListener(v -> {
            String text = urlInput.getText().toString().trim();
            if (text.isEmpty()) return;

            if (text.startsWith("http://") || text.startsWith("https://")) {
                webView.loadUrl(text);
            } else {
                String searchUrl = "https://www.baidu.com/s?wd=" + text;
                webView.loadUrl(searchUrl);
            }
        });

        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        btnRefresh.setOnClickListener(v -> webView.reload());
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
