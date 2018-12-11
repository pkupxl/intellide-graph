package cn.edu.pku.sei.intellide.graph.extraction.git;

import javafx.util.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.Patch;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class GitAnalyzer {
    private Git git;
    private Repository repository;

    String patchString = "";
    private ObjectId firstCommit = null;

    public GitAnalyzer(String filePath){

        try {
            git = Git.open(new File(filePath));
            repository = git.getRepository();
            getFirstCommit();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public Git getGit(){return git;}

    public List<String> getLog(){
        List<String> result = new ArrayList<String>();

        try {
            Iterator<RevCommit> commits = git.log().call().iterator();
            while (commits.hasNext()) {
                RevCommit commit = commits.next();
                result.add(commit.getName());
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Pair<ObjectId, Pair<String, String>>> getAllCommitModifyAFile(String filePath){
        List<Pair<ObjectId, Pair<String, String>>> result = new ArrayList<>();
        try {
            ObjectId endId = repository.resolve("HEAD");
            while(true){
                Iterator<RevCommit> commits = git.log().addPath(filePath)
                        .addRange(firstCommit, endId)
                        .setMaxCount(10000).call().iterator();
                boolean signal = false;
                while(commits.hasNext()){
                    RevCommit commit = commits.next();

                    if(commits.hasNext())
                        result.add(new Pair<ObjectId, Pair<String, String>>(commit, new Pair<String, String>(filePath,filePath)));
                    else{
                        String oldPath = filePath;
                        filePath = getFormerName(commit, filePath);
                        if(filePath != null && !filePath.equals(oldPath)){
                            result.add(new Pair<ObjectId, Pair<String, String>>(commit, new Pair<String, String>(filePath,oldPath)));
                            signal = true;
                            endId = repository.resolve(commit.getName() + "^");
                            break;
                        }else{
                            result.add(new Pair<ObjectId, Pair<String, String>>(commit, new Pair<String, String>(oldPath,oldPath)));
                        }

                    }
                }
                if(!signal)
                    break;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public List<String> getAllFiles(String version, String fileFilter ){
        List<String> result = new ArrayList<>();
        try(TreeWalk treeWalk = new TreeWalk(repository)){
            ObjectId commitId = repository.resolve(version + "^{tree}");
            treeWalk.reset(commitId);

            int count = 0;
            while(treeWalk.next()){
                if(treeWalk.isSubtree()){
                    treeWalk.enterSubtree();
                }else{
                    String path = treeWalk.getPathString();
                    if(fileFilter == null || path.endsWith(fileFilter)){
                        result.add(path);
                        count ++;
                    }
                }
            }
            System.out.println(count);
        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }


    /**
     * 从某次提交中获取所有的被修改的文件路径。
     * 当无参数fileFilters时，表示获取所有的文件
     * 否则，fileFilters中的每一个元素代表一种需要的文件类型，用该类型文件名后缀表示，如“.java”
     * @param commitId 特定的commit的id
     * @param fileFilters 需要的文件类型后缀名
     * @return
     */
    public List<String> getAllFilesModifiedByCommit(String commitId, String ...fileFilters){
        List<String > result = new ArrayList<>();

        try {
            RevWalk rw = new RevWalk(repository);
            ObjectId curId = repository.resolve(commitId);
            RevCommit cur = rw.parseCommit(curId);
            RevCommit par = rw.parseCommit(cur.getParent(0).getId());
            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
            df.setRepository(repository);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);
            List<DiffEntry> diffs = df.scan(par.getTree(), cur.getTree());
            for(DiffEntry diff: diffs){
                String fileName = diff.getNewPath();
                if(fileFilters.length == 0){
                    result.add(fileName);
                }else{
                    for(String filter: fileFilters){
                        if(fileName.endsWith(filter)){
                            result.add(fileName);
                            break;
                        }
                    }
                }
            }
        }catch (Exception e){
            System.out.println(" error + " + commitId + "\n");
            System.out.println(e.getMessage());
        }
        return result;
    }

    public String getCommitMessage(String commitId){
        String result = "";
        try {
            RevWalk rw = new RevWalk(repository);
            ObjectId curId = repository.resolve(commitId);
            RevCommit cur = rw.parseCommit(curId);
            result = cur.getFullMessage();
        }catch (Exception e){
            System.out.println(" error + " + commitId + "\n");
            System.out.println(e.getMessage());
        }
        return result;
    }

    public String getFileFromCommit(ObjectId commitId, String filePath){
        String result = "";
        try(TreeWalk treeWalk = new TreeWalk(repository)){
            treeWalk.reset(repository.resolve(commitId.getName()+"^{tree}"));
            treeWalk.setFilter(PathFilter.create(filePath));
            treeWalk.setRecursive(true);
            if(treeWalk.next()){
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                result = new String(loader.getBytes());
            }
        }catch (Exception e){
            System.out.println(" error + " + commitId.getName() + "\n");
            System.out.println(e.getMessage());
        }
        return result;
    }

    private void getFirstCommit(){
        try {
            Iterator<RevCommit> commits = git.log().call().iterator();
            while (commits.hasNext()) {
                firstCommit = commits.next().getId();
            }
            //System.out.println("first commit: " + firstCommit.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getFormerName(ObjectId commitId, String file){
        try {
            ObjectReader objectReader = repository.newObjectReader();
            ObjectLoader objectLoader = objectReader.open(commitId);
            RevCommit commit = RevCommit.parse(objectLoader.getBytes());
            return getFormerName(commit, file);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private String getFormerName(RevCommit cur, String file){
        String formerName = null;
        try{
            TreeWalk tw = new TreeWalk(repository);
            tw.setRecursive(true);
            tw.addTree(repository.resolve(cur.getName() + "^{tree}"));
            tw.addTree(repository.resolve(cur.getName() + "^^{tree}"));

            RenameDetector rd = new RenameDetector(repository);
            rd.addAll(DiffEntry.scan(tw));

            List<DiffEntry> diffs = rd.compute(tw.getObjectReader(), null);
            for(DiffEntry diff: diffs){
                if(diff.getScore() >= rd.getRenameScore() && diff.getOldPath().equals(file)){
                    formerName = diff.getNewPath();
                    break;
                }else if(diff.getOldPath().equals(file)){
                    formerName = diff.getNewPath();
                }else if(diff.getChangeType() == DiffEntry.ChangeType.ADD){
                    formerName = null;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return formerName;
        }
    }

    public ObjectId getId(String id){
        ObjectId result = null;
        try{
            result = repository.resolve(id);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

   /* public Patch getPatch(ObjectId newId, ObjectId oldId, String filePath){
        Patch patch = new Patch();
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser old = new CanonicalTreeParser();
            ObjectId oldTreeId = repository.resolve((oldId == null? newId.getName() + "^" : oldId.getName())+ "^{tree}");
            old.reset(reader, oldTreeId);

            CanonicalTreeParser n = new CanonicalTreeParser();
            ObjectId newTreeId = repository.resolve(newId.getName() + "^{tree}");
            n.reset(reader, newTreeId);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<DiffEntry> diffs = git.diff().setNewTree(n).setOldTree(old).
                    setPathFilter(PathFilter.create(filePath)).setOutputStream(out).call();
            String s = out.toString();
            patchString = s;

            byte[] bytes = s.getBytes();
            patch.parse(new ByteInputStream(bytes, bytes.length));
        } catch (Exception e) {
            System.out.println(" error + " + newId.getName() + "\n");
            System.out.println(e.getMessage());
        }
        return patch;
    }*/

    /*public Patch getPatch(String oldFile, String newFile){
        Patch patch = new Patch();
        RawText file1 = new RawText(oldFile.getBytes());
        RawText file2 = new RawText(newFile.getBytes());

        EditList diffList= new EditList();
        diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, file1, file2));


        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new DiffFormatter(out).format(diffList, file1, file2);
            System.out.println(out.toString());
            byte[] bytes = out.toString().getBytes();
            patch.parse(new ByteInputStream(bytes, bytes.length));
        }catch (Exception e){
            e.printStackTrace();
        }
        return patch;
    }*/

    public static void main(String[] args){

        //GitAnalyzer poi_analyzer = new GitAnalyzer("C:\\Users\\oliver\\Downloads\\lucene-solr-master\\poi");
        //poi_analyzer.start();

        GitAnalyzer lucene_analyzer = new GitAnalyzer("C:\\Users\\oliver\\Downloads\\lucene-solr-master\\lucene-solr");
        List<String> logs = lucene_analyzer.getLog();
        for (String log: logs) {
            String msg = lucene_analyzer.getCommitMessage(log);
            if(msg.contains("SOLR-12759")){
                System.out.println(log);
            }
        }


        /*GitAnalyzer lucene_analyzer = new GitAnalyzer("C:\\Users\\oliver\\Downloads\\lucene-solr-master\\lucene-solr");
        ClassFile classFile = new ClassFile(lucene_analyzer);
        List<String> files = lucene_analyzer.getAllFiles("HEAD", ".java");
        for (String file: files) {
            System.out.println(file);
            file = "solr/core/src/test/org/apache/solr/update/AutoCommitTest.java";
            classFile.retrieveHistory(file);
            List<CodeLine> codeLines = classFile.getLines();
            WriterTool.append("oneLine.txt", file + "\n");
            for(CodeLine line: codeLines){
                if(line.history.size() > 1) {
                    String msg = "  " + line.lineNumber + "\n";
                    msg += line.toString();
                    WriterTool.append("oneLine.txt",msg);
                }
            }
        }*/

        //file.retrieveHistory("lucene/core/src/java/org/apache/lucene/store/ByteBuffersDirectory.java");




        //lucene_analyzer.getAllFilesModifiedByCommit("c60cd2529b9c9d3e57e23e67e7c55a75269a23f9");
        //lucene_analyzer.getPatch(" 123\n2\n", "32323\n123\2\n");


        //lucene_analyzer.start();


        //lucene_analyzer.print();
        //poi_analyzer.print();

    }

}
