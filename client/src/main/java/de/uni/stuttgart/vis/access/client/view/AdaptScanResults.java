package de.uni.stuttgart.vis.access.client.view;

import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class AdaptScanResults extends RecyclerView.Adapter<AdaptScanResults.ViewHolder> {

    private List<ScanResult> results;

    public AdaptScanResults() {
        results = new ArrayList<>();
    }

    public List<ScanResult> getResults() {
        return results;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdaptScanResults.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.ritem_scan_result, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ScanResult result = results.get(position);
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        byte[] data = result.getScanRecord().getServiceData().get(Constants.UUID_SERVICE_WEATHER);
        holder.txtDeviceName.setText(result.getDevice().getName());
        holder.txtAdInfo.setText("Advertisement: " + new String(data));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            private ScanResult result;

            public View.OnClickListener init(ScanResult result) {
                this.result = result;
                return this;
            }

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext().getString(R.string.intent_bl_device_addr));
                // You can also include some extra data.
                intent.putExtra(v.getContext().getString(R.string.bndl_bl_dev_addr), result.getDevice().getAddress());
                LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(intent);
            }
        }.init(result));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtDeviceName;
        public TextView txtAdInfo;

        public ViewHolder(View item) {
            super(item);
            this.txtDeviceName = (TextView) item.findViewById(R.id.ritem_txt_device_name);
            this.txtAdInfo = (TextView) item.findViewById(R.id.ritem_txt_device_adv_content);
        }
    }
}