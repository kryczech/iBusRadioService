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

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

public class OutputMessageReceiver extends BroadcastReceiver {
	
	private MessageQueues messageQueues;
	private int receiverTempType;
	
	public OutputMessageReceiver(MessageQueues mqs) {
		this.messageQueues = mqs;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		receiverTempType = intent.getIntExtra("iBusMessageType", 0);
		messageQueues.addOutputMessage(receiverTempType);
	}
}
