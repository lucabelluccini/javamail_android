package polito.mailandroid.ui;

import polito.mailandroid.R;
import polito.mailandroid.adapter.UserDBAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class LoginUI extends Activity {
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 2;
    private static final int ACTIVITY_LOGIN = 3;
    
    private UserDBAdapter userDB;

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.userDB = new UserDBAdapter(this);
		setContentView(R.layout.login);
		resetUI();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.getMenuInflater().inflate(R.menu.loginmenu,menu);
        return true;
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
        case R.id.create:
            return createUser();
        case R.id.login:
            return loginUser();
        case R.id.clear:
        	return resetUI();
        case R.id.edit:
        	return editUser();
        case R.id.exit:
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
	}

	private boolean resetUI() {
    	EditText usernameET = (EditText) findViewById(R.id.username);
    	EditText passwordET = (EditText) findViewById(R.id.password);
    	usernameET.setText("");
    	passwordET.setText("");
		return true;
	}

	private boolean editUser() {
		EditText usernameET = (EditText) findViewById(R.id.username);
    	EditText passwordET = (EditText) findViewById(R.id.password);
		Intent i = new Intent(this, UserUI.class);
    	userDB.open();
    	long retval = userDB.fetchUser(usernameET.getText().toString(), passwordET.getText().toString());
    	userDB.close();
    	if(retval == -1){
    		Toast.makeText(this, R.string.alert_user_notfound, Toast.LENGTH_SHORT).show();
    		return false;
    	}
		i.putExtra(UserDBAdapter.KEY_ID, retval);
		startActivityForResult(i, ACTIVITY_EDIT);
		return true;
	}
	
	private boolean createUser() {
		Intent i = new Intent(this, UserUI.class);
		long retval = -1;
		i.putExtra(UserDBAdapter.KEY_ID, retval);
		startActivityForResult(i, ACTIVITY_CREATE);
		return true;
	}
	
	private boolean loginUser() {
    	EditText usernameET = (EditText) findViewById(R.id.username);
    	EditText passwordET = (EditText) findViewById(R.id.password);
    	userDB.open();
    	Long id = userDB.loginUser(usernameET.getText().toString(), passwordET.getText().toString());
    	userDB.close();
    	if( id < 0 ){
    		Toast.makeText(this, R.string.alert_user_loginerror, Toast.LENGTH_SHORT).show();
    		return false;
    	} else {
			Intent i = new Intent(this, AccountsUI.class);
			i.putExtra(UserDBAdapter.KEY_ID, id);
	        startActivityForResult(i, ACTIVITY_LOGIN);
	        return true;
    	}
	}

}
