package interdroid.lifediary;

import interdroid.lifediary.SensorDataListAdapter.CursorIndex;
import interdroid.util.view.AsyncTaskWithProgressDialog;
import interdroid.vdb.content.EntityUriBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ExpandableListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

public class LifeDiaryDay extends ExpandableListActivity {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(LifeDiaryDay.class);


	/** String for the extra day */
	public static final String	EXTRA_DAY	= "d";
	/** String for the extra month */
	public static final String	EXTRA_MONTH	= "m";
	/** String for the extra year */
	public static final String	EXTRA_YEAR	= "y";

	/**
	 * The list adapter that holds all the data.
	 */
	private SensorDataListAdapter	mListAdapter;

	@Override
	public void onCreate(final Bundle saved) {
		super.onCreate(saved);


		Bundle extras = getIntent().getExtras();
		int day = extras.getInt(EXTRA_DAY);
		int month = extras.getInt(EXTRA_MONTH);
		int year = extras.getInt(EXTRA_YEAR);

		Calendar date = Calendar.getInstance();
		date.set(year, month, day);
		String appName = getString(R.string.app_name);
		setTitle(appName + ": " +
				SimpleDateFormat.getDateInstance()
				.format(date.getTimeInMillis()));

		// Load up the data.
		new InitTask().execute(day, month, year);
	}

	/**
	 * Initialization task which loads the list.
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	private class InitTask
		extends AsyncTaskWithProgressDialog<Integer, String, Void> {

		/**
		 * Construct the dialog.
		 */
		public InitTask() {
			super(LifeDiaryDay.this, getString(R.string.label_loading),
					getString(R.string.label_wait));
		}

		@Override
		protected Void doInBackground(final Integer... params) {
			Integer day = params[0];
			Integer month = params[1];
			Integer year = params[2];

			mListAdapter =
					new SensorDataListAdapter(LifeDiaryDay.this,
							day, month, year);

			return null;
		}

		@Override
		protected void onPostExecute(Void value) {
			setListAdapter(mListAdapter);
			super.onPostExecute(null);
		}

	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View view,
			int groupPosition, int childPosition, long id) {
		LOG.debug("OnChildClick Activity: {} {}", groupPosition, childPosition);

		CursorIndex info = (CursorIndex)
				mListAdapter.getChild(groupPosition, childPosition);
		Uri uri = Uri.withAppendedPath(EntityUriBuilder.nativeUri(
				mListAdapter.getAuthority(info.mSensor), info.mSensor),
				String.valueOf(info.mId));
		Intent editIntent = new Intent(Intent.ACTION_EDIT);
		editIntent.addCategory(Intent.CATEGORY_DEFAULT);
		editIntent.setDataAndType(uri,
				"vnd.interdroid.vdb.content/branch.local");
		try {
			startActivity(editIntent);
		} catch (ActivityNotFoundException e) {
			uri = EntityUriBuilder.toInternal(uri);
			LOG.warn("No native view. Trying: {}", uri);
			editIntent.setData(uri);
			try {
				startActivity(editIntent);
			} catch (ActivityNotFoundException e2) {
				LOG.error("No edit activity found.");
			}
		}
		return true;
	}

}
