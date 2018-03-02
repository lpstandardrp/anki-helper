package levy.barak.ankihelper.vocabulary_screen;

import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import levy.barak.ankihelper.AnkiHelperApplication;
import levy.barak.ankihelper.R;
import levy.barak.ankihelper.utils.FileUtils;

public class GoogleImagesFragment extends Fragment {
    public static final int PICK_IMAGE = 0;

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
                String downloadName = getDownloadPath();
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

        googleImagesWebView.loadUrl(getUrl(AnkiHelperApplication.language.getSearchableWord()));

        // Enable the menu on this fragment
        setHasOptionsMenu(true);

        return fragment;
    }

    public void moveToNextScreen() {
        ForvoFragment newFragment = new ForvoFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.replace(R.id.fragmentsContainer, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public String getUrl(String word) {
        return "https://www.google.de/search?q=" + word +
                "&hl=de&tbo=d&site=imghp&tbm=isch&gwd_rd=ssl";
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.vocabulary_menu_images, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final WebView webView = (WebView) getActivity().findViewById(R.id.googleImagesWebView);
        switch (item.getItemId()) {
            case R.id.vocabulary_menu_images_search_in_english:
                webView.loadUrl(getUrl(AnkiHelperApplication.currentWord.firstLanguageWord));
                return true;
            case R.id.vocabulary_menu_images_search_in_german:
                webView.loadUrl(getUrl(AnkiHelperApplication.language.getSearchableWord()));
                return true;
            case R.id.vocabulary_menu_images_pick_image:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_IMAGE:
                // If the user chose an image, this is -1
                if (resultCode == -1) {
                    copyImageFromFile(data.getData());

                    // Move on to the next screen
                    moveToNextScreen();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public String getDownloadPath() {
        return "anki_helper_image_" + AnkiHelperApplication.currentWord.id + "_" + AnkiHelperApplication.currentWord.imagesUrl.size();
    }

    public void copyImageFromFile(Uri imageUri) {
        try {
            InputStream input = getActivity().getContentResolver().openInputStream(imageUri);
            FileOutputStream output = new FileOutputStream(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/anki_helper/" + getDownloadPath());

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            input.close();
            output.close();

            // Add the image's URL
            AnkiHelperApplication.currentWord.imagesUrl.add(getDownloadPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}