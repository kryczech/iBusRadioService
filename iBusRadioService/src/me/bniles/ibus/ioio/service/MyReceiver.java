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

import java.util.Arrays;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
// import android.util.Log;
import android.view.KeyEvent;

public class MyReceiver extends BroadcastReceiver {
	
	// private static final String TAG = MyReceiver.class.getSimpleName();
	private byte[] messageData;
	private static final byte[] trackUp = new byte[] {0x50, 0x04, 0x68, 0x3B, 0x01, 0x06};
	private static final byte[] trackDown = new byte[] {0x50, 0x04, 0x68, 0x3B, 0x08, 0x0F};

	@Override
	public void onReceive(Context context, Intent intent) {

		messageData = intent.getByteArrayExtra("MessageData");
		if (Arrays.equals(messageData, trackUp)) {
			sendMediaEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT);
		} else if (Arrays.equals(messageData, trackDown)) {
			sendMediaEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
		}
	}
	
	private void sendMediaEvent(Context ctx, int keycode) {
		long eventtime = SystemClock.uptimeMillis();
		
		Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); 
		KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, keycode, 0); 
		downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent); 
		ctx.sendOrderedBroadcast(downIntent, null); 

		Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); 
		KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, keycode, 0); 
		upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent); 
		ctx.sendOrderedBroadcast(upIntent, null);
	}

}
