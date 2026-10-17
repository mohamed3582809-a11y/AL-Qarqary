package com.qarqari.followup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.*;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> filePathCb;
    private static final int REQ_FILE = 11;
    private static final String HOME = "https://appassets.androidplatform.net/assets/index.html";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);

        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void save(String name, String mime, String b64) {
                saveToDownloads(name, mime, b64);
            }
        }, "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) {}
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePathCb != null) filePathCb.onReceiveValue(null);
                filePathCb = cb;
                try { startActivityForResult(p.createIntent(), REQ_FILE); }
                catch (Exception e) { filePathCb = null; return false; }
                return true;
            }
        });

        web.setDownloadListener((url, ua, cd, mime, len) -> {
            if (url.startsWith("blob:")) {
                String name = "database.json";
                if (cd != null && cd.contains("filename=")) {
                    name = cd.replaceAll(".*filename=\"?([^\";]+)\"?.*", "$1");
                }
                final String n = name.replace("'", "");
                final String js = "(function(){fetch('" + url + "').then(r=>r.blob()).then(b=>{var f=new FileReader();" +
                    "f.onload=function(){AndroidBridge.save('" + n + "',b.type||'application/octet-stream',f.result.split(',')[1]);};" +
                    "f.readAsDataURL(b);});})();";
                runOnUiThread(() -> web.evaluateJavascript(js, null));
            }
        });

        web.loadUrl(HOME);
    }

    private byte[] pendingBytes;
    private static final int REQ_SAVE = 12;

    // يفتح نافذة اختيار مكان الحفظ، ويحفظ الملف بصيغة .json
    private void saveToDownloads(String name, String mime, String b64) {
        try {
            if (!name.toLowerCase().endsWith(".json")) name = name + ".json";
            pendingBytes = Base64.decode(b64, Base64.DEFAULT);
            final String fname = name;
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, fname);
                startActivityForResult(i, REQ_SAVE);
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "تعذر حفظ الملف", Toast.LENGTH_LONG).show());
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_SAVE) {
            if (res == RESULT_OK && data != null && data.getData() != null && pendingBytes != null) {
                try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                    os.write(pendingBytes);
                    Toast.makeText(this, "اتحفظ الملف", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "تعذر حفظ الملف", Toast.LENGTH_LONG).show();
                }
            }
            pendingBytes = null;
            super.onActivityResult(req, res, data);
            return;
        }
        if (req == REQ_FILE && filePathCb != null) {
            filePathCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            filePathCb = null;
        }
        super.onActivityResult(req, res, data);
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
