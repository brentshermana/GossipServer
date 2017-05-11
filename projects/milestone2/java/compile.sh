#!/bin/bash
echo "Compiling project..."
javac netprog/Main.java
javac netprog/AppUtils.java
javac netprog/client/clientapp/ClientApp.java
javac netprog/server/GossipServer.java
javac netprog/server/networking/UDPServer.java
javac netprog/server/networking/UDPConnectionHandler.java
javac netprog/server/networking/TCPServer.java
javac netprog/server/networking/TCPConnectionHandler.java
echo "Done."


