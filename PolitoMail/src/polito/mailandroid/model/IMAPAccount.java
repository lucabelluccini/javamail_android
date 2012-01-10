package polito.mailandroid.model;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

public final class IMAPAccount extends Account{
	public static final String TYPE = "IMAP";
	protected boolean useSSL;
	protected boolean needSTARTTLS;
	protected Session imapsession;
	protected Store imapstore;
	protected Folder imapfolder;
	
	// Constructor
	public IMAPAccount(String alias, String username, String password, String host, String port, boolean usessl, boolean needstarttls) {
		super(alias, username, password, host, port);
		this.useSSL = usessl;
		this.needSTARTTLS = needstarttls;
	}

	// Open Folder method
	public Folder open( boolean overrideSSLFactory ) throws MessagingException {
		// new Empty Properties
		Properties props = new Properties();
		// SSL Stuff
		if(this.useSSL){
			if(overrideSSLFactory){
				props.setProperty("mail.imap.socketFactory.class", "polito.mailandroid.security.DummySSLSocketFactory" );
			}else {
				props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory" );
			}
			// Prevents to fall into NOT-secure connection
			props.setProperty("mail.imap.socketFactory.fallback", "false");
		}
		// STARTTLS Stuff
		if(this.needSTARTTLS){
			props.setProperty("mail.imap.starttls.enable", "true");
		}
		// Properties
		props.setProperty("mail.imap.port", port);
		props.setProperty("mail.imap.socketFactory.port", port);
		// Session
		this.imapsession = Session.getInstance(props);
		// Store
		this.imapstore = this.imapsession.getStore("imap");
		this.imapstore.connect(host,username,password);
		// Folder
		this.imapfolder = this.imapstore.getFolder("INBOX");
		this.imapfolder.open(Folder.READ_ONLY);
		return this.imapfolder;
	}
	
	// Close Folder method
	public boolean close() throws MessagingException{
		// Close Folder 
		this.imapfolder.close(false);	
		// Close Store
		this.imapstore.close();
		return true;
	}

	public String getType() {
		return TYPE;
	}

	public boolean isOpen() {
		if (imapfolder == null) 
			return false;
		else
			return imapfolder.isOpen();
	}
	
}
