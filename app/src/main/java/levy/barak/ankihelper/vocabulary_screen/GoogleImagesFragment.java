package levy.barak.ankihelper.vocabulary_screen;

import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;

import levy.barak.ankihelper.AnkiHelperApplication;
import levy.barak.ankihelper.R;
import levy.barak.ankihelper.utils.FileUtils;
import levy.barak.ankihelper.utils.GermanUtils;

public class GoogleImagesFragment extends Fragment {
    public class WebAppInterface {
        private Fragment mContext;

        WebAppInterface(Fragment c) {
            mContext = c;
        }

        @JavascriptInterface
        public void catchHref(String href) throws UnsupportedEncodingException {
            String[] parts = href.split("\\?");
            String[] attributes = href.split("&");
            String imageUrl = java.net.URLDecoder.decode(attributes[0].split("=")[1], "UTF-8");

            // Move on to the next screen
            moveToNextScreen();

            new Thread(() -> {
                // Download it
                String downloadName = "anki_helper_image_" + AnkiHelperApplication.currentWord.id + "_" + AnkiHelperApplication.currentWord.imagesUrl.size();
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
                request.allowScanningByMediaScanner();
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "anki_helper/" + downloadName);
                AnkiHelperApplication.currentWord.imagesUrl.add(downloadName);
                DownloadManager dm = (DownloadManager) mContext.getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }).start();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.fragment_vocabulary_google_images, container, false);

        final WebView googleImagesWebView = (WebView) fragment.findViewById(R.id.googleImagesWebView);

        googleImagesWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        WebSettings settings = googleImagesWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        googleImagesWebView.setWebViewClient(new WebViewClient() {
            int resourcesLoaded;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                resourcesLoaded = 0; // This is here because if the user searches for another image, we want to reset the resources loaded count
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);

                // We only attach the script AFTER we load the first resource which is the main URL
                if (resourcesLoaded == 1) {
                    googleImagesWebView.evaluateJavascript(FileUtils.getFileContent(googleImagesWebView.getContext(), "googleImages.js"), null);
                }

                resourcesLoaded++;
            }
        });

        googleImagesWebView.loadUrl(
                "https://www.google.de/search?q=" +
                        GermanUtils.getGermanWordWithoutPrefix() +
                        "&hl=de&tbo=d&site=imghp&tbm=isch&gwd_rd=ssl"
        );

        return fragment;
    }

    public void moveToNextScreen() {
        ForvoFragment newFragment = new ForvoFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.replace(R.id.fragmentsContainer, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
