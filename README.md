# SCOJA

This project was (and still is) originally stored a sourceforge:
[original location](https://sourceforge.net/p/scoja/code/)

This is simply a fork.

The original README of the project is [here](doc/description/README.txt)

Scoja is an implementation of syslogd in Java.
We pretend to build a system as powerful as syslog-ng but with two main
differences.
First, Scoja is multithreaded, and it is possible to configure how many
threads to allocate for a given task.

Second, Scoja configuration files are writen in Python (we use
Jython). So long repetive configuration files can be shortened to a
few lines using Python definitions, loops over arrays, etc.
Event more, for complex event processing, Python expressions can be
used.
For compatibility purposes, syslogd and syslog-ng configuration files
will be understood.

Scoja will be based on a clean core framework so that adding new or
enhanced capabilities will be a simple task.

Of course, Scoja doesn't pretend to be a syslogd substitute for
common workstations.
Scoja, just as syslog-ng, is for those who want to centralize log
collection and organized them on the fly.
