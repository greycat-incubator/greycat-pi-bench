GreyCat on Raspberry Pi - Benchs
--------------------------------

# RocksDB Benchs


## Results

### Environment

**Environment 1:**

- [GreyCat](https://github.com/datathings/greycat) v. 3-SNAPSHOT, commit `b2b84d2`
- [Raspberry Pi 3](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)
- [Raspbian GNU/Linux 8.0 (jessie)](https://www.raspberrypi.org/downloads/raspbian/)
- Java version 1.8.0_65, Oracle JDK for arm32

**Environment 2:**

- [GreyCat](https://github.com/datathings/greycat) v. 3-SNAPSHOT, commit `b2b84d2`
- MacBook Pro (Retina 15-inch, Mid 2015), Intel Core i7 2.8GHz (SSD)
- macOS Sierra, version 10.12.3
- Java version 1.8.0_65, Oracle JDK for arm32

### Bench Read

**What we do:** We created a model of 1M elements, using `org.greycat.benchs.raspberrypi.rocksdb.InitBenchRead` class. (The data are also available [here](https://drive.google.com/drive/folders/0B7xyWA_exiCBb3l6TTRWMTlqWkU?usp=sharing)). During the bench, we navigate 3 times all the graph, by resolving, using the `Graph.lookup` method, each node one by one. We start with 10 warmup iterations, following by 100 iterations. The values reported in the table below is the average of the time execution (in ms) of these 100 iterations. There is two versions of the benchmark:

- Full Memory one: we load all the data in memory, and resolve them. (As the Raspberry Pi has not enough memory to have all the graph in memory, we didn't make this benchmark)
- RocksDB: we resolve one by one the elements, with in memory at most 2500 nodes. So In this benchmark the GC is also used to remove old data when we resolve new nodes.  

Table with final result (in ms)

   | MacBook Pro | Raspberry Pi
---|-------------|-------------
 **Full Memory** | 1378 | X
 **RocksDB** | 8974 | 139625

Table with nodes/seconds (* 1000)

   | MacBook Pro | Raspberry Pi
---|-------------|-------------
 **Full Memory** | 2177 | X
 **RocksDB** | 334 | 21,5



### Bench Write

**What we do:** We created a model with 194_996 elements (this small number is needed to allow the execution on Raspberry Pi, that has only 1GB of memory). There is two versions of the bench:

- Full memory one: the model element are created, store in memory but never persist
- RocksDB: each time a node is created, we persist it in a RocksDB data base.

Table with "pure" results (in ms)

   | MacBook Pro | Raspberry Pi
---|-------------|-------------
**Full Memory** | 525 | 11158
**RocksDB** | XX | XX

Table with nodes/seconds (* 1000)

   | MacBook Pro | Raspberry Pi
---|-------------|-------------
 **Full Memory** | 371,42 | 17,47
 **RocksDB** | XX | XX
