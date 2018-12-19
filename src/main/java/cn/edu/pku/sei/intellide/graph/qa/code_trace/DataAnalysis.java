package cn.edu.pku.sei.intellide.graph.qa.code_trace;
import cn.edu.pku.sei.intellide.graph.extraction.code_mention.CodeMentionExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.git.GitExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.java.JavaExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.tokenization.TokenExtractor;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cn.edu.pku.sei.intellide.graph.extraction.jira.JiraExtractor;

public class DataAnalysis {

    public static void Test() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\hama"));

        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";


        int cnt[]=new int[12];

        double Methodcnt=0;
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);
                    Document document = new Document();

                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        if (relation.isType(CodeMentionExtractor.RELATE_VIA_COMMIT)) {
                            Node otherNode = relation.getOtherNode(node);
                            document.add(new StringField(ID_FIELD, otherNode.getProperty("name").toString(), Field.Store.YES));
                            String content = otherNode.getProperty("summary").toString() + " " + otherNode.getProperty("description").toString();
                            document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                            iwriter.addDocument(document);
                        }
                    }
                    iwriter.close();

                    DirectoryReader ireader = DirectoryReader.open(directory);
                    IndexSearcher isearcher = new IndexSearcher(ireader);
                    QueryParser parser = new QueryParser(CONTENT_FIELD, analyzer);

                    Iterator<Relationship> Iter = node.getRelationships().iterator();
                    while (Iter.hasNext()) {
                        Relationship relation = Iter.next();
                        if (relation.isType(JavaExtractor.HAVE_METHOD)) {
                            Node methodNode = relation.getOtherNode(node);
                            Methodcnt++;



                            String name = (String) methodNode.getProperty(JavaExtractor.NAME);
                            String fullName= (String) methodNode.getProperty(JavaExtractor.FULLNAME);
                            String q = name.toLowerCase();


                            Iterator<Relationship> It = methodNode.getRelationships().iterator();
                            while(It.hasNext()){
                                Relationship r =It.next();
                                if(r.isType(JavaExtractor.METHOD_CALL)){
                                    Node other=r.getOtherNode(methodNode);
                                    q=q+" OR "+other.getProperty(JavaExtractor.NAME).toString().toLowerCase();
                                }
                            }


                            Query query = parser.parse(q);


                            ScoreDoc[] hits = isearcher.search(query, 10000).scoreDocs;
                            if (hits.length > 0) {
                                System.out.println(fullName+":"+hits.length);
                            }

                            if(hits.length<=10){
                                cnt[hits.length]++;
                            }else{
                                cnt[11]++;
                            }

                        }
                    }

                }
            }
            tx.success();
        }

        System.out.println(Methodcnt);

        for(int i=0;i<12;++i){
            System.out.println(i+":"+cnt[i]);
        }
    }


    public static void main(String args[])throws IOException ,ParseException{
        Test();

    }
}
