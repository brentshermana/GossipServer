#!/bin/bash/
#pass along all arguments to the program
javac netprog/client/clientapp/ClientAppForUDPTestScript.java
java netprog/client/clientapp/ClientAppForUDPTestScript -U -s 127.0.0.1 -p 2356 -m "UDP Gossip test message from command line" -t "2017-01-09-16-18-20-001Z"

