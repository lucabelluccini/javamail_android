package polito.mailandroid.model;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;


public final class POP3Account extends Account{
	public static final String TYPE = "POP3";
	protected boolean useSSL;
	protected Session pop3session;
	protected Store pop3store;
	protected Folder pop3folder;
	
	// Constructor
	public POP3Account(String alias, String username, String password, String host, String port, boolean usessl) {
		super(alias, username,password,host,port);
		this.useSSL = usessl;
	}
	
	// Close Folder method
	public boolean close() throws MessagingException{
		// Close Folder 
		this.pop3folder.close(false);
		// Close Store
		this.pop3store.close();
		return true;
	}

	public Folder open(boolean overrideSSLFactory) throws NoSuchProviderException, MessagingException{
		// new Empty Properties
		Properties props = new Properties();
		// SSL Stuff
		if(this.useSSL){
			if(overrideSSLFactory){
				props.setProperty("mail.pop3.socketFactory.class", "polito.mailandroid.security.DummySSLSocketFactory" );
			}else {
				props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory" );
			}
			// Prevents to fall into NOT-secure connection
			props.setProperty("mail.pop3.socketFactory.fallback", "false");
		}
		// Properties
		props.setProperty("mail.pop3.port", port);
		props.setProperty("mail.pop3.socketFactory.port", port);
		// Session
		this.pop3session = Session.getInstance(props);
		// Store
		this.pop3store = this.pop3session.getStore("pop3");
		this.pop3store.connect(host, username, password);
		// Folder
		this.pop3folder = this.pop3store.getFolder("INBOX");
		this.pop3folder.open(Folder.READ_ONLY);
		return this.pop3folder;
	}
	
	public String getType() {
		return TYPE;
	}
	
	public boolean isOpen() {
		if (pop3folder == null) 
			return false;
		else
			return pop3folder.isOpen();
	}
}
