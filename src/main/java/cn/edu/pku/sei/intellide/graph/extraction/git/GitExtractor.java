package cn.edu.pku.sei.intellide.graph.extraction.git;

import cn.edu.pku.sei.intellide.graph.extraction.KnowledgeExtractor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.DepthWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.JSONObject;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import sun.reflect.generics.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.ByteArrayOutputStream;

public class GitExtractor extends KnowledgeExtractor {

    public static final RelationshipType PARENT = RelationshipType.withName("parent");
    public static final String NAME = "name";
    public static final String MESSAGE = "message";
    public static final String COMMIT_TIME = "commitTime";
    public static final String DIFF_SUMMARY = "diffSummary";
    public static final Label COMMIT = Label.label("Commit");
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String DIFF_MESSAGE_MAP = "diffMessageMap";
    public static final Label GIT_USER = Label.label("GitUser");
    public static final RelationshipType CREATOR = RelationshipType.withName("creator");
    public static final RelationshipType COMMITTER = RelationshipType.withName("committer");

    private Map<String, Long> commitMap = new HashMap<>();
    private Map<String, Long> personMap = new HashMap<>();
    private Map<String, Set<String>> parentsMap = new HashMap<>();

    @Override
    public boolean isBatchInsert() {
        return true;
    }

    @Override
    public void extraction() {
        Repository repository = null;
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.setMustExist(true);
        repositoryBuilder.setGitDir(new File(this.getDataDir()));
        try {
            repository = repositoryBuilder.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (repository.getObjectDatabase().exists()) {
            Git git = new Git(repository);
            Iterable<RevCommit> commits = null;
            try {
                commits = git.log().call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
            for (RevCommit commit : commits)
                try {
                    parseCommit(commit, repository, git);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
        }
        parentsMap.entrySet().forEach(entry -> {
            long commitNodeId = commitMap.get(entry.getKey());
            entry.getValue().forEach(parentName -> {
                if (commitMap.containsKey(parentName))
                    this.getInserter().createRelationship(commitNodeId, commitMap.get(parentName), PARENT, new HashMap<>());
            });
        });
    }

    private void parseCommit(RevCommit commit, Repository repository, Git git) throws IOException, GitAPIException {
        //System.out.println(commit.getName());
        Map<String, Object> map = new HashMap<>();
        map.put(NAME, commit.getName());
        String message = commit.getFullMessage();
        map.put(MESSAGE, message != null ? message : "");
        map.put(COMMIT_TIME, commit.getCommitTime());
        List<String> diffStrs = new ArrayList<>();
        Set<String> parentNames = new HashSet<>();

        JSONObject diffMessageMap=new JSONObject();

        for (int i = 0; i < commit.getParentCount(); i++) {
            parentNames.add(commit.getParent(i).getName());
            ObjectId head = repository.resolve(commit.getName() + "^{tree}");
            ObjectId old = repository.resolve(commit.getParent(i).getName() + "^{tree}");
            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, old);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);
            List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
            for (int k = 0; k < diffs.size(); k++) {
                String diffMessage="";
                if(!diffs.get(k).getOldPath().equals("/dev/null")){
                    diffMessage=getDiffMessage(head,old,diffs.get(k).getOldPath(),repository,git);
                    try{
                        diffMessageMap.put(diffs.get(k).getOldPath(),diffMessage);
                        System.out.println("find the diffsummary....................");
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }

                } else if(!diffs.get(k).getNewPath().equals("/dev/null")){
                    diffMessage=getDiffMessage(head,old,diffs.get(k).getNewPath(),repository,git);
                    try{
                        diffMessageMap.put(diffs.get(k).getNewPath(),diffMessage);
                        System.out.println("find the diffsummary....................");
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
                diffStrs.add(diffs.get(k).getChangeType().name() + " " + diffs.get(k).getOldPath() + " to " + diffs.get(k).getNewPath());
            }
        }

        map.put(DIFF_MESSAGE_MAP,diffMessageMap.toString());
        System.out.println("All diffSummary are put into graph----------------------------------");

        map.put(DIFF_SUMMARY, String.join("\n", diffStrs));

        long commitNodeId = this.getInserter().createNode(map, COMMIT);
        commitMap.put(commit.getName(), commitNodeId);
        parentsMap.put(commit.getName(), parentNames);
        PersonIdent author = commit.getAuthorIdent();
        String personStr = author.getName() + ": " + author.getEmailAddress();
        if (!personMap.containsKey(personStr)) {
            Map<String, Object> pMap = new HashMap<>();
            String name = author.getName();
            String email = author.getEmailAddress();
            pMap.put(NAME, name != null ? name : "");
            pMap.put(EMAIL_ADDRESS, email != null ? email : "");
            long personNodeId = this.getInserter().createNode(pMap, GIT_USER);
            personMap.put(personStr, personNodeId);
            this.getInserter().createRelationship(commitNodeId, personNodeId, CREATOR, new HashMap<>());
        } else
            this.getInserter().createRelationship(commitNodeId, personMap.get(personStr), CREATOR, new HashMap<>());
        PersonIdent committer = commit.getCommitterIdent();
        personStr = committer.getName() + ": " + committer.getEmailAddress();
        if (!personMap.containsKey(personStr)) {
            Map<String, Object> pMap = new HashMap<>();
            String name = committer.getName();
            String email = committer.getEmailAddress();
            pMap.put(NAME, name != null ? name : "");
            pMap.put(EMAIL_ADDRESS, email != null ? email : "");
            long personNodeId = this.getInserter().createNode(pMap, GIT_USER);
            personMap.put(personStr, personNodeId);
            this.getInserter().createRelationship(commitNodeId, personNodeId, COMMITTER, new HashMap<>());
        } else
            this.getInserter().createRelationship(commitNodeId, personMap.get(personStr), COMMITTER, new HashMap<>());
    }


    public static String getDiffMessage(ObjectId newId, ObjectId oldId, String filePath, Repository repository,Git git){
        String result=null;
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

            result = out.toString();
        } catch (Exception e) {
            System.out.println(" error + " + newId.getName() + "\n");
            System.out.println(e.getMessage());
        }
        return result;
    }


    public static void main(String args[]){
/*        Repository repository = null;
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.setMustExist(true);
        repositoryBuilder.setGitDir(new File("E:\\lucene-solr\\.git"));
        try {
            repository = repositoryBuilder.build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (repository.getObjectDatabase().exists()) {
            Git git = new Git(repository);
            Iterable<RevCommit> commits = null;
            try {
                commits = git.log().call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }

            for (RevCommit commit : commits){
                for (int i = 0; i < commit.getParentCount(); i++) {
                    try {
                        ObjectId head = repository.resolve(commit.getName() + "^{tree}");
                        ObjectId old = repository.resolve(commit.getParent(i).getName() + "^{tree}");

                        ObjectReader reader = repository.newObjectReader();
                        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                        oldTreeIter.reset(reader, old);
                        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                        newTreeIter.reset(reader, head);

                        List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();

                        for (int j = 0; j < diffs.size(); ++j) {
                            String path = diffs.get(j).getOldPath();
                            System.out.println("------------------------------");
                            String s = getDiffMessage(head, old, path, repository, git);
                            System.out.println(s);
                            System.out.println("------------------------------");
                        }

                    }catch(Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }*/



        try{
            Repository repo = new FileRepository("D:\\项目源代码\\luceneGIT\\.git");
            Git git = new Git(repo);
            Ref head = repo.findRef("HEAD");

            RevWalk walk = new RevWalk(repo);
     /*       String branchName = "refs/heads/master";
            RevCommit masterHead = walk.parseCommit( repo.resolve( branchName ));
            walk.markStart(masterHead);
*/
            String commitid="e92a38af90d12e51390b4307ccbe0c24ac7b6b4e";
            ObjectId id = repo.resolve(commitid);
            RevCommit commit = walk.parseCommit(id);
            RevTree tree = commit.getTree();


            try (TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(tree);
                // not walk the tree recursively so we only get the elements in the top-level directory
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    System.out.println("found: " + treeWalk.getPathString());
                }
            }
       //     System.out.println("commitnum:"+commitnum);


        /*    List<Ref> call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            System.out.println("branch number:"+call.size());
            int sum=0;
            for (Ref ref : call) {

                RevCommit masterHead=walk.parseCommit(ref.getObjectId());
                walk.markStart(masterHead);
                int total=0;
                for (RevCommit commit : walk){
                    total++;
                }
                System.out.println("branch name:"+ref.getName());
                System.out.println(total);
                sum+=total;
            }
            System.out.println("Sum:"+sum);*/



            /*Ref head = repo.exactRef("refs/heads/master");
            RevCommit masterHead=walk.parseCommit(head.getObjectId());
            walk.markStart(masterHead);

            Iterable<RevCommit> commits = null;
            commits = git.log().call();
            int count=0;
            int total=0;
            for (RevCommit commit : walk){
                if(walk.isMergedInto( commit, masterHead )){
                    System.out.println("Not in master!");
                    count++;
                }
                total++;
            }
            System.out.println(count);
            System.out.println(total);*/
        }catch (Exception e){
            e.printStackTrace();
        }






     /*try{
         Repository repo = new FileRepository("D:\\项目源代码\\luceneGIT\\.git");
         RevWalk walk = new RevWalk(repo);
         String commitid="e92a38af90d12e51390b4307ccbe0c24ac7b6b4e";
         ObjectId id = repo.resolve(commitid);
         RevCommit commit = walk.parseCommit( id );
         TreeWalk twalk = TreeWalk.forPath(repo, "lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java", commit.getTree());
         if (twalk != null) {
             byte[] bytes = repo.open(twalk.getObjectId(0)).getBytes();
             System.out.println(new String(bytes, StandardCharsets.UTF_8)) ;
         } else {
             System.out.println("twalk is null");
         }
     }catch(Exception e){
         e.printStackTrace();
     }*/




    }
}
