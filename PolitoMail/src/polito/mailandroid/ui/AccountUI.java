package polito.mailandroid.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import polito.mailandroid.R;
import polito.mailandroid.adapter.AccountDBAdapter;
import polito.mailandroid.model.IMAPAccount;
import polito.mailandroid.model.POP3Account;

public class AccountUI extends Activity{

	
	private long accountID;
	private long userID;
	private AccountDBAdapter accountDB;
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.accountDB = new AccountDBAdapter(this);
		this.accountDB.open();
		setContentView(R.layout.account);
		Bundle extras = getIntent().getExtras(); 
        accountID = extras.getLong(AccountDBAdapter.KEY_ID);
        userID = extras.getLong(AccountDBAdapter.KEY_USERID);
        resetUI();
        if( accountID != -1 ){
        	fillAccounUIfromDB(accountID);
        }
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.getMenuInflater().inflate(R.menu.accountmenu,menu);
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
		Spinner typeS = (Spinner) findViewById(R.id.account_type);
		CheckBox usesslCB = (CheckBox) findViewById(R.id.account_usessl);
		CheckBox needstarttlsCB = (CheckBox) findViewById(R.id.account_needstarttls);
		EditText aliasET = (EditText) findViewById(R.id.account_alias);
		EditText usernameET = (EditText) findViewById(R.id.account_username);
		EditText passwordET = (EditText) findViewById(R.id.account_password);
		EditText hostET = (EditText) findViewById(R.id.account_host);
		EditText portET = (EditText) findViewById(R.id.account_port);
		String type = (String) typeS.getSelectedItem();
		String alias = aliasET.getText().toString();
		String username = usernameET.getText().toString();
		String password = passwordET.getText().toString();
		String host = hostET.getText().toString();
		String port = portET.getText().toString();
		boolean usessl = usesslCB.isChecked();
		boolean needstarttls = needstarttlsCB.isChecked();
		if(accountID == -1){
			long retval = accountDB.createAccount(userID, type, alias, username, password, host, port, usessl, needstarttls);
			if (retval < 0){
				Toast.makeText(this, R.string.alert_account_createerror, Toast.LENGTH_SHORT).show();
    		}
			return retval >= 0;
		} else {
			boolean retval = accountDB.updateAccount(accountID, type, alias, username, password, host, port, usessl, needstarttls);
			if (!retval){
				Toast.makeText(this, R.string.alert_account_editerror, Toast.LENGTH_SHORT).show();
    		}
			return retval;
		}
	}
	
	private boolean resetUI(){
		Spinner typeS = (Spinner) findViewById(R.id.account_type);
		ArrayList<String> accountTypes = new ArrayList<String>();
		accountTypes.add(POP3Account.TYPE);
		accountTypes.add(IMAPAccount.TYPE);
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, accountTypes);
		typeS.setAdapter(adapter);
		CheckBox usesslCB = (CheckBox) findViewById(R.id.account_usessl);
		usesslCB.setChecked(false);
		CheckBox needstarttlsCB = (CheckBox) findViewById(R.id.account_needstarttls);
		needstarttlsCB.setChecked(false);
		EditText aliasET = (EditText) findViewById(R.id.account_alias);
		aliasET.setText("Account");
		EditText usernameET = (EditText) findViewById(R.id.account_username);
		usernameET.setText("");
		EditText passwordET = (EditText) findViewById(R.id.account_password);
		passwordET.setText("");
		EditText hostET = (EditText) findViewById(R.id.account_host);
		hostET.setText("");
		EditText portET = (EditText) findViewById(R.id.account_port);
		portET.setText("");
		return true;
	}
	
	private void fillAccounUIfromDB(Long id) {
		Cursor aC = accountDB.fetchAccount(id);
		String type = aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_TYPE)).toUpperCase();
		Spinner typeS = (Spinner) findViewById(R.id.account_type);
		CheckBox usesslCB = (CheckBox) findViewById(R.id.account_usessl);
		CheckBox needstarttlsCB = (CheckBox) findViewById(R.id.account_needstarttls);
		TextView aliasTV = (TextView) findViewById(R.id.account_alias);
		TextView usernameTV = (TextView) findViewById(R.id.account_username);
		TextView passwordTV = (TextView) findViewById(R.id.account_password);
		TextView hostTV = (TextView) findViewById(R.id.account_host);
		TextView portTV = (TextView) findViewById(R.id.account_port);
		if(type.compareTo(POP3Account.TYPE) == 0){
			typeS.setSelection(0);
		}
		if(type.compareTo(IMAPAccount.TYPE) == 0){
			typeS.setSelection(1);
		}
		aliasTV.setText(aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_ALIAS)));
		usernameTV.setText(aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_USERNAME)));
		passwordTV.setText(aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_PASSWORD)));
		hostTV.setText(aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_HOST)));
		portTV.setText(aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_PORT)));
		usesslCB.setChecked(false);
		if(aC.getInt(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_USESSL)) == 1){
			usesslCB.setChecked(true);
		}
		needstarttlsCB.setChecked(false);
		if(aC.getInt(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_NEEDSTARTTLS)) == 1){
			needstarttlsCB.setChecked(true);
		}
		
	}
	
}
