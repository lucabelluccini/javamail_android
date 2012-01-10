package polito.mailandroid.model;


public interface LocalMessageElement {
	public byte[] getContent();
	public String getDisposition();
	public String getMimeType();
	public long getID();
	public void setID(long id);
}