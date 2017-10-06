package com.home.userone.testtask;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class CityInfoActivity extends AppCompatActivity {

    LinearLayout mainLayout;

    String country;
    String city;

    private static final String LOG_TAG = "CityInfoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_info);

        init();
        displayBackBtnOnTop();

        new getCityInfoTask(this).execute(buildSearchUrl(city, country));
    }

    private void init(){
        Intent intent = getIntent();
        country = intent.getStringExtra("country");
        city = intent.getStringExtra("city");
        Log.i(LOG_TAG, "Country: " + country + " city: " + city );

        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);

        TextView cityInfo = (TextView) View.inflate(this, R.layout.selected_city, null);
        cityInfo.setText(country + ": " + city);
        mainLayout.addView(cityInfo, 0);

    }

    private void displayBackBtnOnTop(){
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private String buildSearchUrl(String city, String country){

        Uri uri = new Uri.Builder()
                .scheme("http")
                .authority("api.geonames.org")
                .path("wikipediaSearch")
                .appendQueryParameter("q", city + " " + country)
                .appendQueryParameter("maxRows", "1")
                .appendQueryParameter("username", "nickgreenex")
                .build();
        return uri.toString();
    }

    class getCityInfoTask extends AsyncTask<String, Void, String> {

        Context context;
        Bitmap img;
        String errorMessage;

        public getCityInfoTask(Context context){
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {

            //get response from wiki search service
            String searchServiceXmlResponse;
            try {
                searchServiceXmlResponse = getRawTextFromURL(params[0]);
            } catch (IOException e) {
                errorMessage = "Error connecting to \n" + params[0];
                return null;
            }

            String articleUrl = getArticleUrl(searchServiceXmlResponse);

            if(articleUrl == null){
                errorMessage = "Wiki has no article about this";
                return null;
            }

            String articleHtml;
            try {
                articleHtml = getRawTextFromURL(articleUrl);
            } catch (IOException e) {
                errorMessage = "Error connecting to \n" + articleUrl;
                return null;
            }

            String imgUrl = getRelatedImageUrl(articleHtml);
            if(imgUrl == null)
                img = null;
            else
                img = getImg(imgUrl);


            return getDescription(articleHtml);
        }

        private String getArticleUrl(String xml){

            final String BEGIN_TAG = "<wikipediaUrl>";
            final String END_TAG = "</wikipediaUrl>";

            try {
                return xml.substring(xml.indexOf(BEGIN_TAG) + BEGIN_TAG.length(), xml.indexOf(END_TAG))
                        .replaceFirst("http", "https");

            } catch (StringIndexOutOfBoundsException e) {
                return null;
            }
        }

        private String getRawTextFromURL(String url) throws IOException {

            InputStream input;

            URL urlConn = new URL(url);
            input = urlConn.openStream();


            BufferedReader r = new BufferedReader(new InputStreamReader(input));
            StringBuilder total = new StringBuilder();
            String line;

            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');

            }



            return total.toString();
        }

        private Bitmap getImg (String url){

            InputStream input;
            try {
                URL urlConn = new URL(url);
                input = urlConn.openStream();
            }
            catch (IOException e) {
                return null;
            }

            return BitmapFactory.decodeStream(input);
        }

        private String getDescription(String html){

            try{
                final int ARTICLE_START_POS = html.indexOf("<p>");
                final int ARTICLE_END_POS = html.indexOf("</p>", ARTICLE_START_POS);

                return html.substring(ARTICLE_START_POS, ARTICLE_END_POS)
                        .replaceAll("[<].*?[>]", "")
                        .replaceAll("[(].*?[)]", "")
                        .replaceAll("[\\[].*?[]]", "");
            }
            catch (StringIndexOutOfBoundsException e){
                return "No description available";
            }

        }

        private String getRelatedImageUrl(String html){

            try{
                final int URL_START_POS = html.indexOf("src=\"//upload.wikimedia.org/wikipedia/commons/thumb", html.indexOf("<img" , html.indexOf("<table"))) + "src=\"".length();
                final int URL_END_POS = html.indexOf("\"", URL_START_POS);

                return "https:" + html.substring(URL_START_POS, URL_END_POS);
            }
            catch (StringIndexOutOfBoundsException e){
                return null;
            }
        }

        private int getScreenHeight(){

            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            return  metrics.heightPixels;
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);

            //remove loading progressbar
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            mainLayout.removeView(progressBar);

            TextView progressBarText = (TextView) findViewById(R.id.progressBarText);
            mainLayout.removeView(progressBarText);

            if(result == null){
                showErrorInfo(errorMessage);
            }
            else{
                //create city description view
                LinearLayout cityDescription = (LinearLayout) View.inflate(context, R.layout.layout_city_description, null);
                mainLayout.addView(cityDescription);

                if(img != null){
                    ImageView relatedImage = (ImageView) findViewById(R.id.image);

                    relatedImage.setMinimumHeight(getScreenHeight()/2);
                    relatedImage.setImageBitmap(img);
                }

                TextView cityInfo = (TextView) findViewById(R.id.text);
                cityInfo.setText(result);
            }
        }
    }

    //close activity on back ptn press
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home)
                finish();
        return super.onOptionsItemSelected(item);
    }

    private void showErrorInfo(String message){

        mainLayout.removeAllViews();
        LinearLayout nothingFound = (LinearLayout) View.inflate(this, R.layout.layout_error_info, null);
        mainLayout.addView(nothingFound);

        TextView messageTextView = (TextView) findViewById(R.id.message);
        messageTextView.setText(message);
    }


}
