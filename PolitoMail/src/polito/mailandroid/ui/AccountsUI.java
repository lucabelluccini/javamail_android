package polito.mailandroid.ui;

import polito.mailandroid.R;
import polito.mailandroid.adapter.AccountDBAdapter;
import polito.mailandroid.adapter.UserDBAdapter;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class AccountsUI extends ListActivity {
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_VIEW = 1;
    private static final int ACTIVITY_EDIT = 3;
    
	private UserDBAdapter userDB;
	private AccountDBAdapter accountDB;
	private Long userID;

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		userDB = new UserDBAdapter(this);
		accountDB = new AccountDBAdapter(this);
		userDB.open();
		accountDB.open();
		Bundle extras = getIntent().getExtras();
		userID = extras.getLong(UserDBAdapter.KEY_ID);
		setContentView(R.layout.account_list);
		refreshUI();
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        setSelection(position);
    }
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.getMenuInflater().inflate(R.menu.accountsmenu,menu);
        return true;
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int iId = item.getItemId();
		switch(iId) {
        case R.id.create:
            createAccount();
            refreshUI();
            return true;
        case R.id.delete:
        	deleteAccount();
        	refreshUI();
        	return true;
        case R.id.edit:
        	editAccount();
        	refreshUI();
        	return true;
        case R.id.access:
        	viewMails();
        	return true;
        case R.id.logout:
        	finish();
        	return true;
        }
        return super.onMenuItemSelected(featureId, item);
	}

	public boolean createAccount() {
		Intent i = new Intent(this, AccountUI.class);
		long dummyID = -1;
		i.putExtra(AccountDBAdapter.KEY_ID, dummyID);
		i.putExtra(AccountDBAdapter.KEY_USERID, userID);
        startActivityForResult(i, ACTIVITY_CREATE);
		return true;
	}
	
	public boolean editAccount() {
		Intent i = new Intent(this, AccountUI.class);
		i.putExtra(AccountDBAdapter.KEY_ID, getSelectedItemId());
		i.putExtra(AccountDBAdapter.KEY_USERID, userID);
		startActivityForResult(i, ACTIVITY_EDIT);
		return true;
	}
	
	public boolean deleteAccount() {
		return accountDB.deleteAccount( getSelectedItemId() );
	}
	
	private void refreshUI() {
		Cursor uC = userDB.fetchUser(userID);
		uC.moveToFirst();
		Cursor asC = accountDB.fetchAllUserAccounts(userID);
		startManagingCursor(asC);
        String[] from = new String[]{
        	AccountDBAdapter.KEY_ALIAS,
        	AccountDBAdapter.KEY_TYPE
        };
        int[] to = new int[]{
        	R.id.account_single_alias,
        	R.id.account_single_type
        };
        SimpleCursorAdapter accounts = 
        	    new SimpleCursorAdapter(this, R.layout.account_list_row, asC, from, to);
        setListAdapter(accounts);
		setTitle(getString(R.string.app_name) + " - " + uC.getString(uC.getColumnIndexOrThrow(UserDBAdapter.KEY_USERNAME)));
	}

	public boolean viewMails() {
		if(getListAdapter().getCount() > 0){
			if(getSelectedItemId()>0){
				Intent i = new Intent(this, MailsUI.class);
				i.putExtra(AccountDBAdapter.KEY_USERID, userID);
				i.putExtra(AccountDBAdapter.KEY_ID, getSelectedItemId());
				startActivityForResult(i, ACTIVITY_VIEW);
			}
		}
        return true;
	}

}
