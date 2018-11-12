package cn.edu.pku.sei.intellide.graph.qa.code_search;

import cn.edu.pku.sei.intellide.graph.extraction.tokenization.TokenExtractor;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.config.StopWords;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jSubGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

public class CodeSearch {

    private String languageIdentifier;
    private GraphDatabaseService db;
    private GraphReader graphReader;
    private APILocater locater;

    public CodeSearch(GraphDatabaseService db, String languageIdentifier){
        this.db = db;
        this.languageIdentifier = languageIdentifier;
        graphReader = new GraphReader(db);
        locater = new APILocater(graphReader);
    }

    /*
     * 对于一个query，首先切词为一个词袋，然后生成一个连通的子图
     */
    public Neo4jSubGraph search(String queryString){
        Set<String> baseTokens = TokenExtractor.tokenization(queryString);
        Set<String> tokens = new HashSet<>();
        for (String token: baseTokens)
            if (!StopWords.getInstance(languageIdentifier).isStopWord(token))
                tokens.add(token);
        //System.out.println("切词结果：" + tokens);
        MySubgraph subgraph = locater.query(tokens);
        if (subgraph == null){
            //System.out.println("codesearcher find no subgraph");
            subgraph = new MySubgraph();
        }
        subgraph.print();

        List<Long> nodes = new ArrayList<>(subgraph.nodes);
        List<Long> rels = new ArrayList<>(subgraph.edges);
        return new Neo4jSubGraph(nodes,rels,db);
    }

    /*
     * 仅返回query中每个词所对应到的那些结点，不返回扩充的那些连接结点
     * 便于调试使用
     */
    public Neo4jSubGraph searchBaseNode(String queryString){
        Set<String> baseTokens = TokenExtractor.tokenization(queryString);
        Set<String> tokens = new HashSet<>();
        for (String token: baseTokens)
            if (!StopWords.getInstance(languageIdentifier).isStopWord(token))
                tokens.add(token);
        MySubgraph subgraph = locater.query(tokens);
        if (subgraph == null){
            //System.out.println("codesearcher find no subgraph");
            subgraph = new MySubgraph();
        }
        List<Long> nodes = new ArrayList<>(subgraph.selectedRoot);
        return new Neo4jSubGraph(nodes, new ArrayList<>(), db);
    }

}
