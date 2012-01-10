package polito.mailandroid.ui;


import java.io.OutputStream;
import java.util.ArrayList;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import polito.mailandroid.ElementProvider;
import polito.mailandroid.R;
import polito.mailandroid.adapter.AccountDBAdapter;
import polito.mailandroid.model.Account;
import polito.mailandroid.model.Attachment;
import polito.mailandroid.model.LocalMessage;
import polito.mailandroid.model.LocalMessageElement;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MailUI extends Activity{
	
	private static final int START_ID = Menu.FIRST;
	private static final int SSLOV_ID = Menu.FIRST + 1;
	
	private long accountID;
	private long userID;
	private int messageID;
	private AccountDBAdapter accountDB;
	private Account account;
	
	LocalMessage message;
	private Handler mHandler = new Handler();
	private Thread t;
	private ProgressDialog progressDialog;
	private String exceptionMsg = null;
	private boolean exceptionOccurr = false;
	private boolean ssloverride;
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.accountDB = new AccountDBAdapter(this);
		this.accountDB.open();
		setContentView(R.layout.account);
		Bundle extras = getIntent().getExtras(); 
        accountID = extras.getLong(AccountDBAdapter.KEY_ID);
        account = accountDB.getAccount( accountID );
        userID = extras.getLong(AccountDBAdapter.KEY_USERID);
        messageID = extras.getInt("MAILID");
        setContentView(R.layout.mail);
        ssloverride = false;
        resetUI();
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.getMenuInflater().inflate(R.menu.mailmenu,menu);
        return true;
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
        case R.id.retrieve:
            retrieveMessage();
            return true;
        case R.id.ssl:
        	ssloverride = !ssloverride;
        	if(ssloverride){
        		item.setTitle(getString(R.string.menu_mail_sslov_on));
        		item.setIcon(R.drawable.menu_mail_bypassssl);
        	} else {
        		item.setTitle(getString(R.string.menu_mail_sslov_off));
        		item.setIcon(R.drawable.menu_mail_nobypassssl);
        	}       		
        	return true;
        }
        return super.onMenuItemSelected(featureId, item);
	}
	
	private void updateUI() {		
		if( message != null ) {
			TextView subjectTV = (TextView) findViewById(R.id.mail_subject);
			subjectTV.setText(R.string.mail_subject_label);
			if(message.getSubject() != null)
				subjectTV.append(message.getSubject().trim());
			else
				subjectTV.append(getString(R.string.mail_nd));
			TextView daterecvTV = (TextView) findViewById(R.id.mail_date_recv);
			daterecvTV.setText(R.string.mail_date_recv_label);
			if(message.getSentDate() != null)
				daterecvTV.append(message.getSentDate());
			else
				daterecvTV.append(getString(R.string.mail_nd));
			TextView datesentTV = (TextView) findViewById(R.id.mail_date_sent);
			datesentTV.setText(R.string.mail_date_sent_label);
			if(message.getRecvDate() != null)
				datesentTV.append(message.getRecvDate());
			else
				datesentTV.append(getString(R.string.mail_nd));
			TextView fromTV = (TextView) findViewById(R.id.mail_from);
			fromTV.setText(R.string.mail_from_label);
			if(message.getFrom().trim() != null)
				fromTV.append(message.getFrom().trim());
			else
				fromTV.append(getString(R.string.mail_nd));
			
			// Store data in ElementProvider
			byte[] data;
			ContentValues cv = new ContentValues();
			ArrayList<String> elements = new ArrayList<String>();
			Attachment a;
			OutputStream os;
			long id;
			for(LocalMessageElement lme : message.getContents()){
				data = lme.getContent();
				Log.v("XXX","byte[]: " + data);
				Log.v("XXX","String: " + new String(data));
				cv.put(ElementProvider.KEY_MIMETYPE, lme.getMimeType());
				if (lme instanceof Attachment){
					a = (Attachment) lme;
					cv.put(ElementProvider.KEY_ORIFILENAME, a.getFilename());
				}
				Uri newE = getContentResolver().insert(ElementProvider.CONTENT_URI,cv);
				id = ContentUris.parseId(newE);
				lme.setID(id);
				try {
					os = getContentResolver().openOutputStream(newE);
					os.write(data);
					os.close();
					elements.add(lme.getDisposition() + " (" + id + ") - " + lme.getMimeType());
				} catch(Exception e) {
					Toast.makeText(MailUI.this, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
				cv.clear();
			}
			
			Spinner elementS = (Spinner) findViewById(R.id.mail_bodypart_spinner);
			ArrayAdapter<String> elementsAA = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, elements);
			elementsAA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			elementS.setAdapter(elementsAA); 
			elementS.setOnItemSelectedListener(spinnerListener);
			elementS.setEnabled(true);
			elementS.setSelection(0, true);
		}
	}

	private OnItemSelectedListener spinnerListener = new OnItemSelectedListener () {
		public void onItemSelected(AdapterView parent, View v, int position, long id) {
			Spinner elementS = (Spinner) findViewById(R.id.mail_bodypart_spinner);
			if( elementS.isEnabled()){
				String selectedElement = (String) elementS.getSelectedItem();
				long eid = Long.parseLong(selectedElement.substring(selectedElement.indexOf("(")+1,selectedElement.indexOf(")")));
				WebView bodypartsWV = (WebView) findViewById(R.id.mail_bodyparts);
				bodypartsWV.loadUrl(ContentUris.withAppendedId(ElementProvider.CONTENT_URI,eid).toString());
			}
		}
		public void onNothingSelected(AdapterView arg0) {
			
		}
    };
	
	private boolean resetUI(){
		Cursor aC = accountDB.fetchAccount(accountID);
		aC.isFirst();
        setTitle(getString(R.string.app_name) + " - " + aC.getString(aC.getColumnIndexOrThrow(AccountDBAdapter.KEY_ALIAS)));
		TextView subjectTV = (TextView) findViewById(R.id.mail_subject);
		subjectTV.setText(R.string.mail_subject_label);
		subjectTV.append(getString(R.string.mail_nd));
		TextView daterecvTV = (TextView) findViewById(R.id.mail_date_recv);
		daterecvTV.setText(R.string.mail_date_recv_label);
		daterecvTV.append(getString(R.string.mail_nd));
		TextView datesentTV = (TextView) findViewById(R.id.mail_date_sent);
		datesentTV.setText(R.string.mail_date_sent_label);
		datesentTV.append(getString(R.string.mail_nd));
		TextView fromTV = (TextView) findViewById(R.id.mail_from);
		fromTV.setText(R.string.mail_from_label);
		fromTV.append(getString(R.string.mail_nd));
		Spinner elementS = (Spinner) findViewById(R.id.mail_bodypart_spinner);
		elementS.setEnabled(false);
		TextView emptyTV = new TextView(this);
		emptyTV.setText(R.string.mail_nd);
		ArrayList<String> elements = new ArrayList<String>();
		ArrayAdapter<String> elementAA = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, elements);
		elementAA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		elementS.setEmptyView(emptyTV);
		elementS.setAdapter(elementAA);
		WebView bodypartsWV = (WebView) findViewById(R.id.mail_bodyparts);
		return true;
	}
	
	// Runnable for Thread Start
	private final Runnable mUIProgressStart = new Runnable() {
        public void run() {
        	startUI();
        }
    };
	
	protected void startUI() {
		progressDialog.setMessage(getString(R.string.mail_retrieving));
	}
	
	// Runnable for Thread Stop
    private final Runnable mCancelRetrieveMail = new Runnable() {
        public void run() {
        	t.interrupt();
        }
    };
    
    // Runnable for UI Progress End
    private final Runnable mUIProgressEnd = new Runnable() {
        public void run() {
        	endUI();
        	updateUI();
        }
    };
    
	protected void endUI() {
		try {
			progressDialog.dismiss();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(exceptionOccurr){
				Toast.makeText(this, exceptionMsg, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	// Retrieves Message (starts Thread)
	protected synchronized boolean retrieveMessage() {
		exceptionMsg = null;
		exceptionOccurr = false;
    	t = new Thread() {
			public void run() {
				try{
					Folder tempFolder = account.open(ssloverride);
					mHandler.post(mUIProgressStart);
					if(!isInterrupted()){
						Message m = tempFolder.getMessage(messageID);
						message = new LocalMessage(m);
					}
				} catch (Exception e){
            		exceptionMsg = e.getMessage();
            		exceptionOccurr = true;
				}
            	if(account.isOpen())
					try {
						account.close();
					} catch (MessagingException e) {
	            		exceptionMsg = e.getMessage();
	            		exceptionOccurr = true;
					}
        		if(!isInterrupted()){
        			mHandler.post(mUIProgressEnd);
        		}
			}
        };
    	progressDialog = new ProgressDialog(this);
    	progressDialog.setCancelable(true);
    	progressDialog.setIndeterminate(true);
    	progressDialog.setMessage(getString(R.string.mail_waitopen));
    	progressDialog.setOnCancelListener( 
    		new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				mHandler.post(mCancelRetrieveMail);
			}
		});
    	progressDialog.show();
    	t.start();
        return true;
    }

	protected void onStop() {
		for(LocalMessageElement lme : message.getContents()){
			Log.v("XXX",ContentUris.withAppendedId(ElementProvider.CONTENT_URI,lme.getID()).toString());
			getContentResolver().delete(ContentUris.withAppendedId(ElementProvider.CONTENT_URI,lme.getID()), null, null);
			lme.setID(0);
		}
		super.onStop();
	}
	
}
