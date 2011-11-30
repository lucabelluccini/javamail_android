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

import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import java.io.*;
import java.util.*;

/**
 * This class represents a mailbox file containing RFC822 style email messages. 
 *
 * @author John Mani
 * @author Bill Shannon
 */

public class MboxFolder extends Folder {

    private String name;	// null => the default folder
    private boolean is_inbox = false;
    private int total;		// total number of messages in mailbox
    private boolean opened = false;
    private Vector message_cache;
    private MboxStore mstore;
    private MailFile folder;
    private long file_size;	// the size the last time we read or wrote it
    private long saved_file_size; // size at the last open, close, or expunge
    private MboxMessage special_imap_message;

    public MboxFolder(MboxStore store, String name) {
	super(store);
	this.mstore = store;
	this.name = name;

	if (name != null && name.equalsIgnoreCase("INBOX"))
	    is_inbox = true;

	folder = mstore.getMailFile(name == null ? "~" : name);
	if (folder.exists())
	    saved_file_size = folder.length();
	else
	    saved_file_size = -1;
    }

    public char getSeparator() {
	return File.separatorChar;
    }

    public Folder[] list(String pattern) throws MessagingException {
	if (!folder.isDirectory())
	    throw new MessagingException("not a directory");

	if (name == null)
	    return list(null, pattern, true);
	else
	    return list(name + File.separator, pattern, false);
    }

    /*
     * Version of list shared by MboxStore and MboxFolder.
     */
    protected Folder[] list(String ref, String pattern, boolean fromStore)
					throws MessagingException {
	if (ref != null && ref.length() == 0)
	    ref = null;
	int i;
	String refdir = null;
	String realdir = null;

	pattern = canonicalize(ref, pattern);
	if ((i = indexOfAny(pattern, "%*")) >= 0) {
	    refdir = pattern.substring(0, i);
	} else {
	    refdir = pattern;
	}
	if ((i = refdir.lastIndexOf(File.separatorChar)) >= 0) {
	    // get rid of anything after directory name
	    refdir = refdir.substring(0, i + 1);
	    realdir = mstore.mb.filename(mstore.user, refdir);
	} else if (refdir.length() == 0 || refdir.charAt(0) != '~') {
	    // no separator and doesn't start with "~" => home dir
	    refdir = null;
	    realdir = mstore.home;
	} else {
	    realdir = mstore.mb.filename(mstore.user, refdir);
	}
	Vector flist = new Vector();
	listWork(realdir, refdir, pattern, fromStore ? 0 : 1, flist);
	if (match.path("INBOX", pattern, '\0'))
	    flist.addElement("INBOX");

	Folder fl[] = new Folder[flist.size()];
	for (i = 0; i < fl.length; i++) {
	    fl[i] = createFolder(mstore, (String)flist.elementAt(i));
	}
	return fl;
    }

    public String getName() {
	if (name == null)
	    return "";
	else if (is_inbox)
	    return "INBOX";
	else
	    return folder.getName();
    }

    public String getFullName() {
	if (name == null)
	    return "";
	else
	    return name;
    }

    public Folder getParent() {
	if (name == null)
	    return null;
	else if (is_inbox)
	    return createFolder(mstore, null);
	else
	    // XXX - have to recognize other folders under default folder
	    return createFolder(mstore, folder.getParent());
    }

    public boolean exists() {
	return folder.exists();
    }

    public int getType() {
	if (folder.isDirectory())
	    return HOLDS_FOLDERS;
	else
	    return HOLDS_MESSAGES;
    }

    public Flags getPermanentFlags() {
	return mstore.permFlags;
    }

    public synchronized boolean hasNewMessages() {
	if (folder instanceof UNIXFile) {
	    UNIXFile f = (UNIXFile)folder;
	    if (f.length() > 0) {
		long atime = f.lastAccessed();
		long mtime = f.lastModified();
//System.out.println(name + " atime " + atime + " mtime " + mtime);
		return atime < mtime;
	    }
	    return false;
	}
	long current_size;
	if (folder.exists())
	    current_size = folder.length();
	else
	    current_size = -1;
	// if we've never opened the folder, remember the size now
	// (will cause us to return false the first time)
	if (saved_file_size < 0)
	    saved_file_size = current_size;
	return current_size > saved_file_size;
    }

    public synchronized Folder getFolder(String name)
					throws MessagingException {
	if (folder.exists() && !folder.isDirectory())
	    throw new MessagingException("not a directory");
	Folder f;
	if (this.name != null)
	    f = createFolder(mstore, this.name + File.separator + name);
	else
	    f = createFolder(mstore, name);
	return f;
    }

    public synchronized boolean create(int type) throws MessagingException {
	switch (type) {
	case HOLDS_FOLDERS:
	    if (!folder.mkdirs()) {
		return false;
	    }
	    break;

	case HOLDS_MESSAGES:
	    if (folder.exists()) {
		return false;
	    }
	    try {
		(new FileOutputStream((File)folder)).close();
	    } catch (FileNotFoundException fe) {
		File parent = new File(folder.getParent());
		if (!parent.mkdirs())
		    throw new
			MessagingException("can't create folder: " + name);
		try {
		    (new FileOutputStream((File)folder)).close();
		} catch (IOException ex3) {
		    throw new
			MessagingException("can't create folder: " + name, ex3);
		}
	    } catch (IOException e) {
		throw new
		    MessagingException("can't create folder: " + name, e);
	    }
	    break;

	default:
	    throw new MessagingException("type not supported");
	}
	notifyFolderListeners(FolderEvent.CREATED);
	return true;
    }

    public synchronized boolean delete(boolean recurse)
					throws MessagingException {
	// XXX - implement recurse
	checkClosed();
	if (name == null)
	    throw new MessagingException("can't delete default folder");
	if (folder.delete()) {
	    notifyFolderListeners(FolderEvent.DELETED);
	    return true;
	}
	return false;
    }

    public synchronized boolean renameTo(Folder f)
				throws MessagingException {
	checkClosed();
	if (name == null)
	    throw new MessagingException("can't rename default folder");
	if (!(f instanceof MboxFolder))
	    throw new MessagingException("can't rename to: " + f.getName());
	String newname = ((MboxFolder)f).folder.getPath();
	if (folder.renameTo(new File(folder.getPath(), newname))) {
	    notifyFolderRenamedListeners(f);
	    return true;
	}
	return false;
    }

    /* Ensure the folder is open */
    void checkOpen() throws IllegalStateException {
	if (!opened) 
	    throw new IllegalStateException("Folder is not Open");
    }

    /* Ensure the folder is not open */
    void checkClosed() throws IllegalStateException {
	if (opened) 
	    throw new IllegalStateException("Folder is Open");
    }

    /* Ensure the folder is open & readable */
    void checkReadable() throws IllegalStateException {
	if (!opened || (mode != READ_ONLY && mode != READ_WRITE))
	    throw new IllegalStateException("Folder is not Readable");
    }

    /* Ensure the folder is open & writable */
    void checkWritable() throws IllegalStateException {
	if (!opened || mode != READ_WRITE)
	    throw new IllegalStateException("Folder is not Writable");
    }

    public boolean isOpen() {
        return opened;
    }

    /*
     * Open the folder in the specified mode.
     */
    public synchronized void open(int mode) throws MessagingException {
	if (opened)
	    throw new IllegalStateException("Folder is already Open");

	this.mode = mode;
	switch (mode) {
	case READ_WRITE:
	default:
	    if (!folder.canWrite())
		throw new MessagingException("Open Failure, can't write");
	    // fall through...

	case READ_ONLY:
	    if (!folder.canRead())
		throw new MessagingException("Open Failure, can't read");
	    break;
	}

	if (is_inbox && folder instanceof InboxFile) {
	    InboxFile inf = (InboxFile)folder;
	    if (!inf.openLock(mode == READ_WRITE ? "rw" : "r"))
		throw new MessagingException("Failed to lock INBOX");
	}
	if (!folder.lock("r"))
	    throw new MessagingException("Failed to lock folder: " + name);
	message_cache = new Vector();
	total = 0;
	Message[] msglist = null;
	try {
	    saved_file_size = folder.length();
	    msglist = load(0L, false);
	} catch (IOException e) {
	    throw new MessagingException("IOException", e);
	} finally {
	    folder.unlock();
	}
	notifyConnectionListeners(ConnectionEvent.OPENED);
	if (msglist != null)
	    notifyMessageAddedListeners(msglist);
	opened = true;
    }

    public synchronized void close(boolean expunge) throws MessagingException {
	checkOpen();

	try {
	    if (mode == READ_WRITE) {
		try {
		    writeFolder(true, expunge);
		} catch (IOException e) {
		    throw new MessagingException("I/O Exception", e);
		}
	    }
	    message_cache = null;
	} finally {
	    opened = false;
	    if (is_inbox && folder instanceof InboxFile) {
		InboxFile inf = (InboxFile)folder;
		inf.closeLock();
	    }
	    notifyConnectionListeners(ConnectionEvent.CLOSED);
	}
    }

    /**
     * Re-write the folder with the current contents of the messages.
     * If closing is true, turn off the RECENT flag.  If expunge is
     * true, don't write out deleted messages (only used from close()
     * when the message cache won't be accessed again).
     *
     * Return the number of messages written.
     */
    protected int writeFolder(boolean closing, boolean expunge)
			throws IOException, MessagingException {

	/*
	 * First, see if there have been any changes.
	 */
	int modified = 0, deleted = 0, recent = 0;
	for (int msgno = 1; msgno <= total; msgno++) {
	    MboxMessage msg =
		(MboxMessage)message_cache.elementAt(msgno - 1);
	    Flags flags = msg.getFlags();
	    if (msg.isModified() || !msg.origFlags.equals(flags))
		modified++;
	    if (flags.contains(Flags.Flag.DELETED))
		deleted++;
	    if (flags.contains(Flags.Flag.RECENT))
	        recent++;
	}
	if ((!closing || recent == 0) && (!expunge || deleted == 0) &&
		modified == 0)
	    return 0;

	/*
	 * Have to save any new mail that's been appended to the
	 * folder since we last loaded it.
	 * XXX - Should do this without actually loading the messages in.
	 */
	if (!folder.lock("rw"))
	    throw new MessagingException("Failed to lock folder: " + name);
	int oldtotal = total;	// XXX
	Message[] msglist = null;
	if (folder.length() != file_size)
	    msglist = load(file_size, !closing);
	// don't use the folder's FD, need to re-open in order to trunc the file
	OutputStream os =
		new BufferedOutputStream(new FileOutputStream((File)folder));
	int wr = 0;
	boolean keep = true;
	try {
	    if (special_imap_message != null)
		writeMboxMessage(special_imap_message, os);
	    for (int msgno = 1; msgno <= total; msgno++) {
		MboxMessage msg =
		    (MboxMessage)message_cache.elementAt(msgno - 1);
		if (expunge && msg.isSet(Flags.Flag.DELETED))
		    continue;	// skip it;
		if (closing && msgno <= oldtotal &&
						msg.isSet(Flags.Flag.RECENT))
		    msg.setFlag(Flags.Flag.RECENT, false);
		writeMboxMessage(msg, os);
		folder.touchlock();
		wr++;
	    }
	    file_size = saved_file_size = folder.length();
	    // If no messages in the mailbox, and we're closing,
	    // maybe we should remove the mailbox.
	    if (wr == 0 && closing) {
		String skeep = ((MboxStore)store).getSession().
					getProperty("mail.mbox.deleteEmpty");
		if (skeep != null && skeep.equalsIgnoreCase("true"))
		    keep = false;
	    }
	} catch (IOException e) {
	    throw e;
	} catch (MessagingException e) {
	    throw e;
	} catch (Exception e) {
e.printStackTrace();
	    throw new MessagingException("unexpected exception " + e);
	} finally {
	    // close the folder, flushing out the data
	    try {
		os.close();
		if (!keep) {
		    folder.delete();
		    file_size = 0;
		}
	    } catch (IOException ex) {}

	    if (keep) {
		// make sure the access time is greater than the mod time
		// XXX - would be nice to have utime()
		try {
		    Thread.sleep(1000);		// sleep for a second
		} catch (InterruptedException ex) {}
		InputStream is = null;
		try {
		    is = new FileInputStream((File)folder);
		    is.read();	// read a byte
		} catch (IOException ex) {}	// ignore errors
		try {
		    if (is != null)
			is.close();
		    is = null;
		} catch (IOException ex) {}	// ignore errors
	    }

	    folder.unlock();
	    if (msglist != null)
		notifyMessageAddedListeners(msglist);
	}
	return wr;
    }

    /**
     * Write a MimeMessage to the specified OutputStream in a
     * format suitable for a UNIX mailbox, i.e., including a correct
     * Content-Length header and with the local platform's line
     * terminating convention. <p>
     *
     * If the message is really a MboxMessage, use its writeToFile
     * method, which has access to the UNIX From line.  Otherwise, do
     * all the work here, creating an appropriate UNIX From line.
     */
    public static void writeMboxMessage(MimeMessage msg, OutputStream os)
				throws IOException, MessagingException {
	try {
	    if (msg instanceof MboxMessage) {
		((MboxMessage)msg).writeToFile(os);
	    } else {
		ContentLengthCounter cos = new ContentLengthCounter();
		NewlineOutputStream nos = new NewlineOutputStream(cos);
		msg.writeTo(nos);
		nos.flush();
		os = new NewlineOutputStream(os);
		os = new ContentLengthUpdater(os, cos.getSize());
		PrintStream pos = new PrintStream(os);
		pos.println(getUnixFrom(msg));
		msg.writeTo(pos);
		pos.println();	// make sure there's a blank line at the end
		pos.flush();
	    }
	} catch (MessagingException me) {
	    throw me;
	} catch (IOException ioe) {
	    throw ioe;
	} catch (Exception e) {
	}
    }

    /**
     * Construct an appropriately formatted UNIX From line using
     * the sender address and the date in the message.
     */
    protected static String getUnixFrom(MimeMessage msg) {
	Address[] afrom;
	String from;
	Date ddate;
	String date;
	try {
	    if ((afrom = msg.getFrom()) == null ||
		    !(afrom[0] instanceof InternetAddress) ||
		    (from = ((InternetAddress)afrom[0]).getAddress()) == null)
		from = "UNKNOWN";
	    if ((ddate = msg.getReceivedDate()) == null ||
		    (ddate = msg.getSentDate()) == null)
		ddate = new Date();
	} catch (MessagingException e) {
	    from = "UNKNOWN";
	    ddate = new Date();
	}
	date = ddate.toString();
	// date is of the form "Sat Aug 12 02:30:00 PDT 1995"
	// need to strip out the timezone
	return "From " + from + " " +
		date.substring(0, 20) + date.substring(24);
    }

    public synchronized int getMessageCount() throws MessagingException {
	if (!opened)
	    return -1;

	boolean locked = false;
	Message[] msglist = null;
	try {
	    if (folder.length() != file_size) {
		if (!folder.lock("r"))
		    throw new MessagingException("Failed to lock folder: " +
							name);
		locked = true;
		msglist = load(file_size, true);
	    }
	} catch (IOException e) {
	    throw new MessagingException("I/O Exception", e);
	} finally {
	    if (locked) {
		folder.unlock();
		if (msglist != null)
		    notifyMessageAddedListeners(msglist);
	    }
	}
	return total;
    }

    /**
     * Get the specified message.  Note that messages are numbered
     * from 1.
     */
    public synchronized Message getMessage(int msgno)
				throws MessagingException {
	checkReadable();

	MboxMessage m = null;

	if (msgno <= total)
	    m = (MboxMessage)message_cache.elementAt(msgno - 1);
	return m;
    }

    public synchronized void appendMessages(Message[] msgs)
				throws MessagingException {
	if (!folder.lock("rw"))
	    throw new MessagingException("Failed to lock folder: " + name);

	OutputStream os = null;
	boolean err = false;
	try {
	    os = new BufferedOutputStream(
		new FileOutputStream(((File)folder).getPath(), true));
		// XXX - should use getAbsolutePath()?
	    for (int i = 0; i < msgs.length; i++) {
		if (msgs[i] instanceof MimeMessage) {
		    writeMboxMessage((MimeMessage)msgs[i], os);
		} else {
		    err = true;
		    continue;
		}
		folder.touchlock();
	    }
	} catch (IOException e) {
	    throw new MessagingException("I/O Exception", e);
	} catch (MessagingException e) {
	    throw e;
	} catch (Exception e) {
e.printStackTrace();
	    throw new MessagingException("unexpected exception " + e);
	} finally {
	    if (os != null)
		try {
		    os.close();
		} catch (IOException e) {}
	    folder.unlock();
	}
	if (opened)
	    getMessageCount();	// loads new messages as a side effect
	if (err)
	    throw new MessagingException("Can't append non-Mime message");
    }

    public synchronized Message[] expunge() throws MessagingException {
	checkWritable();

	/*
	 * First, write out the folder to make sure we have permission,
	 * disk space, etc.
	 */
	int wr = total;		// number of messages written out
	try {
	    wr = writeFolder(false, true);
	} catch (IOException e) {
	    throw new MessagingException("expunge failed", e);
	}
	if (wr == 0)
	    return new Message[0];

	/*
	 * Now, actually get rid of the expunged messages.
	 */
	int del = 0;
	Message[] msglist = new Message[total - wr];
	int msgno = 1;
	while (msgno <= total) {
	    MboxMessage msg =
		(MboxMessage)message_cache.elementAt(msgno - 1);
	    if (msg.isSet(Flags.Flag.DELETED)) {
		msglist[del] = msg;
		del++;
		message_cache.removeElementAt(msgno - 1);
		total--;
	    } else {
		msg.setMessageNumber(msgno);	// update message number
		msgno++;
	    }
	}
	if (del != msglist.length)		// this is really an assert
	    throw new MessagingException("expunge delete count wrong");
	notifyMessageRemovedListeners(true, msglist);
	return msglist;
    }

    /*
     * Load more messages from the folder starting at the specified offset.
     */
    private Message[] load(long offset, boolean notify)
				throws MessagingException, IOException {
	int oldtotal = total;
	try {
	    boolean first = offset == 0;
	    BufferedInputStream in = new BufferedInputStream(
				new FileInputStream(folder.getFD()), 8192);
	    skipFully(in, offset);

	    /*
	     * Keep constructing new messages based on the InputStream
	     * until we get an EOFException indicating the end of the mailbox.
	     */
	    for (;;) {
		MboxMessage msg = loadMessage(in, total, mode == READ_WRITE);
		if (first) {
		    first = false;
		    /*
		     * If the first message is the special message that the
		     * IMAP server adds to the mailbox, hide it away in a
		     * special place and don't let the user see it.
		     */
		    if (msg.getHeader("X-IMAP") != null) {
			special_imap_message = msg;
			continue;
		    }
		    /*
		     * Following only works with Sun server, don't use it...
		    String subj = msg.getSubject();
		    if (subj != null &&
		      subj.equals("IMAP4 Server Data-DO NOT DELETE")) {
			special_imap_message = msg;
			continue;
		    }
		     */
		}
		total++;
		msg.setMessageNumber(total);
		message_cache.addElement((Object)msg);
	    }
	} catch (EOFException e) {
	    // done
	    file_size = folder.length();
	}
	if (notify) {
	    Message[] msglist = new Message[total - oldtotal];
	    for (int i = oldtotal, j = 0; i < total; i++, j++)
		msglist[j] = (Message)message_cache.elementAt(i);
	    return msglist;
	} else
	    return null;
    }

    /**
     * Parse the input stream and return an appropriate message object.
     */
    private MboxMessage loadMessage(BufferedInputStream is, int msgno,
		boolean writable) throws MessagingException, IOException {
	DataInputStream in = new DataInputStream(is);

	/*
	 * Read lines until a UNIX From line,
	 * skipping blank lines.
	 * XXX - rewrite this to not need a DataInputStream.
	 */
	String line;
	String unix_from = null;
	while ((line = in.readLine()) != null) {
	    if (line.trim().length() == 0)
		continue;
	    if (line.startsWith("From ")) {
		/*
		 * A UNIX From line looks like:
		 * From address Day Mon DD HH:MM:SS YYYY
		 */
		unix_from = line;
		int i;
		// find the space after the address, before the date
		i = unix_from.indexOf(' ', 5);
		if (i < 0)
		    continue;	// not a valid UNIX From line
		break;
	    }
	    throw new MessagingException("Garbage in mailbox: " + line);
	}

	if (unix_from == null)
	    throw new EOFException("end of mailbox");

	/*
	 * Now load the RFC822 headers into an InternetHeaders object.
	 */
	InternetHeaders hdrs = new InternetHeaders(is);
	byte[] content = null;

	try {
	    int len;
	    if ((len = contentLength(hdrs)) >= 0) {
		content = new byte[len];
		in.readFully(content);
	    } else {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int b;

		/*
		 * Read bytes until we see "\nFrom ", then
		 * back up to the beginning of that string.
		 */

		while ((b = is.read()) >= 0) {
		    if (b == '\r' || b == '\n') {
			is.mark(6);
			if (b == '\r' && is.read() != '\n') {
			    is.reset();
			    is.mark(5);
			}
			if (is.read() == 'F' &&
			    is.read() == 'r' &&
			    is.read() == 'o' &&
			    is.read() == 'm' &&
			    is.read() == ' ') {
			    is.reset();
			    break;
			}
			is.reset();
		    }
		    buf.write(b);
		}
		content = buf.toByteArray();
		//env.setContentSize(content.length);
	    }
	} catch (EOFException e) {
	    /*
	     * We're done with this message.
	     * Next attempt to read a message
	     * will throw EOFException (see above).
	     */
	}

	return new MboxMessage(this, hdrs, content, msgno, unix_from, writable);
    }

    /**
     * Extract the value of the Content-Length header, if present.
     */
    private int contentLength(InternetHeaders hdrs) {
	int len = -1;
	String cl[] = hdrs.getHeader("Content-Length");
	try {
	    if (cl != null && cl[0] != null)
		len = Integer.parseInt(cl[0]);
	} catch (NumberFormatException e) {}
	return len;
    }

    /**
     * Skip the specified number of bytes, repeatedly calling
     * the skip method as necessary.
     */
    private void skipFully(InputStream in, long offset) throws IOException {
	while (offset > 0) {
	    long cur = in.skip(offset);
	    if (cur <= 0)
		throw new EOFException("can't skip");
	    offset -= cur;
	}
    }

    /*
     * Only here to make accessible to MboxMessage.
     */
    protected void notifyMessageChangedListeners(int type, Message m) {
	super.notifyMessageChangedListeners(type, m);
    }


    /**
     * this is an exact duplicate of the Folder.getURL except it doesn't
     * add a beginning '/' to the URLName.
     */
    public URLName getURLName() {
	// XXX - note:  this should not be done this way with the
	// new javax.mail apis.

	URLName storeURL = getStore().getURLName();
	if (name == null)
	    return storeURL;

	char separator = getSeparator();
	String fullname = getFullName();
	StringBuffer encodedName = new StringBuffer();

	// We need to encode each of the folder's names, and replace
	// the store's separator char with the URL char '/'.
	StringTokenizer tok = new StringTokenizer(
	    fullname, Character.toString(separator), true);

	while (tok.hasMoreTokens()) {
	    String s = tok.nextToken();
	    if (s.charAt(0) == separator)
		encodedName.append("/");
	    else
		// XXX - should encode, but since there's no decoder...
		//encodedName.append(java.net.URLEncoder.encode(s));
		encodedName.append(s);
	}

	return new URLName(storeURL.getProtocol(), storeURL.getHost(),
			    storeURL.getPort(), encodedName.toString(),
			    storeURL.getUsername(),
			    null /* no password */);
    }

    /**
     * Create an MboxFolder object, or a subclass thereof.
     * Can be overridden by subclasses of MboxFolder so that
     * the appropriate subclass is created by the list method.
     */
    protected Folder createFolder(MboxStore store, String name) {
	return new MboxFolder(store, name);
    }

    /*
     * Support routines for list().
     */

    /**
     * Return a canonicalized pattern given a reference name and a pattern.
     */
    private static String canonicalize(String ref, String pat) {
	if (ref == null)
	    return pat;
	try {
	    if (pat.length() == 0) {
		return ref;
	    } else if (pat.charAt(0) == File.separatorChar) {
		return ref.substring(0, ref.indexOf(File.separatorChar)) + pat;
	    } else {
		return ref + pat;
	    }
	} catch (StringIndexOutOfBoundsException e) {
	    return pat;
	}
    }

    /**
     * Return the first index of any of the characters in "any" in "s",
     * or -1 if none are found.
     *
     * This should be a method on String.
     */
    private static int indexOfAny(String s, String any) {
	try {
	    int len = s.length();
	    for (int i = 0; i < len; i++) {
		if (any.indexOf(s.charAt(i)) >= 0)
		    return i;
	    }
	    return -1;
	} catch (StringIndexOutOfBoundsException e) {
	    return -1;
	}
    }

    /**
     * The recursive part of generating the list of mailboxes.
     * realdir is the full pathname to the directory to search.
     * dir is the name the user uses, often a relative name that's
     * relative to the user's home directory.  dir (if not null) always
     * has a trailing file separator character.
     *
     * @param realdir	real pathname of directory to start looking in
     * @param dir	user's name for realdir
     * @param pat	pattern to match against
     * @param level	level of the directory hierarchy we're in
     * @param flist	vector to which to add folder names that match
     */
    // Derived from the c-client listWork() function.
    private void listWork(String realdir, String dir, String pat,
					int level, Vector flist) {
	String sl[];
	File fdir = new File(realdir);
	try {
	    sl = fdir.list();
	} catch (SecurityException e) {
	    return;	// can't read it, ignore it
	}

	if (level == 0 && dir != null &&
		match.path(dir, pat, File.separatorChar))
	    flist.addElement(dir);

	if (sl == null)
	    return;	// nothing return, we're done

	if (realdir.charAt(realdir.length() - 1) != File.separatorChar)
	    realdir += File.separator;

	for (int i = 0; i < sl.length; i++) {
	    if (sl[i].charAt(0) == '.')
		continue;	// ignore all "dot" files for now
	    String md = realdir + sl[i];
	    File mf = new File(md);
	    if (!mf.exists())
		continue;
	    String name;
	    if (dir != null)
		name = dir + sl[i];
	    else
		name = sl[i];
	    if (mf.isDirectory()) {
		if (match.path(name, pat, File.separatorChar)) {
		    flist.addElement(name);
		    name += File.separator;
		} else {
		    name += File.separator;
		    if (match.path(name, pat, File.separatorChar))
			flist.addElement(name);
		}
		if (match.dir(name, pat, File.separatorChar))
		    listWork(md, name, pat, level + 1, flist);
	    } else {
		if (match.path(name, pat, File.separatorChar))
		    flist.addElement(name);
	    }
	}
    }
}

/**
 * Pattern matching support class for list().
 * Should probably be more public.
 */
// Translated from the c-client functions pmatch_full() and dmatch().
class match {
    /**
     * Pathname pattern match
     *
     * @param s		base string
     * @param pat	pattern string
     * @param delim	delimiter character
     * @return		true if base matches pattern
     */
    static public boolean path(String s, String pat, char delim) {
	try {
	    return path(s, 0, s.length(), pat, 0, pat.length(), delim);
	} catch (StringIndexOutOfBoundsException e) {
	    return false;
	}
    }

    static private boolean path(String s, int s_index, int s_len,
	String pat, int p_index, int p_len, char delim)
	    throws StringIndexOutOfBoundsException {

	while (p_index < p_len) {
	    char c = pat.charAt(p_index);
	    switch (c) {
	    case '%':
		if (++p_index >= p_len)		// % at end of pattern
						// ok if no delimiters
		    return delim == 0 || s.indexOf(delim, s_index) < 0;
		// scan remainder until delimiter
		do {
		    if (path(s, s_index, s_len, pat, p_index, p_len, delim))
			return true;
		} while (s.charAt(s_index) != delim && ++s_index < s_len);
		// ran into a delimiter or ran out of string without a match
		return false;

	    case '*':
		if (++p_index >= p_len)		// end of pattern?
		    return true;		// unconditional match
		do {
		    if (path(s, s_index, s_len, pat, p_index, p_len, delim))
			return true;
		} while (++s_index < s_len);
		// ran out of string without a match
		return false;

	    default:
		// if ran out of string or no match, fail
		if (s_index >= s_len || c != s.charAt(s_index))
		    return false;

		// try the next string and pattern characters
		s_index++;
		p_index++;
	    }
	}
	return s_index >= s_len;
    }

    /**
     * Directory pattern match
     *
     * @param s		base string
     * @param pat	pattern string
     * @return		true if base is a matching directory of pattern
     */
    static public boolean dir(String s, String pat, char delim) {
	try {
	    return dir(s, 0, s.length(), pat, 0, pat.length(), delim);
	} catch (StringIndexOutOfBoundsException e) {
	    return false;
	}
    }

    static private boolean dir(String s, int s_index, int s_len,
	String pat, int p_index, int p_len, char delim)
	    throws StringIndexOutOfBoundsException {

	while (p_index < p_len) {
	    char c = pat.charAt(p_index);
	    switch (c) {
	    case '%':
		if (s_index >= s_len)		// end of base?
		    return true;		// subset match
		if (++p_index >= p_len)		// % at end of pattern?
		    return false;		// no inferiors permitted
		do {
		    if (dir(s, s_index, s_len, pat, p_index, p_len, delim))
			return true;
		} while (s.charAt(s_index) != delim && ++s_index < s_len);

		if (s_index + 1 == s_len)	// s ends with a delimiter
		    return true;		// must be a subset of pattern
		return dir(s, s_index, s_len, pat, p_index, p_len, delim);

	    case '*':
		return true;			// unconditional match

	    default:
		if (s_index >= s_len)		// end of base?
		    return c == delim;		// matched if at delimiter

		if (c != s.charAt(s_index))
		    return false;

		// try the next string and pattern characters
		s_index++;
		p_index++;
	    }
	}
	return s_index >= s_len;
    }
}
