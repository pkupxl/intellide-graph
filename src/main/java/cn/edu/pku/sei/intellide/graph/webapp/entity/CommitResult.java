package cn.edu.pku.sei.intellide.graph.webapp.entity;

import cn.edu.pku.sei.intellide.graph.extraction.code_mention.CodeMentionExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.git.GitExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.tokenization.TokenExtractor;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import org.neo4j.graphdb.*;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommitResult {
    private long id;
    private String name;
    private String commitTime;
    private String commitMessage;
    private String gitUser;
    private String gitUserEmail;
    private String diffSummary;
    private String diffMessage;
    public CommitResult(long id,String name,String createdTime,String commitMessage,String gitUser,String gitUserEmail,String diffSummary,String diffMessage){
        this.id=id;
        this.name=name;
        this.commitTime=createdTime;
        this.commitMessage=commitMessage;
        this.gitUser=gitUser;
        this.gitUserEmail=gitUserEmail;
        this.diffSummary=diffSummary;
        this.diffMessage=diffMessage;
    }

    public long getId(){
        return id;
    }

    public String getName(){
        return name;
    }
    public String getCommitTime(){
        return commitTime;
    }
    public String getCommitMessage(){
        return commitMessage;
    }

    public String getGitUser(){
        return gitUser;
    }

    public String getGitUserEmail(){
        return gitUserEmail;
    }
    public String getDiffMessage(){
        return diffMessage;
    }
    public String getDiffSummary(){
        return diffSummary;
    }
    public static CommitResult get(long id, GraphDatabaseService db,String className,String diffMessage){
        CommitResult result=null;
        try (Transaction tx = db.beginTx()) {
            Node oNode = db.getNodeById(id);
            String name=oNode.getProperty("name").toString();
            String createdTime=oNode.getProperty("commitTime").toString();
            String commitMessage=oNode.getProperty("message").toString();
            String diffSummary=oNode.getProperties("diffSummary").toString();

            String gitUser=null;
            String gitUserEmail=null;
            Iterator<Relationship> relationIter = oNode.getRelationships().iterator();

            while (relationIter.hasNext()) {
                Relationship relation = relationIter.next();
                if (relation.isType(GitExtractor.CREATOR)||relation.isType(GitExtractor.COMMITTER)){
                    Node otherNode = relation.getOtherNode(oNode);
                    gitUser=otherNode.getProperty("name").toString();
                    gitUserEmail=otherNode.getProperty("emailAddress").toString();
                    break;
                }
            }


            result=new CommitResult(id,name,createdTime,commitMessage,gitUser,gitUserEmail,diffSummary,diffMessage);
            tx.success();
        }
        return result;
    }


    boolean equals(CommitResult commitResult){
        if(this.diffMessage.equals(commitResult.diffMessage))return true;
        return this.id==commitResult.id;
    }

    public String getPath(){
        String []Diffline=this.diffMessage.split("\n");
        for(int j=0;j<Diffline.length;++j) {
            if (Diffline[j].startsWith("+++")) {
                return Diffline[j].substring(6);
            }
        }
        return null;
    }

    public void setCommitTime(String time){
        this.commitTime=time;
    }

}
