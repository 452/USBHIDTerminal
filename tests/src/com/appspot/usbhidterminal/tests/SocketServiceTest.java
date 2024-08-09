package com.appspot.usbhidterminal.tests;

import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.rule.ActivityTestRule;

import com.appspot.usbhidterminal.USBHIDTerminal;
import com.appspot.usbhidterminal.core.services.SocketService;

import org.junit.Test;

public class SocketServiceTest extends ActivityTestRule<USBHIDTerminal> {

	public SocketServiceTest(){
		super(USBHIDTerminal.class);
	}

	@Test
	public void test() {
		System.out.println("Start tst");
		Intent socketService = new Intent(super.getActivity(), SocketService.class);
		socketService.setAction("ABCD");
		super.getActivity().startService(socketService);
		for (int i = 0; i < 1; i++) {
			//send("ABCD " + i);
		}
		assertNotNull("Socket service is null", socketService);
	}

	private void send(String data){
		Intent socketService = new Intent(super.getActivity(), SocketService.class);
		socketService.setAction(data);
		super.getActivity().startService(socketService);
	}

}
