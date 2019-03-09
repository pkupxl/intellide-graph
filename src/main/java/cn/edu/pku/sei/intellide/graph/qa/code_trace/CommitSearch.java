package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.code_mention.CodeMentionExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.java.JavaExtractor;
import cn.edu.pku.sei.intellide.graph.qa.code_search.GraphReader;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.CommitResult;
import org.neo4j.graphdb.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CommitSearch {
    private GraphDatabaseService db;
    private GraphReader graphReader;
    private List<MyNode> graph;

    public GraphDatabaseService getDb(){
        return this.db;
    }

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
        List<Node> resultNodes=new ArrayList<>();
        List<CommitResult>result=new ArrayList<>();
        try (Transaction tx = db.beginTx()) {

            Node currentNode=db.getNodeById(id);
            Iterator<Relationship> rels = db.getNodeById(id).getRelationships().iterator();
            while (rels.hasNext()) {
                Relationship rel = rels.next();
                if(rel.isType(CodeMentionExtractor.MODIFY)||rel.isType(CodeMentionExtractor.ADD)||rel.isType(CodeMentionExtractor.DELETE)){
                    Node otherNode=rel.getOtherNode(currentNode);
                    if(resultNodes.contains(otherNode))continue;
                    resultNodes.add(otherNode);
                    String diffMessage="";
                    if(rel.hasProperty("diffMessage")){
                        diffMessage=rel.getProperty("diffMessage").toString();
                        //System.out.println(diffMessage);
                    }
                    CommitResult commitResult=CommitResult.get(otherNode.getId(),db,className,diffMessage);
                    if(!result.contains(commitResult)){
                        result.add(commitResult);
                    }
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

        for(int i=result.size()-1;i>=0;--i){
           result.get(i).setCommitTime(TimeStamp2Date(result.get(i).getCommitTime()));
        }
        return result;
    }


    public String TimeStamp2Date(String timestampString){
        Long timestamp = Long.parseLong(timestampString)*1000;
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
        return date;
    }

    public List<CommitResult> search(CodeAnalyzer analyzer){
        if(analyzer.getType().equals("Class")){
            String className=analyzer.getFullNameFromCode();
            return searchCommitResultByClassName(className);
        }else if(analyzer.getType().equals("Method")){
            String methodName=analyzer.getMethodNameFromCode();
            return searchCommitResultByMethodName(methodName);
        }else{
            try (Transaction tx = db.beginTx()) {
                ResourceIterator<Node> iterator = db.findNodes(JavaExtractor.CLASS);
                while (iterator.hasNext()) {
                    Node node = iterator.next();
                    String content=node.getProperty(JavaExtractor.CONTENT).toString();
                    if(content.contains(analyzer.getCode())){
                        return getCommitResult(node.getId(),node.getProperty(JavaExtractor.NAME).toString());
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }
}
