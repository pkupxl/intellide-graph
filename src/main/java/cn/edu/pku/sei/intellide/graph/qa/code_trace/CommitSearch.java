package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.qa.code_search.GraphReader;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.CommitResult;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
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
                    if(rel.hasProperty("diffMessage"))
                        diffMessage=rel.getProperty("diffMessage").toString();

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

    public static void main(String args[]){
        CommitSearch commitSearch=new  CommitSearch(new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:/Lucene")));
        List<CommitResult> list = commitSearch.searchCommitResultByClassName("org.apache.lucene.index.IndexReader");
        for(CommitResult n:list){

            System.out.println("--------------------");

         // System.out.println(n.getId());
         // System.out.println(n.getName());
         //   System.out.println(n.getGitUser());
       //     System.out.println(n.getGitUserEmail());
       //     System.out.println(n.getCommitTime());
        //    System.out.println(n.getCommitMessage());
        //    System.out.println(n.getDiffMessage());
            System.out.println(n.getDiffSummary());
            System.out.println("--------------------");
           // break;
        }
    }
}
