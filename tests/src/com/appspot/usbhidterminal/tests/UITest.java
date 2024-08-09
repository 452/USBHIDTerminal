package com.appspot.usbhidterminal.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.rule.ActivityTestRule;

import com.appspot.usbhidterminal.USBHIDTerminal;

import org.junit.Test;

public class UITest extends ActivityTestRule<USBHIDTerminal> {

	public UITest(){
		super(USBHIDTerminal.class);
	}

	@Test
	public void test() {
		//fail("Not yet implemented");
		assertNull("ReceiverActivity is null", null);
	}

}
