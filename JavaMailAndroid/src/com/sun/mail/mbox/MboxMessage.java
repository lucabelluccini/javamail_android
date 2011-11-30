/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * %W% %E%
 */

package com.sun.mail.mbox;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Date;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.event.MessageChangedEvent;

/**
 * This class represents an RFC822 style email message that resides in a file.
 *
 * @author Bill Shannon
 */

public class MboxMessage extends MimeMessage {

    boolean writable = false;
    // original msg flags, used by MboxFolder to detect modification
    Flags origFlags;
    /*
     * A UNIX From line looks like:
     * From address Day Mon DD HH:MM:SS YYYY
     */
    String unix_from;
    InternetAddress unix_from_user;
    Date rcvDate;
    int lineCount = -1;
    private static OutputStream nullOutputStream = new OutputStream() {
	public void write(int b) { }
	public void write(byte[] b, int off, int len) { }
    };

    /**
     * Construct an MboxMessage from the InputStream.
     */
    public MboxMessage(Session session, InputStream is)
				throws MessagingException, IOException {
	super(session);
	BufferedInputStream bis;
	if (is instanceof BufferedInputStream)
	    bis = (BufferedInputStream)is;
	else
	    bis = new BufferedInputStream(is);
	DataInputStream dis = new DataInputStream(bis);
	bis.mark(1024);
	String line = dis.readLine();
	if (line != null && line.startsWith("From "))
	    this.unix_from = line;
	else
	    bis.reset();
	parse(bis);
	saved = true;
    }

    /**
     * Construct an MboxMessage using the given InternetHeaders object
     * and byte array contents.
     */
    public MboxMessage(MboxFolder folder, InternetHeaders hdrs, byte[] content,
				int msgno, String unix_from, boolean writable)
				throws MessagingException {
	super(folder, hdrs, content, msgno);
	setFlagsFromHeaders();
	origFlags = getFlags();
	this.unix_from = unix_from;
	this.writable = writable;
    }

    /**
     * Returns the "From" attribute. The "From" attribute contains
     * the identity of the person(s) who wished this message to 
     * be sent. <p>
     * 
     * If our superclass doesn't have a value, we return the address
     * from the UNIX From line.
     *
     * @return          array of Address objects
     * @exception       MessagingException
     */
    public Address[] getFrom() throws MessagingException {
	Address[] ret = super.getFrom();
	if (ret == null) {
	    InternetAddress ia = getUnixFrom();
	    if (ia != null)
		ret = new InternetAddress[] { ia };
	}
	return ret;
    }

    /**
     * Returns the address from the UNIX "From" line.
     *
     * @return          UNIX From address
     * @exception       MessagingException
     */
    public synchronized InternetAddress getUnixFrom()
				throws MessagingException {
	if (unix_from_user == null && unix_from != null) {
	    int i;
	    // find the space after the address, before the date
	    i = unix_from.indexOf(' ', 5);
	    if (i > 5) {
		try {
		    unix_from_user =
			new InternetAddress(unix_from.substring(5, i));
		} catch (AddressException e) {
		    // ignore it
		}
	    }
	}
	return unix_from_user != null ?
		(InternetAddress)unix_from_user.clone() : null;
    }

    /**
     * Get the date this message was received, from the UNIX From line.
     *
     * @return          the date this message was received
     * @exception       MessagingException
     */
    public Date getReceivedDate() throws MessagingException {
	if (rcvDate == null && unix_from != null) {
	    int i;
	    // find the space after the address, before the date
	    i = unix_from.indexOf(' ', 5);
	    if (i > 5) {
		try {
		    rcvDate = new Date(unix_from.substring(i));
		} catch (IllegalArgumentException iae) {
		    // ignore it
		}
	    }
	}
	return rcvDate == null ? null : new Date(rcvDate.getTime());
    }

    /**
     * Return the number of lines for the content of this message.
     * Return -1 if this number cannot be determined. <p>
     *
     * Note that this number may not be an exact measure of the 
     * content length and may or may not account for any transfer 
     * encoding of the content. <p>
     *
     * This implementation returns -1.
     *
     * @return          number of lines in the content.
     * @exception	MessagingException
     */  
     public int getLineCount() throws MessagingException {
	if (lineCount < 0 && isMimeType("text/plain")) {
	    LineCounter lc = new LineCounter(nullOutputStream);
	    // writeTo will set the SEEN flag, remember the original state
	    boolean seen = isSet(Flags.Flag.SEEN);
	    try {
		getDataHandler().writeTo(lc);
		lineCount = lc.getLineCount();
		lc.close();
	    } catch (IOException ex) {
		// ignore it, can't happen
	    }
	    if (!seen)
		setFlag(Flags.Flag.SEEN, false);
	}
	return lineCount;
     }

    /**
     * Set the specified flags on this message to the specified value.
     *
     * @param flags	the flags to be set
     * @param set	the value to be set
     */
    public void setFlags(Flags newFlags, boolean set)
				throws MessagingException {
	Flags oldFlags = (Flags)flags.clone();
	super.setFlags(newFlags, set);
	if (!flags.equals(oldFlags)) {
	    setHeadersFromFlags();
	    if (folder != null)
		((MboxFolder)folder).notifyMessageChangedListeners(
				MessageChangedEvent.FLAGS_CHANGED, this);
	}
    }

    /**
     * Return the content type, mapping from SunV3 types to MIME types
     * as necessary.
     */
    public String getContentType()  throws MessagingException {
	String ct = super.getContentType();
	if (ct.indexOf('/') < 0)
	    ct = SunV3BodyPart.MimeV3Map.toMime(ct);
	return ct;
    }

    /**
     * Produce the raw bytes of the content. This method is used during
     * parsing, to create a DataHandler object for the content. Subclasses
     * that can provide a separate input stream for just the message 
     * content might want to override this method. <p>
     *
     * This implementation just returns a ByteArrayInputStream constructed
     * out of the <code>content</code> byte array.
     *
     * @see #content
     */
    protected InputStream getContentStream() throws MessagingException {
	if (!isSet(Flags.Flag.SEEN))
	    setFlag(Flags.Flag.SEEN, true);
	return super.getContentStream();
    }

    /**                                                            
     * Return a DataHandler for this Message's content.
     * If this is a SunV3 multipart message, handle it specially.
     *
     * @exception	MessagingException
     */
    public synchronized DataHandler getDataHandler() 
		throws MessagingException {
	if (dh == null) {
	    // XXX - Following is a kludge to avoid having to register
	    // the "multipart/x-sun-attachment" data type with the JAF.
	    String ct = getContentType();
	    if (ct.equalsIgnoreCase("multipart/x-sun-attachment"))
		dh = new DataHandler(
		    new SunV3Multipart(new MimePartDataSource(this)), ct);
	    else
		return super.getDataHandler();	// will set "dh"
	}
	return dh;
    }

    // XXX - We assume that only body parts that are part of a SunV3
    // multipart will use the SunV3 headers (X-Sun-Content-Length,
    // X-Sun-Content-Lines, X-Sun-Data-Type, X-Sun-Encoding-Info,
    // X-Sun-Data-Description, X-Sun-Data-Name) so we don't handle
    // them here.

    // here only to allow package private access from MboxFolder
    protected void setMessageNumber(int msgno) {
	super.setMessageNumber(msgno);
    }

    /**
     * Set the flags for this message based on the Status,
     * X-Status, and X-Dt-Delete-Time headers.
     *
     * SIMS 2.0:
     * "X-Status: DFAT", deleted, flagged, answered, draft.
     * Unset flags represented as "$".
     * User flags not supported.
     *
     * University of Washington IMAP server:
     * "X-Status: DFAT", deleted, flagged, answered, draft.
     * Unset flags not present.
     * "X-Keywords: userflag1 userflag2"
     */
    private synchronized void setFlagsFromHeaders() {
	flags = new Flags(Flags.Flag.RECENT);
	try {
	    String s = getHeader("Status", null);
	    if (s != null) {
		if (s.indexOf('R') >= 0)
		    flags.add(Flags.Flag.SEEN);
		if (s.indexOf('O') >= 0)
		    flags.remove(Flags.Flag.RECENT);
	    }
	    s = getHeader("X-Dt-Delete-Time", null);	// set by dtmail
	    if (s != null)
		flags.add(Flags.Flag.DELETED);
	    s = getHeader("X-Status", null);		// set by IMAP server
	    if (s != null) {
		if (s.indexOf('D') >= 0)
		    flags.add(Flags.Flag.DELETED);
		if (s.indexOf('F') >= 0)
		    flags.add(Flags.Flag.FLAGGED);
		if (s.indexOf('A') >= 0)
		    flags.add(Flags.Flag.ANSWERED);
		if (s.indexOf('T') >= 0)
		    flags.add(Flags.Flag.DRAFT);
	    }
	    s = getHeader("X-Keywords", null);		// set by IMAP server
	    if (s != null) {
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens())
		    flags.add(st.nextToken());
	    }
	} catch (MessagingException e) {
	    // ignore it
	}
    }

    /**
     * Set the various header fields that represent the message flags.
     */
    private synchronized void setHeadersFromFlags() {
	try {
	    StringBuffer status = new StringBuffer();
	    if (flags.contains(Flags.Flag.SEEN))
		status.append('R');
	    if (!flags.contains(Flags.Flag.RECENT))
		status.append('O');
	    setHeader("Status", status.toString());

	    boolean sims = false;
	    String s = getHeader("X-Status", null);
	    // is it a SIMS 2.0 format X-Status header?
	    sims = s != null && s.length() == 4 && s.indexOf('$') >= 0;
	    status.setLength(0);
	    if (flags.contains(Flags.Flag.DELETED))
		status.append('D');
	    else if (sims)
		status.append('$');
	    if (flags.contains(Flags.Flag.FLAGGED))
		status.append('F');
	    else if (sims)
		status.append('$');
	    if (flags.contains(Flags.Flag.ANSWERED))
		status.append('A');
	    else if (sims)
		status.append('$');
	    if (flags.contains(Flags.Flag.DRAFT))
		status.append('T');
	    else if (sims)
		status.append('$');
	    setHeader("X-Status", status.toString());

	    String[] userFlags = flags.getUserFlags();
	    if (userFlags.length > 0) {
		status.setLength(0);
		for (int i = 0; i < userFlags.length; i++)
		    status.append(userFlags[i]).append(' ');
		status.setLength(status.length() - 1);	// smash trailing space
		setHeader("X-Keywords", status.toString());
	    }
	    if (flags.contains(Flags.Flag.DELETED)) {
		s = getHeader("X-Dt-Delete-Time", null);
		if (s == null)
		    // XXX - should be time
		    setHeader("X-Dt-Delete-Time", "1");
	    }
	} catch (MessagingException e) {
	    // ignore it
	}
    }

    protected void updateHeaders() throws MessagingException {
	super.updateHeaders();
	setHeadersFromFlags();
    }

    /**
     * Save any changes made to this message.
     */
    public void saveChanges() throws MessagingException {
	if (!writable)
	    throw new MessagingException("Message is read-only");

	super.saveChanges();

	try {
	    /*
	     * Count the size of the body, in order to set the Content-Length
	     * header.  (Should we only do this to update an existing
	     * Content-Length header?)
	     * XXX - We could cache the content bytes here, for use later
	     * in writeTo.
	     */
	    ContentLengthCounter cos = new ContentLengthCounter();
	    OutputStream os = new NewlineOutputStream(cos);
	    super.writeTo(os);
	    os.flush();
	    setHeader("Content-Length", String.valueOf(cos.getSize()));
	    // setContentSize((int)cos.getSize());
	} catch (MessagingException e) {
	    throw e;
	} catch (Exception e) {
	    throw new MessagingException("unexpected exception " + e);
	}
    }

    /**
     * Expose modified flag to MboxFolder.
     */
    boolean isModified() {
	return modified;
    }

    /**
     * Put out a byte stream suitable for saving to a file.
     * XXX - ultimately implement "ignore headers" here?
     */
    public void writeToFile(OutputStream os) throws IOException {
	try {
	    if (getHeader("Content-Length") == null) {
		/*
		 * Count the size of the body, in order to set the
		 * Content-Length header.
		 */
		ContentLengthCounter cos = new ContentLengthCounter();
		OutputStream oos = new NewlineOutputStream(cos);
		super.writeTo(oos);
		oos.flush();
		setHeader("Content-Length", String.valueOf(cos.getSize()));
		// setContentSize((int)cos.getSize());
	    }

	    os = new NewlineOutputStream(os);
	    PrintStream pos = new PrintStream(os);

	    pos.println(unix_from);
	    super.writeTo(pos);
	    pos.println();	// make sure there's a blank line at the end
	} catch (MessagingException e) {
	    throw new IOException("unexpected exception " + e);
	}
    }
}
