package de.uni.stuttgart.vis.access.client.helper;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.NonNull;

/**
 * @author Alexander Dridiger
 */
public class SoundPlayer {

    public static void playExampleBeep(@NonNull final double freqOfTone, @NonNull final int duration, final int hard) {
        final int    sampleRate     = 8000;
        final int    numSamples     = duration * sampleRate;
        final double sample[]       = new double[numSamples];
        final byte   generatedSnd[] = new byte[2 * numSamples];

        final Thread thread = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < numSamples; ++i) {
                    if (hard == 0) {
                        sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
                    } else {
                        if ((i / (sampleRate / freqOfTone) % 2) == 1) {
                            sample[i] = 1;
                        } else {
                            sample[i] = -1;
                        }
                    }
                }

                // convert to 16 bit pcm sound array
                // assumes the sample buffer is normalised.
                int idx = 0;
                for (final double dVal : sample) {
                    // scale to maximum amplitude
                    final short val = (short) ((dVal * 32767));
                    // in 16 bit wav PCM, first byte is the low order byte
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

                }
                PosterUi.postOnUiThread(new Runnable() {
                    public void run() {
                        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                                                                     AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                                                                     AudioTrack.MODE_STATIC);
                        audioTrack.write(generatedSnd, 0, generatedSnd.length);
                        audioTrack.play();
                    }
                });
            }
        });
        thread.start();


    }
}
