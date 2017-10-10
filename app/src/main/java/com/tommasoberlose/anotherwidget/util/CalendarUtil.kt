package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.provider.CalendarContract
import android.util.Log
import com.tommasoberlose.anotherwidget.`object`.CalendarSelector
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.`object`.Event
import java.util.*

/**
 * Created by tommaso on 08/10/17.
 */

object CalendarUtil {

    fun updateEventList(context: Context) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val eventList = ArrayList<Event>()

        val now = Calendar.getInstance()
        val hourLimit = Calendar.getInstance()
        hourLimit.add(Calendar.HOUR, 6)

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now.timeInMillis)
        ContentUris.appendId(builder, hourLimit.timeInMillis)

        if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            resetNextEventData(context)
        }

        val instanceCursor = context.contentResolver.query(builder.build(), arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END), null, null, null)
        instanceCursor.moveToFirst()



        for (i in 0 until instanceCursor.count) {
            val ID = instanceCursor.getInt(0)

            val eventCursor = context.contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.ALL_DAY, CalendarContract.Events.CALENDAR_ID),
                    CalendarContract.Events._ID + " is ?",
                    arrayOf(Integer.toString(ID)), null)
            eventCursor.moveToFirst()

            for (j in 0 until eventCursor.count) {
                val e = Event(eventCursor, instanceCursor)
                val allDay: Boolean = !eventCursor.getString(1).equals("0")
                if (e.endDate - now.timeInMillis > 1000 * 60 * 30 && (SP.getBoolean(Constants.PREF_CALENDAR_ALL_DAY, false) || !allDay) && !(SP.getString(Constants.PREF_CALENDAR_FILTER, "").contains(" " + e.calendarID + ","))) {
                    eventList.add(e)
                }
                eventCursor.moveToNext()
            }

            eventCursor.close()

            instanceCursor.moveToNext()
        }

        instanceCursor.close()

        if (eventList.isEmpty()) {
            resetNextEventData(context)
        } else {
            saveNextEventData(context, eventList.get(0))
        }

    }

    fun getCalendarList(context: Context): List<CalendarSelector> {
        val calendarList = ArrayList<CalendarSelector>()

        if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            return calendarList
        }

        val calendarCursor = context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME, CalendarContract.Calendars.ACCOUNT_NAME),
                null,
                null,
                null) ?: return calendarList

        calendarCursor.moveToFirst()

        for (j in 0 until calendarCursor.count) {
            calendarList.add(CalendarSelector(calendarCursor.getInt(0), calendarCursor.getString(1), calendarCursor.getString(2)))
            calendarCursor.moveToNext()
        }

        calendarCursor.close()

        return calendarList
    }

    fun resetNextEventData(context: Context) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        SP.edit()
                .remove(Constants.PREF_NEXT_EVENT_ID)
                .remove(Constants.PREF_NEXT_EVENT_NAME)
                .remove(Constants.PREF_NEXT_EVENT_START_DATE)
                .remove(Constants.PREF_NEXT_EVENT_END_DATE)
                .remove(Constants.PREF_NEXT_EVENT_CALENDAR_ID)
                .apply()
    }

    @SuppressLint("ApplySharedPref")
    fun saveNextEventData(context: Context, event: Event) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        SP.edit()
                .putInt(Constants.PREF_NEXT_EVENT_ID, event.id)
                .putString(Constants.PREF_NEXT_EVENT_NAME, event.title)
                .putLong(Constants.PREF_NEXT_EVENT_START_DATE, event.startDate)
                .putLong(Constants.PREF_NEXT_EVENT_END_DATE, event.endDate)
                .putInt(Constants.PREF_NEXT_EVENT_CALENDAR_ID, event.calendarID)
                .commit()
        Util.updateWidget(context)
    }

    fun getNextEvent(context: Context): Event {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return Event(SP.getInt(Constants.PREF_NEXT_EVENT_ID, 0), SP.getString(Constants.PREF_NEXT_EVENT_NAME, ""), SP.getLong(Constants.PREF_NEXT_EVENT_START_DATE, 0), SP.getLong(Constants.PREF_NEXT_EVENT_END_DATE, 0), SP.getInt(Constants.PREF_NEXT_EVENT_CALENDAR_ID, 0))
    }
}