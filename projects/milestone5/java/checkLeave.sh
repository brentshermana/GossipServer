#!/bin/bash/
echo "Since this script starts our actual server, it first calls the compile.sh script to make sure it is compiled."
./compile.sh
echo "This will start a specific testing master program that starts our actual server with a special client program made for automated testing.  The testing client will register a peer Abel on port 56050, the same port our witness server is automatically set to start on (via the scriptServerPeerWitness.sh script) so that the witness server will be forwarded the gossip message that is sent before the peer is forgotten after the specified five seconds.  The testing client will then use UDP (arbitrarily chosen) to send a gossip message 2 seconds after registering the peer, and a second gossip message 7 seconds after that."
echo "Since this actually starts our server, it will run in an infinite loop as the server should do, waiting for clients to connect.  As a result, when you are done testing / running the client test scripts, you will have to manually close the server program to stop it, such as with an abort command like Ctrl-C"
java netprog/MasterForLeaveTestScript -d ./check_leave_data -p 2356 -v -U -D 5
