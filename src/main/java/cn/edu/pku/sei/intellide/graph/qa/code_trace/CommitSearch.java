package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.java.JavaExtractor;
import cn.edu.pku.sei.intellide.graph.qa.code_search.GraphReader;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.CommitResult;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jNode;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CommitSearch {
    private GraphDatabaseService db;
    private GraphReader graphReader;
    private List<MyNode> graph;

    public CommitSearch(GraphDatabaseService db){
        this.db=db;
        this.graphReader=new GraphReader(db);
        this.graph=graphReader.getAjacentGraph();
    }

    public List<CommitResult> searchCommitResultByClassName(String className){
        for (MyNode node : graph) {
            if(node.fullName.equals(className)){
                System.out.println("Hit!!");
                return getCommitResult(node.getId(),className);
            }
        }
        return null;
    }

    public List<CommitResult> searchCommitResultByMethodName(String methodName){

        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Node> iterator = db.findNodes(JavaExtractor.METHOD);
            while(iterator.hasNext()){
                Node node =iterator.next();
                if(node.getProperty("name").toString().equals(methodName)){
                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while(relationIter.hasNext()){
                        Relationship r=relationIter.next();
                        if(r.isType(JavaExtractor.HAVE_METHOD)){
                            Node otherNode=r.getOtherNode(node);
                            return getCommitResult(otherNode.getId(),methodName);
                        }
                    }
                }
            }
            tx.success();
        }
        return null;
    }

    public List<CommitResult> getCommitResult(Long id,String className){

        List<CommitResult>result=new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Iterator<Relationship> rels = db.getNodeById(id).getRelationships().iterator();
            while (rels.hasNext()) {
                Relationship rel = rels.next();
                Node otherNode = rel.getOtherNode(db.getNodeById(id));
                if(otherNode.getLabels().iterator().next().name().equals("Commit")){

                    String diffMessage=null;
                    if(rel.hasProperty("diffMessage")){
                        diffMessage=rel.getProperty("diffMessage").toString();

           //             System.out.println(diffMessage);
                    }


                    result.add(CommitResult.get(otherNode.getId(),db,className,diffMessage));
                }
            }
            result.sort(new Comparator<CommitResult>() {
                @Override
                public int compare(CommitResult o1, CommitResult o2) {
                    String date1=o1.getCommitTime();
                    String date2=o2.getCommitTime();
                    return date2.compareTo(date1);
                }
            });
            tx.success();
        }

        return result;
    }


    public static String Recover(String code,String diff ){
        String result="";
        String []Diffline=diff.split("\n");
        String[]content=code.split("\n");
        int prestart=1;
        int prelength=0;
        int next=1;
        for(int j=0;j<Diffline.length;++j) {
            if (Diffline[j].startsWith("@@")) {
                String temp = Diffline[j].split(" ")[2];
                int start = Integer.parseInt(temp.split(",")[0]);
                int len = Integer.parseInt(temp.split(",")[1]);
                for (int k = prestart + prelength; k < start && k <= content.length; ++k) {
                    result+=content[k-1]  + "\n";
                }
                prestart = start;
                prelength = len;
                int t;
                for (t = j + 1; t<Diffline.length&&!Diffline[t].startsWith("@@"); ++t) {
                    if (!Diffline[t].startsWith("+")) {
                        result += Diffline[t].substring(1) + "\n";
                    }
                }
                j = t - 1;
            }
        }

        if(prelength+prestart<=content.length){
            for (int k = prestart + prelength; k <= content.length; ++k) {
                result+=content[k-1]  + "\n";
            }
        }
        return result;
    }

    public static void main(String args[]){
        GraphDatabaseService db=new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\hama"));
        CommitSearch commitSearch=new  CommitSearch(db);


        String content="/**\n" +
                " * Licensed to the Apache Software Foundation (ASF) under one\n" +
                " * or more contributor license agreements.  See the NOTICE file\n" +
                " * distributed with this work for additional information\n" +
                " * regarding copyright ownership.  The ASF licenses this file\n" +
                " * to you under the Apache License, Version 2.0 (the\n" +
                " * \"License\"); you may not use this file except in compliance\n" +
                " * with the License.  You may obtain a copy of the License at\n" +
                " *\n" +
                " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
                " *\n" +
                " * Unless required by applicable law or agreed to in writing, software\n" +
                " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                " * See the License for the specific language governing permissions and\n" +
                " * limitations under the License.\n" +
                " */\n" +
                "package org.apache.hama.pipes;\n" +
                "\n" +
                "import java.io.IOException;\n" +
                "\n" +
                "import org.apache.commons.logging.Log;\n" +
                "import org.apache.commons.logging.LogFactory;\n" +
                "import org.apache.hadoop.conf.Configuration;\n" +
                "import org.apache.hadoop.io.BytesWritable;\n" +
                "import org.apache.hama.bsp.Partitioner;\n" +
                "\n" +
                "/**\n" +
                " * \n" +
                " * PipesPartitioner is a Wrapper for C++ Partitioner Java Partitioner ->\n" +
                " * BinaryProtocol -> C++ Partitioner and back\n" +
                " * \n" +
                " */\n" +
                "public class PipesPartitioner<K, V> implements Partitioner<K, V> {\n" +
                "\n" +
                "  private static final Log LOG = LogFactory.getLog(PipesPartitioner.class);\n" +
                "  private PipesApplication<K, V, ?, ?, BytesWritable> application = new PipesApplication<K, V, Object, Object, BytesWritable>();\n" +
                "\n" +
                "  public PipesPartitioner(Configuration conf) {\n" +
                "    LOG.debug(\"Start Pipes client for PipesPartitioner.\");\n" +
                "    try {\n" +
                "      application.start(conf);\n" +
                "    } catch (IOException e) {\n" +
                "      LOG.error(e);\n" +
                "    } catch (InterruptedException e) {\n" +
                "      LOG.error(e);\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  public void cleanup() {\n" +
                "    try {\n" +
                "      application.cleanup(true);\n" +
                "    } catch (IOException e) {\n" +
                "      LOG.error(e);\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  /**\n" +
                "   * Partitions a specific key value mapping to a bucket.\n" +
                "   * \n" +
                "   * @param key\n" +
                "   * @param value\n" +
                "   * @param numTasks\n" +
                "   * @return a number between 0 and numTasks (exclusive) that tells which\n" +
                "   *         partition it belongs to.\n" +
                "   */\n" +
                "  @Override\n" +
                "  public int getPartition(K key, V value, int numTasks) {\n" +
                "    int returnVal = 0;\n" +
                "    try {\n" +
                "\n" +
                "      if ((application != null) && (application.getDownlink() != null)) {\n" +
                "        returnVal = application.getDownlink()\n" +
                "            .getPartition(key, value, numTasks);\n" +
                "      } else {\n" +
                "        LOG.warn(\"PipesApplication or application.getDownlink() might be null! (application==null): \"\n" +
                "            + ((application == null) ? \"true\" : \"false\"));\n" +
                "      }\n" +
                "\n" +
                "    } catch (IOException e) {\n" +
                "      LOG.error(e);\n" +
                "    }\n" +
                "    LOG.debug(\"getPartition returns: \" + returnVal);\n" +
                "    return returnVal;\n" +
                "  }\n" +
                "  \n" +
                "}\n";

        List<CommitResult> list = commitSearch.searchCommitResultByClassName("org.apache.hama.pipes.PipesPartitioner");
        String history[]=new String[list.size()];
        for(int i=0;i<history.length;++i){
            String diff=list.get(i).getDiffMessage();
            if(i==0){
                history[i]=Recover(content,diff);
            }else{
                history[i]=Recover(history[i-1],diff);
            }


            System.out.println("-------------------------------------");
            System.out.println(diff);
            System.out.println("-------------------------------------");
            System.out.println(history[i]);
            System.out.println("-------------------------------------");
        }
    }
}
