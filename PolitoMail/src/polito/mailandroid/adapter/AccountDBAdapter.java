package polito.mailandroid.adapter;
import polito.mailandroid.model.Account;
import polito.mailandroid.model.IMAPAccount;
import polito.mailandroid.model.POP3Account;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class AccountDBAdapter {
	private static final String DATABASE_NAME = "polito_messages";
	private static final String DATABASE_TABLE = "accounts";
	public static final int DATABASE_VERSION = 2;
	public static final String KEY_TYPE = "type";
	public static final String KEY_ALIAS = "alias";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_HOST = "host";
	public static final String KEY_PORT = "port";
	public static final String KEY_USESSL = "useSSL";
	public static final String KEY_NEEDSTARTTLS = "needSTARTTLS";
	public static final String KEY_USERID = "_uid";
	public static final String KEY_ID = "_id";
	private static final String DATABASE_CREATE =
	"create table " + DATABASE_TABLE + " (" +
	KEY_ID + " integer primary key autoincrement, " +
	KEY_USERID + " integer, " +
	KEY_TYPE + " varchar(5) not null, " +
	KEY_ALIAS + " varchar(20) not null, " +
	KEY_USERNAME + " varchar(64) not null, " +
	KEY_PASSWORD + " varchar(64) not null, " +
	KEY_HOST + " varchar(255) not null, " +
	KEY_PORT + " varchar(6) not null, " +
	KEY_USESSL + " integer, " +
	KEY_NEEDSTARTTLS + " integer " +
	" );";

	
	private SQLiteDatabase mDb;
	private final Context mCtx;

	public AccountDBAdapter(Context ctx) {
	    this.mCtx = ctx;
	}

	public AccountDBAdapter open() throws SQLException {
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

	public long createAccount(Long userID, String type, String alias, String username, String password, String host, String port, boolean usessl, boolean needstarttls) {
	    ContentValues aValues = new ContentValues();
	    if(type.length() == 0)
	    	return -1;
	    if(alias.length() == 0)
	    	return -1;
	    if(username.length() == 0)
	    	return -1;
	    if(password.length() == 0)
	    	return -1;
	    if(host.length() == 0)
	    	return -1;
	    if(port.length() == 0)
	    	return -1;
	    aValues.put(KEY_USERID, userID);
	    aValues.put(KEY_TYPE, type);
	    aValues.put(KEY_ALIAS, alias);
	    aValues.put(KEY_USERNAME, username);
	    aValues.put(KEY_PASSWORD, password);
	    aValues.put(KEY_HOST, host);
	    aValues.put(KEY_PORT, port);
	    aValues.put(KEY_USESSL, (usessl)? 1 : 0);
	    aValues.put(KEY_NEEDSTARTTLS, (needstarttls)? 1 : 0);
	    return mDb.insert(DATABASE_TABLE, null, aValues);
	}

	public boolean deleteAccount(Long rowId) {
		return mDb.delete(DATABASE_TABLE, KEY_ID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllUserAccounts(Long rowId) {
		return mDb.query(DATABASE_TABLE, new String[] {
			KEY_ID, KEY_ALIAS, KEY_TYPE }, KEY_USERID + "=" + rowId, null, null, null, null);
	}

	public Cursor fetchAccount(Long rowId) throws SQLException {
		Cursor result = mDb.query(DATABASE_TABLE, new String[] {
			KEY_ID, KEY_ALIAS, KEY_TYPE, KEY_USERNAME, KEY_PASSWORD, KEY_HOST, KEY_PORT, KEY_USESSL, KEY_NEEDSTARTTLS }, KEY_ID + "=" + rowId, null, null, null, null);
		if (result.getCount() == 0)
			return null;
		result.moveToFirst();
	    return result;
	}

	public boolean updateAccount(long rowId, String type, String alias, String username, String password, String host, String port, boolean usessl, boolean needstarttls) {
	    ContentValues args = new ContentValues();
	    if(type.length() == 0)
	    	return false;
	    if(alias.length() == 0)
	    	return false;
	    if(username.length() == 0)
	    	return false;
	    if(password.length() == 0)
	    	return false;
	    if(host.length() == 0)
	    	return false;
	    if(port.length() == 0)
	    	return false;
	    args.put(KEY_TYPE, type);
	    args.put(KEY_ALIAS, alias);
	    args.put(KEY_USERNAME, username);
	    args.put(KEY_PASSWORD, password);
	    args.put(KEY_HOST, host);
	    args.put(KEY_PORT, port);
	    args.put(KEY_USESSL, (usessl)? 1 : 0);
	    args.put(KEY_NEEDSTARTTLS, (needstarttls)? 1 : 0);
	    return mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + rowId, null) > 0;
	}
	
	public Account getAccount(Long id) {
		Account a = null;
		Cursor aC = fetchAccount(id);
		String type = aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_TYPE)).toUpperCase();
		if(type.compareTo(IMAPAccount.TYPE) == 0){
			 a = new IMAPAccount(
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_ALIAS)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_USERNAME)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_PASSWORD)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_HOST)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_PORT)),
				(aC.getInt(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_USESSL)) == 1)? true : false,
				(aC.getInt(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_NEEDSTARTTLS)) == 1)? true : false
			);
		}
		if(type.compareTo(POP3Account.TYPE) == 0){
			a = new POP3Account(
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_ALIAS)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_USERNAME)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_PASSWORD)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_HOST)),
				aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_PORT)),
				(aC.getInt(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_USESSL)) == 1)? true : false
			);
		}
		return (a == null)? null : a;
	}
	
}


