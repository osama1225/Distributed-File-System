Distributed-File-System
=======================

A simple Distributed File System that is implemented in java using RMI with the following features:               

1- Each file is replicated on several servers.

2- A Master server that contains metadata about replica-servers locations and mapping between files and their locations.

3- Clients request read/write on file by:
  - Send the request to the master server with the file name.
  - Server responds with the locations of the file.
  - Client then forward the request to the primary server.

4- Updates are propagated to the other replicas of the same file using "primary based consistency protocol".

5- Handling abort operations for the transaction.

6- Handling multiple Access to the same file, such that operations from transaction with lower time stamp are done before operations from transactions with higher time stamp.

Hint: if you will use this code, Write your own Main Class.
