package de.stuttgart.uni.vis.access.common;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * @author Alexander Dridiger
 */
public class DialogCreator {

    public static void createDialogAlert(Context context, int title, int mssg, DialogInterface.OnDismissListener lDiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(title));
        builder.setMessage(context.getString(mssg));
        if (lDiss != null) {
            builder.setOnDismissListener(lDiss);
        }
        builder.show();
    }

    public static void createDialogAlert(Context context, int title, int mssg, int bttnPos, DialogInterface.OnClickListener lPos,
                                         DialogInterface.OnDismissListener lDiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(title));
        builder.setMessage(context.getString(mssg));
        builder.setPositiveButton(bttnPos, lPos);
        if (lDiss != null) {
            builder.setOnDismissListener(lDiss);
        }
        builder.show();
    }

    public static void createDialogAlert(Context context, int title, int mssg, int bttnPos, DialogInterface.OnClickListener lPos,
                                         int bttnNeg, DialogInterface.OnClickListener lNeg, DialogInterface.OnDismissListener lDiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(title));
        builder.setMessage(context.getString(mssg));
        builder.setPositiveButton(bttnPos, lPos);
        builder.setNegativeButton(bttnNeg, lNeg);
        if (lDiss != null) {
            builder.setOnDismissListener(lDiss);
        }
        builder.show();
    }
}
