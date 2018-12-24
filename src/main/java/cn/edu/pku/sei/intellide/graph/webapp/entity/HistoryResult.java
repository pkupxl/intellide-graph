package cn.edu.pku.sei.intellide.graph.webapp.entity;

public class HistoryResult {
    String preContent;
    String content;
    String commitMessage;
 //   String IssueSummary;
    public String getPreContent(){
        return preContent;
    }
    public String getContent(){
        return content;
    }
    public String getCommitMessage(){
        return commitMessage;
    }
  /*  public String getIssueSummary() {
        return IssueSummary;
    }*/
    public HistoryResult(String preContent,String Content,String CommitMessage){
        this.preContent=preContent;
        this.content=Content;
        this.commitMessage=CommitMessage;
 //       this.IssueSummary=IssueSummary;
    }
}
