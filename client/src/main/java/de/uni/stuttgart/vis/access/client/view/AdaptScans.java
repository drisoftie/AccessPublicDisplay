package de.uni.stuttgart.vis.access.client.view;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
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
        boolean start = true;
        holder.txtAdInfo.setText("");
        for (AbstractMap.SimpleEntry<Constants.AdvertiseConst, byte[]> advertEntry : ParserData.parseAdvert(data.getAdvertisement())) {
            switch (advertEntry.getKey()) {
                case ADVERTISE_WEATHER: {
                    if (start) {
                        start = false;
                    } else {
                        holder.txtAdInfo.append(System.lineSeparator());
                    }
                    String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), data);
                    if (g != null) {
                        holder.txtAdInfo.append(App.inst().getString(R.string.weater_today_display));
                        holder.txtAdInfo.append(g);
                    } else {
                        holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_WEATHER.getDescr()));
                    }
                    break;
                }
                case ADVERTISE_WEATHER_DATA: {
                    if (start) {
                        start = false;
                    } else {
                        holder.txtAdInfo.append(System.lineSeparator());
                    }
                    String g = getGattData(Constants.WEATHER.GATT_WEATHER_TODAY.getUuid(), data);
                    if (g != null) {
                        holder.txtAdInfo.append(App.inst().getString(R.string.weater_today_display));
                        holder.txtAdInfo.append(g);
                    } else {
                        holder.txtAdInfo.append(App.inst().getString(R.string.weather_curr_temp));
                        holder.txtAdInfo.append(new DecimalFormat("#.#").format(ParserData.parseByteToFloat(advertEntry.getValue())));
                        holder.txtAdInfo.append(App.inst().getString(R.string.Celcius));
                    }
                    break;
                }
                case ADVERTISE_TRANSP: {
                    if (start) {
                        start = false;
                    } else {
                        holder.txtAdInfo.append(System.lineSeparator());
                    }
                    holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_TRANSP.getDescr()));
                    break;
                }
                case ADVERTISE_SHOUT: {
                    if (start) {
                        start = false;
                    } else {
                        holder.txtAdInfo.append(System.lineSeparator());
                    }
                    String g = getGattData(Constants.SHOUT.GATT_SHOUT.getUuid(), data);
                    if (g != null) {
                        holder.txtAdInfo.append(App.inst().getString(R.string.shout_newest));
                        holder.txtAdInfo.append(g);
                    } else {
                        holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_SHOUT.getDescr()));
                    }
                    break;
                }
                case ADVERTISE_NEWS: {
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
                    break;
                }
                case ADVERTISE_NEWS_DATA: {
                    if (start) {
                        start = false;
                    } else {
                        holder.txtAdInfo.append(System.lineSeparator());
                    }
                    String g = getGattData(Constants.NEWS.GATT_NEWS.getUuid(), data);
                    if (g != null) {
                        holder.txtAdInfo.append(g);
                    } else {
                        holder.txtAdInfo.append(App.inst().getString(R.string.news_amount));
                        holder.txtAdInfo.append(String.valueOf(advertEntry.getValue()));
                    }
                    break;
                }
                case ADVERTISE_BOOKING: {
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
                    break;
                }
                case ADVERTISE_CHAT: {
                    if (start) {
                        start = false;
                    } else {
                        holder.txtAdInfo.append(System.lineSeparator());
                    }
                    String g = getGattData(Constants.CHAT.GATT_CHAT_WRITE.getUuid(), data);
                    if (g != null) {
                        holder.txtAdInfo.append(g);
                    } else {
                        holder.txtAdInfo.append(App.string(Constants.AdvertiseConst.ADVERTISE_CHAT.getDescr()));
                    }
                    break;
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