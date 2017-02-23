package org.greycat.benchs.raspberrypi.rocksdb;


import greycat.*;
import greycat.rocksdb.RocksDBStorage;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.greycat.benchs.raspberrypi.rocksdb.InitBenchRead.*;

class BenchWrite  {
    public static final String DB_PATH = "benchWriteDB";

    private static final int MEMORY_SIZE = 240_000;

    private static final String currentRootNodeVar = "currentRoot";
    private static final String currentChildNodeVar = "currentChildNode";
    private static final String inMemoryVar = "inMemory";
    private static final Task benchTask = Tasks.newTask()
            .travelInTime("0")
            .travelInWorld("0")
            .loop("1","4", Tasks.newTask()
                    .createNode()
                    .setAttribute(ATT_NAME, ATT_NAME_TYPE,ROOT_NAME_BASE + "{{i}}")
                    .save()
                    .addToGlobalIndex(INDEXNAME,ATT_NAME)
                    .save()
                    .defineAsGlobalVar(currentRootNodeVar)
                    .loop("1","15000", Tasks.newTask()
                            .createNode()
                            .setAttribute(ATT_NAME,ATT_NAME_TYPE,CHILD_NAME_BASE + "{{i}}")
                            .setAsVar(currentChildNodeVar)
                            .readVar(currentRootNodeVar)
                            .addVarToRelation(REL_CHILDREND,currentChildNodeVar)
                            .save()
                    )

            )
            .save();

    public void run(final int warmupIter, final int nbIter, final boolean inFullMemory) {
        final GraphBuilder builder = new GraphBuilder()
                .withMemorySize(MEMORY_SIZE);

        if(!inFullMemory) {
            builder.withStorage(new RocksDBStorage(DB_PATH));
        }

        System.out.print("Run Bench Write");
        final Graph graph = builder.build();

        Task finakBenchTask = Tasks.newTask()
                .inject(inFullMemory)
                .defineAsGlobalVar(inMemoryVar)
                .map(benchTask);

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean succeed) {
                TaskResult result;
                long start, end;
                for(int i=0;i<warmupIter;i++) {
                    System.out.print("Warmup " + i + " -> ");
                    start = System.currentTimeMillis();
                    result = finakBenchTask.executeSync(graph);
                    end = System.currentTimeMillis();
                    System.out.println(end - start);
                    result.clear();
                    if(result.exception() != null) {
                        result.exception().printStackTrace();
                        System.exit(2);
                    }
                }

                long sum = 0;
                for(int i=0;i<nbIter;i++) {
                    System.out.print("Execution " + i + " -> ");
                    start = System.currentTimeMillis();
                    result = finakBenchTask.executeSync(graph);
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
                graph.disconnect(new Callback<Boolean>() {
                    @Override
                    public void on(Boolean result) {
                        end();
                        System.out.println("End");

                    }
                });
            }
        });


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
