package com.appspot.usbhidterminal.tests;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.appspot.usbhidterminal.USBHIDTerminal;
import com.appspot.usbhidterminal.core.services.SocketService;

public class SocketServiceTest extends ActivityInstrumentationTestCase2<USBHIDTerminal> {

	public SocketServiceTest(){
		super(USBHIDTerminal.class);
	}

	private Intent socketService;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("Start tst");
		socketService = new Intent(super.getActivity(), SocketService.class);
	}

	public void test() {
		socketService.setAction("ABCD");
		super.getActivity().startService(socketService);
		for (int i = 0; i < 1; i++) {
			//send("ABCD " + i);
		}
		assertNotNull("Socket service is null", socketService);
	}

	private void send(String data){
		socketService.setAction(data);
		super.getActivity().startService(socketService);
	}

}
