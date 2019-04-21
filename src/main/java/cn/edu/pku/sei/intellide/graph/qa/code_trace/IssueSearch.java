package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.code_mention.CodeMentionExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.java.JavaExtractor;
import cn.edu.pku.sei.intellide.graph.qa.code_search.GraphReader;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.CommitResult;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jNode;
import org.neo4j.graphdb.*;
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

    public List<Neo4jNode> search(CodeAnalyzer analyzer){
        if(analyzer.getType().equals("Class")){
            return searchIssueResultByClassName(analyzer);
        }else if(analyzer.getType().equals("Method")){
            return searchIssueResultByMethodName(analyzer);
        }else{
            return searchIssueResultByLines(analyzer);
        }
    }

    public List<Neo4jNode> searchIssueResultByClassName(CodeAnalyzer analyzer){
        String className=analyzer.getFullNameFromCode();
        for (MyNode node : graph) {
            if(node.fullName.equals(className)){
                System.out.println("Hit!!");
                return getIssueResult(node.getId(),analyzer);
            }
        }
        return null;
    }

    public List<Neo4jNode> searchIssueResultByMethodName(CodeAnalyzer analyzer){
        return searchIssueResultByLines(analyzer);
       /* String methodName = analyzer.getMethodNameFromCode();
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
                            return getIssueResult(otherNode.getId(),analyzer);
                        }
                    }
                }
            }
            tx.success();
        }
        return null;*/
    }

    public List<Neo4jNode> searchIssueResultByLines(CodeAnalyzer analyzer){
        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Node> iterator = db.findNodes(JavaExtractor.CLASS);
            while (iterator.hasNext()) {
                Node node = iterator.next();
                String content=node.getProperty(JavaExtractor.CONTENT).toString();
                if(content.contains(analyzer.getCode())){
                    return getIssueResult(node.getId(),analyzer);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }


   /* public List<Neo4jNode> searchIssueNodeByClassName(String className){
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
*/


    public List<Neo4jNode> getIssueResult(Long id,CodeAnalyzer analyzer) {
        List<Long> issueIdResult = new ArrayList();
        List<Neo4jNode> result = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Iterator<Relationship> rels = db.getNodeById(id).getRelationships().iterator();
            while (rels.hasNext()) {
                Relationship rel = rels.next();
                if (rel.isType(CodeMentionExtractor.ADD) || rel.isType(CodeMentionExtractor.DELETE) || rel.isType(CodeMentionExtractor.MODIFY)) {
                    boolean isRelated=false;
                    if(analyzer.getType().equals("Class")){
                        isRelated=true;
                    }else{
                        String content=analyzer.getCode();
                        String[] lines=content.split("\n");
                        String diffMessage="";
                        if(rel.hasProperty("diffMessage")){
                            diffMessage=rel.getProperty("diffMessage").toString();
                        }
                        for(int i=0;i<lines.length;++i){
                            if(diffMessage.contains(lines[i])){
                                isRelated=true;
                                break;
                            }
                        }
                    }
                    if(!isRelated)continue;
                    Node commitNode = rel.getOtherNode(db.getNodeById(id));
                    Iterator<Relationship> issueRels = commitNode.getRelationships().iterator();
                    while (issueRels.hasNext()) {
                        Relationship issueRel = issueRels.next();
                        if (issueRel.isType(CodeMentionExtractor.COMMIT_FOR_ISSUE)) {
                            Node issueNode = issueRel.getOtherNode(commitNode);
                            if (!issueIdResult.contains(issueNode.getId())) {
                                issueIdResult.add(issueNode.getId());
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < issueIdResult.size(); ++i) {
                result.add(Neo4jNode.get(issueIdResult.get(i), db));
            }

            result.sort(new Comparator<Neo4jNode>() {
                @Override
                public int compare(Neo4jNode o1, Neo4jNode o2) {
                    String date1 = db.getNodeById(o1.getId()).getProperty("createdDate").toString();
                    String date2 = db.getNodeById(o2.getId()).getProperty("createdDate").toString();
                    return date2.compareTo(date1);
                }
            });
            tx.success();
        }
        return result;
    }
}