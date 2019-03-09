package cn.edu.pku.sei.intellide.graph.webapp.entity;

public class HistoryResult {
    private String preContent;
    private String content;
    private String commitMessage;

    public String getPreContent(){
        return preContent;
    }
    public String getContent(){
        return content;
    }
    public String getCommitMessage(){
        return commitMessage;
    }

    public HistoryResult(String preContent,String Content,String CommitMessage){
        this.preContent=preContent;
        this.content=Content;
        this.commitMessage=CommitMessage;
    }
}

