package com.appspot.usbhidterminal.tests;

import android.test.ActivityInstrumentationTestCase2;

import com.appspot.usbhidterminal.USBHIDTerminal;

public class UITest extends ActivityInstrumentationTestCase2<USBHIDTerminal> {

	public UITest(){
		super(USBHIDTerminal.class);
	}

	//@Test
	public void test() {
		//fail("Not yet implemented");
		assertNotNull("ReceiverActivity is null", null);
	}

}
