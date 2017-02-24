package org.greycat.benchs.raspberrypi.rocksdb;


import greycat.*;
import greycat.internal.CoreDeferCounterSync;
import greycat.rocksdb.RocksDBStorage;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.greycat.benchs.raspberrypi.rocksdb.InitBenchRead.*;

class BenchWrite  {
    public static final String DB_PATH = "benchWriteDB";

    private static final int MEMORY_SIZE = 780_000;

    private static final String currentRootNodeVar = "currentRoot";
    private static final String currentChildNodeVar = "currentChildNode";
    private static final String inMemoryVar = "inMemory";
    private static final Task benchTask = Tasks.newTask()
            .travelInTime("0")
            .travelInWorld("0")
            .loop("1","4", Tasks.newTask()
                    .createNode()
                    .setAttribute(ATT_NAME, ATT_NAME_TYPE,ROOT_NAME_BASE + "{{i}}")
                    .ifThen(ctx -> ctx.variable(inMemoryVar).get(0).equals(false),Tasks.newTask().save())
                    .addToGlobalIndex(INDEXNAME,ATT_NAME)
                    .ifThen(ctx -> ctx.variable(inMemoryVar).get(0).equals(false),Tasks.newTask().save())
                    .defineAsGlobalVar(currentRootNodeVar)
                    .loop("1","48748", Tasks.newTask()
                            .createNode()
                            .setAttribute(ATT_NAME,ATT_NAME_TYPE,CHILD_NAME_BASE + "{{i}}")
                            .setAsVar(currentChildNodeVar)
                            .readVar(currentRootNodeVar)
                            .addVarToRelation(REL_CHILDREND,currentChildNodeVar)
                            .ifThen(ctx -> ctx.variable(inMemoryVar).get(0).equals(false),Tasks.newTask().save())
                    )

            );
    private long internalRun(final Graph graph, final Task finakBenchTask) {
        DeferCounterSync defer = new CoreDeferCounterSync(1);
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean succed) {
                long start, end;
                TaskResult result;
                start = System.currentTimeMillis();
                result = finakBenchTask.executeSync(graph);
                end = System.currentTimeMillis();
                result.clear();
                if(result.exception() != null) {
                    result.exception().printStackTrace();
                    System.exit(2);
                }
                defer.wrap().on((end - start));
                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        defer.count();
                    }
                });


            }
        });
        return (long) defer.waitResult();
    }

    public void run(final int warmupIter, final int nbIter, final boolean inFullMemory) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                end();
            }
        });

        final GraphBuilder builder = new GraphBuilder()
                .withMemorySize(MEMORY_SIZE);

        if(!inFullMemory) {
            builder.withStorage(new RocksDBStorage(DB_PATH));
        }

        System.out.println("Run Bench Write");

        Task finakBenchTask = Tasks.newTask()
                .inject(inFullMemory)
                .defineAsGlobalVar(inMemoryVar)
                .map(benchTask);

        long duration;
        for(int i=0;i<warmupIter;i++) {
            Graph graph = builder.build();
            duration = internalRun(graph,finakBenchTask);
            System.out.println("Warmup " + i + " -> " + duration);
            if(!inFullMemory) {
                end();
            }
        }

        long sum = 0;
        for(int i=0;i<nbIter;i++) {
            Graph graph = builder.build();
            duration = internalRun(graph,finakBenchTask);
            System.out.println("Execution " + i + " -> " + duration);
            sum += duration;
            if(!inFullMemory) {
                end();
            }
        }
        System.out.println("Average duration: " + sum / nbIter + " ms");

    }


    private void end() {
        Path directory = Paths.get(DB_PATH);
        try {
            if (Files.exists(directory)) {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
