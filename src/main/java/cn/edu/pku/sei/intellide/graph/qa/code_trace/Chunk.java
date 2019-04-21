package cn.edu.pku.sei.intellide.graph.qa.code_trace;

public class Chunk {
    public int leftStart;
    public int leftEnd;
    public int rightStart;
    public int rightEnd;
    public Chunk(int leftStart,int leftEnd,int rightStart,int rightEnd){
        this.leftStart=leftStart;
        this.leftEnd=leftEnd;
        this.rightStart=rightStart;
        this.rightEnd=rightEnd;
    }
}