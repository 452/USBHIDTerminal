package com.appspot.usbhidterminal.tests;

import static org.junit.Assert.assertEquals;

import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.appspot.usbhidterminal.USBHIDTerminal;

import org.junit.Rule;
import org.junit.Test;

public class UITest {

	@Rule
	public ActivityScenarioRule<USBHIDTerminal> rule = new ActivityScenarioRule<>(USBHIDTerminal.class);

	@Test
	public void test() {
		assertEquals(Lifecycle.State.RESUMED, rule.getScenario().getState());
	}

}
