# Zookeeper microservices library

This library provides an implementation to microservices discovery and registry interfaces, as well as resolving
a unique cluster id for every registered node

## Cluster-wide Unique Identifier

This library is able to integrate with the `UniqueIdGenerator` to manage the node id, in order to provide the guarantee 
that IDs generated from different microservices never conflict.  This is achieved by managing the node id bit section
in the unique ids.  This library uses zookeeper to register the different microservice instances and during startup, 
each service is assigned a unique ID.

The maximum allowed bit width for cluster node id is 16 bits, and first 8 ids are reserved (effective range is 8 - 0xFFFF).
The reserved node ids are intended to be used by services which are not managed as microservices (e.g. IPS1). 
IDs are organised in 256 groups, with each group having 256 IDs. The reason for this is that zookeeper does not like znodes to have thousands
of child nodes, so flattening this would result in a worst case of having 65535 child nodes.

The znodes are organised as follows:

- discovery
  - nodes
    - 00 (group 0, indices 00 - 07 are reserved)
      - 08 (interpreted as id 0x0008)
      - 09
      - ...
      - FF (interpreted as id 0x00FF, i.e. id 255)
    - 01
      - 00 (interpreted as id 0x0100, i.e. id 256)
      - 01
      - ...
    - ...

A node obtains a unique id as follows:

- for each group
  - if group is not full
    - find first gap in ids and claim it by creating an ephemeral node

The Unique Node Id resolver also watches the groups znode and increases the required bit width in the unique id generator 
accordingly. The unique id generator uses any extra bits for sub-millisecond id burst generation. 
Thus if, e.g. we have 1000 nodes,
then we are only using 4 groups, we only need 11 bits out of the maximum width of 16 for the node id, leaving the
remaining 5 bits for sub-millisecond id generation.

## Service Discovery

A microservice is represented in service discovery as a service type.  Each microservice can have multiple instances
for high-availability purposes running in parallel.  These different instances have their own unique node id assigned
from the algorithm in the Unique Id resolver (described above).  Each service type can expose a number of (scsl) endpoints. 

Data is organised as follows:

- discovery
  - servicetypes
    - <service name>
      - <node id> (ephemeral)
  - endpoints
    - <endpoint name>
      - <key> (actual key for spi implementations, _ for normal services)
        - <service node ref> (ephemeral)
