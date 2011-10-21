package interdroid.lifediary;

import interdroid.contextdroid.ConnectionListener;
import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import interdroid.contextdroid.contextexpressions.TypedValue.HistoryReductionMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

public class LifeDiaryService extends Service {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(LifeDiaryService.class);

	private static final String	LIFE_DIARY_EXPRESSION	= "lifediary/";

	private SharedPreferences mPrefs;
	private SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefsChangeListener;

	private ContextManager	mContextManager;

	private LooperThread	mLooperThread;

	@Override
	public void onCreate() {
		LOG.debug("Service Started.");

		// Grab the notifications manager we will use to interact with the user
		//		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mPrefs = getSharedPreferences(LifeDiaryPreferences.PREFERENCES_NAME, MODE_WORLD_READABLE);

		mSharedPrefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				mPrefs = sharedPreferences;
				LOG.debug("Shared Preference Changed: {}", key);
				togglePreference(key);
			}

		};
		mPrefs.registerOnSharedPreferenceChangeListener(mSharedPrefsChangeListener);

		mContextManager = new ContextManager(this);
		mContextManager.start(getConnectionListener());

		runInitializer();

		super.onCreate();
	}

	class LooperThread extends Thread {
		public Handler mHandler;

		public void run() {
			Looper.prepare();

			mHandler = new Handler() {
				public void handleMessage(Message msg) {
					String key = (String) msg.obj;
					LOG.debug("Handling toggle of: {}", key);
					if (mPrefs.contains(key) && mPrefs.getBoolean(key, false)) {
						registerListener(key);
					} else {
						unregisterListener(key);
					}
				}
			};

			LOG.debug("Handler thread waiting for connection.");
			waitForConnection();
			LOG.debug("Handler Connected: {} Registering Expressions.",
					mContextManager.isConnected());

			registerSensors();

			Looper.loop();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LOG.debug("onStart: {} {}", intent, flags);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	private void waitForConnection() {
		LOG.debug("Waiting for connection.");
		while (!mContextManager.isConnected()) {
			synchronized (mPrefs) {
				try {
					mPrefs.wait();
				} catch (InterruptedException e) {
					LOG.warn("Interrupted waiting on ContextManager");
				}
			}
		}
	}

	private void runInitializer() {
		mLooperThread = new LooperThread();
		mLooperThread.start();
	}

	@Override
	public void onDestroy() {
		try {
			mContextManager.destroy();
		} catch (ContextDroidException e) {
			LOG.warn("Error destroying context manager.", e);
		}
		super.onDestroy();
	}


	private ConnectionListener getConnectionListener() {
		return new ConnectionListener() {

			@Override
			public void onConnected() {
				LOG.debug("Got connection notification.");
				synchronized (mPrefs) {
					mPrefs.notifyAll();
				}
			}

			@Override
			public void onDisconnected() {
				LOG.debug("Got disconnection notification.");
				// Try to reconnect.
				mContextManager.start(getConnectionListener());
			}
		};
	}


	private void registerSensors() {
		for (int i = 0; i < LifeDiaryPreferences.serviceKeys.length; i++) {
			togglePreference(LifeDiaryPreferences.serviceKeys[i]);
		}
	}

	private void togglePreference(String key) {
		Message message = new Message();
		message.obj = key;
		mLooperThread.mHandler.dispatchMessage(message);
	}

	private void unregisterListener(String key) {
		LOG.debug("Unregistering: {}", key);
		try {
			mContextManager
			.unregisterContextTypedValue(LIFE_DIARY_EXPRESSION + key);
		} catch (ContextDroidException e) {
			LOG.warn("Error unregistering.", e);
			mContextManager.start(getConnectionListener());
		}
	}

	private void registerListener(String key) {
		LOG.debug("Register: {}", key);

		final ContextTypedValue left = new ContextTypedValue(key,
				HistoryReductionMode.NONE, 500);

		System.out.println("registering expression");
		try {
			mContextManager.registerContextTypedValue(LIFE_DIARY_EXPRESSION + key,
					left, null);
		} catch (ContextDroidException e) {
			LOG.warn("Error registering", e);
			mContextManager.start(getConnectionListener());
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
