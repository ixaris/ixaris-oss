# Zookeeper Clustering

## Sequences

This module maintains a default sequence, which is used to provide each node with a unique id, 
which in turn is used by the UniqueIdGenerator to cluster-wise unique ids by combining this
unique node id with a monotonically increasing timestamp. In addition, each cluster has its own
named sequence to provide close to sequential node ids for the cluster. The current 
maximum for sequences in 65527 (16 bits less the first 8 spaces reserved) which implies that the
total number of nodes supported is 65527. 

Sequences are maintained under /discovery/sequences. Sequences are split in 256 groups stored as 
hex string paths from 00 to FF, each group having up to 256 children 00 to FF. The Children nodes 
are ephemeral, i.e. removed in the node shuts down or crashes. The first 8 children of the first 
group are reserved, so the first item in a sequence is /discovery/sequences/<name>/00/08

## Clusters    

Clusters are registered by name. It is essential that cluster names are unique. Each cluster has a
corresponding sequence with the same name from each each node gets a node id. The cluster discovery
data is organised under /discovery/clusters/<name>/<node_id> where node id is represented as 4 hex
characters, the first node being 0008.

