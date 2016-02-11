package de.uni.stuttgart.vis.access.client.act;

import android.view.View;
import android.widget.TextView;

import com.drisoftie.action.async.android.AndroidAction;

import org.apache.commons.lang3.ArrayUtils;

import de.uni.stuttgart.vis.access.client.R;

/**
 * @author Alexander Dridiger
 */
public class ActionSetText extends AndroidAction<View, Void, Void, Void, Void> {

    private TextView txt;

    public ActionSetText(TextView view, Class<?> actionType, String regMethodName) {
        super(view, actionType, regMethodName);
        this.txt = view;
    }

    public ActionSetText(TextView view, Class<?>[] actionTypes, String regMethodName) {
        super(view, actionTypes, regMethodName);
        this.txt = view;
    }

    @Override
    public Object onActionPrepare(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
        return null;
    }

    @Override
    public Void onActionDoWork(String methodName, Object[] methodArgs, Void tag1, Void tag2, Object[] additionalTags) {
        return null;
    }

    @Override
    public void onActionAfterWork(String methodName, Object[] methodArgs, Void workResult, Void tag1, Void tag2, Object[] additionalTags) {
        Object[] args = stripMethodArgs(methodArgs);
        if (ArrayUtils.isNotEmpty(args)) {
            txt.setText((String) args[0]);
            if (R.id.txt_weather_dat == txt.getId()) {
                ((TextView) txt.getRootView().findViewById(R.id.txt_weather_more_info)).setText(R.string.txt_weather_more_info);
            }
        }
    }
}
