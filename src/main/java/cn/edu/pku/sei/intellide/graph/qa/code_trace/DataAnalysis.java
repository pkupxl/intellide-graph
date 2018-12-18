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
    public static void main(String args[])throws IOException, ParseException {
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\isis"));

        double Classcnt = 0;
        double count0 = 0;
        double count1 = 0;
        double count2 = 0;
        double count3 = 0;
        double count4 = 0;
        double count5 = 0;
        double count6 = 0;
        double count7 = 0;
        double count8 = 0;
        double count9 = 0;
        double count10 = 0;
        double count11 = 0;
        double count12 = 0;
        double count13 = 0;
        double count14 = 0;
        double count15 = 0;
        double count16 = 0;
        double count17 = 0;
        double count18 = 0;
        double count19 = 0;
        double count20 = 0;
        double count20more = 0;
double sum=0;
double sum1=0;
String projectName="ISIS";
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Classcnt++;
                    Iterator<Relationship> methodIter = node.getRelationships().iterator();

                    int cnt = 0;
                    int cnt1=0;
                    while (methodIter.hasNext()) {
                        Relationship Metherrelation = methodIter.next();
                        Node otherNode = Metherrelation.getOtherNode(node);
                        if (otherNode.hasLabel(GitExtractor.COMMIT) && !Metherrelation.isType(CodeMentionExtractor.CODE_MENTION)) {

                            String message=otherNode.getProperty("message").toString();
                            Pattern pattern = Pattern.compile(projectName+"-(\\d+):(.*)");
                            Matcher matcher = pattern.matcher(message);
                            if(matcher.find() && matcher.start()==0 ){
                               cnt1++;
                            }

                            cnt++;
                        }
                    }

                    sum+=cnt;
                    sum1+=cnt1;

                    if (cnt1 == 0) {
                        count0++;
                    } else if (cnt1 == 1) {
                        count1++;
                    } else if (cnt1 == 2) {
                        count2++;
                    } else if (cnt1 == 3) {
                        count3++;
                    } else if (cnt1 == 4) {
                        count4++;
                    } else if (cnt1 == 5) {
                        count5++;
                    } else if (cnt1 == 6) {
                        count6++;
                    } else if (cnt1 == 7) {
                        count7++;
                    } else if (cnt1 == 8) {
                        count8++;
                    } else if (cnt1 == 9) {
                        count9++;
                    } else if (cnt1 == 10) {
                        count10++;
                    } else if (cnt1 == 11) {
                        count11++;
                    } else if (cnt1 == 12) {
                        count12++;
                    } else if (cnt1 == 13) {
                        count13++;
                    } else if (cnt1 == 14) {
                        count14++;
                    } else if (cnt1 == 15) {
                        count15++;
                    } else if (cnt1 == 16) {
                        count16++;
                    } else if (cnt1 == 17) {
                        count17++;
                    } else if (cnt1 == 18) {
                        count18++;
                    } else if (cnt1 == 19) {
                        count19++;
                    } else if (cnt1 == 20) {
                        count20++;
                    } else {
                        count20more++;
                    }
                }
                tx.success();
            }


            System.out.println(Classcnt);
            System.out.println(count0);
            System.out.println(count1);
            System.out.println(count2);
            System.out.println(count3);
            System.out.println(count4);
            System.out.println(count5);
            System.out.println(count6);
            System.out.println(count7);
            System.out.println(count8);
            System.out.println(count9);
            System.out.println(count10);
            System.out.println(count11);
            System.out.println(count12);
            System.out.println(count13);
            System.out.println(count14);
            System.out.println(count15);
            System.out.println(count16);
            System.out.println(count17);
            System.out.println(count18);
            System.out.println(count19);
            System.out.println(count20);
            System.out.println(count20more);
            System.out.println(sum1/Classcnt);

        }
    }
}
