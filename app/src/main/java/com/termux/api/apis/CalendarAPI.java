package com.termux.api.apis;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.api.util.ResultReturner.ResultJsonWriter;
import com.termux.shared.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class CalendarAPI {

    private static final String LOG_TAG = "CalendarAPI";

    public static void onReceiveCalendars(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveCalendars");

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                listCalendars(context, out);
            }
        });
    }

    public static void onReceiveEvents(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveEvents");

        final long calendarId = intent.getLongExtra("calendar_id", -1);
        final long begin = intent.getLongExtra("begin", -1);
        final long end = intent.getLongExtra("end", -1);

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                listEvents(context, out, calendarId, begin, end);
            }
        });
    }

    public static void onReceiveAddEvent(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveAddEvent");

        final long calendarId = intent.getLongExtra("calendar_id", -1);
        final String title = intent.getStringExtra("title");
        final long dtstart = intent.getLongExtra("dtstart", -1);
        final long dtend = intent.getLongExtra("dtend", -1);
        final boolean allDay = intent.getBooleanExtra("all_day", false);
        final String timezone = intent.getStringExtra("event_timezone");
        final int reminderMinutes = intent.getIntExtra("reminder_minutes", -1);

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                addEvent(context, out, calendarId, title, dtstart, dtend, allDay, timezone, reminderMinutes);
            }
        });
    }

    static void listCalendars(Context context, JsonWriter out) throws IOException {
        ContentResolver cr = context.getContentResolver();

        String[] projection = {
                BaseColumns._ID,
                Calendars.CALENDAR_DISPLAY_NAME,
                Calendars.ACCOUNT_NAME,
                Calendars.ACCOUNT_TYPE,
                Calendars.OWNER_ACCOUNT,
                Calendars.VISIBLE
        };

        out.beginArray();
        try (Cursor cur = cr.query(Calendars.CONTENT_URI, projection, null, null,
                Calendars.CALENDAR_DISPLAY_NAME + " ASC")) {
            if (cur != null) {
                int idIdx = cur.getColumnIndexOrThrow(BaseColumns._ID);
                int displayNameIdx = cur.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
                int accountNameIdx = cur.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
                int accountTypeIdx = cur.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);
                int ownerAccountIdx = cur.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);
                int visibleIdx = cur.getColumnIndexOrThrow(Calendars.VISIBLE);

                while (cur.moveToNext()) {
                    out.beginObject();
                    out.name("id").value(cur.getLong(idIdx));
                    out.name("name").value(cur.getString(displayNameIdx));
                    out.name("account_name").value(cur.getString(accountNameIdx));
                    out.name("account_type").value(cur.getString(accountTypeIdx));
                    out.name("owner_account").value(cur.getString(ownerAccountIdx));
                    out.name("visible").value(cur.getInt(visibleIdx) != 0);
                    out.endObject();
                }
            }
        }
        out.endArray();
    }

    static void listEvents(Context context, JsonWriter out, long calendarId, long begin, long end) throws IOException {
        ContentResolver cr = context.getContentResolver();

        String[] projection = {
                BaseColumns._ID,
                Events.CALENDAR_ID,
                Events.TITLE,
                Events.DESCRIPTION,
                Events.EVENT_LOCATION,
                Events.DTSTART,
                Events.DTEND,
                Events.ALL_DAY,
                Events.EVENT_COLOR,
                Events.EVENT_TIMEZONE
        };

        List<String> selectionArgs = new ArrayList<>();
        StringBuilder selection = new StringBuilder();
        if (calendarId >= 0) {
            selection.append(Events.CALENDAR_ID + "=?");
            selectionArgs.add(String.valueOf(calendarId));
        }
        if (begin >= 0) {
            appendSelectionAnd(selection);
            selection.append(Events.DTSTART + ">=?");
            selectionArgs.add(String.valueOf(begin));
        }
        if (end >= 0) {
            appendSelectionAnd(selection);
            selection.append(Events.DTSTART + "<=?");
            selectionArgs.add(String.valueOf(end));
        }

        out.beginArray();
        try (Cursor cur = cr.query(Events.CONTENT_URI, projection,
                selection.length() > 0 ? selection.toString() : null,
                selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]),
                Events.DTSTART + " ASC")) {
            if (cur != null) {
                int idIdx = cur.getColumnIndexOrThrow(BaseColumns._ID);
                int calendarIdIdx = cur.getColumnIndexOrThrow(Events.CALENDAR_ID);
                int titleIdx = cur.getColumnIndexOrThrow(Events.TITLE);
                int descriptionIdx = cur.getColumnIndexOrThrow(Events.DESCRIPTION);
                int eventLocationIdx = cur.getColumnIndexOrThrow(Events.EVENT_LOCATION);
                int dtstartIdx = cur.getColumnIndexOrThrow(Events.DTSTART);
                int dtendIdx = cur.getColumnIndexOrThrow(Events.DTEND);
                int allDayIdx = cur.getColumnIndexOrThrow(Events.ALL_DAY);
                int eventColorIdx = cur.getColumnIndexOrThrow(Events.EVENT_COLOR);
                int eventTimezoneIdx = cur.getColumnIndexOrThrow(Events.EVENT_TIMEZONE);

                while (cur.moveToNext()) {
                    out.beginObject();
                    out.name("id").value(cur.getLong(idIdx));
                    out.name("calendar_id").value(cur.getLong(calendarIdIdx));
                    out.name("title").value(cur.getString(titleIdx));
                    out.name("description").value(cur.getString(descriptionIdx));
                    out.name("event_location").value(cur.getString(eventLocationIdx));
                    out.name("dtstart").value(cur.getLong(dtstartIdx));
                    out.name("dtend").value(cur.getLong(dtendIdx));
                    out.name("all_day").value(cur.getInt(allDayIdx) != 0);
                    out.name("event_color").value(cur.getInt(eventColorIdx));
                    out.name("event_timezone").value(cur.getString(eventTimezoneIdx));
                    out.endObject();
                }
            }
        }
        out.endArray();
    }

    static void addEvent(Context context, JsonWriter out, long calendarId, String title, long dtstart,
                         long dtend, boolean allDay, String timezone, int reminderMinutes) throws IOException {
        if (calendarId < 0) {
            out.beginObject().name("error").value("Missing or invalid 'calendar_id'").endObject();
            return;
        }
        if (title == null || title.isEmpty()) {
            out.beginObject().name("error").value("Missing 'title'").endObject();
            return;
        }
        if (dtstart < 0) {
            out.beginObject().name("error").value("Missing or invalid 'dtstart'").endObject();
            return;
        }

        String eventTimezone = timezone != null && !timezone.isEmpty() ? timezone : TimeZone.getDefault().getID();
        if (dtend < 0) {
            dtend = dtstart + (allDay ? 24L * 60 * 60 * 1000 : 60 * 60 * 1000);
        }

        ContentValues values = new ContentValues();
        values.put(Events.CALENDAR_ID, calendarId);
        values.put(Events.TITLE, title);
        values.put(Events.DTSTART, dtstart);
        values.put(Events.DTEND, dtend);
        values.put(Events.ALL_DAY, allDay ? 1 : 0);
        values.put(Events.EVENT_TIMEZONE, eventTimezone);

        ContentResolver cr = context.getContentResolver();
        Uri uri = cr.insert(Events.CONTENT_URI, values);
        if (uri == null) {
            out.beginObject().name("error").value("Failed to add event").endObject();
            return;
        }

        long eventId = Long.parseLong(uri.getLastPathSegment());

        if (reminderMinutes > 0) {
            ContentValues reminderValues = new ContentValues();
            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
            reminderValues.put(CalendarContract.Reminders.MINUTES, reminderMinutes);
            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            cr.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);
        }

        out.beginObject().name("id").value(eventId).endObject();
    }

    private static void appendSelectionAnd(StringBuilder selection) {
        if (selection.length() > 0) {
            selection.append(" AND ");
        }
    }
}
