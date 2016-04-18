package de.uni.stuttgart.vis.access.client.helper;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Vibrator;
import android.widget.Toast;

import de.uni.stuttgart.vis.access.client.App;

/**
 * Created by florian on 23.10.15.
 */
public class VibratorBuilder {
    public static  int             SHORT_ONCE       = 0;
    public static  int             LONG_ONCE        = 1;
    public static  int             SHORT_SHORT      = 2;
    public static  int             LONG_LONG        = 3;
    public static  int             LONG_SHORT       = 4;
    public static  int             SHORT_LONG_SHORT = 5;
    public static  int             SHORT_LONG       = 6;
    private static VibratorBuilder instance         = null;
    private static long[]          short_once       = {0, 150};
    private static long[]          long_once        = {0, 300};
    private static long[]          short_short      = {0, 150, 0, 150};
    private static long[]          long_long        = {0, 300, 0, 300};
    private static long[]          long_short       = {0, 300, 0, 150};
    private static long[]          short_long_short = {0, 150, 0, 300, 0, 150};
    private static long[]          short_long       = {0, 150, 0, 300};
    private static long[][]        patternArray     =
            {short_once, long_once, short_short, long_long, long_short, short_long_short, short_long};
    private static Vibrator        vibrator;
    private static AudioAttributes audioAttributes;

    public VibratorBuilder(Context context) {
        vibrator = (Vibrator) context.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= 21) {
            audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(
                    AudioAttributes.CONTENT_TYPE_SPEECH).build();
        }
        if (!vibrator.hasVibrator()) {
            Toast.makeText(context.getApplicationContext(), "vibration function is disabled.", Toast.LENGTH_SHORT).show();
        }
    }

    public static VibratorBuilder getInstance(Context context) {
        if (instance == null) {
            instance = new VibratorBuilder(context);
        }
        return instance;
    }

    /**
     * the vibrate with certain pattern
     *
     * @param patternCode <ul>
     *                    <li>0 for short once pattern</li>
     *                    <li>1 for long once pattern</li>
     *                    <li>2 for short twice pattern</li>
     *                    </ul>
     */
    public static void vibrate(int patternCode) {
        vibrateFunc(patternArray[patternCode]);
    }

    private static void vibrateFunc(long[] pattern) {
        if (vibrator == null) {
            vibrator = getVibrator();
            if (vibrator == null) {
                return;
            }
        }
        if (audioAttributes == null) {
            audioAttributes = getAudioAttr();
        }
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 21) {
                vibrator.vibrate(pattern, -1, audioAttributes);
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    public static Vibrator getVibrator() {
        Vibrator vibrator = (Vibrator) App.inst().getSystemService(Context.VIBRATOR_SERVICE);

        if (!vibrator.hasVibrator()) {
//            Toast.makeText(App.inst(), "vibration function is disabled.", Toast.LENGTH_SHORT).show();
        }
        return vibrator;
    }


    public static AudioAttributes getAudioAttr() {
        if (Build.VERSION.SDK_INT >= 21) {
            audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(
                    AudioAttributes.CONTENT_TYPE_SPEECH).build();
        }
        return audioAttributes;
    }
}
