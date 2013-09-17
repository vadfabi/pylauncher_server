tcPIp Sockets
by LittleBytesOfPi.com

This project is a simple TCP/IP socket server/client implementation.  

The server is written in standard C++ for Linux, and the client is 
written in Java for Android.  This project is a demonstration of a 
simple framework you can use for a system where you want an Android 
application as a remote control for a C++ program running on 
your Linux computer, such as the Raspberry Pi.

This program is offered so that anyone who needs a simple TCP/IP 
socket server/client system for local area network remote control 
of an application running on a Linux machine can start here.

Note:  This program is suitable for use on a secure local network only.  
This program does not implement any security for the connection, 
and therefore should not be used on an unsecure network.

There are two parts to this project:

> tcPIp_Sockets:  a program showing a generic framework for a 
Linux server (C++) and Android client (Java) to send and receive messages 
using TCP/IP sockets.

> pyLauncher: an example program using most of the tcPIp_Sockets code as a framework. 
This program launches python scripts on the Linux server using the Android 
client for a remote control.


Full details and technical specifications can be found at: 

http://littlebytesofpi.com/index.php/projects/our-pro/tcp-client-serve/

