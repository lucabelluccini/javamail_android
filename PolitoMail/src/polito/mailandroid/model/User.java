package polito.mailandroid.model;

import java.util.ArrayList;
import java.util.Collection;


public class User {
	String username;
	String password;
	Collection<Account> accounts;
	
	public User(String un, String pwd) {
		super();
		this.username = un;
		this.password = pwd;
		this.accounts = new ArrayList<Account>();
	}
	
	public void addAccount(Account newAccount){
		this.accounts.add(newAccount);
	}
	
	public void addAllAccount(Collection<Account> newAccounts){
		this.accounts.addAll(newAccounts);
	}
	
}
