package polito.mailandroid.model;

public class Attachment implements LocalMessageElement{
	public static final String disposition = "ATTACHMENT";
    private String contentType;
    private String filename;
    private byte[] content;
	private long id;
        
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contenttype) {
        this.contentType = contenttype;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getContent() {
        return content;
    }
    
    public void setContent(byte[] content) {
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
