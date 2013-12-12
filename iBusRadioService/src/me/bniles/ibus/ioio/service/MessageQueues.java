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

import java.util.LinkedList;

public class MessageQueues {
	
	private LinkedList<byte[]> inputMessages = new LinkedList<byte[]>();
	// private LinkedList<byte[]> outputMessages = new LinkedList<byte[]>();
	private LinkedList<Integer> outputMessages = new LinkedList<Integer>();
	
	public synchronized boolean isInputQueueEmpty() {
		return inputMessages.isEmpty();
	}
	
	public synchronized boolean isOutputQueueEmpty() {
		return outputMessages.isEmpty();
	}

	public synchronized byte[] getNextInputMessage() {
		return inputMessages.poll();
	}
	
	public synchronized int getNextOutputMessage() {
		return outputMessages.poll();
	}
	
	public synchronized void addInputMessage(byte[] inputMessage) {
		inputMessages.add(inputMessage);
	}
	
	public synchronized void addOutputMessage(int outputMessage) {
		outputMessages.add(outputMessage);
	}

}
