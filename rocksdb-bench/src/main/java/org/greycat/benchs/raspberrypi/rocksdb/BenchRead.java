package org.greycat.benchs.raspberrypi.rocksdb;

import greycat.*;
import greycat.rocksdb.RocksDBStorage;
import greycat.struct.Relation;

public class BenchRead {
    public static final String DB_PATH = "bench1DB";

    private static final int MEMORY_SIZE = 10_000;
    private static final int MEMORY_SIZE_FULLMEM = 5_000_000;


    private static final String IN_MEMORY_VAR = "inMemory";

    private static final Task benchTask = Tasks.newTask()
            .travelInTime("0")
            .travelInWorld("0")
            .thenDo(new ActionFunction() {
                @Override
                public void eval(TaskContext ctx) {
                    ctx.graph().index(0, 0, InitBenchRead.INDEXNAME, new Callback<NodeIndex>() {
                        @Override
                        public void on(NodeIndex indexNode) {
                            final long[] indexed = indexNode.all();
                            indexNode.free();
                            ctx.continueWith(ctx.wrap(indexed));
                        }
                    });
                }
            })
            .loop("1","3", Tasks.newTask()
                    .forEach(
                            Tasks.newTask()
                                    .lookup("{{result}}")
                                    .thenDo(new ActionFunction() {
                                        @Override
                                        public void eval(TaskContext ctx) {
                                            Node node = (Node) ctx.result().get(0);
                                            final long[] children = ((Relation)node.get(InitBenchRead.REL_CHILDREND)).all();
                                            ctx.continueWith(ctx.wrap(children));
                                        }
                                    })
                                    .forEach(
                                            Tasks.newTask()
                                                    .lookup("{{result}}")
                                                    .clearResult()
                                                    .ifThen(ctx -> ctx.graph().space().available() < MEMORY_SIZE * 0.1, Tasks.newTask().save())
                                    )
                    )
            )
            ;


    public void run(final int warmupIter, final int nbIter, final boolean inFullMemory) {
        GraphBuilder builder = GraphBuilder.newBuilder()
                .withReadOnlyStorage(new RocksDBStorage(DB_PATH));

        if(inFullMemory) {
            builder.withMemorySize(MEMORY_SIZE_FULLMEM);
        } else {
            builder.withMemorySize(MEMORY_SIZE);
        }

        final Task finalBenchTask = Tasks.newTask()
                .inject(inFullMemory)
                .defineAsGlobalVar(IN_MEMORY_VAR)
                .map(benchTask);

        final Graph graph = builder.build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean succeed) {
                TaskResult result;
                System.out.print("Init Bench Read");
                if(inFullMemory) {
                    System.out.println(" in Full Memory");
                    result = finalBenchTask.executeSync(graph);
                    result.clear();
                    if(result.exception() != null) {
                        result.exception().printStackTrace();
                        System.exit(2);
                    }
                    result = null;
                } else {
                    System.out.print("\n");
                }

                long start, end;
                for(int i=0;i<warmupIter;i++) {
                    System.out.print("Warmup " + i + " -> ");
                    start = System.currentTimeMillis();
                    result = finalBenchTask.executeSync(graph);
                    end = System.currentTimeMillis();
                    System.out.println(end - start);
                    result.clear();
                    if(result.exception() != null) {
                        result.exception().printStackTrace();
                        System.exit(2);
                    }
                    result = null;
                }

                long sum = 0;
                for(int i=0;i<nbIter;i++) {
                    System.out.print("Execution " + i + " -> ");
                    start = System.currentTimeMillis();
                    result =  finalBenchTask.executeSync(graph);
                    end = System.currentTimeMillis();
                    System.out.println(end - start);
                    sum += (end - start);
                    result.clear();
                    if(result.exception() != null) {
                        result.exception().printStackTrace();
                        System.exit(2);
                    }
                }

                System.out.println("Average duration: " + sum / nbIter + " ms");
            }
        });
    }

}
