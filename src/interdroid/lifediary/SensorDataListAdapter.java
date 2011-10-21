package interdroid.lifediary;

import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.SensorServiceInfo;
import interdroid.util.view.LayoutUtil.LayoutParameters;
import interdroid.util.view.LayoutUtil.LayoutWeight;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AbsListView.LayoutParams;

public class SensorDataListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {
	private static final String	TIMESTAMP	= "_timestamp";

	private static final String	SENSOR	= "sensor";

	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(SensorDataListAdapter.class);

	/** Number of hours in a day. */
	private static final int	HOURS_IN_A_DAY	= 24;

	/** Size of the hour group text. */
	private static final float	HOUR_TEXT_SIZE	= 14;

	/** The format for time. */
	private static final DateFormat	TIME_FORMAT	=
			SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);

	/**
	 * A class which holds an index into a particular cursor.
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 */
	public static class CursorIndex {
		public final long mTimestamp;
		public final String mSensor;
		public final int mId;

		/**
		 * Constructs an index into a cursor
		 * @param timestamp the timestamp for sorting
		 * @param sensor the sensor entity
		 */
		public CursorIndex(final int id, final long timestamp,
				final String sensor) {
			mTimestamp = timestamp;
			mSensor = sensor;
			mId = id;
		}

		/**
		 * Comparator for this type for easy sorting.
		 */
		public static final Comparator<CursorIndex> SORT =
				new Comparator<CursorIndex>() {

			@Override
			public int compare(CursorIndex left, CursorIndex right) {
				if (left.mTimestamp > right.mTimestamp) {
					return 1;
				} else if (left.mTimestamp < right.mTimestamp){
					return -1;
				}
				return 0;
			}
		};
	}

	/**
	 * The sorted indexes into our cursors.
	 */
	// Stupid non construction of arrays of generics!
	@SuppressWarnings("unchecked")
	private final List<CursorIndex>[] mSortedData = (List<CursorIndex>[])
			new List<?>[HOURS_IN_A_DAY];
	{
		for (int i = 0; i < HOURS_IN_A_DAY; i++) {
			mSortedData[i] = new ArrayList<CursorIndex>();
		}
	}

	/**
	 * The map of sensors we know about.
	 */
	private Map<String, SensorServiceInfo>	mSensors;

	/**
	 * The context we are working in.
	 */
	private final Context	mContext;

	private final DateFormat mTimeFormat =
			SimpleDateFormat.getTimeInstance();

	/**
	 * The date we are listing for.
	 */
	private final Calendar	mDate;

	/**
	 * Build an adapter to look at data from the given day.
	 * @param day the day to look at
	 * @param month the month to look at
	 * @param year the year to look at
	 */
	public SensorDataListAdapter(final Context context,
			final int day, final int month, final int year) {
		mContext = context;
		mDate = Calendar.getInstance();
		mDate.set(year, month, day, 0, 0, 0);
		setupCursors(context, day, month, year);
	}

	private void setupCursors(final Context context, final int day,
			final int month, final int year) {
		/**
		 * The map of sensor entity to cursors we manage.
		 */
		final Map<String, Cursor> sensorCursors =
				new HashMap<String, Cursor>();

		List<SensorServiceInfo> sensors = ContextManager.getSensors(context);
		mSensors = new HashMap<String, SensorServiceInfo>(sensors.size());

		Calendar start = Calendar.getInstance();
		start.set(year, month, day, 0, 0, 0);
		Calendar end = Calendar.getInstance();
		end.set(year, month, day, 23, 59, 59);

		for (SensorServiceInfo sensor : sensors) {
			LOG.debug("Processing sensor: {} {}", sensor.getEntity(),
					sensor.getAuthority());
			if (sensor.getAuthority() != null) {
				mSensors.put(sensor.getEntity(), sensor);

				// Get the uri for the sensor provider
				Uri sensorUri =
						new Uri.Builder()
				.scheme("content")
				.authority(sensor.getAuthority())
				.path("branches/master/" + sensor.getEntity() + "/")
				.build();
				// Get a cursor for the data from the sensor.
				Cursor cursor = context.getContentResolver()
				.query(sensorUri, new String[] {"_id", TIMESTAMP},
						"_timestamp >= ? AND _timestamp <= ?",
						new String[] {
							String.valueOf(start.getTimeInMillis()),
							String.valueOf(end.getTimeInMillis()) },
						"_timestamp ASC");
				// If that worked add it to the hash.
				if (cursor != null) {
					if (cursor.getCount() > 0) {
						sensorCursors.put(sensor.getEntity(), cursor);
					} else {
						cursor.close();
					}
				} else {
					LOG.error("Unable to query for {}", sensor.getEntity());
				}
			}
		}

		// Now sort all the data we pulled out.
		sortCursorData(sensorCursors);
	}

	private void sortCursorData(Map<String, Cursor> sensorCursors) {
		// Run through each cursor pulling the timestamp.
		Calendar date = Calendar.getInstance();

		for (Entry<String, Cursor> entry : sensorCursors.entrySet()) {
			LOG.debug("Processing cursor: {}", entry.getKey());
			Cursor cursor = entry.getValue();
			cursor.moveToFirst();
			int timeIndex = cursor.getColumnIndex(TIMESTAMP);
			int idIndex = cursor.getColumnIndex("_id");
			// Make sure there is a timestamp column
			if (timeIndex == -1) {
				continue;
			}
			while (!cursor.isAfterLast()) {
				long timestamp = cursor.getLong(timeIndex);
				int id = cursor.getInt(idIndex);
				CursorIndex cursorIndex = new CursorIndex(id, timestamp,
						entry.getKey());
				date.setTimeInMillis(timestamp);
				mSortedData[date.get(Calendar.HOUR_OF_DAY)].add(cursorIndex);
				cursor.moveToNext();
			}
			cursor.close();
		}
		LOG.debug("Sorting lists.");
		// Now sort the resulting list by timestamp.
		for (int i = 0; i < HOURS_IN_A_DAY; i++) {
			Collections.sort(mSortedData[i], CursorIndex.SORT);
		}
		LOG.debug("Sorting completed.");
	}

	public String getAuthority(String sensor) {
		return mSensors.get(sensor).getAuthority();
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mSortedData[groupPosition].get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return mSortedData[groupPosition].get(childPosition).mId;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			// Build the view
			LinearLayout layout = new LinearLayout(mContext);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			TextView sensor = new TextView(mContext);
			sensor.setTag(SENSOR);
			sensor.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			LayoutParameters.setLinearLayoutParams(
					LayoutParameters.W_WRAP_H_WRAP, LayoutWeight.One,
					sensor);
			TextView timestamp = new TextView(mContext);
			timestamp.setTag(TIMESTAMP);
			timestamp.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
			LayoutParameters.setLinearLayoutParams(
					LayoutParameters.W_WRAP_H_WRAP, LayoutWeight.Zero,
					timestamp);
			layout.addView(sensor);
			layout.addView(timestamp);

			view = layout;
		}

		// What we will use to fill in the row.
		CursorIndex index = mSortedData[groupPosition].get(childPosition);

		// Fill in the data.
		TextView sensor = (TextView) view.findViewWithTag(SENSOR);
		sensor.setText(index.mSensor);
		TextView timestamp = (TextView) view.findViewWithTag(TIMESTAMP);
		timestamp.setText(mTimeFormat.format(index.mTimestamp));

		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mSortedData[groupPosition].size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mSortedData[groupPosition];
	}

	@Override
	public int getGroupCount() {
		return HOURS_IN_A_DAY;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		TextView view;
		if (convertView == null) {
			view = new TextView(mContext);
			view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 60));
			view.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			view.setPadding(60, 0, 0, 0);
			view.setTextSize(HOUR_TEXT_SIZE);
		} else {
			view = (TextView) convertView;
		}

		mDate.set(Calendar.HOUR_OF_DAY, groupPosition);
		view.setText(TIME_FORMAT.format(mDate.getTimeInMillis()));

		return view;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
