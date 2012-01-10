package polito.mailandroid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import polito.mailandroid.R;
import polito.mailandroid.adapter.UserDBAdapter;

public class UserUI extends Activity{
	
    private static final int CONFIRM_ID = Menu.FIRST;
    private static final int CLEAR_ID = Menu.FIRST + 1;
    private static final int CANCEL_ID = Menu.FIRST + 2;
	
	private long userID;
	private UserDBAdapter userDB;
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.userDB = new UserDBAdapter(this);
		this.userDB.open();
		setContentView(R.layout.user);
		Bundle extras = getIntent().getExtras(); 
        userID = extras.getLong(UserDBAdapter.KEY_ID);
        resetUI();
        if( userID != -1 ){
        	fillUserUIfromDB(userID);
        }
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.getMenuInflater().inflate(R.menu.usermenu,menu);
        return true;
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
        case R.id.confirm:
            if(confirm()) {
            	finish();
            }
            return true;
        case R.id.clear:
        	resetUI();
        	return true;
        case R.id.cancel:
        	finish();
        	return true;
        }
        return super.onMenuItemSelected(featureId, item);
	}
	
	private boolean confirm(){
		Spinner contactS = (Spinner) findViewById(R.id.user_contact);
		EditText usernameET = (EditText) findViewById(R.id.user_username);
		EditText passwordET = (EditText) findViewById(R.id.user_password);
		String temp = (String) contactS.getSelectedItem();
		long cid = 0;
		if(contactS.getCount()==0){
			Toast.makeText(this, "Create a contact first.", Toast.LENGTH_SHORT).show();
		} else {
			temp = temp.substring(0, temp.indexOf("-") -1 );
			temp.trim();
			cid = Long.parseLong(temp);
		}
		String username = usernameET.getText().toString();
		String password = passwordET.getText().toString();
		if(userID == -1){
			long retval = userDB.createUser(cid, username, password);
    		if (retval < 0){
    			Toast.makeText(this, R.string.alert_user_createerror, Toast.LENGTH_SHORT).show();
    		}
			return retval >= 0;
		} else {
			boolean retval = userDB.updateUser(userID, cid, username, password);
    		if (!retval){
    			Toast.makeText(this, R.string.alert_user_editerror, Toast.LENGTH_SHORT).show();
    		}
			return retval;
		}
	}
	
	private boolean resetUI(){
		Spinner contactS = (Spinner) findViewById(R.id.user_contact);
		TextView usernameTV = (TextView) findViewById(R.id.user_username);
		TextView passwordTV = (TextView) findViewById(R.id.user_password);
		String[] projection = new String[] {
		    android.provider.BaseColumns._ID,
		    android.provider.Contacts.PeopleColumns.NAME
		};
		Cursor contactCursor = managedQuery(android.provider.Contacts.Phones.CONTENT_URI, projection, null, null, android.provider.Contacts.PeopleColumns.NAME + " ASC");
		ArrayAdapter<CharSequence> contactAA = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item);
		if(contactCursor != null){
			contactCursor.moveToFirst();
			if(contactCursor.getCount()>0)
				do{
					contactAA.add(contactCursor.getLong(contactCursor.getColumnIndexOrThrow(android.provider.BaseColumns._ID)) + " - " + contactCursor.getString(contactCursor.getColumnIndexOrThrow(android.provider.Contacts.PeopleColumns.NAME)));
				}while(contactCursor.moveToNext());
		}
		contactAA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		contactS.setAdapter(contactAA);
		usernameTV.setText("");
		passwordTV.setText("");
		return true;
	}
	
	private void fillUserUIfromDB(Long id) {
		Cursor uC = userDB.fetchUser(id);
		if( uC == null ){
			Toast.makeText(this, R.string.alert_user_notfound, Toast.LENGTH_SHORT).show();
			return;
		}
		long cid = uC.getLong(uC.getColumnIndexOrThrow(UserDBAdapter.KEY_CONTACTID));
		Spinner contactS = (Spinner) findViewById(R.id.user_contact);
		String[] projection = new String[] {
		    android.provider.BaseColumns._ID,
		    android.provider.Contacts.PeopleColumns.NAME
		};
		Cursor contactCursor = managedQuery( android.provider.Contacts.Phones.CONTENT_URI,
		                        projection,
		                        null,
		                        null,
		                        android.provider.Contacts.PeopleColumns.NAME + " ASC");
		ArrayAdapter<CharSequence> contactAA = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item);
		CharSequence tempCS = null;
		int selposition = 0;
		if(contactCursor != null){
			contactCursor.moveToFirst();
			do{
				tempCS = contactCursor.getLong(contactCursor.getColumnIndexOrThrow(android.provider.BaseColumns._ID)) + " - " + contactCursor.getString(contactCursor.getColumnIndexOrThrow(android.provider.Contacts.PeopleColumns.NAME));
				contactAA.add(tempCS);
				if(contactCursor.getLong(contactCursor.getColumnIndexOrThrow(android.provider.BaseColumns._ID)) == cid )
					selposition = contactAA.getPosition(tempCS);
			}while(contactCursor.moveToNext());
		}
		contactAA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		contactS.setAdapter(contactAA);
		contactS.setSelection(selposition);
		TextView usernameTV = (TextView) findViewById(R.id.user_username);
		TextView passwordTV = (TextView) findViewById(R.id.user_password);
		usernameTV.setText(uC.getString(uC.getColumnIndexOrThrow(UserDBAdapter.KEY_USERNAME)));
		passwordTV.setText(uC.getString(uC.getColumnIndexOrThrow(UserDBAdapter.KEY_PASSWORD)));
	}
	
}
