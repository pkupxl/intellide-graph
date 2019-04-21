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

    public List<CommitResult> search(CodeAnalyzer analyzer){
        if(analyzer.getType().equals("Class")){
            return searchCommitResultByClassName(analyzer);
        }else if(analyzer.getType().equals("Method")){
            return searchCommitResultByMethodName(analyzer);
        }else{
            return searchCommitResultByLines(analyzer);
        }
    }

    public List<CommitResult> searchCommitResultByClassName(CodeAnalyzer analyzer){
        String className=analyzer.getFullNameFromCode();
        for (MyNode node : graph) {
            if(node.fullName.equals(className)){
                System.out.println("Hit!!");
                return getCommitResult(node.getId(),analyzer);
            }
        }
        return null;
    }

    public List<CommitResult> searchCommitResultByMethodName(CodeAnalyzer analyzer){
        return searchCommitResultByLines(analyzer);

        /*String methodName = analyzer.getMethodNameFromCode();
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
                            return getCommitResult(otherNode.getId(),analyzer);
                        }
                    }
                }
            }
            tx.success();
        }
        return null;*/
    }

    public List<CommitResult> searchCommitResultByLines(CodeAnalyzer analyzer){
        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Node> iterator = db.findNodes(JavaExtractor.CLASS);
            while (iterator.hasNext()) {
                Node node = iterator.next();
                String content=node.getProperty(JavaExtractor.CONTENT).toString();
                if(content.contains(analyzer.getCode())){
                    return getCommitResult(node.getId(),analyzer);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public List<CommitResult> getCommitResult(Long id, CodeAnalyzer analyzer){
        List<CommitResult>result=new ArrayList<>();
        List<Node>ResultNode=new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Node currentNode=db.getNodeById(id);
            String className=currentNode.getProperty("name").toString();
            Iterator<Relationship> rels = db.getNodeById(id).getRelationships().iterator();
            while (rels.hasNext()) {
                Relationship rel = rels.next();
                if(rel.isType(CodeMentionExtractor.MODIFY)||rel.isType(CodeMentionExtractor.ADD)||rel.isType(CodeMentionExtractor.DELETE)){
                    Node otherNode=rel.getOtherNode(currentNode);
                    if(ResultNode.contains(otherNode))continue;
                    String diffMessage="";
                    if(rel.hasProperty("diffMessage")){
                        diffMessage=rel.getProperty("diffMessage").toString();
                    }

                    CommitResult commitResult=CommitResult.get(otherNode.getId(),db,className,diffMessage);
                    ResultNode.add(otherNode);
                    result.add(commitResult);
                    /*if(analyzer.getType().equals("Class")){
                        CommitResult commitResult=CommitResult.get(otherNode.getId(),db,className,diffMessage);
                        ResultNode.add(otherNode);
                        result.add(commitResult);
                    }else{
                        String content=analyzer.getCode();
                        String[] lines=content.split("\n");
                        boolean isRelated=false;
                        for(int i=0;i<lines.length;++i){
                            if(diffMessage.contains(lines[i])){
                                isRelated=true;
                                break;
                            }
                        }
                        if(isRelated){
                            ResultNode.add(otherNode);
                            CommitResult commitResult = CommitResult.get(otherNode.getId(),db,className,diffMessage);
                            result.add(commitResult);
                        }
                    }*/
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
           result.get(i).setCommitMessage(result.get(i).getCommitMessage().split("git-svn-id")[0]);
        }
        return result;
    }

    public String TimeStamp2Date(String timestampString){
        Long timestamp = Long.parseLong(timestampString)*1000;
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
        return date;
    }
}
