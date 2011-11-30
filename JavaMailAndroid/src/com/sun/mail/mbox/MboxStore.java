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
import javax.mail.*;

public class MboxStore extends Store {

    String user;
    String home;
    Mailbox mb;
    static Flags permFlags;

    static {
	// we support all flags
	permFlags = new Flags();
	permFlags.add(Flags.Flag.SEEN);
	permFlags.add(Flags.Flag.RECENT);
	permFlags.add(Flags.Flag.DELETED);
	permFlags.add(Flags.Flag.FLAGGED);
	permFlags.add(Flags.Flag.ANSWERED);
	permFlags.add(Flags.Flag.DRAFT);
	permFlags.add(Flags.Flag.USER);
    }

    public MboxStore(Session session, URLName url) {
	super(session, url);

	// XXX - handle security exception
	user = System.getProperty("user.name");
	home = System.getProperty("user.home");
	String os = System.getProperty("os.name");
	try {
	    String cl = "com.sun.mail.mbox." + os + "Mailbox";
	    mb = (Mailbox)Class.forName(cl).newInstance();
	} catch (Exception e) {
	    mb = new DefaultMailbox();
	}
    }

    /**
     * since we do not have any authentication
     * to do and we do not want a dialog put up asking the user for a 
     * password we always succeed in connecting.
     */
    protected boolean protocolConnect(String host, int port, String user,
			      String passwd) throws MessagingException {

	// we always succeed in connecting
	// XXX - should we use the user?
	return true;
    }

    protected void setURLName(URLName url) {
	// host, user, password, and file don't matter so we strip them out
	if (url != null && (url.getUsername() != null ||
			    url.getHost() != null ||
			    url.getFile() != null))
	    url = new URLName(url.getProtocol(), null, -1, null, null, null);
	super.setURLName(url);
    }


    public Folder getDefaultFolder() throws MessagingException {
	checkConnected();

	return new MboxFolder(this, null);
    }

    public Folder getFolder(String name) throws MessagingException {
	checkConnected();

	return new MboxFolder(this, name);
    }

    public Folder getFolder(URLName url) throws MessagingException {
	checkConnected();
	return getFolder(url.getFile());
    }

    private void checkConnected() throws MessagingException {
	if (!isConnected())
	    throw new MessagingException("Not connected");
    }

    MailFile getMailFile(String folder) {
	return mb.getMailFile(user, folder);
    }

    Session getSession() {
	return session;
    }
}
