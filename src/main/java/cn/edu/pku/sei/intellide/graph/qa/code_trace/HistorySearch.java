package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.webapp.entity.CommitResult;
import cn.edu.pku.sei.intellide.graph.webapp.entity.HistoryResult;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HistorySearch {
    private Repository repository;
    private CommitSearch commitSearch;
    public HistorySearch(Repository repository,CommitSearch commitSearch){
        this.repository=repository;
        this.commitSearch=commitSearch;
    }

    public List<HistoryResult> search(CodeAnalyzer analyzer){
        if(!analyzer.getType().equals("Class"))return null;
        List<CommitResult> commitResults=commitSearch.searchCommitResultByClassName(analyzer.getFullNameFromCode());
        List<HistoryResult>result=new ArrayList<>();
        String content=analyzer.getCode();
        String preContent="";
        System.out.println("查找历史.............");
        for(int i=0;i<commitResults.size()-1;++i){
            try{
                RevWalk walk = new RevWalk(repository);
                String commitid=commitResults.get(i+1).getName();
                ObjectId id = repository.resolve(commitid);
                RevCommit commit = walk.parseCommit( id );

                TreeWalk twalk = TreeWalk.forPath(repository, commitResults.get(i+1).getPath(), commit.getTree());
                if (twalk != null) {
                    byte[] bytes = repository.open(twalk.getObjectId(0)).getBytes();
                    System.out.println(new String(bytes, StandardCharsets.UTF_8)) ;
                    preContent=new String(bytes, StandardCharsets.UTF_8);
                } else {
                    System.out.println("twalk is null");
                    continue;
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            System.out.println("---------------------------");
            if(!preContent.equals(content))
                result.add(new HistoryResult(preContent,content,commitResults.get(i).getCommitMessage()));
            content=preContent;
        }
        System.out.println("查找历史完毕");
        return result;
    }
}