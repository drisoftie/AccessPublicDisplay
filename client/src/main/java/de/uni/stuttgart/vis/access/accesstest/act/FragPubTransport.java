package de.uni.stuttgart.vis.access.accesstest.act;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.uni.stuttgart.vis.access.accesstest.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragPubTransport extends Fragment {

    public FragPubTransport() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_pub_transport, container, false);
    }
}
