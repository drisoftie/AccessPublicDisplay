/*****************************************************************************
 * Copyright 2012-2013 Sony Corporation
 * <p/>
 * The information contained here-in is the property of Sony corporation and
 * is not to be disclosed or used without the prior written permission of
 * Sony corporation. This copyright extends to all media in which this
 * information may be preserved including magnetic storage, computer
 * print-out or visual display.
 * <p/>
 * Contains proprietary information, copyright and database rights Sony.
 * Decompilation prohibited save as permitted by law. No using, disclosing,
 * reproducing, accessing or modifying without Sony prior written consent.
 ****************************************************************************/
package de.stuttgart.uni.vis.access.server;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.stuttgart.uni.vis.access.common.AppBase;
import de.stuttgart.uni.vis.access.common.domain.Station;
import de.stuttgart.uni.vis.access.server.db.SqliteOpenHelper;

/**
 * Central {@link Application} class for handling application states. Uses the Singleton Pattern.
 *
 * @author Alexander Dridiger
 */
public class App extends AppBase {

    public List<Station> stations;

    @SuppressWarnings("rawtypes")
    @Override
    public void onCreate() {
        super.onCreate();

        Thread dlThread = new Thread() {
            @Override
            public void run() {
                ConnectivityManager connMgr     = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo         networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {


                    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
                    Type collectionType = new TypeToken<Collection<Station>>() {
                    }.getType();

                    InputStream is = null;
                    // Only display the first 500 characters of the retrieved
                    // web page content.
                    try {
                        URL               url  = new URL("https://efa-api.asw.io/api/v1/station/");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(10000 /* milliseconds */);
                        conn.setConnectTimeout(15000 /* milliseconds */);
                        conn.setRequestMethod("GET");
                        conn.setDoInput(true);
                        // Starts the query
                        conn.connect();
                        int response = conn.getResponseCode();
                        Log.d("RESPONSE EFA", "The response is: " + response);
                        is = conn.getInputStream();

                        Reader reader = null;
                        reader = new InputStreamReader(is, "UTF-8");

                        Collection<Station> stations = gson.fromJson(reader, collectionType);

                        if (stations != null) {
                            App.this.stations = new ArrayList<>();
                            App.this.stations.addAll(stations);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                }
            }
        };
        dlThread.start();

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public Class<? extends OrmLiteSqliteOpenHelper> getSqliteOpenHelperClass() {
        return SqliteOpenHelper.class;
    }
}
