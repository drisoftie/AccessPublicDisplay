package de.uni.stuttgart.vis.access.client.helper;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Dridiger
 */
public class TtsWrapper implements TextToSpeech.OnInitListener {

    private boolean init   = false;
    private boolean access = false;
    private TextToSpeech tts;

    private List<String> queue = new ArrayList<>();

    public TtsWrapper(Context c) {
        if (checkAccessibility(c)) {
            tts = new TextToSpeech(c, this);
            access = true;
        }
    }

    private boolean checkAccessibility(Context c) {
        AccessibilityManager am = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am.isEnabled() | am.isTouchExplorationEnabled();
    }

    @Override
    public void onInit(int status) {
        init = true;
        if (!queue.isEmpty()) {
            readOut(queue);
        }
    }


    public void queueRead(String read) {
        if (access) {
            if (init) {
                readOut(read);
            } else {
                queue.add(read);
            }
        }
    }

    public void queueRead(String... read) {
        if (access) {
            for (String r : read) {
                readOut(r);
            }
        }
    }

    private void readOut(List<String> read) {
        if (access) {
            for (String r : read) {
                readOut(r);
            }
        }
    }

    private void readOut(String read) {
        if (access) {
            tts.speak(read, TextToSpeech.QUEUE_ADD, null, read);
        }
    }

    public void shutDown() {
        if (access) {
            tts.shutdown();
        }
    }
}
