package polito.mailandroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

public class ElementProvider extends ContentProvider {
	private static final String DATABASE_NAME = "poli";
	private static final String DATABASE_TABLE = "elements";
	private static final int DATABASE_VERSION = 2;
	public static final String KEY_ID = "_id";
	public static final String KEY_DATA = "_data";
	public static final String KEY_MIMETYPE = "mimetype";
	public static final String KEY_ORIFILENAME = "orifilename";
	
	private DatabaseHelper mDbH;
	
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);
		}

		public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + DATABASE_TABLE + " (" +
            		KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            		KEY_ORIFILENAME + " TEXT, " +
            		KEY_MIMETYPE + " TEXT, " +
            		KEY_DATA + " TEXT" +
            		" );");
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }
	
	
	
	public static final int ELEMENTS = 1;
	public static final int ELEMENT_ID = 2;
	public static final Uri CONTENT_URI = Uri.parse("content://polito.mailandroid/elements");
	private static final UriMatcher URI_MATCHER;
	private static HashMap<String, String> ELEMENTS_LIST_PROJECTION_MAP;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("polito.mailandroid", "elements", ELEMENTS);
        URI_MATCHER.addURI("polito.mailandroid", "elements/#", ELEMENT_ID);

        ELEMENTS_LIST_PROJECTION_MAP = new HashMap<String, String>();
        ELEMENTS_LIST_PROJECTION_MAP.put(KEY_ID, KEY_ID);
        ELEMENTS_LIST_PROJECTION_MAP.put(KEY_DATA, KEY_DATA);
        ELEMENTS_LIST_PROJECTION_MAP.put(KEY_MIMETYPE, KEY_MIMETYPE);
    }
	
	public boolean onCreate() {
        mDbH = new DatabaseHelper(getContext());
        return true;
	}

	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		String[] projection = new String[] {
		    ElementProvider.KEY_DATA
		};
		Cursor c = query(uri,projection,null,null,null);
		c.moveToFirst();
		String file = c.getString(c.getColumnIndexOrThrow(ElementProvider.KEY_DATA));
		c.close();
		File f = new File(file);
		int m = ParcelFileDescriptor.MODE_READ_ONLY;
		if (mode.equalsIgnoreCase("rw"))
			m = ParcelFileDescriptor.MODE_READ_WRITE;
		ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f,m);
		return pfd;
		}
	
	public Uri insert(Uri uri, ContentValues initialValues) {
        if (URI_MATCHER.match(uri) != ELEMENTS) {
        	// !!!
        }
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        
        if (!values.containsKey(KEY_MIMETYPE))
        	values.put(KEY_MIMETYPE, "plain/text");

        SQLiteDatabase mDb = mDbH.getWritableDatabase();
        long rowID = mDb.insert(DATABASE_TABLE, "notnull", values);
        if (rowID > 0) {
            if (!values.containsKey(KEY_DATA)){
	            try {
	            	String filename = rowID + "";
	            	if (values.containsKey(KEY_ORIFILENAME))
	            		filename = values.getAsString(KEY_ORIFILENAME);
	    			getContext().openFileOutput(filename, Context.MODE_PRIVATE).close();
	    			String path = getContext().getFileStreamPath(filename).getAbsolutePath();
		    		values.put(KEY_DATA, path);
		    		update(uri,values,KEY_ID + "=" + rowID, null);
	    		} catch (Exception e) {
	    			// !!!
	    		}
            }
            Uri newUri = ContentUris.withAppendedId(ElementProvider.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        // !!!
        return null;
    }

	public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URI_MATCHER.match(uri)) {
	        case ELEMENTS:
	            qb.setTables(DATABASE_TABLE);
	            qb.setProjectionMap(ELEMENTS_LIST_PROJECTION_MAP);
	            break;
	        case ELEMENT_ID:
	            qb.setTables(DATABASE_TABLE);
	            qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
	            break;
	        default:
	            throw new IllegalArgumentException("Unknown URL " + uri);
        }
        SQLiteDatabase mDb = mDbH.getReadableDatabase();
        Cursor c = qb.query(mDb, projection, selection, selectionArgs, null, null, "_id DESC");
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int code = URI_MATCHER.match(uri);
        int count = 0;
        SQLiteDatabase mDb = mDbH.getWritableDatabase();
        switch (code) {
        	case ELEMENTS:
	            count = mDb.update(DATABASE_TABLE, values, selection, selectionArgs);
	            break;
	        case ELEMENT_ID:
	            String id = uri.getPathSegments().get(1);
	            count =
	                    mDb.update(DATABASE_TABLE, values, KEY_ID + "=" + id
	                            + (!TextUtils.isEmpty(selection) ? " AND ("
	                                    + selection + ')' : ""), selectionArgs);
	            break;
	        default:
	            throw new IllegalArgumentException("Unknown URL " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	public int delete(Uri uri, String where, String[] whereArgs) {
        int code = URI_MATCHER.match(uri);
        int count = 0;
        SQLiteDatabase mDb = mDbH.getWritableDatabase();
        switch (code) {
	        case ELEMENTS:
	        	// !!! REMOVE ALL FILES
	            count = mDb.delete(DATABASE_TABLE, where, whereArgs);
	            break;
	        case ELEMENT_ID:
	        	String id = uri.getPathSegments().get(1);
	        	String[] projection = new String[] {
        		    ElementProvider.KEY_DATA
        		};
	        	Cursor c = query(uri,projection,null,null,null);
	    		c.moveToFirst();
	    		String filename = c.getString(c.getColumnIndexOrThrow(ElementProvider.KEY_DATA));
	            File f = new File(filename);
	            if(f.isFile())
	            	f.delete();
	            count = mDb.delete(DATABASE_TABLE, KEY_ID + "=" + id
	                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
	            break;
	        default:
	            // !!!
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	public String getType(Uri uri) {
		String[] projection = new String[] {
		    ElementProvider.KEY_MIMETYPE
		};
		Cursor c = query(uri,projection,null,null,null);
		c.moveToFirst();
		String mimetype = c.getString(c.getColumnIndexOrThrow(ElementProvider.KEY_MIMETYPE));
		mimetype.substring(0, mimetype.indexOf(";")).trim();
		c.close();
        return mimetype;
	}

}
