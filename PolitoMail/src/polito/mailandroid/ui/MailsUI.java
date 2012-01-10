package polito.mailandroid.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import polito.mailandroid.R;
import polito.mailandroid.adapter.AccountDBAdapter;
import polito.mailandroid.adapter.FolderAdapter;
import polito.mailandroid.model.Account;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MailsUI extends ListActivity {
    private static final int ACTIVITY_VIEW = 1;

    private static final int START_ID = Menu.FIRST;
    private static final int VIEW_ID = Menu.FIRST + 1;
    private static final int SSLOV_ID = Menu.FIRST + 2;
	
	private AccountDBAdapter accountDB;
	private Account account;
	private Long accountID;
	private Long userID;
	
	private Folder retrievedFolder;
	private List<Message> retrievedMails;
	private boolean ssloverride = false;

	private RetrieveFolderTask mRFTask;
	private ProgressBar mRFProgressBar;
	private ProgressDialog mRFProgressDialog;
	
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ssloverride = false;
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.mail_list);
        accountDB = new AccountDBAdapter(this);
        accountDB.open();
        Bundle extras = getIntent().getExtras();
        accountID = extras.getLong(AccountDBAdapter.KEY_ID);
        userID = extras.getLong(AccountDBAdapter.KEY_USERID);
        account = accountDB.getAccount( accountID );
        retrievedMails = new ArrayList<Message>();
        FolderAdapter fa = new FolderAdapter(this, R.layout.mail_list_row, retrievedMails);
        setListAdapter(fa);
        setTitle(getString(R.string.app_name) + " - " + account.getAlias());
    }

	protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        setSelection(position);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.getMenuInflater().inflate(R.menu.mailsmenu,menu);
        return true;
    }

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
        case R.id.retrieve:
        	mRFTask = (RetrieveFolderTask) new RetrieveFolderTask().execute();
            return true;
        case R.id.ssl:
        	ssloverride = !ssloverride;
        	if(ssloverride){
        		item.setTitle(getString(R.string.menu_mails_sslov_on));
        		item.setIcon(R.drawable.menu_mails_bypassssl);
        	} else {
        		item.setTitle(getString(R.string.menu_mails_sslov_off));
        		item.setIcon(R.drawable.menu_mails_nobypassssl);
        	}       		
        	return true;
        case R.id.select:
        	if(retrievedMails.size()>0)
        		return viewMail();
        	else
        		return true;
        }
        return super.onMenuItemSelected(featureId, item);
	}

//    // Runnable for UI refresh
//	private final Runnable mUIProgressUpdate = new Runnable() {
//        public void run() {
//        	updateUI();
//        }
//    };
//    
//	protected void updateUI() {
//		mRFProgressDialog.setProgress(mProgressStatus);
//    	setProgress(mProgressStatus);
//    	FolderAdapter fa = new FolderAdapter(this,R.layout.mail_list_row, retrievedMails);
//    	setListAdapter(fa);
//    	fa.notifyDataSetChanged();
//    	
//	}
    
//    // Runnable for Thread Stop
//    private final Runnable mCancelRetrieveFolder = new Runnable() {
//        public void run() {
//        	t.interrupt();
//        	setProgress(10000);
//    		setProgressBarVisibility(false);
//        }
//    };
//    
//    // Runnable for UI Progress End
//    private final Runnable mUIProgressEnd = new Runnable() {
//        public void run() {
//        	endUI();
//        	
//        }
//    };
//    
//	protected void endUI() {
//		try {
//			mRFProgressDialog.dismiss();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			setProgress(10000);
//			setProgressBarVisibility(false);
//			if(exceptionOccurr){
//				Toast.makeText(this, exceptionMsg, Toast.LENGTH_SHORT).show();
//			}
//		}
//	}
    
//    // Runnable for UI Progress Start
//    private final Runnable mUIProgressStart = new Runnable() {
//        public void run() {
//        	startUI();
//        }
//    };
	
//	protected void startUI() {
//		mRFProgressDialog.setIndeterminate(false);
//		setProgress(mProgressStatus);
//		setProgressBarVisibility(true);
//		mRFProgressDialog.setMessage(getString(R.string.mails_retrieving_progress_pre));
//		mRFProgressDialog.setProgress(mProgressStatus);
//	}
    
//    // Retrieves Folder (starts Thread)
//    protected synchronized boolean retrieveFolder() {
//    	retrievedMails.clear();
//		exceptionMsg = null;
//		exceptionOccurr = false;
//    	t = new Thread() {
//			public void run() {    	
//            	try {
//            		retrievedFolder = account.open(ssloverride);		
//                	mProgressStatus = 0;
//                	if(!isInterrupted())
//                		mHandler.post(mUIProgressStart);
//            		if(retrievedFolder != null) {
//            			for(Message msg : retrievedFolder.getMessages()){
//            				if(isInterrupted()){      			
//            					break;
//            				}
//            				msg.getFrom();
//            				msg.getSubject();
//            				retrievedMails.add(msg);
//            				mProgressStatus = 10000*msg.getMessageNumber()/retrievedFolder.getMessageCount();
//            				mHandler.post(mUIProgressUpdate);
//            			}
//            		}
//            	} catch (Exception e){
//            		exceptionMsg = e.getMessage();
//            		exceptionOccurr = true;
//            	}
//            	if(account.isOpen())
//					try {
//						account.close();
//					} catch (MessagingException e) {
//	            		exceptionMsg = e.getMessage();
//	            		exceptionOccurr = true;
//					}
//        		if(!isInterrupted()){
//        			mHandler.post(mUIProgressEnd);
//        		}
//            }
//        };
//    	mRFProgressDialog = new ProgressDialog(this);
//    	mRFProgressDialog.setCancelable(true);
//    	mRFProgressDialog.setIndeterminate(true);
//    	mRFProgressDialog.setMessage(getString(R.string.mails_waitopen));
//    	mRFProgressDialog.setOnCancelListener( 
//    		new DialogInterface.OnCancelListener() {
//			public void onCancel(DialogInterface dialog) {
//				mHandler.post(mCancelRetrieveFolder);
//			}
//		});
//    	mRFProgressDialog.show();
//    	t.start();
//        return true;
//    }

	private boolean viewMail() {
		if(retrievedMails.size() > 0){
			Intent i = new Intent(this, MailUI.class);
			i.putExtra(AccountDBAdapter.KEY_ID, accountID);
			i.putExtra(AccountDBAdapter.KEY_USERID, userID);
	        Long messageID = getSelectedItemId()+1;
			i.putExtra("MAILID", messageID.intValue());
	        startActivityForResult(i, ACTIVITY_VIEW);
	        return true;
		}
		return false;
	}
	
	private class RetrieveFolderTask extends AsyncTask<Void, Integer, Integer> {
		final AtomicInteger mCount = new AtomicInteger();
		final AtomicInteger mMaxCount = new AtomicInteger();
		
		protected Integer doInBackground(Void... params) {
         	try {
         		retrievedFolder = account.open(ssloverride);
        		if(retrievedFolder != null)
        		{
        			mCount.set(retrievedFolder.getMessageCount());
         			for(Message msg : retrievedFolder.getMessages())
         			{
         				publishProgress(mCount.get(), mMaxCount.get());
         				if (isCancelled())
         					return null;
         				msg.getFrom();
         				msg.getSubject();
         				retrievedMails.add(msg);
         				mCount.incrementAndGet();
         			}
        		}
         	} catch (Exception e){
         		e.getMessage();
         		return null;
         	}
			return mCount.get();

         }

	     protected void onProgressUpdate (Integer... values){
			if(mRFProgressBar.isIndeterminate())
				mRFProgressBar.setIndeterminate(false);
	    	mRFProgressBar.setMax(values[1]);
	    	mRFProgressBar.setProgress(values[0]);
	    	
	    	if(mRFProgressDialog.isIndeterminate())
	    		mRFProgressDialog.setIndeterminate(false);
	    	mRFProgressDialog.setMax(values[1]);
	    	mRFProgressDialog.setProgress(values[0]);
	     }
	     
	     protected void onPreExecute() {
	    	 initUI();
	     }
	     

		@Override
		public void onCancelled() {
			cancelUI();
		}
	     
	     protected void onPostExecute(Integer retrievedCount) {
			if (retrievedCount == null) {
				Log.w("LOL","EMPTY");
			} else {
		     	//FolderAdapter fa = new FolderAdapter(,R.layout.mail_list_row, retrievedMails);
		     	//setListAdapter(fa);
		     	//fa.notifyDataSetChanged();
			}
			cancelUI();
	     }
	 }
	
	protected void initUI(){
   	 // Clear data
   	 retrievedMails.clear();
   	 // ProgressBar
   	 setProgressBarIndeterminate(true);
   	 setProgressBarVisibility(true);
   	 setProgress(0);
   	 // ProgressDialog
   	 mRFProgressDialog = new ProgressDialog(this);
   	 mRFProgressDialog.setCancelable(true);
   	 mRFProgressDialog.setIndeterminate(true);
   	 mRFProgressDialog.setMessage(getString(R.string.mails_waitopen));
   	 mRFProgressDialog.setOnCancelListener( 
	     		new DialogInterface.OnCancelListener() {
		 			public void onCancel(DialogInterface dialog) {
		 				// Progress Dialog dismiss
		 			}
		 		});
   	 mRFProgressDialog.show();
	}
	
	protected void cancelUI() {
		setProgressBarVisibility(false);
		mRFProgressDialog.cancel();
	}
	

	
}