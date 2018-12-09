package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.qa.code_search.GraphReader;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jRelation;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

public class IssueSearch {
    private GraphDatabaseService db;
    private GraphReader graphReader;
    private List<MyNode> graph;

    public IssueSearch(GraphDatabaseService db){
        this.db=db;
        this.graphReader=new GraphReader(db);
        this.graph=graphReader.getAjacentGraph();
    }

    public List<Neo4jNode> searchIssueNodeByClassName(String className){
        for (MyNode node : graph) {
           if(node.fullName.equals(className)){
               System.out.println("Hit!!");
               return getNeighborIssueNode(node.getId());
           }
        }
        return null;
    }

    public List<Neo4jNode> getNeighborIssueNode(Long id){

        List<Neo4jNode>result=new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Iterator<Relationship> rels = db.getNodeById(id).getRelationships().iterator();
            while (rels.hasNext()) {
                Relationship rel = rels.next();
                if(!rel.getType().name().equals("codeMention"))continue;
                Node otherNode = rel.getOtherNode(db.getNodeById(id));
                if(otherNode.getLabels().iterator().next().name().equals("JiraIssue")){
                    result.add(Neo4jNode.get(otherNode.getId(),db));
                }
            }
            tx.success();
        }
        return result;
    }

    public static void main(String args[]){
        IssueSearch issueSearch=new IssueSearch(new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:/Work/Lucene")));
        List<Neo4jNode> list = issueSearch.searchIssueNodeByClassName("org.apache.lucene.index.IndexReader");
        for(Neo4jNode n:list){
            System.out.println(n.getLabel());
        }
    }

}