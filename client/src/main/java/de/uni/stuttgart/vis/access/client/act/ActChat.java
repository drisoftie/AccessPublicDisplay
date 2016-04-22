package de.uni.stuttgart.vis.access.client.act;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.stuttgart.uni.vis.access.common.Constants;
import de.uni.stuttgart.vis.access.client.R;
import de.uni.stuttgart.vis.access.client.data.ChatMessage;
import de.uni.stuttgart.vis.access.client.helper.PosterUi;
import de.uni.stuttgart.vis.access.client.service.bl.IConnGattProvider;
import de.uni.stuttgart.vis.access.client.view.AdaptChat;

public class ActChat extends ActGattScan {

    private GattChat          gattListenChat;
    private IConnGattProvider gattProviderChat;
    /**
     * Array adapter for the conversation thread
     */
    private AdaptChat         mConversationArrayAdapter;
    // Layout Views
    private ListView          mConversationView;
    private EditText          mOutEditText;
    private Button            mSendButton;
    /**
     * String buffer for outgoing messages
     */
    private StringBuffer      mOutStringBuffer;
    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio  = Math.min(maxImageSize / realImage.getWidth(), maxImageSize / realImage.getHeight());
        int   width  = Math.round(ratio * realImage.getWidth());
        int   height = Math.round(ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width, height, filter);
        return newBitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //        fab.setOnClickListener(new View.OnClickListener() {
        //            @Override
        //            public void onClick(View view) {
        //                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        //            }
        //        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //        findViewById(R.id.txt_booking).setOnClickListener(new View.OnClickListener() {
        //            @Override
        //            public void onClick(View v) {
        //                gattProviderChat.writeGattCharacteristic(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
        //                                                            Constants.BOOKING.GATT_BOOKING_WRITE.getUuid(),
        //                                                            ConstantsBooking.StateBooking.START.getState().getBytes());
        //            }
        //        });

        // Initialize the array adapter for the conversation thread

        mConversationView = (ListView) findViewById(R.id.my_recycler_view);
        mOutEditText = (EditText) findViewById(R.id.edttxt_chat);
        mSendButton = (Button) findViewById(R.id.btn_chat);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        mConversationArrayAdapter = new AdaptChat(this, R.layout.litem_message, new ArrayList<ChatMessage>());

        mConversationView.setAdapter(mConversationArrayAdapter);
        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView textView = (TextView) findViewById(R.id.edttxt_chat);
                String   message  = textView.getText().toString();
                sendMessage(message);
            }
        });

        findViewById(R.id.btn_chat_pic).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, 10);
                }
            }
        });


        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        //        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
        //            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
        //            return;
        //        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            Long   tsLong = System.currentTimeMillis() / 1000;
            String ts     = tsLong.toString();
            String msg    = ts + ":" + message;
            byte[] send   = msg.getBytes();
            if (gattProviderChat != null) {
                gattProviderChat.writeGattCharacteristic(Constants.CHAT.GATT_SERVICE_CHAT.getUuid(),
                                                         Constants.CHAT.GATT_CHAT_WRITE.getUuid(), send);
            }
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        //        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
        //            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
        //            return;
        //        }

        // Check that there's actually something to send
        if (message.length > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            Long   tsLong = System.currentTimeMillis() / 1000;
            String ts     = tsLong.toString();
            String msg    = ts + ":" + message;
            byte[] send   = msg.getBytes();
            if (gattProviderChat != null) {
                gattProviderChat.writeGattCharacteristic(Constants.CHAT.GATT_SERVICE_CHAT.getUuid(),
                                                         Constants.CHAT.GATT_CHAT_WRITE.getUuid(), send);
            }
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK) {
            Bundle extras      = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Bitmap scale       = scaleDown(imageBitmap, 10, true);
            int    bytes       = scale.getByteCount();
            //or we can calculate bytes this way. Use a different value than 4 if you don't use 32bit images.
            //int bytes = b.getWidth()*b.getHeight()*4;

            ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
            scale.copyPixelsToBuffer(buffer); //Move the byte data to the buffer

            byte[] array = buffer.array(); //Get the underlying array containing the data.
            sendMessage(array);
        }
    }

    @Override
    protected void onResuming() {

    }

    @Override
    protected void onPausing() {

    }

    @Override
    void deregisterGattComponents() {
        gattProviderChat.deregisterConnGattSub(gattListenChat);
    }

    @Override
    public void onScanResultReceived(ScanResult result) {

    }

    @Override
    public void onScanResultsReceived(List<ScanResult> results) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_act_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, 10);
                }
                break;
        }
        return true;
    }

    @Override
    public void onRefreshedScanReceived(ScanResult result) {

    }

    @Override
    public void onRefreshedScansReceived(List<ScanResult> results) {

    }

    @Override
    public void onScanLost(ScanResult lostResult) {

    }

    @Override
    public void onScanFailed(int errorCode) {

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        super.onServiceConnected(name, binder);
        if (gattListenChat == null) {
            gattProviderChat = service.subscribeGattConnection(Constants.CHAT.GATT_SERVICE_CHAT.getUuid(), gattListenChat = new GattChat());
        }
        service.getTtsProvider().provideTts().queueRead(getString(R.string.chat_with_people));
    }

    private class GattChat extends GattSub {

        @Override
        public void onServicesReady(String macAddress) {
            gattProviderChat.registerConnGattSub(gattListenChat);
        }

        @Override
        public void onGattValueReceived(String macAddress, UUID uuid, byte[] value) {
            super.onGattValueReceived(macAddress, uuid, value);
        }

        @Override
        public void onGattValueWriteReceived(String macAddress, UUID uuid, byte[] value) {
            if (Constants.CHAT.GATT_CHAT_WRITE.getUuid().equals(uuid)) {
                //                updateHolderData(macAddress, uuid, value);
                //                gattProviderChat.getGattCharacteristicRead(Constants.BOOKING.GATT_SERVICE_BOOKING.getUuid(),
                //                                                              Constants.BOOKING.GATT_BOOKING_WRITE.getUuid());
            }
        }

        @Override
        public void onGattValueChanged(String macAddress, UUID uuid, byte[] value) {
            if (Constants.CHAT.GATT_CHAT_NOTIFY.getUuid().equals(uuid)) {
                service.getTtsProvider().provideTts().queueRead(new String(value));

                String      strVal  = new String(value);
                int         index   = StringUtils.indexOf(strVal, ":") + 1;
                String      message = StringUtils.substring(strVal, index);
                ChatMessage newMsg  = new ChatMessage();
                if (value.length > 200) {
                    byte[] picData = Arrays.copyOfRange(value, index, value.length);
                    newMsg.pic = BitmapFactory.decodeByteArray(picData, 0, picData.length);
                } else {
                    newMsg.message = message;
                }
                PosterUi.postOnUiThread(new Runnable() {
                    public ChatMessage newMsg;

                    public Runnable init(ChatMessage value) {
                        this.newMsg = value;
                        return this;
                    }

                    @Override
                    public void run() {
                        mConversationArrayAdapter.add(this.newMsg);
                    }
                }.init(newMsg));
            }
        }
    }
}