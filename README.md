# gribbit-rox

Update of the RoX (RPC over XML) XML-RPC system for modern Java, originally hosted at [http://rox-xmlrpc.sourceforge.net/](http://rox-xmlrpc.sourceforge.net/).

RoX is a well-written, efficient and scalable XML-RPC server. Its code has not been modified by its author (James Greenfield) since 2008. It was developed for Java 1.4 and updated for Java 1.5, but lacks generic type parameters, and depends on old versions of a number of libraries, including obsolete Sun SSL APIs.  

I plan to use RoX as a foundation for an HTTP server project, so I have imported the code here, and I am gradually transforming it into modern Java.


------------------------------ 


## Original README.txt:

### Release notes for RoX version 1.2

1. What this is

On the off chance you're reading this on its own or without any idea
what the random bits strewn around next to it are this is the readme
for RoX, an XML-RPC library for Java.

Currently RoX is hosted by SourceForge and can be found online at

  http://rox-xmlrpc.sourceforge.net/

2. What it contains

You should find the following files and top-level directories along 
with this document:

  CHANGES.txt      A change history for RoX.
  LICENSE.txt      A copy of the license this verion of RoX is released under.
  README.txt       This document.
  doc              General documentation and JavaDoc API documentation.
  src              The sample code included in the examples.
  nanoxml-*.jar    The NanoXML library (required at runtime).
  rox.jar          The RoX library (required at runtime).
  xmlunit-1.0.jar  XMLUnit unit testing library (required for testing).
  
3. Getting Started

Open your favourite IDE (or text editor) and start playing with
the demos. If you're the kind of programmer who reads all the
documentation first then start with the examples page which will
talk you through the various ways in which you can use RoX.
