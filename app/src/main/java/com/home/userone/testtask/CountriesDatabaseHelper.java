package com.home.userone.testtask;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class CountriesDatabaseHelper extends SQLiteOpenHelper {

    SQLiteDatabase database;
    final String TABLE_NAME = "Countries";

    public CountriesDatabaseHelper(Context context) {
        super(context, "Countries", null, 1);
        connect();
        createCountriesTable();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void createCountriesTable(){

        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        database.execSQL("create table " + TABLE_NAME + " ("
                + "id integer primary key autoincrement,"
                + "country text,"
                + "cities text" + ");");
    }

    public void insertCountryRow(String countryName, String citiesJSON){

        try {
            if(!countryName.equals("")) {
                ContentValues cv = new ContentValues(2);
                cv.put("country", countryName);
                cv.put("cities", citiesJSON);
                database.insert(TABLE_NAME, null, cv);
            }
        }catch (IllegalStateException e){
            return;
        }
    }

    public void connect(){
        database  = this.getWritableDatabase();
    }

    public ArrayList getCountries(){

        try {
            ArrayList countries = new ArrayList();
            Cursor c = database.query(TABLE_NAME, new String[]{"country"}, null, null, null, null, null);

            if (c.moveToFirst()) {  //false if empty
                int countryColumnIndex = c.getColumnIndex("country");
                do {
                    countries.add(c.getString(countryColumnIndex));
                } while (c.moveToNext());   //false if last
            }
            c.close();

            return countries;
        }catch (IllegalStateException e){
            return null;
        }
    }

    public ArrayList getCitiesByCountry(String countryName) throws JSONException {

        try {
            String citiesJSON = "";
            Cursor c = database.query(TABLE_NAME, new String[]{"cities"}, "country = '" + countryName + "'", null, null, null, null);

            if (c.moveToFirst()) {
                citiesJSON = c.getString(c.getColumnIndex("cities"));
            }
            c.close();

            return citiesJSONToArrayList(citiesJSON);
        }catch (IllegalStateException e){
            return null;
        }

    }

    private ArrayList citiesJSONToArrayList(String JSON) throws JSONException {

        ArrayList citiesList = new ArrayList();

        JSONArray cities = new JSONArray(JSON);

        for(int i=0; i<cities.length(); i++){
            citiesList.add(cities.get(i).toString());
        }

        return citiesList;
    }
}
