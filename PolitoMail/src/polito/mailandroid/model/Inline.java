package polito.mailandroid.model;

public class Inline implements LocalMessageElement {
	public static final String disposition = "INLINE";
    private String contentType;
    private String content;
    private long id;
    
	public String getContentType() {
	    return contentType;
	}
	
	public void setContentType(String contenttype) {
	    this.contentType = contenttype;
	}
	
	public byte[] getContent() {
	    return content.getBytes();
	}
	
	public void setContent(String content) {
	    this.content = content;
	}
	
	public String getDisposition() {
	    return disposition;
	}
	
	public String getMimeType() {
		return contentType;
	}
	
	public long getID() {
		return id;
	}
	
	public void setID(long id) {
		this.id = id;
	}

}
