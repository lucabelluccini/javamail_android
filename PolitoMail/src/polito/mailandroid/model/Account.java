package polito.mailandroid.model;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

public abstract class Account{
	protected String alias;
	protected String username;
	protected String password;
	protected String host;
	protected String port;
	
	public Account(String alias, String username, String password, String host, String port) {
		super();
		this.alias = alias;
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
	}

	public abstract Folder open(boolean overrideSSL) throws NoSuchProviderException, MessagingException;
	public abstract boolean close() throws MessagingException;
	public abstract String getType();
	
	public String getAlias() {
		return alias;
	}
	public String getHost() {
		return host;
	}
	public String getPort() {
		return port;
	}

	public abstract boolean isOpen();
}
