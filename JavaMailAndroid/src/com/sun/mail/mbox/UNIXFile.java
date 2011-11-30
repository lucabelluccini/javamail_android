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

import java.io.File;
import java.io.FileDescriptor;
import java.util.StringTokenizer;

public class UNIXFile extends File {
    protected static final boolean loaded;

    private static final long serialVersionUID = -7972156315284146651L;

    public UNIXFile(String name) {
	super(name);
    }

    static {
	boolean lloaded = false;
	try {
	    System.loadLibrary("mbox");
	    lloaded = true;
	} catch (UnsatisfiedLinkError e) {
	    String classpath = System.getProperty("java.class.path");
	    String sep = System.getProperty("path.separator");
	    StringTokenizer st = new StringTokenizer(classpath, sep);
	    while (st.hasMoreTokens()) {
		String path = st.nextToken();
		int i = path.length() - 7;	// 7 == "classes".length()
		if (i > 0 && path.substring(i).equals("classes")) {
		    // XXX - pathname is Solaris/SPARC specific
		    String lib = path.substring(0, i) + "lib/sparc/libmbox.so";
		    try {
			System.load(lib);
			lloaded = true;
			break;
		    } catch (UnsatisfiedLinkError e2) {
			continue;
		    }
		} else if ((i = path.length() - 8) > 0 &&
					path.substring(i).equals("mail.jar")) {
		    // XXX - pathname is Solaris/SPARC specific
		    String lib = path.substring(0, i) + "lib/sparc/libmbox.so";
		    try {
			System.load(lib);
			lloaded = true;
			break;
		    } catch (UnsatisfiedLinkError e2) {
			continue;
		    }
		}
	    }
	}
	loaded = lloaded;
	if (loaded)
	    initIDs(FileDescriptor.class, FileDescriptor.in);
    }

    /**
     * Return the access time of the file.
     */
    public static long lastAccessed(File file) {
	return lastAccessed0(file.getPath());
    }

    public long lastAccessed() {
	return lastAccessed0(getPath());
    }

    private static native void initIDs(Class fdClass, FileDescriptor stdin);

    /**
     * Lock the file referred to by fd.  The string mode is "r"
     * for a read lock or "rw" for a write lock.  Don't block
     * if lock can't be acquired.
     */
    public static boolean lock(FileDescriptor fd, String mode) {
	return lock(fd, mode, false);
    }

    /**
     * Lock the file referred to by fd.  The string mode is "r"
     * for a read lock or "rw" for a write lock.  If block is set,
     * block waiting for the lock if necessary.
     */
    private static boolean lock(FileDescriptor fd, String mode, boolean block) {
	//return loaded && lock0(fd, mode);
	if (loaded) {
	    boolean ret;
	    //System.out.println("UNIXFile.lock(" + fd + ", " + mode + ")");
	    ret = lock0(fd, mode, block);
	    //System.out.println("UNIXFile.lock returns " + ret);
	    return ret;
	}
	return false;
    }

    private static native boolean lock0(FileDescriptor fd, String mode,
								boolean block);

    public static native long lastAccessed0(String name);
}
