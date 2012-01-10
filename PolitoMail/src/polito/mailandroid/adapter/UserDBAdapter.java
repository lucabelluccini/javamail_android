package polito.mailandroid.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class UserDBAdapter {
	private static final String DATABASE_NAME = "polito_messages";
	private static final String DATABASE_TABLE = "users";
	public static final int DATABASE_VERSION = 2;
	public static final String KEY_CONTACTID = "_cid";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_ID = "_id";
	private static final String DATABASE_CREATE =
    "create table " + DATABASE_TABLE + " (" +
    KEY_ID + " integer primary key autoincrement, " +
    KEY_CONTACTID + " integer, " +
    KEY_USERNAME + " text not null, " +
    KEY_PASSWORD + " text not null " +
    " );";

	private SQLiteDatabase mDb;
	private final Context mCtx;

	public UserDBAdapter(Context ctx) {
	    this.mCtx = ctx;
	}

	public UserDBAdapter open() throws SQLException {
	    mDb = mCtx.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
	    try {
	    	mDb.query(DATABASE_TABLE, new String[] { KEY_ID }, null, null, null, null, null);
	    } catch (Exception e) {
	    	mDb.execSQL(DATABASE_CREATE);
	    }
	    return this;
	}

	public void close() {
	    mDb.close();
	}

	public long createUser(long cid, String username, String password) {
		if(username.length() == 0)
			return -1;
		if(password.length() == 0)
			return -1;
		if(checkUser(username)){
			return -1;
		}
	    ContentValues uValues = new ContentValues();
	    uValues.put(KEY_CONTACTID, cid);
	    uValues.put(KEY_USERNAME, username);
	    uValues.put(KEY_PASSWORD, password);
	    return mDb.insert(DATABASE_TABLE, null, uValues);
	}

	public boolean deleteUser(Long rowId) {
		return mDb.delete(DATABASE_TABLE, KEY_ID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllUsers() {
		return mDb.query(DATABASE_TABLE, new String[] {
            KEY_ID, KEY_USERNAME }, null, null, null, null, null);
	}
	
	public Cursor fetchUser(Long id) {
		Cursor result = mDb.query(DATABASE_TABLE, new String[] {
            KEY_ID, KEY_CONTACTID, KEY_USERNAME, KEY_PASSWORD }, KEY_ID + "=" + id, null, null, null, null);
		if (result.getCount() == 0)
			return null;
		result.moveToFirst();
	    return result;
	}

	public long fetchUser(String username, String password) {
		Cursor result = mDb.query(DATABASE_TABLE, new String[] {
			KEY_ID, KEY_CONTACTID, KEY_USERNAME, KEY_PASSWORD }, KEY_USERNAME + "='" + username + "' AND " + KEY_PASSWORD + "='" + password +"'", null, null, null, null);
		if (result.getCount() == 0)
			return -1;
		result.moveToFirst();
		return result.getLong(result.getColumnIndexOrThrow(KEY_ID));
	}
	
	public long loginUser(String username, String password) {
		Cursor result = mDb.query(DATABASE_TABLE, new String[] {
	            KEY_ID, KEY_CONTACTID, KEY_USERNAME, KEY_PASSWORD }, KEY_USERNAME + "='" + username + "' AND " + KEY_PASSWORD + "='" + password +"'", null, null, null, null, null);
		if (result.getCount() == 0)
			return -1;
		result.moveToFirst();
		return result.getLong(result.getColumnIndexOrThrow(KEY_ID));
	}
	
	public boolean checkUser(String username) {
		Cursor result = mDb.query(true, DATABASE_TABLE, new String[] {
	            KEY_ID, KEY_CONTACTID, KEY_USERNAME, KEY_PASSWORD }, KEY_USERNAME + "='" + username + "'", null, null, null, null, null);
		if (result.getCount() == 0)
			return false;
		result.moveToFirst();
	    return true;
	}

	public boolean updateUser(Long rowId, long cid, String username, String password) {
	    ContentValues args = new ContentValues();
	    if(username.length() == 0)
	    	return false;
	    if(password.length() == 0)
	    	return false;
	    args.put(KEY_USERNAME, username);
	    args.put(KEY_PASSWORD, password);
	    args.put(KEY_CONTACTID, cid);
	    return mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + rowId, null) > 0;
	    }
	}
