package interdroid.lifediary;

import interdroid.util.view.CalendarActivity;
import interdroid.util.view.CalendarClickListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class LifeDiaryCalendar extends CalendarActivity {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(LifeDiaryCalendar.class);
	private static final int	MENU_ITEM_PREFS	= 0;

	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);

		getCalendarView().
		setOnCalendarClickListener(new CalendarClickListener() {

			@Override
			public void onCalendarClicked(int day, int month, int year) {
				startDayActivity(day, month, year);
			}

		});
	}

	/**
	 * Start the day view activity.
	 * @param day the day to view
	 * @param month the month to view
	 * @param year the year to view
	 */
	private void startDayActivity(int day, int month, int year) {

		LOG.debug("Calendar clicked: {} {} {}",
				new Object[] {day, month, year});

		Intent dayIntent = new Intent(this,
				LifeDiaryDay.class);
		dayIntent.putExtra(LifeDiaryDay.EXTRA_DAY, day);
		dayIntent.putExtra(LifeDiaryDay.EXTRA_MONTH, month);
		dayIntent.putExtra(LifeDiaryDay.EXTRA_YEAR, year);
		startActivity(dayIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_PREFS, 0, getString(R.string.label_prefs))
		.setShortcut('1', 'p')
		.setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = false;
		if (item.getItemId() == MENU_ITEM_PREFS) {
			Intent prefsIntent = new Intent(this, LifeDiaryPreferences.class);
			startActivity(prefsIntent);
			ret = true;
		}

		return super.onOptionsItemSelected(item);
	}

}
