package com.home.userone.testtask;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    LinearLayout mainLayout;
    Spinner countriesSpinner;
    ListView citiesListView;

    CountriesDatabaseHelper CountriesDB;
    Context context = this;

    String errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);

        new ShowCountriesTask().execute();
    }

   class ShowCountriesTask extends AsyncTask<Void, Void, ArrayList> {
        @Override
        protected ArrayList doInBackground(Void... params) {

            final String URL = "https://raw.githubusercontent.com/David-Haim/CountriesToCitiesJSON/master/countriesToCities.json";
            String responseTxt;
            try {
                responseTxt = getRawTextFromURL(URL);
            } catch (IOException e) {
                errorMessage = "Error connecting to \n" + URL;
                return null;
            }
            try {
                JSONObject jsonObj = new JSONObject(responseTxt);
                CountriesDB = new CountriesDatabaseHelper(context);
                fillDatabase(jsonObj);
            } catch (JSONException e) {
                errorMessage = "Error parsing server response";
                return null;
            }

            return CountriesDB.getCountries();
        }

        private String getRawTextFromURL(String url) throws IOException{

            URL urlConn = new URL(url);
            InputStream input = urlConn.openStream();

            BufferedReader r = new BufferedReader(new InputStreamReader(input));
            StringBuilder total = new StringBuilder();
            String line;

            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }

            return total.toString();
        }

        private void fillDatabase(JSONObject jsonObj) throws JSONException {

            for (int i=0; i<jsonObj.names().length(); i++) {
                String countryName = jsonObj.names().get(i).toString();
                CountriesDB.insertCountryRow(countryName, jsonObj.get(countryName).toString());
            }
        }

        @Override
        protected void onPostExecute(final ArrayList result) {
            super.onPostExecute(result);

            if(result == null){
                showErrorInfo(errorMessage);
            }else{
                //remove loading progressbar
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                mainLayout.removeView(progressBar);
                TextView progressBarText = (TextView) findViewById(R.id.progressBarText);
                mainLayout.removeView(progressBarText);

                // import countries spinner with list
                LinearLayout citiesByCountryLayout = (LinearLayout) View.inflate(context, R.layout.layout_cities_by_country, null);
                mainLayout.addView(citiesByCountryLayout);
                countriesSpinner = (Spinner) findViewById(R.id.countries);
                citiesListView = (ListView) findViewById(R.id.cities);

                // set adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.countries_spinner_item, result);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                countriesSpinner.setAdapter(adapter);

                countriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedCountryName = result.get(position).toString();
                        new ShowCitiesListTask(selectedCountryName).execute();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
            }
        }
    }

    class ShowCitiesListTask extends AsyncTask<Void, Void, ArrayList> {

        String country;
        String errorMessage;

        public ShowCitiesListTask (String countryName) {

            country = countryName;
        }

        @Override
        protected ArrayList doInBackground(Void... params) {

            try {
               return CountriesDB.getCitiesByCountry(country);
            } catch (JSONException e) {
                errorMessage = "Error parsing cities list";
                return null;
            }
        }

        @Override
        protected void onPostExecute(final ArrayList result) {
            super.onPostExecute(result);

            if(result == null) {
                showErrorInfo(errorMessage);
            }else{



                ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.cities_list_item, result);
                citiesListView.setAdapter(adapter);

                citiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Intent intent = new Intent(context, CityInfoActivity.class);

                        intent.putExtra("country", country);
                        intent.putExtra("city", result.get(position).toString());

                        startActivity(intent);
                    }
                });
            }



        }
    }

    private void showErrorInfo(String message){

        mainLayout.removeAllViews();
        LinearLayout nothingFound = (LinearLayout) View.inflate(this, R.layout.layout_error_info, null);
        mainLayout.addView(nothingFound);

        TextView messageTextView = (TextView) findViewById(R.id.message);
        messageTextView.setText(message);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(CountriesDB != null)
            CountriesDB.close();
    }


}
