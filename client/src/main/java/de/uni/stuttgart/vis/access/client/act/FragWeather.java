package de.uni.stuttgart.vis.access.client.act;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.uni.stuttgart.vis.access.client.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragWeather extends Fragment {

    public FragWeather() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_weather, container, false);
    }
}
