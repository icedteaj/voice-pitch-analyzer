package de.lilithwittmann.voicepitchanalyzer.models.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.lilithwittmann.voicepitchanalyzer.models.PitchRange;
import de.lilithwittmann.voicepitchanalyzer.models.Recording;


/**
 * Created by Lilith on 05/07/15.
 */
public class RecordingDB {

    private static final String LOG_TAG = RecordingDB.class.getSimpleName();

    private static final String TEXT_TYPE = " TEXT";
    private static final String DOUBLE_TYPE = " REAL";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_RECORDING_TABLE =
            "CREATE TABLE " + RecordingEntry.TABLE_NAME + " (" +
                    RecordingEntry._ID + " INTEGER PRIMARY KEY," +
                    RecordingEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_DATE + DOUBLE_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_FILE + TEXT_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_AVG_PITCH + DOUBLE_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_MAX_PITCH + DOUBLE_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_MIN_PITCH + DOUBLE_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_DEF_FILE_SIZE +
                    " );";
    private static final String SQL_CREATE_PITCH_TABLE =
            "CREATE TABLE " + PitchEntry.TABLE_NAME + " (" +
                    PitchEntry._ID + " INTEGER PRIMARY KEY," +
                    PitchEntry.COLUMN_NAME_PITCH + DOUBLE_TYPE + COMMA_SEP +
                    PitchEntry.COLUMN_NAME_OFFSET + DOUBLE_TYPE + COMMA_SEP +
                    PitchEntry.COLUMN_NAME_RECORDING_ID + INTEGER_TYPE +
                    " );";
    private static final String SQL_DELETE_RECORDING_TABLE =
            "DROP TABLE IF EXISTS " + RecordingEntry.TABLE_NAME + ";";
    private static final String SQL_DELETE_PITCH_TABLE =
            "DROP TABLE IF EXISTS " + PitchEntry.TABLE_NAME + ";";
    private static final String SQL_ALTER_TABLE = "ALTER TABLE ";
    private static final String SQL_ADD_COLUMN = " ADD COLUMN ";
    private final Context context;


    public RecordingDB(Context context) {
        this.context = context;
    }

    public Recording saveRecording(Recording recording) {
        // Gets the data repository in write mode
        RecordingDbHelper mDbHelper = new RecordingDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(RecordingEntry.COLUMN_NAME_NAME, recording.getName());
        values.put(RecordingEntry.COLUMN_NAME_DATE, recording.getDate().getTime());
        values.put(RecordingEntry.COLUMN_NAME_AVG_PITCH, recording.getRange().getAvg());
        values.put(RecordingEntry.COLUMN_NAME_MAX_PITCH, recording.getRange().getMax());
        values.put(RecordingEntry.COLUMN_NAME_MIN_PITCH, recording.getRange().getMin());
        values.put(RecordingEntry.COLUMN_NAME_FILE, recording.getRecording());
        values.put(RecordingEntry.COLUMN_NAME_FILE_SIZE, recording.getRecordingFileSize());

        // Insert the new row, returning the primary key value of the new row
        long newRowId;
        newRowId = db.insert(
                RecordingEntry.TABLE_NAME,
                null,
                values);
        for (Double pitch : recording.getRange().getPitches()) {
            values = new ContentValues();
            values.put(PitchEntry.COLUMN_NAME_PITCH, pitch);
            values.put(PitchEntry.COLUMN_NAME_OFFSET, 0);
            values.put(PitchEntry.COLUMN_NAME_RECORDING_ID, newRowId);
            db.insert(
                    PitchEntry.TABLE_NAME,
                    null,
                    values);
        }

        recording.setId(newRowId);
        return recording;
    }

    public List<Recording> getRecordingsWithFiles() {
        return getRecordings(RecordingEntry.COLUMN_NAME_FILE + " IS NOT NULL");
    }

    public List<Recording> getRecordings() {
        return getRecordings(null);
    }

    private List<Recording> getRecordings(String whereClause) {
        List<Recording> recordings = new ArrayList<Recording>();
        RecordingDbHelper mDbHelper = new RecordingDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                RecordingEntry._ID,
                RecordingEntry.COLUMN_NAME_FILE,
                RecordingEntry.COLUMN_NAME_AVG_PITCH,
                RecordingEntry.COLUMN_NAME_MAX_PITCH,
                RecordingEntry.COLUMN_NAME_MIN_PITCH,
                RecordingEntry.COLUMN_NAME_DATE,
                RecordingEntry.COLUMN_NAME_NAME,
                RecordingEntry.COLUMN_NAME_FILE_SIZE,
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = RecordingEntry.COLUMN_NAME_DATE + " DESC";

        Cursor c = db.query(
                RecordingEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                whereClause,                              // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        c.moveToFirst();
        while (!c.isAfterLast()) {
            Recording recording = new Recording();
            recording.setId(c.getLong(0));
            recording.setRecording(c.getString(1));
            recording.setRecordingFileSize(c.getLong(7));
            recording.setDate(new Date(c.getLong(5)));
            recording.setName(c.getString(6));
            PitchRange pitch = new PitchRange();
            pitch.setAvg(c.getDouble(2));
            pitch.setMax(c.getDouble(3));
            pitch.setMin(c.getDouble(4));
            recording.setRange(pitch);
            recordings.add(recording);
            c.moveToNext();
        }
        return recordings;
    }

    public Recording getRecording(long id) {
        List<Recording> recordings = new ArrayList<Recording>();
        RecordingDbHelper mDbHelper = new RecordingDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                RecordingEntry._ID,
                RecordingEntry.COLUMN_NAME_FILE,
                RecordingEntry.COLUMN_NAME_AVG_PITCH,
                RecordingEntry.COLUMN_NAME_MAX_PITCH,
                RecordingEntry.COLUMN_NAME_MIN_PITCH,
                RecordingEntry.COLUMN_NAME_DATE,
                RecordingEntry.COLUMN_NAME_NAME,
                RecordingEntry.COLUMN_NAME_FILE_SIZE,
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = RecordingEntry.COLUMN_NAME_DATE + " DESC";

        Cursor c = db.query(
                RecordingEntry.TABLE_NAME,  // The table to query
                projection,                 // The columns to return
                RecordingEntry._ID + "=?",         // The columns for the WHERE clause
                new String[]{String.valueOf(id)}, // The values for the WHERE clause
                null,                       // don't group the rows
                null,                       // don't filter by row groups
                sortOrder                   // The sort order
        );
        c.moveToFirst();

        Recording recording = new Recording();
        recording.setId(c.getLong(0));
        recording.setRecording(c.getString(1));
        recording.setRecordingFileSize(c.getLong(7));
        recording.setDate(new Date(c.getLong(5)));
        recording.setName(c.getString(6));
        PitchRange pitch = new PitchRange();
        pitch.setAvg(c.getDouble(2));
        pitch.setMax(c.getDouble(3));
        pitch.setMin(c.getDouble(4));
        pitch.setPitches(this.getPitch(recording.getId()));
        recording.setRange(pitch);

        return recording;
    }

    public void deleteRecording(long id) {
        RecordingDbHelper mDbHelper = new RecordingDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        // Define 'where' part of query.
        String selection = RecordingEntry._ID + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {String.valueOf(id)};
        // Issue SQL statement.
        db.delete(RecordingEntry.TABLE_NAME, selection, selectionArgs);

        selection = PitchEntry._ID + " LIKE ?";
        db.delete(RecordingEntry.TABLE_NAME, selection, selectionArgs);

    }

    private List<Double> getPitch(long id) {
        List<Double> pitches = new ArrayList<Double>();
        RecordingDbHelper mDbHelper = new RecordingDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                PitchEntry._ID,
                PitchEntry.COLUMN_NAME_PITCH,
                PitchEntry.COLUMN_NAME_OFFSET
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = PitchEntry._ID + " ASC";


        Cursor c = db.query(
                PitchEntry.TABLE_NAME,  // The table to query
                projection,                 // The columns to return
                PitchEntry.COLUMN_NAME_RECORDING_ID + "=?",         // The columns for the WHERE clause
                new String[]{String.valueOf(id)}, // The values for the WHERE clause
                null,                       // don't group the rows
                null,                       // don't filter by row groups
                sortOrder                   // The sort order
        );
        c.moveToFirst();

        while (!c.isAfterLast()) {
            pitches.add(c.getDouble(1));
            System.out.println("c.getDouble(): " + c.getDouble(1));
            c.moveToNext();
        }

        return pitches;
    }

    public void updateRecordingFilename(long id, String filename) {
        ContentValues updates = new ContentValues();
        updates.put(RecordingEntry.COLUMN_NAME_FILE, filename);
        updateRecording(id, updates);
    }

    public void updateRecording(long id, ContentValues updates) {
        RecordingDbHelper dbHelper = new RecordingDbHelper(this.context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String whereClause = RecordingEntry._ID + " LIKE ?";
        String[] whereValues = { String.valueOf(id) };
        db.update(RecordingEntry.TABLE_NAME, updates, whereClause, whereValues);
    }

    public static abstract class RecordingEntry implements BaseColumns {
        public static final String TABLE_NAME = "recording";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_AVG_PITCH = "avg_pitch";
        public static final String COLUMN_NAME_MIN_PITCH = "min_pitch";
        public static final String COLUMN_NAME_MAX_PITCH = "max_pitch";
        public static final String COLUMN_NAME_FILE = "file";
        public static final String COLUMN_NAME_FILE_SIZE = "file_size";
        public static final String COLUMN_DEF_FILE_SIZE = COLUMN_NAME_FILE_SIZE + INTEGER_TYPE;
    }

    public static abstract class PitchEntry implements BaseColumns {
        public static final String TABLE_NAME = "pitch";
        public static final String COLUMN_NAME_PITCH = "p";
        public static final String COLUMN_NAME_OFFSET = "offset";
        public static final String COLUMN_NAME_RECORDING_ID = "recording_id";
    }

    public class RecordingDbHelper extends SQLiteOpenHelper {
        private static final int NO_MIGRATE_VERSION = 3;
        private static final int ADD_FILE_SIZE_VERSION = 5;

        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 5;
        public static final String DATABASE_NAME = "Recording.db";

        public RecordingDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_RECORDING_TABLE);
            db.execSQL(SQL_CREATE_PITCH_TABLE);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion <= NO_MIGRATE_VERSION) {
                Log.i(LOG_TAG, "Deleting old database version " + oldVersion);
                recreateDatabase(db);
                return;
            }

            Log.i(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
            long startTime = System.currentTimeMillis();

            db.beginTransaction();

            try {
                if (oldVersion < ADD_FILE_SIZE_VERSION && newVersion >= ADD_FILE_SIZE_VERSION) {
                    db.execSQL(SQL_ALTER_TABLE + RecordingEntry.TABLE_NAME + SQL_ADD_COLUMN + RecordingEntry.COLUMN_DEF_FILE_SIZE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            long duration = System.currentTimeMillis() - startTime;
            Log.i(LOG_TAG, "Database upgrade from version " + oldVersion + " to " + newVersion + " finished in " + duration + "ms");
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(LOG_TAG, "Cannot downgrade database from version " + oldVersion + " to " + newVersion);
            recreateDatabase(db);
        }

        private void recreateDatabase(SQLiteDatabase db) {
            db.execSQL(SQL_DELETE_RECORDING_TABLE);
            db.execSQL(SQL_DELETE_PITCH_TABLE);
            onCreate(db);
        }
    }

}
