package org.greycat.benchs.raspberrypi.rocksdb;


import greycat.*;
import greycat.rocksdb.RocksDBStorage;

public class InitBenchRead {
    public static final String ATT_NAME = "name";
    public static final byte ATT_NAME_TYPE = Type.STRING;
    public static final String INDEXNAME = "rootIndex";
    public static final String REL_CHILDREND = "children";

    public static final String ROOT_NAME_BASE = "rootNode_";
    public static final String CHILD_NAME_BASE = "childNode_";



    private static final String level1SizeVar = "level1Size";
    private static final String level2SizeVar = "level2Size";
    private static final String currentRootNodeVar = "currentRoot";
    private static final String currentChildNodeVar = "currentChildNode";

    public static final Task initBench = Tasks.newTask()
            .travelInTime("0")
            .travelInWorld("0")
            .loop("1","{{" + level1SizeVar + "}}",
                Tasks.newTask()
                .createNode()
                .setAttribute(ATT_NAME, ATT_NAME_TYPE,ROOT_NAME_BASE + "{{i}}")
                .addToGlobalIndex(INDEXNAME,ATT_NAME)
                .defineAsGlobalVar(currentRootNodeVar)
                .loop("1","{{" + level2SizeVar + "}}",
                    Tasks.newTask()
                    .createNode()
                    .setAttribute(ATT_NAME,ATT_NAME_TYPE,CHILD_NAME_BASE + "{{i}}")
                    .setAsVar(currentChildNodeVar)
                    .readVar(currentRootNodeVar)
                    .addVarToRelation(REL_CHILDREND,currentChildNodeVar)
                )
                .save()
            )
            .save();

    /**
     * Build a model with two levels: the first level nodes has one relationship to the nodes of the second level
     * The first level nodes are indexed
     *
     * @param level1Size number of nodes in the level 1 (in total)
     * @param level2Size number of nodes in the level 2 (in total)
     * @param dbPath path to the DB
     * @param memorySize size of the memory allocated for the Graph
     */
    public static void initBench(int level1Size, int level2Size, String dbPath, long memorySize) {
        Graph graph = GraphBuilder.newBuilder()
                .withMemorySize(memorySize)
                .withStorage(new RocksDBStorage(dbPath))
                .build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                System.out.println("Start creation...");
                Tasks.newTask()
                        .inject(level1Size)
                        .setAsVar(level1SizeVar)
                        .inject((level2Size / level1Size))
                        .setAsVar(level2SizeVar)
                        .map(initBench)
                        .save()
                        .clearResult()
                        .execute(graph, new Callback<TaskResult>() {
                            @Override
                            public void on(TaskResult result) {
                                if(result.exception() != null) {
                                    result.exception().printStackTrace();
                                }
                                graph.disconnect(new Callback<Boolean>() {
                                    @Override
                                    public void on(Boolean result) {
                                        System.out.println("Done");
                                    }
                                });
                            }
                        });
            }
        });

    }
}
