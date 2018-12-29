package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.code_mention.CodeMentionExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.java.JavaExtractor;
import cn.edu.pku.sei.intellide.graph.qa.code_search.GraphReader;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jRelation;
import org.neo4j.graphdb.*;
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
               return getIssueNode(node.getId());
           }
        }
        return null;
    }

    public List<Neo4jNode> searchIssueNodeByMethodName(String methodName){

        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Node> iterator = db.findNodes(JavaExtractor.METHOD);
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getProperty("name").toString().equals(methodName)) {
                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship r = relationIter.next();
                        if (r.isType(JavaExtractor.HAVE_METHOD)) {
                            Node otherNode = r.getOtherNode(node);
                            return getIssueNode(otherNode.getId());
                        }
                    }
                }
            }
            tx.success();
        }

        return null;
    }

    public List<Neo4jNode> getIssueNode(Long id){

        List<Long> issueIdResult=new ArrayList();
        List<Neo4jNode>result=new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Iterator<Relationship> rels = db.getNodeById(id).getRelationships().iterator();
            while (rels.hasNext()) {
                Relationship rel = rels.next();
                if(rel.isType(CodeMentionExtractor.ADD)||rel.isType(CodeMentionExtractor.DELETE)||rel.isType(CodeMentionExtractor.MODIFY)){
                    Node commitNode = rel.getOtherNode(db.getNodeById(id));
                    Iterator<Relationship> issueRels = commitNode.getRelationships().iterator();
                    while(issueRels.hasNext()){
                        Relationship issueRel = issueRels.next();
                        if(issueRel.isType(CodeMentionExtractor.COMMIT_FOR_ISSUE)){
                            Node issueNode=issueRel.getOtherNode(commitNode);
                            if(!issueIdResult.contains(issueNode.getId())){
                                issueIdResult.add(issueNode.getId());
                            }
                        }
                    }
                }
            }

            for(int i=0;i<issueIdResult.size();++i){
                result.add(Neo4jNode.get(issueIdResult.get(i),db));
            }

            result.sort(new Comparator<Neo4jNode>() {
                @Override
                public int compare(Neo4jNode o1, Neo4jNode o2) {
                    String date1=db.getNodeById(o1.getId()).getProperty("createdDate").toString();
                    String date2=db.getNodeById(o2.getId()).getProperty("createdDate").toString();
                    return date2.compareTo(date1);
                }
            });
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
