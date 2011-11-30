JavaMailAndroid
===============

This project is a porting of ORACLE (ex-Sun) JavaMail API to Android.
It is delivered as a Library Project.

Background 
----------

This code is part of my thesis for degree in 2008(before the Master Degree). 
I hope it would be useful for all developers whose encountered issues or difficulties using JavaMail on Android.
There were few troubles:

* java.awt.Datatransfer class was missing (and related interfaces): they are available in the Apache Harmony SVN. There were some java.awt.Image dependancies, which have been removed.
* The lastest Sun JavaMail API was not working on Android: at build time, the APK popped up an error within an IMAP class. I downloaded source code of it (now Sun JM is opensource) and I fixed some classes.
* Sun JavaMail was working, but not at 100%: it was unable to manage Multipart elements of an email (so most of the email we receive). This API stored `MIME-Types` to/from Java Object association (a sort of mapping between them) in 2 files within the JAR file. Android is unable to read files in the classic way (due to security policy), so I decided to hardcode the MIME mappings.

Contributing
------------
Want to contribute? Great!
I have no time to mantain this code, but I wanted to share it. Feel free to request info or request an handover on this project or to work on it.

Using JavaMailAndroid in your projects
--------------------------------------

Checkout the code and use it with you Android project.
You'll need to link them following this [guide](http://developer.android.com/guide/developing/projects/projects-eclipse.html), in the section `Setting up a Library Project`.

Testing
-------

No test has been performed on this code. I used it on a simple mail client which supports POP3 and IMAP.

Code sources
------------

This project is based on the code available in:

* [Oracle JavaMail API](http://www.oracle.com/technetwork/java/javamail/index.html)
* [Apache Harmony](http://harmony.apache.org/svn.html)

Licence
-------
Please help me to find out a proper licence to this work.

* Source License for Oracle JavaMail: CDDL-1.0, BSD, GPL-2.0, GNU-Classpath
* Source License for Apache Harmony: Apache License v2.0

TODO
----
It would be perfect to:

* integrate latest changes from Oracle JavaMail API on this project
* remove the hardcoded MIME-Types to make the library more flexible
* write some unit tests
* provide Android App sources for simple mail retrival via POP3 and IMAP (working on it, I am porting the app from Android 0.9 to 2.1)
