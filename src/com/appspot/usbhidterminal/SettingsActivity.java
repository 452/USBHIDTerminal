package com.appspot.usbhidterminal;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    public static final String WEB_SERVER_ENABLED = "web_server_enabled";
    public static final String WEB_SERVER_PORT = "web_server_port";
    public static final String SOCKET_SERVER_ENABLED = "socket_server_enabled";
    public static final String SOCKET_SERVER_PORT = "socket_server_port";
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        private static final InputFilter[] PORT_LENGTH_FILTER = new InputFilter[]{new InputFilter.LengthFilter(5)};

        private EditTextPreference webPort;
        private EditTextPreference socketPort;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());

            SwitchPreferenceCompat webEnabled = buildSwitch(WEB_SERVER_ENABLED, R.string.pref_title_web_server);
            webPort = buildPortPreference(WEB_SERVER_PORT, R.string.pref_title_web_server_port);

            SwitchPreferenceCompat socketEnabled = buildSwitch(SOCKET_SERVER_ENABLED, R.string.pref_title_socket_server);
            socketPort = buildPortPreference(SOCKET_SERVER_PORT, R.string.pref_title_socket_server_port);

            screen.addPreference(webEnabled);
            screen.addPreference(webPort);
            screen.addPreference(socketEnabled);
            screen.addPreference(socketPort);
            setPreferenceScreen(screen);

            if (!sp().contains(WEB_SERVER_PORT)) {
                webPort.setText(getString(R.string.pref_default_web_server_port));
            }
            if (!sp().contains(SOCKET_SERVER_PORT)) {
                socketPort.setText(getString(R.string.pref_default_socket_server_port));
            }

            syncEnabledStates();

            setupEnableToggle(webEnabled, webPort, SOCKET_SERVER_ENABLED, SOCKET_SERVER_PORT);
            setupEnableToggle(socketEnabled, socketPort, WEB_SERVER_ENABLED, WEB_SERVER_PORT);
        }

        private SwitchPreferenceCompat buildSwitch(String key, int titleRes) {
            SwitchPreferenceCompat s = new SwitchPreferenceCompat(requireContext());
            s.setKey(key);
            s.setTitle(titleRes);
            s.setDefaultValue(false);
            s.setIconSpaceReserved(false);
            return s;
        }

        private EditTextPreference buildPortPreference(String key, int titleRes) {
            EditTextPreference p = new EditTextPreference(requireContext());
            p.setKey(key);
            p.setTitle(titleRes);
            p.setDialogTitle(titleRes);
            p.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            p.setIconSpaceReserved(false);
            p.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setKeyListener(DigitsKeyListener.getInstance(false, false));
                editText.setFilters(PORT_LENGTH_FILTER);
                editText.setSelectAllOnFocus(true);
            });
            p.setOnPreferenceChangeListener(this);
            return p;
        }

        private void setupEnableToggle(SwitchPreferenceCompat selfSwitch, EditTextPreference selfPort, String otherEnabledKey, String otherPortKey) {
            selfSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enable = newValue instanceof Boolean && (Boolean) newValue;
                if (enable) {
                    boolean otherOn = sp().getBoolean(otherEnabledKey, false);
                    if (otherOn) {
                        String otherPort = sp().getString(otherPortKey, "");
                        String myPort = sp().getString(selfPort.getKey(), "");
                        if (!otherPort.isEmpty() && otherPort.equals(myPort)) {
                            String newPort = nextDifferentPort(otherPort);
                            selfPort.setText(newPort);
                            Toast.makeText(requireContext(), "Port changed to " + newPort, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                selfPort.setEnabled(enable);
                return true;
            });
        }

        private void syncEnabledStates() {
            boolean webOn = sp().getBoolean(WEB_SERVER_ENABLED, false);
            boolean socketOn = sp().getBoolean(SOCKET_SERVER_ENABLED, false);
            webPort.setEnabled(webOn);
            socketPort.setEnabled(socketOn);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            String stringValue = newValue == null ? "" : newValue.toString();
            if (WEB_SERVER_PORT.equals(key) || SOCKET_SERVER_PORT.equals(key)) {
                if (!isValidPort(stringValue)) {
                    Toast.makeText(requireContext(), "Invalid port. Use 1–65535.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                boolean webOn = sp().getBoolean(WEB_SERVER_ENABLED, false);
                boolean socketOn = sp().getBoolean(SOCKET_SERVER_ENABLED, false);
                if (webOn && socketOn) {
                    String other = WEB_SERVER_PORT.equals(key) ? sp().getString(SOCKET_SERVER_PORT, "") : sp().getString(WEB_SERVER_PORT, "");
                    if (stringValue.equals(other) && !stringValue.isEmpty()) {
                        Toast.makeText(requireContext(), "Ports must be different when both services are enabled.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                return true;
            }
            return true;
        }

        private boolean isValidPort(String value) {
            try {
                if (value.isEmpty()) return false;
                int port = Integer.parseInt(value);
                return port >= MIN_PORT && port <= MAX_PORT;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private String nextDifferentPort(String otherPortStr) {
            int other;
            try {
                other = Integer.parseInt(otherPortStr);
            } catch (NumberFormatException e) {
                other = MIN_PORT;
            }
            int candidate = other >= MAX_PORT ? MIN_PORT : other + 1;
            if (candidate < MIN_PORT) candidate = MIN_PORT;
            if (Integer.toString(candidate).equals(otherPortStr)) candidate = other == MAX_PORT ? MIN_PORT : other + 1;
            return Integer.toString(candidate);
        }

        private SharedPreferences sp() {
            return PreferenceManager.getDefaultSharedPreferences(requireContext());
        }
    }
}
