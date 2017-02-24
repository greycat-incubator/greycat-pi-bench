package org.greycat.benchs.raspberrypi.rocksdb;


public class Runner {

    public static void main(String[] args) {
        //BenchRead
//        InitBenchRead.initBench(4,250_000,BenchRead.DB_PATH,4_500_00);
//        BenchRead bench = new BenchRead();

        // Full Memory
//        bench.run(10,100,true);

        // Each lookup will read in DB
        // The cache is empty every 2500 elements
//        bench.run(10,100,false);




        //Bench Write
        BenchWrite benchWrite = new BenchWrite();

        //Full memory
//        benchWrite.run(10,100,true);

        // Nodes are directly persists after their creation
        benchWrite.run(10,100,false);

    }
}
