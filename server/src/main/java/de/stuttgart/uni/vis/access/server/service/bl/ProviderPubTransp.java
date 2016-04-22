package de.stuttgart.uni.vis.access.server.service.bl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import de.stuttgart.uni.vis.access.common.domain.Departure;
import de.stuttgart.uni.vis.access.common.domain.PubTranspType;
import de.stuttgart.uni.vis.access.common.domain.PublicTransport;

/**
 * @author Alexander Dridiger
 */
public class ProviderPubTransp {

    private static ProviderPubTransp inst;


    private List<PublicTransport> transports = new ArrayList<>();

    private ProviderPubTransp() {
        inst = this;
    }

    public static ProviderPubTransp inst() {
        if (inst == null) {
            new ProviderPubTransp();
        }
        return inst;
    }

    public boolean hasTransportInfo() {
        return !transports.isEmpty();
    }

    public List<PublicTransport> getTransports() {
        return transports;
    }

    public void createTransportInfo() {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
        Type collectionType = new TypeToken<Collection<Departure>>() {
        }.getType();

        InputStream is = null;
        try {
            URL               url  = new URL("https://efa-api.asw.io/api/v1/station/5006056/departures/?format=json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            is = conn.getInputStream();

            Reader reader = null;
            reader = new InputStreamReader(is, "UTF-8");

            Collection<Departure> departures = gson.fromJson(reader, collectionType);

            transports = new ArrayList<>();

            Calendar c       = Calendar.getInstance();
            int      minutes = c.get(Calendar.MINUTE);

            if (departures != null) {
                for (Departure d : departures) {
                    PublicTransport t = new PublicTransport();
                    t.setLine(d.number);
                    t.setTime(d.departureTime.hour + ":" + d.departureTime.minute);
                    t.setDirection(d.direction);
                    if (StringUtils.isNumericSpace(d.number)) {
                        t.setType(PubTranspType.BUS);
                        transports.add(t);
                        //                        .append(d.number).append(":").append(
                        //                                String.valueOf(Math.min(Integer.valueOf(d.departureTime.minute) - minutes, 0)));
                    } else if (StringUtils.startsWithIgnoreCase(d.number, "s")) {
                        t.setType(PubTranspType.TRAIN);
                        transports.add(t);
                    } else if (StringUtils.startsWithIgnoreCase(d.number, "u")) {
                        t.setType(PubTranspType.METRO);
                        transports.add(t);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
