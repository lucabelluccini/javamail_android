package polito.mailandroid.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import javax.mail.Message;
import javax.mail.MessagingException;

import polito.mailandroid.R;

public class FolderAdapter extends BaseAdapter {
    private Context context;
    private List<Message> messageList;
	private int rowResID;

    public FolderAdapter(Context context, int rowResID,	List<Message> messages ) { 
        this.context = context;
		this.rowResID = rowResID;
        this.messageList = messages;
    }

    public int getCount() {                        
        return messageList.size();
    }

    public Object getItem(int position) {     
        return messageList.get(position);
    }

    public long getItemId(int position) {  
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) { 
        Message message = messageList.get(position);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(rowResID, parent, false);
		try {
			TextView mailSubject = (TextView) v.findViewById (R.id.mail_single_subject);
			mailSubject.setText( message.getSubject() );
			TextView mailFrom = (TextView) v.findViewById (R.id.mail_single_from);
			mailFrom.setText( message.getFrom()[0].toString() );
		} catch (MessagingException e) {
			e.printStackTrace();
		}
        return v;
    }

}
