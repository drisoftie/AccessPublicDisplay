package de.uni.stuttgart.vis.access.client.view;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.stuttgart.uni.vis.access.common.util.ParserData;
import de.uni.stuttgart.vis.access.client.App;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.data.BlData;
import de.uni.stuttgart.vis.access.client.data.GattData;

/**
 * @author Alexander Dridiger
 */
public class AdaptScans extends RecyclerView.Adapter<AdaptScans.ViewHolder> {

    private List<BlData> blData;

    public AdaptScans() {
        blData = new ArrayList<>();
    }

    public List<BlData> getBlData() {
        return blData;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdaptScans.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.ritem_scan_result, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BlData  data  = blData.get(position);
        boolean start = false;
        for (int i = 0; i < data.getAdvertisement().length; i++) {
            byte b = data.getAdvertisement()[i];
            if (b == Constants.AdvertiseConst.ADVERTISE_START) {
                start = true;
                holder.txtAdInfo.setText("");
                //                holder.txtAdInfo.append(data.getRssi() + System.lineSeparator());
            } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER.getFlag()) {
                if (start) {
                    start = false;
                } else {
                    holder.txtAdInfo.append(System.lineSeparator());
                }
                String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), data);
                if (g != null) {
                    holder.txtAdInfo.append("Todays weather is: ");
                    holder.txtAdInfo.append(g);
                } else {
                    holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()));
                }
            } else if (b == Constants.AdvertiseConst.ADVERTISE_WEATHER_DATA.getFlag()) {
                if (start) {
                    start = false;
                } else {
                    holder.txtAdInfo.append(System.lineSeparator());
                }
                String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), data);
                if (g != null) {
                    holder.txtAdInfo.append("Todays weather is: ");
                    holder.txtAdInfo.append(g);
                } else {
                    holder.txtAdInfo.append("Current temperature is: ");
                    holder.txtAdInfo.append(new DecimalFormat("#.#").format(ParserData.parseByteToFloat(
                            Arrays.copyOfRange(data.getAdvertisement(), i + 1, i + 5))));
                }
            } else if (b == Constants.AdvertiseConst.ADVERTISE_TRANSP.getFlag()) {
                if (start) {
                    start = false;
                } else {
                    holder.txtAdInfo.append(System.lineSeparator());
                }
                holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()));
            } else if (b == Constants.AdvertiseConst.ADVERTISE_SHOUT.getFlag()) {
                if (start) {
                    start = false;
                } else {
                    holder.txtAdInfo.append(System.lineSeparator());
                }
                String g = getGattData(Constants.SHOUT.GATT_SHOUT.getUuid(), data);
                if (g != null) {
                    holder.txtAdInfo.append("Newest shout: ");
                    holder.txtAdInfo.append(g);
                } else {
                    holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_SHOUT.getDescr()));
                }
            } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS.getFlag()) {
                if (start) {
                    start = false;
                } else {
                    holder.txtAdInfo.append(System.lineSeparator());
                }
                String g = getGattData(Constants.NEWS.GATT_NEWS.getUuid(), data);
                if (g != null) {
                    holder.txtAdInfo.append(g);
                } else {
                    holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_NEWS.getDescr()));
                }
            } else if (b == Constants.AdvertiseConst.ADVERTISE_NEWS_DATA.getFlag()) {
                if (start) {
                    start = false;
                } else {
                    holder.txtAdInfo.append(System.lineSeparator());
                }
                String g = getGattData(Constants.NEWS.GATT_NEWS.getUuid(), data);
                if (g != null) {
                    holder.txtAdInfo.append(g);
                } else {
                    holder.txtAdInfo.append("Current amount of news: ");
                    holder.txtAdInfo.append(String.valueOf(data.getAdvertisement()[i + 1]));
                }
            } else if (b == Constants.AdvertiseConst.ADVERTISE_BOOKING.getFlag()) {
                if (start) {
                    start = false;
                } else {
                    holder.txtAdInfo.append(System.lineSeparator());
                }
                String g = getGattData(Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(), data);
                if (g != null) {
                    holder.txtAdInfo.append(g);
                } else {
                    holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_BOOKING.getDescr()));
                }
            }
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            private BlData data;

            public View.OnClickListener init(BlData data) {
                this.data = data;
                return this;
            }

            @Override
            public void onClick(View v) {
                Intent showIntent = new Intent(v.getContext().getString(R.string.intent_advert_gatt_connect_weather));
                showIntent.putExtra(v.getContext().getString(R.string.bndl_bl_show), Constants.UUID_ADVERT_SERVICE_MULTI);
                showIntent.putExtra(v.getContext().getString(R.string.bndl_bl_address), data.getAddress());
                LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(showIntent);
                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                v.getContext().sendBroadcast(it);
            }
        }.init(data));
    }

    private String getGattData(UUID uuid, BlData data) {
        String value = null;
        for (GattData g : data.getGattData()) {
            if (g.getUuid().equals(uuid)) {
                value = new String(g.getData());
                break;
            }
        }
        return value;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return blData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtAdInfo;

        public ViewHolder(View item) {
            super(item);
            this.txtAdInfo = (TextView) item.findViewById(R.id.ritem_txt_device_adv_content);
        }
    }
}