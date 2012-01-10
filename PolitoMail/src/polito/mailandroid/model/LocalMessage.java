package polito.mailandroid.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import javax.mail.*;

public class LocalMessage {
    
	private Message m;
    private String subject;
    private Address[] from;
    private Address[] rcps;
    private int size;
    private Date sentdate;
    private Date recvdate;
    private ArrayList<LocalMessageElement> contents;
    
    public LocalMessage(Message msg) throws MessagingException,IOException {
    	this.m = msg;
    	this.subject = m.getSubject();
    	this.from = m.getFrom();
    	this.sentdate = m.getSentDate();
    	this.recvdate = msg.getReceivedDate();
    	this.contents = new ArrayList<LocalMessageElement>();
        this.rcps = this.m.getAllRecipients();
        this.size = this.m.getSize();
        recursiveExtract(m);
    }
    
    private void recursiveExtract(final Part part) throws MessagingException, IOException {
    	
    	String disposition = part.getDisposition();
    	
    	if( part.getContent() instanceof String) {
    		if( (disposition == null) || disposition.equalsIgnoreCase(Part.INLINE) ) {
    			LocalMessageElement inline = createInline(part);
    			this.contents.add(inline);
    			return;
    		}
    	}
    	if( part.getContent() instanceof Multipart ){
    		Multipart mpart = (Multipart) part.getContent();
    		for( int mpc = 0; mpc < mpart.getCount(); mpc++ ){
    			Part tempBP = mpart.getBodyPart(mpc);
    			tempBP.getSize();
    			recursiveExtract(tempBP);
    		}
    		return;
    	}
    	Attachment attachment = createAttachment(part);	
    	this.contents.add(attachment);
    }
    
    public Attachment createAttachment ( Part p ) throws MessagingException,IOException {
        Attachment attachment = new Attachment();
        attachment.setContentType( p.getContentType() );
        attachment.setFilename( p.getFileName() );
        attachment.setContentType(p.getContentType());
        InputStream in = p.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count=0;
        while( (count=in.read(buffer)) >= 0 )
        	bos.write(buffer,0,count);
        in.close();
        attachment.setContent( bos.toByteArray() );
        return attachment;
    }
    	
    public Inline createInline ( Part p ) throws MessagingException,IOException {
    	Inline inline = new Inline();
    	inline.setContentType( p.getContentType() );
        inline.setContent((String) p.getContent());
        return inline;
    }
    
    public String getSubject() {
        return subject;
    }

    public String getFrom() {
    	if(from[0] != null)
    		return from[0].toString();
    	return null;
    }

	public String getSentDate() {
		if(sentdate != null)
			return sentdate.toString();
		return null;
	}

	public Address[] getRcps() {
		return rcps;
	}

	public String getRecvDate() {
		if(recvdate != null)
			return recvdate.toString();
		return null;
	}

	public ArrayList<LocalMessageElement> getContents() {
		return contents;
	}

}
