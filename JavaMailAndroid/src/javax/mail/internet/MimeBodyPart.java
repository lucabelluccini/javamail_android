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
 * @(#)MimeBodyPart.java	1.67 07/05/04
 */

package javax.mail.internet;

import javax.mail.*;
import javax.activation.*;
import java.io.*;
import java.util.*;
import com.sun.mail.util.*;

/**
 * This class represents a MIME body part. It implements the 
 * <code>BodyPart</code> abstract class and the <code>MimePart</code>
 * interface. MimeBodyParts are contained in <code>MimeMultipart</code>
 * objects. <p>
 *
 * MimeBodyPart uses the <code>InternetHeaders</code> class to parse
 * and store the headers of that body part. <p>
 *
 * <hr><strong>A note on RFC 822 and MIME headers</strong><p>
 *
 * RFC 822 header fields <strong>must</strong> contain only
 * US-ASCII characters. MIME allows non ASCII characters to be present
 * in certain portions of certain headers, by encoding those characters.
 * RFC 2047 specifies the rules for doing this. The MimeUtility
 * class provided in this package can be used to to achieve this.
 * Callers of the <code>setHeader</code>, <code>addHeader</code>, and
 * <code>addHeaderLine</code> methods are responsible for enforcing
 * the MIME requirements for the specified headers.  In addition, these
 * header fields must be folded (wrapped) before being sent if they
 * exceed the line length limitation for the transport (1000 bytes for
 * SMTP).  Received headers may have been folded.  The application is
 * responsible for folding and unfolding headers as appropriate. <p>
 *
 * @author John Mani
 * @author Bill Shannon
 * @author Kanwar Oberoi
 * @see javax.mail.Part
 * @see javax.mail.internet.MimePart
 * @see javax.mail.internet.MimeUtility
 */

public class MimeBodyPart extends BodyPart implements MimePart {

    // Paranoia:
    // allow this last minute change to be disabled if it causes problems
    private static boolean setDefaultTextCharset = true;

    private static boolean setContentTypeFileName = true;

    private static boolean encodeFileName = false;
    private static boolean decodeFileName = false;

    // Paranoia:
    // allow this last minute change to be disabled if it causes problems
    static boolean cacheMultipart = true;	// accessed by MimeMessage

    static {
	try {
	    String s = System.getProperty("mail.mime.setdefaulttextcharset");
	    // default to true
	    setDefaultTextCharset = s == null || !s.equalsIgnoreCase("false");

	    s = System.getProperty("mail.mime.setcontenttypefilename");
	    // default to true
	    setContentTypeFileName = s == null || !s.equalsIgnoreCase("false");

	    s = System.getProperty("mail.mime.encodefilename");
	    // default to false
	    encodeFileName = s != null && !s.equalsIgnoreCase("false");

	    s = System.getProperty("mail.mime.decodefilename");
	    // default to false
	    decodeFileName = s != null && !s.equalsIgnoreCase("false");

	    s = System.getProperty("mail.mime.cachemultipart");
	    // default to true
	    cacheMultipart = s == null || !s.equalsIgnoreCase("false");
	} catch (SecurityException sex) {
	    // ignore it
	}
    }
   
    /**
     * The DataHandler object representing this Part's content.
     */
    protected DataHandler dh;

    /**
     * Byte array that holds the bytes of the content of this Part.
     */
    protected byte[] content;

    /**
     * If the data for this body part was supplied by an
     * InputStream that implements the SharedInputStream interface,
     * <code>contentStream</code> is another such stream representing
     * the content of this body part.  In this case, <code>content</code>
     * will be null.
     *
     * @since	JavaMail 1.2
     */
    protected InputStream contentStream;

    /**
     * The InternetHeaders object that stores all the headers
     * of this body part.
     */
    protected InternetHeaders headers;

    /**
     * If our content is a Multipart of Message object, we save it
     * the first time it's created by parsing a stream so that changes
     * to the contained objects will not be lost.
     */
    private Object cachedContent;

    /**
     * An empty MimeBodyPart object is created.
     * This body part maybe filled in by a client constructing a multipart
     * message.
     */
    public MimeBodyPart() {
	super();
	headers = new InternetHeaders();
    }

    /**
     * Constructs a MimeBodyPart by reading and parsing the data from
     * the specified input stream. The parser consumes data till the end
     * of the given input stream.  The input stream must start at the
     * beginning of a valid MIME body part and must terminate at the end
     * of that body part. <p>
     *
     * Note that the "boundary" string that delimits body parts must 
     * <strong>not</strong> be included in the input stream. The intention 
     * is that the MimeMultipart parser will extract each body part's bytes
     * from a multipart stream and feed them into this constructor, without 
     * the delimiter strings.
     *
     * @param	is	the body part Input Stream
     */
    public MimeBodyPart(InputStream is) throws MessagingException {
	if (!(is instanceof ByteArrayInputStream) &&
	    !(is instanceof BufferedInputStream) &&
	    !(is instanceof SharedInputStream))
	    is = new BufferedInputStream(is);
	
	headers = new InternetHeaders(is);

	if (is instanceof SharedInputStream) {
	    SharedInputStream sis = (SharedInputStream)is;
	    contentStream = sis.newStream(sis.getPosition(), -1);
	} else {
	    try {
		content = ASCIIUtility.getBytes(is);
	    } catch (IOException ioex) {
		throw new MessagingException("Error reading input stream", ioex);
	    }
	}

    }

    /**
     * Constructs a MimeBodyPart using the given header and
     * content bytes. <p>
     *
     * Used by providers.
     *
     * @param	headers	The header of this part
     * @param	content	bytes representing the body of this part.
     */
    public MimeBodyPart(InternetHeaders headers, byte[] content) 
			throws MessagingException {
	super();
	this.headers = headers;
	this.content = content;
    }

    /**
     * Return the size of the content of this body part in bytes.
     * Return -1 if the size cannot be determined. <p>
     *
     * Note that this number may not be an exact measure of the
     * content size and may or may not account for any transfer
     * encoding of the content. <p>
     *
     * This implementation returns the size of the <code>content</code>
     * array (if not null), or, if <code>contentStream</code> is not
     * null, and the <code>available</code> method returns a positive
     * number, it returns that number as the size.  Otherwise, it returns
     * -1.
     *
     * @return size in bytes, or -1 if not known
     */
    public int getSize() throws MessagingException {
	if (content != null)
	    return content.length;
	if (contentStream != null) {
	    try {
		int size = contentStream.available();
		// only believe the size if it's greate than zero, since zero
		// is the default returned by the InputStream class itself
		if (size > 0)
		    return size;
	    } catch (IOException ex) {
		// ignore it
	    }
	}
	return -1;
    }

    /**
     * Return the number of lines for the content of this Part.
     * Return -1 if this number cannot be determined. <p>
     *
     * Note that this number may not be an exact measure of the 
     * content length and may or may not account for any transfer 
     * encoding of the content. <p>
     *
     * This implementation returns -1.
     *
     * @return number of lines, or -1 if not known
     */  
     public int getLineCount() throws MessagingException {
	return -1;
     }

    /**
     * Returns the value of the RFC 822 "Content-Type" header field.
     * This represents the content type of the content of this
     * body part. This value must not be null. If this field is
     * unavailable, "text/plain" should be returned. <p>
     *
     * This implementation uses <code>getHeader(name)</code>
     * to obtain the requisite header field.
     *
     * @return	Content-Type of this body part
     */
    public String getContentType() throws MessagingException {
	String s = getHeader("Content-Type", null);
	if (s == null)
	    s = "text/plain";
	
	return s;
    }

    /**
     * Is this Part of the specified MIME type?  This method
     * compares <strong>only the <code>primaryType</code> and 
     * <code>subType</code></strong>.
     * The parameters of the content types are ignored. <p>
     *
     * For example, this method will return <code>true</code> when
     * comparing a Part of content type <strong>"text/plain"</strong>
     * with <strong>"text/plain; charset=foobar"</strong>. <p>
     *
     * If the <code>subType</code> of <code>mimeType</code> is the
     * special character '*', then the subtype is ignored during the
     * comparison.
     */
    public boolean isMimeType(String mimeType) throws MessagingException {
	return isMimeType(this, mimeType);
    }

    /**
     * Returns the value of the "Content-Disposition" header field.
     * This represents the disposition of this part. The disposition
     * describes how the part should be presented to the user. <p>
     *
     * If the Content-Disposition field is unavailable,
     * null is returned. <p>
     *
     * This implementation uses <code>getHeader(name)</code>
     * to obtain the requisite header field.
     *
     * @see #headers
     */
    public String getDisposition() throws MessagingException {
	return getDisposition(this);
    }

    /**
     * Set the "Content-Disposition" header field of this body part.
     * If the disposition is null, any existing "Content-Disposition"
     * header field is removed.
     *
     * @exception	IllegalWriteException if the underlying
     *			implementation does not support modification
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     */
    public void setDisposition(String disposition) throws MessagingException {
	setDisposition(this, disposition);
    }

    /**
     * Returns the content transfer encoding from the
     * "Content-Transfer-Encoding" header
     * field. Returns <code>null</code> if the header is unavailable
     * or its value is absent. <p>
     *
     * This implementation uses <code>getHeader(name)</code>
     * to obtain the requisite header field.
     *
     * @see #headers
     */
    public String getEncoding() throws MessagingException {
	return getEncoding(this);
    }

    /**
     * Returns the value of the "Content-ID" header field. Returns
     * <code>null</code> if the field is unavailable or its value is 
     * absent. <p>
     *
     * This implementation uses <code>getHeader(name)</code>
     * to obtain the requisite header field.
     */
    public String getContentID() throws MessagingException {
	return getHeader("Content-Id", null);
    }

    /**
     * Set the "Content-ID" header field of this body part.
     * If the <code>cid</code> parameter is null, any existing 
     * "Content-ID" is removed.
     *
     * @exception	IllegalWriteException if the underlying
     *			implementation does not support modification
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     * @exception	MessagingException
     * @since		JavaMail 1.3
     */
    public void setContentID(String cid) throws MessagingException {
	if (cid == null)
	    removeHeader("Content-ID");
	else
	    setHeader("Content-ID", cid);
    }

    /**
     * Return the value of the "Content-MD5" header field. Returns 
     * <code>null</code> if this field is unavailable or its value
     * is absent. <p>
     *
     * This implementation uses <code>getHeader(name)</code>
     * to obtain the requisite header field.
     */
    public String getContentMD5() throws MessagingException {
	return getHeader("Content-MD5", null);
    }

    /**
     * Set the "Content-MD5" header field of this body part.
     *
     * @exception	IllegalWriteException if the underlying
     *			implementation does not support modification
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     */
    public void setContentMD5(String md5) throws MessagingException {
	setHeader("Content-MD5", md5);
    }

    /**
     * Get the languages specified in the Content-Language header
     * of this MimePart. The Content-Language header is defined by
     * RFC 1766. Returns <code>null</code> if this header is not
     * available or its value is absent. <p>
     *
     * This implementation uses <code>getHeader(name)</code>
     * to obtain the requisite header field.
     */
    public String[] getContentLanguage() throws MessagingException {
	return getContentLanguage(this);
    }

    /**
     * Set the Content-Language header of this MimePart. The
     * Content-Language header is defined by RFC 1766.
     *
     * @param languages 	array of language tags
     */
    public void setContentLanguage(String[] languages)
			throws MessagingException {
	setContentLanguage(this, languages);
    }

    /**
     * Returns the "Content-Description" header field of this body part.
     * This typically associates some descriptive information with 
     * this part. Returns null if this field is unavailable or its
     * value is absent. <p>
     *
     * If the Content-Description field is encoded as per RFC 2047,
     * it is decoded and converted into Unicode. If the decoding or 
     * conversion fails, the raw data is returned as is. <p>
     *
     * This implementation uses <code>getHeader(name)</code>
     * to obtain the requisite header field.
     * 
     * @return	content description
     */
    public String getDescription() throws MessagingException {
	return getDescription(this);
    }

    /**
     * Set the "Content-Description" header field for this body part.
     * If the description parameter is <code>null</code>, then any 
     * existing "Content-Description" fields are removed. <p>
     *
     * If the description contains non US-ASCII characters, it will 
     * be encoded using the platform's default charset. If the 
     * description contains only US-ASCII characters, no encoding 
     * is done and it is used as is. <p>
     *
     * Note that if the charset encoding process fails, a
     * MessagingException is thrown, and an UnsupportedEncodingException
     * is included in the chain of nested exceptions within the
     * MessagingException.
     * 
     * @param description content description
     * @exception	IllegalWriteException if the underlying
     *			implementation does not support modification
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     * @exception       MessagingException otherwise; an
     *                  UnsupportedEncodingException may be included
     *                  in the exception chain if the charset
     *                  conversion fails.
     */
    public void setDescription(String description) throws MessagingException {
	setDescription(description, null);
    }

    /**
     * Set the "Content-Description" header field for this body part.
     * If the description parameter is <code>null</code>, then any 
     * existing "Content-Description" fields are removed. <p>
     *
     * If the description contains non US-ASCII characters, it will 
     * be encoded using the specified charset. If the description 
     * contains only US-ASCII characters, no encoding  is done and 
     * it is used as is. <p>
     *
     * Note that if the charset encoding process fails, a
     * MessagingException is thrown, and an UnsupportedEncodingException
     * is included in the chain of nested exceptions within the
     * MessagingException.
     *
     * @param	description	Description
     * @param	charset		Charset for encoding
     * @exception	IllegalWriteException if the underlying
     *			implementation does not support modification
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     * @exception       MessagingException otherwise; an
     *                  UnsupportedEncodingException may be included
     *                  in the exception chain if the charset
     *                  conversion fails.
     */
    public void setDescription(String description, String charset) 
		throws MessagingException {
	setDescription(this, description, charset);
    }

    /**
     * Get the filename associated with this body part. <p>
     *
     * Returns the value of the "filename" parameter from the
     * "Content-Disposition" header field of this body part. If its
     * not available, returns the value of the "name" parameter from
     * the "Content-Type" header field of this body part.
     * Returns <code>null</code> if both are absent. <p>
     *
     * If the <code>mail.mime.encodefilename</code> System property
     * is set to true, the {@link MimeUtility#decodeText
     * MimeUtility.decodeText} method will be used to decode the
     * filename.  While such encoding is not supported by the MIME
     * spec, many mailers use this technique to support non-ASCII
     * characters in filenames.  The default value of this property
     * is false.
     *
     * @return	filename
     */
    public String getFileName() throws MessagingException {
	return getFileName(this);
    }

    /**
     * Set the filename associated with this body part, if possible. <p>
     *
     * Sets the "filename" parameter of the "Content-Disposition"
     * header field of this body part.  For compatibility with older
     * mailers, the "name" parameter of the "Content-Type" header is
     * also set. <p>
     *
     * If the <code>mail.mime.encodefilename</code> System property
     * is set to true, the {@link MimeUtility#encodeText
     * MimeUtility.encodeText} method will be used to encode the
     * filename.  While such encoding is not supported by the MIME
     * spec, many mailers use this technique to support non-ASCII
     * characters in filenames.  The default value of this property
     * is false.
     *
     * @exception	IllegalWriteException if the underlying
     *			implementation does not support modification
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     */
    public void setFileName(String filename) throws MessagingException {
	setFileName(this, filename);
    }

    /**
     * Return a decoded input stream for this body part's "content". <p>
     *
     * This implementation obtains the input stream from the DataHandler.
     * That is, it invokes getDataHandler().getInputStream();
     *
     * @return 		an InputStream
     * @exception	MessagingException
     * @exception       IOException this is typically thrown by the
     *			DataHandler. Refer to the documentation for
     *			javax.activation.DataHandler for more details.
     *
     * @see	#getContentStream
     * @see 	javax.activation.DataHandler#getInputStream
     */
    public InputStream getInputStream() 
		throws IOException, MessagingException {
	return getDataHandler().getInputStream();
    }

   /**
     * Produce the raw bytes of the content. This method is used
     * when creating a DataHandler object for the content. Subclasses
     * that can provide a separate input stream for just the Part
     * content might want to override this method. <p>
     * 
     * @see #content
     * @see MimeMessage#getContentStream
     */
    protected InputStream getContentStream() throws MessagingException {
	if (contentStream != null)
	    return ((SharedInputStream)contentStream).newStream(0, -1);
	if (content != null)
	    return new ByteArrayInputStream(content);
	
	throw new MessagingException("No content");
    }

    /**
     * Return an InputStream to the raw data with any Content-Transfer-Encoding
     * intact.  This method is useful if the "Content-Transfer-Encoding"
     * header is incorrect or corrupt, which would prevent the
     * <code>getInputStream</code> method or <code>getContent</code> method
     * from returning the correct data.  In such a case the application may
     * use this method and attempt to decode the raw data itself. <p>
     *
     * This implementation simply calls the <code>getContentStream</code>
     * method.
     *
     * @see	#getInputStream
     * @see	#getContentStream
     * @since	JavaMail 1.2
     */
    public InputStream getRawInputStream() throws MessagingException {
	return getContentStream();
    }

    /**
     * Return a DataHandler for this body part's content. <p>
     *
     * The implementation provided here works just like the
     * the implementation in MimeMessage.
     * @see	MimeMessage#getDataHandler
     */  
    public DataHandler getDataHandler() throws MessagingException {
	if (dh == null)
	    dh = new DataHandler(new MimePartDataSource(this));
	return dh;
    }

    /**
     * Return the content as a Java object. The type of the object
     * returned is of course dependent on the content itself. For 
     * example, the native format of a text/plain content is usually
     * a String object. The native format for a "multipart"
     * content is always a Multipart subclass. For content types that are
     * unknown to the DataHandler system, an input stream is returned
     * as the content. <p>
     *
     * This implementation obtains the content from the DataHandler.
     * That is, it invokes getDataHandler().getContent();
     * If the content is a Multipart or Message object and was created by
     * parsing a stream, the object is cached and returned in subsequent
     * calls so that modifications to the content will not be lost.
     *
     * @return          Object
     * @exception       MessagingException
     * @exception       IOException this is typically thrown by the
     *			DataHandler. Refer to the documentation for
     *			javax.activation.DataHandler for more details.
     */  
    public Object getContent() throws IOException, MessagingException {
	if (cachedContent != null)
	    return cachedContent;
	Object c;
	try {
	    c = getDataHandler().getContent();
	} catch (FolderClosedIOException fex) {
	    throw new FolderClosedException(fex.getFolder(), fex.getMessage());
	} catch (MessageRemovedIOException mex) {
	    throw new MessageRemovedException(mex.getMessage());
	}
	if (cacheMultipart &&
		(c instanceof Multipart || c instanceof Message) &&
		(content != null || contentStream != null)) {
	    cachedContent = c;
	}
	return c;
    }

    /**
     * This method provides the mechanism to set this body part's content.
     * The given DataHandler object should wrap the actual content.
     * 
     * @param   dh      The DataHandler for the content
     * @exception       IllegalWriteException if the underlying
     * 			implementation does not support modification
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     */                 
    public void setDataHandler(DataHandler dh) 
		throws MessagingException {
	this.dh = dh;
	cachedContent = null;
	MimeBodyPart.invalidateContentHeaders(this);
    }

    /**
     * A convenience method for setting this body part's content. <p>
     *
     * The content is wrapped in a DataHandler object. Note that a
     * DataContentHandler class for the specified type should be
     * available to the JavaMail implementation for this to work right.
     * That is, to do <code>setContent(foobar, "application/x-foobar")</code>,
     * a DataContentHandler for "application/x-foobar" should be installed.
     * Refer to the Java Activation Framework for more information.
     *
     * @param	o	the content object
     * @param	type	Mime type of the object
     * @exception       IllegalWriteException if the underlying
     *			implementation does not support modification of
     *			existing values
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     */
    public void setContent(Object o, String type) 
		throws MessagingException {
	if (o instanceof Multipart) {
	    setContent((Multipart)o);
	} else {
	    setDataHandler(new DataHandler(o, type));
	}
    }

    /**
     * Convenience method that sets the given String as this
     * part's content, with a MIME type of "text/plain". If the
     * string contains non US-ASCII characters, it will be encoded
     * using the platform's default charset. The charset is also
     * used to set the "charset" parameter. <p>
     *
     * Note that there may be a performance penalty if
     * <code>text</code> is large, since this method may have
     * to scan all the characters to determine what charset to
     * use. <p>
     *
     * If the charset is already known, use the
     * <code>setText</code> method that takes the charset parameter.
     *
     * @param	text	the text content to set
     * @exception	MessagingException	if an error occurs
     * @see	#setText(String text, String charset)
     */
    public void setText(String text) throws MessagingException {
	setText(text, null);
    }

    /**
     * Convenience method that sets the given String as this part's
     * content, with a MIME type of "text/plain" and the specified
     * charset. The given Unicode string will be charset-encoded
     * using the specified charset. The charset is also used to set
     * the "charset" parameter.
     *
     * @param	text	the text content to set
     * @param	charset	the charset to use for the text
     * @exception	MessagingException	if an error occurs
     */
    public void setText(String text, String charset)
			throws MessagingException {
	setText(this, text, charset, "plain");
    }

    /**
     * Convenience method that sets the given String as this part's
     * content, with a primary MIME type of "text" and the specified
     * MIME subtype.  The given Unicode string will be charset-encoded
     * using the specified charset. The charset is also used to set
     * the "charset" parameter.
     *
     * @param	text	the text content to set
     * @param	charset	the charset to use for the text
     * @param	subtype	the MIME subtype to use (e.g., "html")
     * @exception	MessagingException	if an error occurs
     * @since	JavaMail 1.4
     */
    public void setText(String text, String charset, String subtype)
                        throws MessagingException {
	setText(this, text, charset, subtype);
    }
 
    /**
     * This method sets the body part's content to a Multipart object.
     *
     * @param  mp      	The multipart object that is the Message's content
     * @exception       IllegalWriteException if the underlying
     *			implementation does not support modification of
     *			existing values.
     * @exception	IllegalStateException if this body part is
     *			obtained from a READ_ONLY folder.
     */
    public void setContent(Multipart mp) throws MessagingException {
	setDataHandler(new DataHandler(mp, mp.getContentType()));
	mp.setParent(this);
    }

    /**
     * Use the specified file to provide the data for this part.
     * The simple file name is used as the file name for this
     * part and the data in the file is used as the data for this
     * part.  The encoding will be chosen appropriately for the
     * file data.
     *
     * @param		file		the File object to attach
     * @exception	IOException	errors related to accessing the file
     * @exception	MessagingException	message related errors
     * @since		JavaMail 1.4
     */
    public void attachFile(File file) throws IOException, MessagingException {
    	FileDataSource fds = new FileDataSource(file);   	
        this.setDataHandler(new DataHandler(fds));
        this.setFileName(fds.getName());
    }

    /**
     * Use the specified file to provide the data for this part.
     * The simple file name is used as the file name for this
     * part and the data in the file is used as the data for this
     * part.  The encoding will be chosen appropriately for the
     * file data.
     *
     * @param		file		the name of the file to attach
     * @exception	IOException	errors related to accessing the file
     * @exception	MessagingException	message related errors
     * @since		JavaMail 1.4
     */
    public void attachFile(String file) throws IOException, MessagingException {
    	File f = new File(file);
    	attachFile(f);
    }

    /**
     * Save the contents of this part in the specified file.  The content
     * is decoded and saved, without any of the MIME headers.
     *
     * @param		file		the File object to write to
     * @exception	IOException	errors related to accessing the file
     * @exception	MessagingException	message related errors
     * @since		JavaMail 1.4
     */
    public void saveFile(File file) throws IOException, MessagingException {
    	OutputStream out = null;
        InputStream in = null;
        try {
	    out = new BufferedOutputStream(new FileOutputStream(file));
	    in = this.getInputStream();
	    byte[] buf = new byte[8192];
	    int len;
	    while ((len = in.read(buf)) > 0)
		out.write(buf, 0, len); 
        } finally {
	    // close streams, but don't mask original exception, if any
	    try {
		if (in != null)
		    in.close();
	    } catch (IOException ex) { }
	    try {
		if (out != null)
		    out.close();
	    } catch (IOException ex) { }
        }
    }

    /**
     * Save the contents of this part in the specified file.  The content
     * is decoded and saved, without any of the MIME headers.
     *
     * @param		file		the name of the file to write to
     * @exception	IOException	errors related to accessing the file
     * @exception	MessagingException	message related errors
     * @since		JavaMail 1.4
     */
    public void saveFile(String file) throws IOException, MessagingException {
    	File f = new File(file);
    	saveFile(f);
    }

    /**
     * Output the body part as an RFC 822 format stream.
     *
     * @exception MessagingException
     * @exception IOException	if an error occurs writing to the
     *				stream or if an error is generated
     *				by the javax.activation layer.
     * @see javax.activation.DataHandler#writeTo
     */
    public void writeTo(OutputStream os)
				throws IOException, MessagingException {
	writeTo(this, os, null);
    }

    /**
     * Get all the headers for this header_name. Note that certain
     * headers may be encoded as per RFC 2047 if they contain
     * non US-ASCII characters and these should be decoded.
     *
     * @param   name    name of header
     * @return  array of headers
     * @see     javax.mail.internet.MimeUtility
     */  
    public String[] getHeader(String name) throws MessagingException {
	return headers.getHeader(name);
    }

    /**
     * Get all the headers for this header name, returned as a single
     * String, with headers separated by the delimiter. If the
     * delimiter is <code>null</code>, only the first header is 
     * returned.
     *
     * @param name		the name of this header
     * @param delimiter		delimiter between fields in returned string
     * @return			the value fields for all headers with 
     *				this name
     * @exception       	MessagingException
     */
    public String getHeader(String name, String delimiter)
				throws MessagingException {
	return headers.getHeader(name, delimiter);
    }

    /**
     * Set the value for this header_name. Replaces all existing
     * header values with this new value. Note that RFC 822 headers
     * must contain only US-ASCII characters, so a header that
     * contains non US-ASCII characters must be encoded as per the
     * rules of RFC 2047.
     *
     * @param   name    header name
     * @param   value   header value
     * @see     javax.mail.internet.MimeUtility
     */
    public void setHeader(String name, String value)
                                throws MessagingException {
	headers.setHeader(name, value);
    }
 
    /**
     * Add this value to the existing values for this header_name.
     * Note that RFC 822 headers must contain only US-ASCII
     * characters, so a header that contains non US-ASCII characters
     * must be encoded as per the rules of RFC 2047.
     *
     * @param   name    header name
     * @param   value   header value
     * @see     javax.mail.internet.MimeUtility
     */
    public void addHeader(String name, String value)
                                throws MessagingException {
	headers.addHeader(name, value);    
    }

    /**
     * Remove all headers with this name.
     */
    public void removeHeader(String name) throws MessagingException {
	headers.removeHeader(name);
    }
 
    /**
     * Return all the headers from this Message as an Enumeration of
     * Header objects.
     */
    public Enumeration getAllHeaders() throws MessagingException {
	return headers.getAllHeaders();
    }
   
    /**
     * Return matching headers from this Message as an Enumeration of
     * Header objects. <p>
     */
    public Enumeration getMatchingHeaders(String[] names)
                        throws MessagingException {
	return headers.getMatchingHeaders(names);
    }
 
    /**
     * Return non-matching headers from this Message as an
     * Enumeration of Header objects.
     */
    public Enumeration getNonMatchingHeaders(String[] names)
                        throws MessagingException {
	return headers.getNonMatchingHeaders(names);
    }
      
    /**
     * Add a header line to this body part
     */
    public void addHeaderLine(String line) throws MessagingException {
	headers.addHeaderLine(line);
    }
     
    /**
     * Get all header lines as an Enumeration of Strings. A Header
     * line is a raw RFC 822 header line, containing both the "name"
     * and "value" field.
     */
    public Enumeration getAllHeaderLines() throws MessagingException {
  	return headers.getAllHeaderLines(); 
    }
 
    /**
     * Get matching header lines as an Enumeration of Strings.
     * A Header line is a raw RFC 822 header line, containing both
     * the "name" and "value" field.
     */
    public Enumeration getMatchingHeaderLines(String[] names)
                                    throws MessagingException {
	return headers.getMatchingHeaderLines(names);
    }
 
    /**
     * Get non-matching header lines as an Enumeration of Strings.
     * A Header line is a raw RFC 822 header line, containing both
     * the "name"  and "value" field.
     */
    public Enumeration getNonMatchingHeaderLines(String[] names)  
                                        throws MessagingException {
	return headers.getNonMatchingHeaderLines(names);
    }

    /**
     * Examine the content of this body part and update the appropriate
     * MIME headers.  Typical headers that get set here are
     * <code>Content-Type</code> and <code>Content-Transfer-Encoding</code>.
     * Headers might need to be updated in two cases:
     *
     * <br>
     * - A message being crafted by a mail application will certainly
     * need to activate this method at some point to fill up its internal
     * headers.
     *
     * <br>
     * - A message read in from a Store will have obtained
     * all its headers from the store, and so doesn't need this.
     * However, if this message is editable and if any edits have
     * been made to either the content or message structure, we might
     * need to resync our headers.
     *
     * <br>
     * In both cases this method is typically called by the
     * <code>Message.saveChanges</code> method.
     */
    protected void updateHeaders() throws MessagingException {
	updateHeaders(this);
	/*
	 * If we've cached a Multipart or Message object then
	 * we're now committed to using this instance of the
	 * object and we discard any stream data used to create
	 * this object.
	 */
	if (cachedContent != null) {
	    dh = new DataHandler(cachedContent, getContentType());
	    cachedContent = null;
	    content = null;
	    if (contentStream != null) {
		try {
		    contentStream.close();
		} catch (IOException ioex) { }	// nothing to do
	    }
	    contentStream = null;
	}
    }

    /////////////////////////////////////////////////////////////
    // Package private convenience methods to share code among //
    // MimeMessage and MimeBodyPart                            //
    /////////////////////////////////////////////////////////////

    static boolean isMimeType(MimePart part, String mimeType)
				throws MessagingException {
	// XXX - lots of room for optimization here!
	try {
	    ContentType ct = new ContentType(part.getContentType());
	    return ct.match(mimeType);
	} catch (ParseException ex) {
	    return part.getContentType().equalsIgnoreCase(mimeType);
	}
    }

    static void setText(MimePart part, String text, String charset,
			String subtype) throws MessagingException {
	if (charset == null) {
	    if (MimeUtility.checkAscii(text) != MimeUtility.ALL_ASCII)
		charset = MimeUtility.getDefaultMIMECharset();
	    else
		charset = "us-ascii";
	}
	// XXX - should at least ensure that subtype is an atom
	part.setContent(text, "text/" + subtype + "; charset=" +
			MimeUtility.quote(charset, HeaderTokenizer.MIME));
    }

    static String getDisposition(MimePart part) throws MessagingException {
	String s = part.getHeader("Content-Disposition", null);

	if (s == null)
	    return null;

	ContentDisposition cd = new ContentDisposition(s);
	return cd.getDisposition();
    }

    static void setDisposition(MimePart part, String disposition)
			throws MessagingException {
	if (disposition == null)
	    part.removeHeader("Content-Disposition");
	else {
	    String s = part.getHeader("Content-Disposition", null);
	    if (s != null) { 
		/* A Content-Disposition header already exists ..
		 *
		 * Override disposition, but attempt to retain 
		 * existing disposition parameters
		 */
		ContentDisposition cd = new ContentDisposition(s);
		cd.setDisposition(disposition);
		disposition = cd.toString();
	    }
	    part.setHeader("Content-Disposition", disposition);
	}
    }

    static String getDescription(MimePart part) 
			throws MessagingException {
	String rawvalue = part.getHeader("Content-Description", null);

	if (rawvalue == null)
	    return null;

	try {
	    return MimeUtility.decodeText(MimeUtility.unfold(rawvalue));
	} catch (UnsupportedEncodingException ex) {
	    return rawvalue;
	}
    }

    static void 
    setDescription(MimePart part, String description, String charset) 
			throws MessagingException {
	if (description == null) {
	    part.removeHeader("Content-Description");
	    return;
	}
	
	try {
	    part.setHeader("Content-Description", MimeUtility.fold(21,
		MimeUtility.encodeText(description, charset, null)));
	} catch (UnsupportedEncodingException uex) {
	    throw new MessagingException("Encoding error", uex);
	}
    }

    static String getFileName(MimePart part) throws MessagingException {
	String filename = null;
	String s = part.getHeader("Content-Disposition", null);

	if (s != null) {
	    // Parse the header ..
	    ContentDisposition cd = new ContentDisposition(s);
	    filename = cd.getParameter("filename");
	}
	if (filename == null) {
	    // Still no filename ? Try the "name" ContentType parameter
	    s = part.getHeader("Content-Type", null);
	    if (s != null) {
		try {
		    ContentType ct = new ContentType(s);
		    filename = ct.getParameter("name");
		} catch (ParseException pex) { }	// ignore it
	    }
	}
	if (decodeFileName && filename != null) {
	    try {
		filename = MimeUtility.decodeText(filename);
	    } catch (UnsupportedEncodingException ex) {
		throw new MessagingException("Can't decode filename", ex);
	    }
	}
	return filename;
    }

    static void setFileName(MimePart part, String name) 
		throws MessagingException {
	if (encodeFileName && name != null) {
	    try {
		name = MimeUtility.encodeText(name);
	    } catch (UnsupportedEncodingException ex) {
		throw new MessagingException("Can't encode filename", ex);
	    }
	}

	// Set the Content-Disposition "filename" parameter
	String s = part.getHeader("Content-Disposition", null);
	ContentDisposition cd = 
		new ContentDisposition(s == null ? Part.ATTACHMENT : s);
	cd.setParameter("filename", name);
	part.setHeader("Content-Disposition", cd.toString());

	/*
	 * Also attempt to set the Content-Type "name" parameter,
	 * to satisfy ancient MUAs.  XXX - This is not RFC compliant.
	 */
	if (setContentTypeFileName) {
	    s = part.getHeader("Content-Type", null);
	    if (s != null) {
		try {
		    ContentType cType = new ContentType(s);
		    cType.setParameter("name", name);
		    part.setHeader("Content-Type", cType.toString());
		} catch (ParseException pex) { }	// ignore it
	    }
	}
    }

    static String[] getContentLanguage(MimePart part) 
		throws MessagingException {
	String s = part.getHeader("Content-Language", null);

	if (s == null)
	    return null;

	// Tokenize the header to obtain the Language-tags (skip comments)
	HeaderTokenizer h = new HeaderTokenizer(s, HeaderTokenizer.MIME);
	Vector v = new Vector();

	HeaderTokenizer.Token tk;
	int tkType;

	while (true) {
	    tk = h.next(); // get a language-tag
	    tkType = tk.getType();
	    if (tkType == HeaderTokenizer.Token.EOF)
		break; // done
	    else if (tkType == HeaderTokenizer.Token.ATOM)
		v.addElement(tk.getValue());
	    else // invalid token, skip it.
		continue;
	}

	if (v.size() == 0)
	    return null;

	String[] language = new String[v.size()];
	v.copyInto(language);
	return language;	
    }

    static void setContentLanguage(MimePart part, String[] languages)
			throws MessagingException {
	StringBuffer sb = new StringBuffer(languages[0]);
	for (int i = 1; i < languages.length; i++)
	    sb.append(',').append(languages[i]);
	part.setHeader("Content-Language", sb.toString());
    }

    static String getEncoding(MimePart part) throws MessagingException {
	String s = part.getHeader("Content-Transfer-Encoding", null);

	if (s == null)
	    return null;

	s = s.trim();	// get rid of trailing spaces
	// quick check for known values to avoid unnecessary use
	// of tokenizer.
	if (s.equalsIgnoreCase("7bit") || s.equalsIgnoreCase("8bit") ||
		s.equalsIgnoreCase("quoted-printable") ||
		s.equalsIgnoreCase("binary") ||
		s.equalsIgnoreCase("base64"))
	    return s;

	// Tokenize the header to obtain the encoding (skip comments)
	HeaderTokenizer h = new HeaderTokenizer(s, HeaderTokenizer.MIME);

	HeaderTokenizer.Token tk;
	int tkType;

	for (;;) {
	    tk = h.next(); // get a token
	    tkType = tk.getType();
	    if (tkType == HeaderTokenizer.Token.EOF)
		break; // done
	    else if (tkType == HeaderTokenizer.Token.ATOM)
		return tk.getValue();
	    else // invalid token, skip it.
		continue;
	}
	return s;
    }

    static void setEncoding(MimePart part, String encoding)
				throws MessagingException {
	part.setHeader("Content-Transfer-Encoding", encoding);
    }

    static void updateHeaders(MimePart part) throws MessagingException {
	DataHandler dh = part.getDataHandler();
	if (dh == null) // Huh ?
	    return;

	try {
	    String type = dh.getContentType();
	    boolean composite = false;
	    boolean needCTHeader = part.getHeader("Content-Type") == null;

	    ContentType cType = new ContentType(type);
	    if (cType.match("multipart/*")) {
		// If multipart, recurse
		composite = true;
		Object o;
		if (part instanceof MimeBodyPart) {
		    MimeBodyPart mbp = (MimeBodyPart)part;
		    o = mbp.cachedContent != null ?
				mbp.cachedContent : dh.getContent();
		} else if (part instanceof MimeMessage) {
		    MimeMessage msg = (MimeMessage)part;
		    o = msg.cachedContent != null ?
				msg.cachedContent : dh.getContent();
		} else
		    o = dh.getContent();
		if (o instanceof MimeMultipart)
		    ((MimeMultipart)o).updateHeaders();
		else
		    throw new MessagingException("MIME part of type \"" +
			type + "\" contains object of type " +
			o.getClass().getName() + " instead of MimeMultipart");
	    } else if (cType.match("message/rfc822")) {
		composite = true;
		// XXX - call MimeMessage.updateHeaders()?
	    }

	    // Content-Transfer-Encoding, but only if we don't
	    // already have one
	    if (!composite) {	// not allowed on composite parts
		if (part.getHeader("Content-Transfer-Encoding") == null)
		    setEncoding(part, MimeUtility.getEncoding(dh));

		if (needCTHeader && setDefaultTextCharset &&
			cType.match("text/*") &&
			cType.getParameter("charset") == null) {
		    /*
		     * Set a default charset for text parts.
		     * We really should examine the data to determine
		     * whether or not it's all ASCII, but that's too
		     * expensive so we make an assumption:  If we
		     * chose 7bit encoding for this data, it's probably
		     * ASCII.  (MimeUtility.getEncoding will choose
		     * 7bit only in this case, but someone might've
		     * set the Content-Transfer-Encoding header manually.)
		     */
		    String charset;
		    String enc = part.getEncoding();
		    if (enc != null && enc.equalsIgnoreCase("7bit"))
			charset = "us-ascii";
		    else
			charset = MimeUtility.getDefaultMIMECharset();
		    cType.setParameter("charset", charset);
		    type = cType.toString();
		}
	    }

	    // Now, let's update our own headers ...

	    // Content-type, but only if we don't already have one
	    if (needCTHeader) {
		/*
		 * Pull out "filename" from Content-Disposition, and
		 * use that to set the "name" parameter. This is to
		 * satisfy older MUAs (DtMail, Roam and probably
		 * a bunch of others).
		 */
		String s = part.getHeader("Content-Disposition", null);
		if (s != null) {
		    // Parse the header ..
		    ContentDisposition cd = new ContentDisposition(s);
		    String filename = cd.getParameter("filename");
		    if (filename != null) {
			cType.setParameter("name", filename);
			type = cType.toString();
		    }
		}
		
		part.setHeader("Content-Type", type);
	    }
	} catch (IOException ex) {
	    throw new MessagingException("IOException updating headers", ex);
	}
    }

    static void invalidateContentHeaders(MimePart part)
					throws MessagingException {
	part.removeHeader("Content-Type");
	part.removeHeader("Content-Transfer-Encoding");
    }
    
    static void writeTo(MimePart part, OutputStream os, String[] ignoreList)
			throws IOException, MessagingException {

	// see if we already have a LOS
	LineOutputStream los = null;
	if (os instanceof LineOutputStream) {
	    los = (LineOutputStream) os;
	} else {
	    los = new LineOutputStream(os);
	}

	// First, write out the header
	Enumeration hdrLines = part.getNonMatchingHeaderLines(ignoreList);
	while (hdrLines.hasMoreElements())
	    los.writeln((String)hdrLines.nextElement());

	// The CRLF separator between header and content
	los.writeln();

	// Finally, the content. Encode if required.
	// XXX: May need to account for ESMTP ?
	os = MimeUtility.encode(os, part.getEncoding());
	part.getDataHandler().writeTo(os);
	os.flush(); // Needed to complete encoding
    }
}
