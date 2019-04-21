package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.git.GitAnalyzer;
import cn.edu.pku.sei.intellide.graph.webapp.entity.CommitResult;
import cn.edu.pku.sei.intellide.graph.webapp.entity.HistoryResult;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import javafx.util.Pair;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.compiler.SpoonResource;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;

public class HistorySearch {
    private Repository repository;
    private CommitSearch commitSearch;
    private String project;
    public HistorySearch(Repository repository,CommitSearch commitSearch,String project){
        this.repository=repository;
        this.commitSearch=commitSearch;
        this.project=project;
    }

    public List<HistoryResult> search(CodeAnalyzer analyzer){
        if(analyzer.getType().equals("Class")){
            return searchClassHistory(analyzer);
        }else if(analyzer.getType().equals("Method")){
            return searchMethodHistory(analyzer);
        }else if(analyzer.getType().equals("OneLine")){
            return searchOneLineHistory(analyzer);
        }else if(analyzer.getType().equals("MultiLines")){
            return searchMultilinesHistory(analyzer);
        }else{
            return null;
        }
    }

    public List<HistoryResult> searchClassHistory(CodeAnalyzer analyzer){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=null;
        String FileContent=null;
        String PreFileContent=null;
        List<CommitResult> cr=commitSearch.search(analyzer);
        FilePath = cr.get(0).getPath();
        if(FilePath==null){
            System.out.println("FilePath is null");
            return null;
        }else{
            System.out.println(FilePath);
        }

        try{
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                FileContent = new String(loader.getBytes());
                                First=false;
                                break;
                            }
                            PreFileContent=new String(loader.getBytes());
                            if(!PreFileContent.equals(FileContent)){
                                Diff diff =new AstComparator().compare(PreFileContent,FileContent);
                                List<Operation> allOperations=diff.getRootOperations();
                                List<String> changeSummary=new ArrayList<>();
                                for(Operation op : allOperations){
                                   changeSummary.add(op.toString());
                                }

                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                }
                                result.add(new HistoryResult(PreFileContent,FileContent,Message,time,changeSummary,hasIssue));
                            }

                            FileContent=PreFileContent;
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public List<HistoryResult> searchMethodHistory(CodeAnalyzer analyzer){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=null;
        String Content=analyzer.getCode();
        String MethodContent=null;
        String PreMethodContent=null;
        String MethodSignature=null;
        String MethodName=null;

        List<CommitResult> cr=commitSearch.search(analyzer);
        FilePath = cr.get(0).getPath();
        if(FilePath==null){
            System.out.println("FilePath is null");
        }else{
            System.out.println(FilePath);
        }

        try{
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                boolean Finish=false;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        if(Finish)break;
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                String fileContent=new String(loader.getBytes());
                                int start=0;
                                int end=0;
                                String filelines[]=fileContent.split("\\n");
                                String methodlines[]=Content.split(("\\n"));
                                for(int i=0;i<filelines.length;++i){
                                    boolean find=true;
                                    for(int j=0;j<methodlines.length;++j){
                                        if(!filelines[i+j].trim().equals(methodlines[j].trim())){
                                            find=false;
                                            break;
                                        }
                                    }
                                    if(find){
                                        start=i+1;
                                        end=start+methodlines.length-1;
                                    }
                                }
                                SpoonAPI spoon = new Launcher();
                                VirtualFile resource = new VirtualFile(new String(loader.getBytes()), "/test");
                                ((Launcher) spoon).addInputResource((SpoonResource) resource);
                                spoon.buildModel();

                                final int Start=start;
                                final int End=end;
                                for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
                                    @Override
                                    public boolean matches(CtMethod element) {
                                        return super.matches(element)&&element.getPosition().getLine()>=Start &&element.getPosition().getEndLine()<=End;
                                    }
                                })) {
                                    System.out.println("找到第一个版本的方法");
                                    MethodContent=meth.toString();
                                    MethodSignature=meth.getSignature();
                                    MethodName=meth.getSimpleName();
                                }
                                First=false;
                                break;
                            }
                            PreMethodContent=getPreMethodContent(new String(loader.getBytes()),MethodName,MethodSignature);

                            String Message=commit.getFullMessage().split("git-svn-id")[0];
                            Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                            Matcher matcher = pattern.matcher(Message);
                            boolean hasIssue=false;
                            if(matcher.find() && matcher.start()==0){
                                hasIssue=true;
                            }

                            if(PreMethodContent==null){
                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                result.add(new HistoryResult(PreMethodContent,MethodContent,Message,time,null,hasIssue));
                                Finish=true;
                                break;
                            }else if(!MethodContent.equals(PreMethodContent)) {
                                String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                result.add(new HistoryResult(PreMethodContent, MethodContent, Message,time,null,hasIssue));
                            }

                            MethodContent=PreMethodContent;
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public String getPreMethodContent(String content,String methodName,String methodSignature){
        SpoonAPI spoon = new Launcher();
        VirtualFile resource = new VirtualFile(content, "/test");
        ((Launcher) spoon).addInputResource((SpoonResource) resource);
        spoon.buildModel();
        List<CtMethod>potentialMethod=new ArrayList<>();
        for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
            @Override
            public boolean matches(CtMethod element) {
                return super.matches(element)&&element.getSimpleName().equals(methodName);
            }
        })) {
            potentialMethod.add(meth);
        }
        if(potentialMethod.size()==0){
            return null;
        }else if(potentialMethod.size()==1){
            return potentialMethod.get(0).toString();
        }else{
            double Maxsim=0;
            int index=0;
            for(int i=0;i<potentialMethod.size();++i){
                double similarity=getSimilariry(methodSignature,potentialMethod.get(i).getSignature());
                if(similarity<Maxsim){
                    Maxsim=similarity;
                    index=i;
                }
            }
            return potentialMethod.get(index).toString();
        }
    }

    public List<HistoryResult> searchOneLineHistory(CodeAnalyzer analyzer){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=null;
        String Content=analyzer.getCode();
        String MethodContent=null;
        String MethodSignature=null;
        String MethodName=null;

        List<CommitResult> cr=commitSearch.search(analyzer);
        System.out.println("commit size:"+cr.size());
        for(int i=0;i<cr.size();++i){
            System.out.println(cr.get(i).getPath());
        }
        FilePath = cr.get(0).getPath();

        if(FilePath==null){
            System.out.println("FilePath is null");
        }else{
            System.out.println(FilePath);
        }

        try{
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                boolean Finish=false;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    if(Finish)break;
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                String fileContent=new String(loader.getBytes());
                                int linenum=0;
                                String filelines[]=fileContent.split("\\n");
                                for(int i=0;i<filelines.length;++i){
                                    if(filelines[i].trim().equals(Content.trim())){
                                        linenum=i+1;
                                    }
                                }

                                SpoonAPI spoon = new Launcher();
                                VirtualFile resource = new VirtualFile(new String(loader.getBytes()), "/test");
                                ((Launcher) spoon).addInputResource((SpoonResource) resource);
                                spoon.buildModel();

                                final int Linenum=linenum;
                                for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
                                    @Override
                                    public boolean matches(CtMethod element) {
                                        return super.matches(element)&&element.getPosition().getLine()<=Linenum &&element.getPosition().getEndLine()>=Linenum;
                                    }
                                })) {
                                    System.out.println("找到第一个版本的方法");
                                    MethodContent=meth.getBody().toString();
                                    MethodSignature=meth.getSignature();
                                    MethodName=meth.getSimpleName();
                                }
                                First=false;
                                break;
                            }

                            String PreMethodContent=getPreMethodContent(new String(loader.getBytes()),MethodName,MethodSignature);
                            if(PreMethodContent==null){
                                Finish=true;
                                break;
                            }else{

                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                }

                                String PreContent=getPreContentForOneline(PreMethodContent,Content);
                                if(PreContent==null){
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue));
                                    Finish=true;
                                    break;
                                } else if (!PreContent.equals(Content)) {
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue));
                                }
                                Content=PreContent;
                            }
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public List<HistoryResult> searchMultilinesHistory(CodeAnalyzer analyzer){
        List<HistoryResult>result=new ArrayList<>();
        String FilePath=null;
        String Content=analyzer.getCode();
        String MethodContent=null;
        String MethodSignature=null;
        String MethodName=null;

        List<CommitResult> cr=commitSearch.search(analyzer);
        System.out.println("commit size:"+cr.size());
        for(int i=0;i<cr.size();++i){
            System.out.println(cr.get(i).getPath());
        }
        FilePath = cr.get(0).getPath();

        if(FilePath==null){
            System.out.println("FilePath is null");
        }else{
            System.out.println(FilePath);
        }

        try{
            GitAnalyzer gitAnalyzer=new GitAnalyzer(repository);
            List<Pair<ObjectId, Pair<String, String>>>res= gitAnalyzer.getAllCommitModifyAFile(FilePath);
            try (RevWalk revWalk = new RevWalk(repository)) {
                boolean First=true;
                boolean Finish=false;
                for(Pair<ObjectId, Pair<String, String>> p:res){
                    if(Finish)break;
                    RevCommit commit = revWalk.parseCommit(p.getKey());
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        FilePath=p.getValue().getValue();
                        treeWalk.setFilter(PathFilter.create(FilePath));
                        while(treeWalk.next()){
                            ObjectId objectId = treeWalk.getObjectId(0);
                            ObjectLoader loader = repository.open(objectId);
                            if(First){
                                String fileContent=new String(loader.getBytes());
                                int start=0;
                                int end=0;
                                String filelines[]=fileContent.split("\\n");
                                String methodlines[]=Content.split(("\\n"));
                                for(int i=0;i<filelines.length;++i){
                                    boolean find=true;
                                    for(int j=0;j<methodlines.length;++j){
                                        if(!filelines[i+j].trim().equals(methodlines[j].trim())){
                                            find=false;
                                            break;
                                        }
                                    }
                                    if(find){
                                        start=i+1;
                                        end=start+methodlines.length-1;
                                    }
                                }
                                SpoonAPI spoon = new Launcher();
                                VirtualFile resource = new VirtualFile(new String(loader.getBytes()), "/test");
                                ((Launcher) spoon).addInputResource((SpoonResource) resource);
                                spoon.buildModel();

                                final int Start=start;
                                final int End=end;
                                for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
                                    @Override
                                    public boolean matches(CtMethod element) {
                                        return super.matches(element)&&element.getPosition().getLine()<=Start &&element.getPosition().getEndLine()>=End;
                                    }
                                })) {
                                    System.out.println("找到第一个版本的方法");
                                    MethodContent=meth.getBody().toString();
                                    MethodSignature=meth.getSignature();
                                    MethodName=meth.getSimpleName();
                                }
                                First=false;
                                break;
                            }

                            String PreMethodContent=getPreMethodContent(new String(loader.getBytes()),MethodName,MethodSignature);
                            if(PreMethodContent==null){
                                Finish=true;
                                break;
                            }else{

                                String Message=commit.getFullMessage().split("git-svn-id")[0];
                                Pattern pattern = Pattern.compile(project.toUpperCase()+"-(\\d+):(.*)");
                                Matcher matcher = pattern.matcher(Message);
                                boolean hasIssue=false;
                                if(matcher.find() && matcher.start()==0){
                                    hasIssue=true;
                                }

                                String PreContent=getPreContentForMultilines(PreMethodContent,Content);
                                if(PreContent==null){
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue));
                                    Finish=true;
                                    break;
                                } else if (!PreContent.equals(Content)) {
                                    String time = TimeStamp2Date(String.valueOf(commit.getCommitTime()));
                                    result.add(new HistoryResult(PreContent,Content,Message,time,null,hasIssue));
                                }
                                Content=PreContent;
                            }
                        }
                    }
                    revWalk.dispose();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public String TimeStamp2Date(String timestampString){
        Long timestamp = Long.parseLong(timestampString)*1000;
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
        return date;
    }

    public String getPreContentForOneline(String PreMethodContent,String Content){
        String lines[]=PreMethodContent.split("\\n");
        double MaxSim=0;
        int index=1;
        for(int i=1;i<lines.length-1;++i){
            double similarity=getSimilariry(lines[i],Content);
            if(similarity>MaxSim){
                MaxSim=similarity;
                index=i;
            }
        }
        if(MaxSim<0.1)return null;

        return lines[index];
    }

    public String getPreContentForMultilines(String PreMethodContent,String Content){
        String MethodLine[]=PreMethodContent.split("\\n");
        String ContentLine[]=Content.split("\\n");
        int MLen=MethodLine.length;
        int CLen=ContentLine.length;
        int Max=0;
        int Min=MLen-1;
        for(int i=0;i<CLen;++i){
            int index=1;
            double MaxSim=0;
            for(int j=1;j<MLen-1;++j){
                double similarity=getSimilariry(MethodLine[j],ContentLine[i]);
                if(similarity>MaxSim){
                    MaxSim=similarity;
                    index=j;
                }
            }
            if(Max<index)Max=index;
            if(Min>index)Min=index;
        }
        String result="";
        for(int i=Min;i<=Max;++i){
            result+=MethodLine[i]+"\n";
        }
        return result;
    }

    public String getPreContentForMultilines(String PreFileContent,String FileContent,String Content){
        RawText file1 = new RawText(PreFileContent.getBytes());
        RawText file2 = new RawText(FileContent.getBytes());
        EditList diffList= new EditList();
        diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, file1, file2));
        String diff="";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df=new DiffFormatter(out);
            df.format(diffList, file1, file2);
            diff=out.toString();
        }catch (Exception e){
            e.printStackTrace();
        }

        List<Chunk>Chunks=new ArrayList<Chunk>();
        String difflines[]=diff.split("\\n");
        for(int i=0;i<difflines.length;++i){
            if(difflines[i].startsWith("@@")){
                String A=difflines[i].split(" ")[1];
                String B=difflines[i].split(" ")[2];
                int startA=Integer.parseInt(A.substring(1).split(",")[0]);
                int lenA=Integer.parseInt(A.substring(1).split(",")[1]);
                int startB=Integer.parseInt(B.substring(1).split(",")[0]);
                int lenB=Integer.parseInt(B.substring(1).split(",")[1]);
                Chunks.add(new Chunk(startA,startA+lenA-1,startB,startB+lenB-1));
            }
        }

        String []PreFilelines=PreFileContent.split("\\n");
        int Prelen=PreFilelines.length;
        String []Filelines=FileContent.split("\\n");
        int len=Filelines.length;
        String []lines=Content.split("\\n");
        int l=lines.length;

        int Map[]=new int[len+1];
        for(int i=0;i<=len;++i){
            Map[i]=-1;
        }

        for(int i=0;i<Chunks.size();++i){
            int startA=Chunks.get(i).leftStart;
            int endA=Chunks.get(i).leftEnd;
            int startB=Chunks.get(i).rightStart;
            int endB=Chunks.get(i).rightEnd;
            int PreA=1;
            int PreB=1;
            if(i>0){
                PreA=Chunks.get(i-1).leftEnd+1;
                PreB=Chunks.get(i-1).rightEnd+1;
            }

            for(int j=PreB,k=PreA;j<=startB-1&&k<=startA-1;++j,++k){
                Map[j]=k;
            }

            for(int j=startB;j<=endB;++j){
                double Similarity=0;
                int index=startA;
                for(int k=startA;k<=endA;++k){
                    double sim=getSimilariry(Filelines[j-1],PreFilelines[k-1]);
                    if(sim>Similarity){
                        Similarity=sim;
                        index=k;
                    }
                }
                Map[j]=index;
            }

            if(i==Chunks.size()-1){
                for(int j=endB+1,k=endA+1; j<=len&&k<=Prelen;++j,++k){
                    Map[j]=k;
                }
            }
        }

        int start=0;
        int end=0;
        for(int i=0;i<=len-l;++i){
           boolean find=true;
           for(int j=0;j<l;++j){
               if(!Filelines[i+j].equals(lines[j])){
                   find =false;
                   break;
               }
           }
           if(find){
               start=i+1;
               end=start+l-1;
               break;
           }
        }

        int Max=1;
        int Min=Prelen;
        for(int i=start;i<=end;++i){
            if(Map[i]==-1)continue;
            if(Max<Map[i]){
                Max=Map[i];
            }
            if(Min>Map[i]){
                Min=Map[i];
            }
        }
        String result="";
        for(int i=Min;i<=Max&&i<=Prelen;++i){
            result+=PreFilelines[i-1]+"\n";
        }
        return result;
    }

    public double getSimilariry(String source, String target) {

        source=source.trim();
        target=target.trim();
        char[] sources = source.toCharArray();
        char[] targets = target.toCharArray();
        int sourceLen = sources.length;
        int targetLen = targets.length;
        if(sourceLen==0||targetLen==0){
            return 0;
        }
        int[][] d = new int[sourceLen + 1][targetLen + 1];
        for (int i = 0; i <= sourceLen; i++) {
            d[i][0] = i;
        }
        for (int i = 0; i <= targetLen; i++) {
            d[0][i] = i;
        }

        for (int i = 1; i <= sourceLen; i++) {
            for (int j = 1; j <= targetLen; j++) {
                if (sources[i - 1] == targets[j - 1]) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    //插入
                    int insert = d[i][j - 1] + 1;
                    //删除
                    int delete = d[i - 1][j] + 1;
                    //替换
                    int replace = d[i - 1][j - 1] + 1;
                    d[i][j] = Math.min(insert, delete) > Math.min(delete, replace) ? Math.min(delete, replace) :
                            Math.min(insert, delete);
                }
            }
        }
        double Max=0;
        if(sourceLen>targetLen){
            Max=sourceLen;
        }else{
            Max=targetLen;
        }
        double result=1-(double)d[sourceLen][targetLen]/Max;
        return result;
    }

    public static void main(String args[]) throws Exception{
        File f1 = new File("C:\\Users\\DELL\\Desktop\\新建文件夹\\1.java");
        File f2 = new File("C:\\Users\\DELL\\Desktop\\新建文件夹\\2.java");

        SpoonAPI spoon = new Launcher();
//        spoon.addInputResource("C:\\Users\\DELL\\Desktop\\新建文件夹\\1.java");
        String  content="/*\n" +
                " * Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                " * contributor license agreements.  See the NOTICE file distributed with\n" +
                " * this work for additional information regarding copyright ownership.\n" +
                " * The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                " * (the \"License\"); you may not use this file except in compliance with\n" +
                " * the License.  You may obtain a copy of the License at\n" +
                " *\n" +
                " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
                " *\n" +
                " * Unless required by applicable law or agreed to in writing, software\n" +
                " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                " * See the License for the specific language governing permissions and\n" +
                " * limitations under the License.\n" +
                " */\n" +
                "package org.apache.lucene.analysis;\n" +
                "\n" +
                "\n" +
                "import java.io.IOException;\n" +
                "import java.io.Reader;\n" +
                "\n" +
                "import org.apache.lucene.util.AttributeFactory;\n" +
                "import org.apache.lucene.util.AttributeSource;\n" +
                "\n" +
                "/** A Tokenizer is a TokenStream whose input is a Reader.\n" +
                "  <p>\n" +
                "  This is an abstract class; subclasses must override {@link #incrementToken()}\n" +
                "  <p>\n" +
                "  NOTE: Subclasses overriding {@link #incrementToken()} must\n" +
                "  call {@link AttributeSource#clearAttributes()} before\n" +
                "  setting attributes.\n" +
                " */\n" +
                "public abstract class Tokenizer extends TokenStream {  \n" +
                "  /** The text source for this Tokenizer. */\n" +
                "  protected Reader input = ILLEGAL_STATE_READER;\n" +
                "  \n" +
                "  /** Pending reader: not actually assigned to input until reset() */\n" +
                "  private Reader inputPending = ILLEGAL_STATE_READER;\n" +
                "\n" +
                "  /**\n" +
                "   * Construct a tokenizer with no input, awaiting a call to {@link #setReader(java.io.Reader)}\n" +
                "   * to provide input.\n" +
                "   */\n" +
                "  protected Tokenizer() {\n" +
                "    //\n" +
                "  }\n" +
                "\n" +
                "  /**\n" +
                "   * Construct a tokenizer with no input, awaiting a call to {@link #setReader(java.io.Reader)} to\n" +
                "   * provide input.\n" +
                "   * @param factory attribute factory.\n" +
                "   */\n" +
                "  protected Tokenizer(AttributeFactory factory) {\n" +
                "    super(factory);\n" +
                "  }\n" +
                "\n" +
                "  /**\n" +
                "   * {@inheritDoc}\n" +
                "   * <p>\n" +
                "   * <b>NOTE:</b> \n" +
                "   * The default implementation closes the input Reader, so\n" +
                "   * be sure to call <code>super.close()</code> when overriding this method.\n" +
                "   */\n" +
                "  @Override\n" +
                "  public void close() throws IOException {\n" +
                "    input.close();\n" +
                "    // LUCENE-2387: don't hold onto Reader after close, so\n" +
                "    // GC can reclaim\n" +
                "    inputPending = input = ILLEGAL_STATE_READER;\n" +
                "  }\n" +
                "  \n" +
                "  /** Return the corrected offset. If {@link #input} is a {@link CharFilter} subclass\n" +
                "   * this method calls {@link CharFilter#correctOffset}, else returns <code>currentOff</code>.\n" +
                "   * @param currentOff offset as seen in the output\n" +
                "   * @return corrected offset based on the input\n" +
                "   * @see CharFilter#correctOffset\n" +
                "   */\n" +
                "  protected final int correctOffset(int currentOff) {\n" +
                "    return (input instanceof CharFilter) ? ((CharFilter) input).correctOffset(currentOff) : currentOff;\n" +
                "  }\n" +
                "\n" +
                "  /** Expert: Set a new reader on the Tokenizer.  Typically, an\n" +
                "   *  analyzer (in its tokenStream method) will use\n" +
                "   *  this to re-use a previously created tokenizer. */\n" +
                "  public final void setReader(Reader input) {\n" +
                "    if (input == null) {\n" +
                "      throw new NullPointerException(\"input must not be null\");\n" +
                "    } else if (this.input != ILLEGAL_STATE_READER) {\n" +
                "      throw new IllegalStateException(\"TokenStream contract violation: close() call missing\");\n" +
                "    }\n" +
                "    this.inputPending = input;\n" +
                "    setReaderTestPoint();\n" +
                "  }\n" +
                "  \n" +
                "  @Override\n" +
                "  public void reset() throws IOException {\n" +
                "    super.reset();\n" +
                "    input = inputPending;\n" +
                "    inputPending = ILLEGAL_STATE_READER;\n" +
                "  }\n" +
                "\n" +
                "  // only used for testing\n" +
                "  void setReaderTestPoint() {}\n" +
                "  \n" +
                "  private static final Reader ILLEGAL_STATE_READER = new Reader() {\n" +
                "    @Override\n" +
                "    public int read(char[] cbuf, int off, int len) {\n" +
                "      throw new IllegalStateException(\"TokenStream contract violation: reset()/close() call missing, \" +\n" +
                "          \"reset() called multiple times, or subclass does not call super.reset(). \" +\n" +
                "          \"Please see Javadocs of TokenStream class for more information about the correct consuming workflow.\");\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void close() {} \n" +
                "  };\n" +
                "}\n" +
                "\n";

        String method = "  public void nextSlice() {\n" +
                "\n" +
                "    // Skip to our next slice\n" +
                "    final int nextIndex = ((buffer[limit]&0xff)<<24) + ((buffer[1+limit]&0xff)<<16) + ((buffer[2+limit]&0xff)<<8) + (buffer[3+limit]&0xff);\n" +
                "\n" +
                "    level = ByteBlockPool.NEXT_LEVEL_ARRAY[level];\n" +
                "    final int newSize = ByteBlockPool.LEVEL_SIZE_ARRAY[level];\n" +
                "\n" +
                "    bufferUpto = nextIndex / ByteBlockPool.BYTE_BLOCK_SIZE;\n" +
                "    bufferOffset = bufferUpto * ByteBlockPool.BYTE_BLOCK_SIZE;\n" +
                "\n" +
                "    buffer = pool.buffers[bufferUpto];\n" +
                "    upto = nextIndex & ByteBlockPool.BYTE_BLOCK_MASK;\n" +
                "\n" +
                "    if (nextIndex + newSize >= endIndex) {\n" +
                "      // We are advancing to the final slice\n" +
                "      assert endIndex - nextIndex > 0;\n" +
                "      limit = endIndex - bufferOffset;\n" +
                "    } else {\n" +
                "      // This is not the final slice (subtract 4 for the\n" +
                "      // forwarding address at the end of this new slice)\n" +
                "      limit = upto+newSize-4;\n" +
                "    }\n" +
                "  }\n";
/*
        VirtualFile resource = new VirtualFile(content, "/test");
        ((Launcher) spoon).addInputResource((SpoonResource) resource);
        spoon.buildModel();

        for (CtMethod<?> meth : spoon.getModel().getRootPackage().getElements(new TypeFilter<CtMethod>(CtMethod.class) {
            @Override
            public boolean matches(CtMethod element) {
                return super.matches(element)&&element.getSignature()!=null;
            }
        })) {
           System.out.println(meth.getSignature());
           System.out.println(meth.toString());
           System.out.println(meth.getSimpleName());
        }*/
            Diff diff =new AstComparator().compare(f1,f2);
        List<Operation> allOperations=diff.getRootOperations();
        for(Operation op : allOperations){
              if(op instanceof DeleteOperation){

            }else if(op instanceof InsertOperation){

            }else if(op instanceof MoveOperation){

            }else if(op instanceof UpdateOperation){

            }
            if(op.getNode()!=null){
//                System.out.println(op.getNode().getPosition().getLine());
 //               System.out.println(op.getNode().getPosition().getEndLine());
                System.out.println(op.toString());
            }
        }
    }
}