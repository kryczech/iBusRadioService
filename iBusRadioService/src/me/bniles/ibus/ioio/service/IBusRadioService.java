/* This file is part of iBusRadioService.

    iBusRadioService is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    iBusRadioService is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with iBusRadioService.  If not, see <http://www.gnu.org/licenses/>.
    
*/

package me.bniles.ibus.ioio.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;


/**
 * An example IOIO service. While this service is alive, it will attempt to
 * connect to a IOIO and blink the LED. A notification will appear on the
 * notification bar, enabling the user to stop the service.
 */
public class IBusRadioService extends IOIOService {
	
	private MessageQueues messageQueues;
	
	private Uart uart_;
	private Uart arduinoUart;
	private InputStream in;
	private OutputStream out;
	
	private int messagelen;
	private byte[] tempreadbyte;
	
	private BroadcastReceiver outputMessageReceiver;
	
	// readBuffer holds bytes read. 
	// the 2nd incoming byte indicates the length of the message
	// once that length is read, check the xor checksum to make
	// sure message is valid.
	private ArrayList<Byte> readBuffer;
	
	// lastReadMillis is set at each read. Used as timeout timer.
	// lastSentMillis is set at the end of each sent message. Used to throttle sends.
	private Calendar t;
	private long lastReadMillis;
	private long lastSendMillis;
	// private boolean serialSendFailed;
	
	private DigitalOutput led_;
	
	private static final String TAG = IBusRadioService.class.getSimpleName();
	
	private Context context;
	
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new BaseIOIOLooper() {
			// private DigitalOutput led_;

			@Override
			protected void setup() throws ConnectionLostException, InterruptedException {
				led_ = ioio_.openDigitalOutput(IOIO.LED_PIN);
				uart_ = ioio_.openUart(11, IOIO.INVALID_PIN, 9600, Uart.Parity.EVEN, Uart.StopBits.ONE);
				arduinoUart = ioio_.openUart(IOIO.INVALID_PIN, 12, 9600, Uart.Parity.NONE, Uart.StopBits.ONE);
				in = uart_.getInputStream();
				out = arduinoUart.getOutputStream();
				readBuffer = new ArrayList<Byte>();
				messagelen = 0;
				t = Calendar.getInstance();
				lastReadMillis = t.getTimeInMillis();
				tempreadbyte = new byte[1];
				// messageQueues = new MessageQueues();
				led_.write(true);
			}

			@Override
			public void loop() throws ConnectionLostException, InterruptedException {
				
				// If the whole message isn't read, and more than 15ms (tweak value), 
				// then timeout messages - dump readBuffer
				t = Calendar.getInstance();
				if ((t.getTimeInMillis() - lastReadMillis) > 25) {
					readBuffer.clear();
				}
				
				

				// Handle incoming serial data. Read incoming bytes into arraylist readBuffer.
				// this skips if there is no serial data to read
				try {
				if (in.available() > 0) {
					// Log.i(TAG, "Input bytes available. Not sending.");
					led_.write(false);
					
					// read serial byte and add it to readBuffer
					// also set lastReadMillis to current time
					tempreadbyte[0] = (byte) in.read();
					readBuffer.add(tempreadbyte[0]);
					t = Calendar.getInstance();
					lastReadMillis = t.getTimeInMillis();
					
					// If this is the first byte in readBuffer, then we don't know how long 
					// the message will be, so set the messagelen to something large.
					// The second byte of the message indicates how many more bytes to read.
					// So if this is the second byte, set messagelen to that value.
					if (readBuffer.size() == 1) {
						messagelen = 256;
					} else if (readBuffer.size() == 2) {
						messagelen = (int) readBuffer.get(1);
					}
					
					// keep reading bytes into readbuffer until it contains messagelen + 2 bytes.
					// then the message is complete.
					if (readBuffer.size() == messagelen + 2) {
						byte[] readBufferBytes = new byte[readBuffer.size()];
						for (int i = 0; i < readBuffer.size(); i++) {
							readBufferBytes[i] = readBuffer.get(i);
						}
						
						// messageOK performs checksum on the message in readBuffer.
						// if checksum returns OK, then add message to the input messageQueue for 
						// handleInputMessage to take care of. Also clear readbuffer to start over. 
						if (messageOK(readBufferBytes)) {
							// messageQueues.addInputMessage(readBufferBytes);
							broadcastInputMessage(readBufferBytes);
						}
						readBuffer.clear();
					}
				} else {
					led_.write(true);
					// This is block handles output data. Right now it just grabs output
					// messages if they're available and writes them out over uart outputstream.
					// might add some echo testing to make sure messages are sent OK - the 
					// next message received after a send should equal the sent message.
					if (!messageQueues.isOutputQueueEmpty()) {
						// Log.i(TAG, "Processing output queue");
						t = Calendar.getInstance();
						// check current time agains last send. Wait at least 10ms between sends. Just keep tweaking this.
						if ((t.getTimeInMillis() - lastSendMillis) > 30) {
							// byte[] outputBytes = messageQueues.getNextOutputMessage();
							int outputType = messageQueues.getNextOutputMessage();
							out.write((byte) outputType & 0xff);
							Log.i(TAG, "Sent message: " + outputType);
							// I have to do this masking because I need to send 0 to 255 and 
							// the byte type in java is signed. So I'm casting int to byte to store 
							// in the byte array and then doing the & 0xff to convert back.
							// sendMessage(outputType);
						}
					}
				}
				} catch (IOException e) {
					// do anything?
				}

			} // End of loop() method
		};
	}
	
	private boolean messageOK(byte[] msg) {
		byte cksum = 0x00;
		for (int i = 0; i < msg.length; i++){
			cksum = (byte) (cksum ^ msg[i]);
		}
		// Log.i(TAG, "message checksum: " + cksum);
		if (cksum == 0x00) {
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		context = this;

		final IntentFilter theFilter = new IntentFilter();
		theFilter.addAction("me.bniles.ibus.addOutputMessage");
		// theFilter.setPriority(1);

		messageQueues = new MessageQueues();
		outputMessageReceiver = new OutputMessageReceiver(messageQueues);

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (intent != null && intent.getAction() != null && intent.getAction().equals("stop")) {
			// User clicked the notification. Need to stop the service.
			// unregisterReceiver(outputMessageReceiver);
			nm.cancel(0);
			stopSelf();
		} else {
			registerReceiver(outputMessageReceiver, theFilter);
			// Service starting. Create a notification.
			Notification notification = new Notification(R.drawable.ic_launcher, "IOIO service running", System.currentTimeMillis());
			notification.setLatestEventInfo(this, "IOIO Service", "Click to stop", PendingIntent.getService(this, 0, new Intent("stop", null, this, this.getClass()), 0));
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			nm.notify(0, notification);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private void broadcastInputMessage(byte[] inputMsgBytes)
	{
		Intent intent = new Intent();
		
		// Check to see if message is incoming phone call phone number
		if (inputMsgBytes[0] == (byte)0xC8 && inputMsgBytes[2] == 0x3B && inputMsgBytes[3] == 0x23 && inputMsgBytes[4] == (byte)0x82) {
			intent.setComponent(new ComponentName("me.bniles.ibus.phone.incoming", "me.bniles.ibus.phone.incoming.MainActivity"));
			intent.putExtra("MessageData", inputMsgBytes);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} else {

			intent.setAction("me.bniles.ibus.inputMessageBroadcast");
			intent.putExtra("MessageData", inputMsgBytes);
			// Log.i(TAG, "iBusRadioService: Sending intent.");
			sendOrderedBroadcast(intent, null);
		}
	}
}
