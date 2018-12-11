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
    private final long id;
    private final String name;
    private final String commitTime;
    private final String commitMessage;
    private final String gitUser;
    private final String gitUserEmail;
    private final String diffSummary;
    private final String diffMessage;
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


            Pattern pattern = Pattern.compile("\\{diffSummary=(ADD|MODIFY|DELETE)\\s+(\\S+)\\s+to\\s+(\\S+)\\s+::+(\\S+)}");
            Matcher matcher = pattern.matcher(diffSummary);

            String diffMessage1=null;
            String sig = className.replace('.', '/') + ".java";

            while (matcher.find()) {
                String relStr = matcher.group(1);
                String srcPath = matcher.group(2);
                String dstPath = matcher.group(3);
                String message = matcher.group(4);
                if (srcPath.contains(sig) || dstPath.contains(sig)){
                    diffMessage1=message;
                }
            }
            result=new CommitResult(id,name,createdTime,commitMessage,gitUser,gitUserEmail,diffSummary,diffMessage1);
            tx.success();
        }
        return result;
    }
}
