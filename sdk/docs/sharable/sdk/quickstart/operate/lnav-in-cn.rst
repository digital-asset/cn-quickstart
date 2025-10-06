.. _quickstart-lnav-in-cn:

====================================
Operating lnav in the Canton Network
====================================

.. contents:: Contents
   :depth: 2
   :local:
   :backlinks: top

Overview
========

``lnav`` is a helpful debugger tool that is essential as part of your Canton Network application development journey.
This guide acts as an introduction to lnav for CN application development.
Screenshots and instructions are made with lnav version 0.13.1.

lnav download & documentation
-----------------------------

Download lnav for your OS at the lnav download page.

You can check the location of lnav on your machine with ``which lnav``.
Find your version of lnav with ``lnav --version``

..image:: /images_lnav/lnav-version.png
   :alt: lnav version

If you’re unfamiliar with lnav, read their docs.

Install Canton lnav format
--------------------------

The Canton Network has collaborated with lnav to produce a custom Canton Network lnav format to assist in debugging CN applications.

The format is defined in JSON and lives in the open source hyperledger labs splice repository. 
You can read the schema to understand available values and the construction of logs when debugging with lnav in CN applications.

..image:: /images_lnav/hyperledger-labs.png
   :alt: Splice lnav format

splice/canton/canton-json.lnav.json

Download the format and install it for use with lnav:

::

curl -L https://raw.githubusercontent.com/hyperledger-labs/splice/main/canton/canton-json.lnav.json -o /tmp/canton-json.lnav.json && lnav -i /tmp/canton-json.lnav.json

Capture Logs
============

You’re ready to debug CN apps with lnav once the format is successfully installed. 
In a terminal, from the quickstart directory run ``make capture-logs`` and then in a separate terminal run ``make start``.

..image:: /images_lnav/make-capture-logs.png
   :alt: Capture logs

Start ``make capture-logs`` in terminal 1

Allow the terminal running ``capture-logs`` to operate in the background while working with quickstart (or your CN app) in another terminal window to track events and errors in your application. 

..image:: /images_lnav/capture-logs-terminal-log.png
   :alt: Start quickstart

Capture logs running in terminal 1 during ``make start`` operates in terminal 2

Find the logs
=============

Containers create logs when they first start. 
See the available logs by running ``ls logs``.

..image:: /images_lnav/list-logs.png
   :alt: List logs

For more detailed information, use ``ls -lia logs``.

..image:: /images_lnav/list-all-logs.png
   :alt: List logs detailed

Canton logs
===========

``clog`` pronounced “c-log” are Canton logs that follow the custom Canton log formatting, as mentioned above. 
``clogs`` are generally used for long-running services such as Canton and Splice, while the standard log files usually indicate initialization scripts and utilities.

View live ``clogs`` by running ``lnav logs/*.clog`` from the ``quickstart/`` directory.
(Exit at anytime with “control+c” or “:quit”)

..image:: /images_lnav/lnav-view-live-logs.png
   :alt: View clogs

a live clog stream

Explore clogs
=============

Initially, you will view a running stream of clog events.

Press “g” on the keyboard to go to the top of the logs.

..image:: /images_lnav/lnav-top-of-log.png
   :alt: Clog top

“Shift + g” takes you to the end of the logs and reinitiates the stream.

..image:: /images_lnav/lnav-date-of-entry.png
   :alt: Clog bottom

Pause and unpause the stream with “=”.

Use the left cursor key to view the log entry’s file origination point.

..image:: /images_lnav/lnav-view-log-origination.png
   :alt: Clog file origin

Use the right cursor key to view the log entry.

..image:: /images_lnav/lnav-view-log-entry.png
   :alt: Clog log entry

Using “shift + right” and “shift + left” moves the view in shorter increments.

Use “x” to expand and collapse information within the square brackets after the date.

..image:: /images_lnav/lnav-expand-info.png
   :alt: Clog expand collapse

collapse information:

..image:: /images_lnav/lnav-collapse-info.png
   :alt: Clog collapsed

Command mode
------------

Enter command mode with the colon key, “:”.
All guidance in this section assumes you are in command mode. 

Use the up arrow to scroll through command history.
