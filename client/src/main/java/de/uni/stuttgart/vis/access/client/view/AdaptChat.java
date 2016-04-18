package de.uni.stuttgart.vis.access.client.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.data.ChatMessage;

/**
 * @author Alexander Dridiger
 */
public class AdaptChat extends ArrayAdapter<ChatMessage> {

    public AdaptChat(Context context, int resource, List<ChatMessage> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage msg = getItem(position);
        ViewHolder  holder;
        if (convertView == null) {
            View viewItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.litem_message, null);
            holder = new ViewHolder();
            holder.itemIcon = (ImageView) viewItem.findViewById(R.id.img_chat);
            holder.text = (TextView) viewItem.findViewById(R.id.txt_chat);
            convertView = viewItem;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (msg.pic != null) {
            holder.itemIcon.setImageBitmap(msg.pic);
        } else {
            holder.itemIcon.setImageBitmap(null);
        }

        holder.text.setText(msg.message);
        return convertView;
    }

    private class ViewHolder {
        ImageView itemIcon;
        TextView  text;
    }
}
