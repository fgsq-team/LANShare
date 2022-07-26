package com.fgsqw.lanshare.activity.web;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.mUtil;

public class PrivacyWebActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        String name = getIntent().getStringExtra("name");
        WebView webView = findViewById(R.id.web_view);
        webView.loadData(IOUtil.readAssetsTxt(this, name), FileUtil.getMyMIMEType("html"), FileUtil.UTF_8.toString());
    }
}
