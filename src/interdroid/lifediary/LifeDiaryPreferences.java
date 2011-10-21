package interdroid.lifediary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class LifeDiaryPreferences extends PreferenceActivity {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(LifeDiaryPreferences.class);

	private static final String	ACTIVITY_CONTEXT_SERVICE	=
			"interdroid.lifediary.CONTEXT_SERVICE";

	public static final String PREFERENCES_NAME =
			"interdroid.lifediary_preferences";

	public static final String[] serviceKeys = {
		"battery/level", "bluetooth/name", "call/call_state",
		"location/speed", "screen/is_screen_on", "signal/gsm_signal_strength",
		"wifi/ssid"
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.context_preferences);
		LOG.debug("Storing preferences to: " +
				getPreferenceManager().getSharedPreferencesName());
		setResult(RESULT_OK);
		LOG.debug("Starting service.");
		Intent intent = new Intent(ACTIVITY_CONTEXT_SERVICE);
		startService(intent);
	}
}
