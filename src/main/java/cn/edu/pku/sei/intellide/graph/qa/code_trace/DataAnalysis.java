package cn.edu.pku.sei.intellide.graph.qa.code_trace;
import cn.edu.pku.sei.intellide.graph.extraction.code_mention.CodeMentionExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.git.GitExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.java.JavaExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.mail.MailExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.qa.StackOverflowExtractor;
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

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cn.edu.pku.sei.intellide.graph.extraction.jira.JiraExtractor;

public class DataAnalysis {

    static List<String> camelSplit(String e) {
        List<String> r = new ArrayList<String>();
        Matcher m = Pattern.compile("^([a-z]+)|([A-Z][a-z]+)|([A-Z]+(?=([A-Z]|$)))").matcher(e);
        if (m.find()) {
            String s = m.group().toLowerCase();
            r.add(s);
            if (s.length() < e.length())
                r.addAll(camelSplit(e.substring(s.length())));
        }
        return r;
    }

    public static void TestIssueUseAPIName() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));
        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APInameForIssue.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);


                    List<Node>issues=new ArrayList();

                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        if (relation.isType(CodeMentionExtractor.ADD)||relation.isType(CodeMentionExtractor.DELETE)||relation.isType(CodeMentionExtractor.MODIFY)) {
                            Node otherNode = relation.getOtherNode(node);
                            Iterator<Relationship> relationIter1 = otherNode.getRelationships().iterator();
                            while(relationIter1.hasNext()){
                                Relationship relation1 = relationIter1.next();
                                if(relation1.isType(CodeMentionExtractor.COMMIT_FOR_ISSUE)){
                                    Node otherNode1=relation1.getOtherNode(otherNode);
                                    if(!issues.contains(otherNode1)){
                                        issues.add(otherNode1);
                                    }
                                }
                            }
                        }
                    }

                    for(int i=0;i<issues.size();++i){
                        Node no=issues.get(i);
                        Document document = new Document();
                        document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                        String content =no.getProperty("summary").toString() + " " + no.getProperty("description").toString();
                        document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                        iwriter.addDocument(document);
                    }

                    iwriter.close();

                    DirectoryReader ireader = DirectoryReader.open(directory);
                    IndexSearcher isearcher = new IndexSearcher(ireader);
                    QueryParser parser = new QueryParser(CONTENT_FIELD, analyzer);

                    Iterator<Relationship> Iter = node.getRelationships().iterator();
                    while (Iter.hasNext()) {
                        Relationship relation = Iter.next();
                        if (relation.isType(JavaExtractor.HAVE_METHOD)) {
                            methodNum++;
                            Node methodNode = relation.getOtherNode(node);

                            String name = (String) methodNode.getProperty(JavaExtractor.NAME);
                            String fullName= (String) methodNode.getProperty(JavaExtractor.FULLNAME);
                            String q = name;

                            Query query = parser.parse(q);

                            ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

                            if (hits.length > 0) {
                                if(hits.length<=10){
                                    hitnumber[hits.length]++;
                                }else{
                                    hitnumber[11]++;
                                }
                                System.out.println(fullName+":"+hits.length);
                                bw.write(fullName+":"+hits.length+"\r\n");
                                bw.write("\r\n");
                                bw.write("\r\n");
                                for(int i=0;i<hits.length;++i){
                                    Document doc = ireader.document(hits[i].doc);
                                    long id = Long.parseLong(doc.getField(ID_FIELD).stringValue());
                                    Node result = graphDb.getNodeById(id);
                                    System.out.println("FindISSUE-NO"+(i+1));
                                    bw.write("FindISSUE-NO"+(i+1)+"\r\n");
                                    System.out.println("summary:"+result.getProperty("summary").toString());
                                    bw.write("summary:"+result.getProperty("summary").toString()+"\r\n");
                                    System.out.println("description:"+result.getProperty("description").toString());
                                    bw.write("description:"+result.getProperty("description").toString()+"\r\n");
                                    bw.write("\r\n");

                                }
                                bw.write("-------------------------------");
                                bw.write("\r\n");
                                System.out.println("-------------------------------");

                            }else{
                                hitnumber[0]++;
                            }
                        }
                    }
                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }



    public static void TestIssueUseAPIComment() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));
        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APIcommentForIssue.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);


                    List<Node>issues=new ArrayList();

                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        if (relation.isType(CodeMentionExtractor.ADD)||relation.isType(CodeMentionExtractor.DELETE)||relation.isType(CodeMentionExtractor.MODIFY)) {
                            Node otherNode = relation.getOtherNode(node);
                            Iterator<Relationship> relationIter1 = otherNode.getRelationships().iterator();
                            while(relationIter1.hasNext()){
                                Relationship relation1 = relationIter1.next();
                                if(relation1.isType(CodeMentionExtractor.COMMIT_FOR_ISSUE)){
                                    Node otherNode1=relation1.getOtherNode(otherNode);
                                    if(!issues.contains(otherNode1)){
                                        issues.add(otherNode1);
                                    }
                                }
                            }
                        }
                    }

                    for(int i=0;i<issues.size();++i){
                        Node no=issues.get(i);
                        Document document = new Document();
                        document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                        String content =no.getProperty("summary").toString() + " " + no.getProperty("description").toString();
                        document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                        iwriter.addDocument(document);
                    }

                    iwriter.close();

                    DirectoryReader ireader = DirectoryReader.open(directory);
                    IndexSearcher isearcher = new IndexSearcher(ireader);
                    QueryParser parser = new QueryParser(CONTENT_FIELD, analyzer);

                    Iterator<Relationship> Iter = node.getRelationships().iterator();
                    while (Iter.hasNext()) {
                        Relationship relation = Iter.next();
                        if (relation.isType(JavaExtractor.HAVE_METHOD)) {
                            methodNum++;
                            Node methodNode = relation.getOtherNode(node);

                            String name = (String) methodNode.getProperty(JavaExtractor.NAME);
                            String fullName= (String) methodNode.getProperty(JavaExtractor.FULLNAME);


                            List<String> r=camelSplit(name);

                            String q = "method ";
                            if(r.size()>0){
                                q+="AND (";
                            }
                            for(int i=0;i<r.size();++i){
                                if(i==0){
                                    q+=r.get(i);
                                } else{
                                    q+=" AND "+r.get(i);
                                }

                            }

                            if(r.size()>0){
                                q+=" )";
                            }
                            if(!methodNode.getProperty("returnType").toString().equals("void")){
                                q+=" AND ( return OR "+methodNode.getProperty("returnType").toString().split("\\[")[0].split("<")[0]+")";
                            }else{
                             //   q+=")";
                            }

                            Query query = parser.parse(q);
                            ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

                            if (hits.length > 0) {
                                if(hits.length<=10){
                                    hitnumber[hits.length]++;
                                }else{
                                    hitnumber[11]++;
                                }
                                System.out.println(fullName+":"+hits.length);
                                bw.write(fullName+":"+hits.length+"\r\n");
                                bw.write("\r\n");
                                bw.write("\r\n");
                                for(int i=0;i<hits.length;++i){
                                    Document doc = ireader.document(hits[i].doc);
                                    long id = Long.parseLong(doc.getField(ID_FIELD).stringValue());
                                    Node result = graphDb.getNodeById(id);
                                    System.out.println("FindISSUE-NO"+(i+1));
                                    bw.write("FindISSUE-NO"+(i+1)+"\r\n");
                                    System.out.println("summary:"+result.getProperty("summary").toString());
                                    bw.write("summary:"+result.getProperty("summary").toString()+"\r\n");
                                    System.out.println("description:"+result.getProperty("description").toString());
                                    bw.write("description:"+result.getProperty("description").toString()+"\r\n");
                                    bw.write("\r\n");

                                }
                                bw.write("-------------------------------");
                                bw.write("\r\n");
                                System.out.println("-------------------------------");

                            }else{
                                hitnumber[0]++;
                            }
                        }
                    }
                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }




    public static void TestIssueAndCommentsUseAPIName() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));

        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APInameForIssueAndComnents.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);


                    List<Node>issues=new ArrayList();

                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        if (relation.isType(CodeMentionExtractor.ADD)||relation.isType(CodeMentionExtractor.DELETE)||relation.isType(CodeMentionExtractor.MODIFY)) {
                            Node otherNode = relation.getOtherNode(node);
                            Iterator<Relationship> relationIter1 = otherNode.getRelationships().iterator();
                            while(relationIter1.hasNext()){
                                Relationship relation1 = relationIter1.next();
                                if(relation1.isType(CodeMentionExtractor.COMMIT_FOR_ISSUE)){
                                    Node otherNode1=relation1.getOtherNode(otherNode);
                                    if(!issues.contains(otherNode1)){
                                        issues.add(otherNode1);
                                    }
                                }
                            }
                        }
                    }

                    for(int i=0;i<issues.size();++i){
                        Node no=issues.get(i);
                        Document document = new Document();
                        document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                        String content =no.getProperty("summary").toString() + " " + no.getProperty("description").toString();
                        document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                        iwriter.addDocument(document);

                        Iterator<Relationship> it = no.getRelationships().iterator();
                        while(it.hasNext()){
                            Relationship rel = it.next();
                            if(rel.getType().name().equals("jira_have_issue_comment")){
                                Node no1=rel.getOtherNode(no);
                                Document document1 = new Document();
                                document1.add(new StringField(ID_FIELD, "" + no1.getId(), Field.Store.YES));
                                String content1 =no1.getProperty("body").toString();
                                document1.add(new TextField(CONTENT_FIELD, content1, Field.Store.YES));
                                iwriter.addDocument(document1);

                            }
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
                            methodNum++;
                            Node methodNode = relation.getOtherNode(node);

                            String name = (String) methodNode.getProperty(JavaExtractor.NAME);
                            String fullName= (String) methodNode.getProperty(JavaExtractor.FULLNAME);
                            String q = name;

                            Query query = parser.parse(q);

                            ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

                            if (hits.length > 0) {
                                if(hits.length<=10){
                                    hitnumber[hits.length]++;
                                }else{
                                    hitnumber[11]++;
                                }
                                System.out.println(fullName+":"+hits.length);
                                bw.write(fullName+":"+hits.length+"\r\n");
                                bw.write("\r\n");
                                bw.write("\r\n");
                                for(int i=0;i<hits.length;++i){
                                    Document doc = ireader.document(hits[i].doc);
                                    long id = Long.parseLong(doc.getField(ID_FIELD).stringValue());
                                    Node result = graphDb.getNodeById(id);


                                    System.out.println("NO"+(i+1));
                                    bw.write("NO"+(i+1)+"\r\n");
                                    if(result.hasLabel(JiraExtractor.ISSUE)){
                                        System.out.println("summary:"+result.getProperty("summary").toString());
                                        bw.write("summary:"+result.getProperty("summary").toString()+"\r\n");
                                        System.out.println("description:"+result.getProperty("description").toString());
                                        bw.write("description:"+result.getProperty("description").toString()+"\r\n");

                                        bw.write("\r\n");
                                    }else if(result.hasLabel(JiraExtractor.ISSUECOMMENT)){
                                        System.out.println("comment:"+result.getProperty("body").toString());
                                        bw.write("comment:"+result.getProperty("body").toString()+"\r\n");
                                        bw.write("\r\n");
                                    }


                                }

                                bw.write("-------------------------------");
                                bw.write("\r\n");
                                System.out.println("-------------------------------");

                            }else{
                                hitnumber[0]++;
                            }
                        }
                    }
                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }


    public static void TestIssueAndCommentsUseAPIComment() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));

        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APIcommentForIssueAndComnents.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);


                    List<Node>issues=new ArrayList();

                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        if (relation.isType(CodeMentionExtractor.ADD)||relation.isType(CodeMentionExtractor.DELETE)||relation.isType(CodeMentionExtractor.MODIFY)) {
                            Node otherNode = relation.getOtherNode(node);
                            Iterator<Relationship> relationIter1 = otherNode.getRelationships().iterator();
                            while(relationIter1.hasNext()){
                                Relationship relation1 = relationIter1.next();
                                if(relation1.isType(CodeMentionExtractor.COMMIT_FOR_ISSUE)){
                                    Node otherNode1=relation1.getOtherNode(otherNode);
                                    if(!issues.contains(otherNode1)){
                                        issues.add(otherNode1);
                                    }
                                }
                            }
                        }
                    }

                    for(int i=0;i<issues.size();++i){
                        Node no=issues.get(i);
                        Document document = new Document();
                        document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                        String content =no.getProperty("summary").toString() + " " + no.getProperty("description").toString();
                        document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                        iwriter.addDocument(document);

                        Iterator<Relationship> it = no.getRelationships().iterator();
                        while(it.hasNext()){
                            Relationship rel = it.next();
                            if(rel.getType().name().equals("jira_have_issue_comment")){
                                Node no1=rel.getOtherNode(no);
                                Document document1 = new Document();
                                document1.add(new StringField(ID_FIELD, "" + no1.getId(), Field.Store.YES));
                                String content1 =no1.getProperty("body").toString();
                                document1.add(new TextField(CONTENT_FIELD, content1, Field.Store.YES));
                                iwriter.addDocument(document1);

                            }
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
                            methodNum++;
                            Node methodNode = relation.getOtherNode(node);

                            String name = (String) methodNode.getProperty(JavaExtractor.NAME);
                            String fullName= (String) methodNode.getProperty(JavaExtractor.FULLNAME);
                            List<String> r=camelSplit(name);

                            String q = "method ";
                            if(r.size()>0){
                                q+="AND (";
                            }
                            for(int i=0;i<r.size();++i){
                                if(i==0){
                                    q+=r.get(i);
                                } else{
                                    q+=" AND "+r.get(i);
                                }

                            }

                            if(r.size()>0){
                                q+=" )";
                            }
                            if(!methodNode.getProperty("returnType").toString().equals("void")){
                                q+=" AND ( return OR "+methodNode.getProperty("returnType").toString().split("\\[")[0].split("<")[0]+")";
                            }else{
                                //   q+=")";
                            }
                            Query query = parser.parse(q);

                            ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

                            if (hits.length > 0) {
                                if(hits.length<=10){
                                    hitnumber[hits.length]++;
                                }else{
                                    hitnumber[11]++;
                                }
                                System.out.println(fullName+":"+hits.length);
                                bw.write(fullName+":"+hits.length+"\r\n");
                                bw.write("\r\n");
                                bw.write("\r\n");
                                for(int i=0;i<hits.length;++i){
                                    Document doc = ireader.document(hits[i].doc);
                                    long id = Long.parseLong(doc.getField(ID_FIELD).stringValue());
                                    Node result = graphDb.getNodeById(id);


                                    System.out.println("NO"+(i+1));
                                    bw.write("NO"+(i+1)+"\r\n");
                                    if(result.hasLabel(JiraExtractor.ISSUE)){
                                        System.out.println("summary:"+result.getProperty("summary").toString());
                                        bw.write("summary:"+result.getProperty("summary").toString()+"\r\n");
                                        System.out.println("description:"+result.getProperty("description").toString());
                                        bw.write("description:"+result.getProperty("description").toString()+"\r\n");

                                        bw.write("\r\n");
                                    }else if(result.hasLabel(JiraExtractor.ISSUECOMMENT)){
                                        System.out.println("comment:"+result.getProperty("body").toString());
                                        bw.write("comment:"+result.getProperty("body").toString()+"\r\n");
                                        bw.write("\r\n");
                                    }


                                }

                                bw.write("-------------------------------");
                                bw.write("\r\n");
                                System.out.println("-------------------------------");

                            }else{
                                hitnumber[0]++;
                            }
                        }
                    }
                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }


    public static void TestEmailUseAPIName() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));

        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APInameForEmail.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);


                    List<Node>emails=new ArrayList();

                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        if (relation.isType(CodeMentionExtractor.CODE_MENTION)) {
                            Node otherNode = relation.getOtherNode(node);
                            if(otherNode.hasLabel(MailExtractor.MAIL)){
                                if(!emails.contains(otherNode)){
                                    emails.add(otherNode);
                                }
                            }
                        }
                    }

                    for(int i=0;i<emails.size();++i){
                        Node no=emails.get(i);
                        Document document = new Document();
                        document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                        String content =no.getProperty("body").toString();
                        document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                        iwriter.addDocument(document);
                    }



                    iwriter.close();

                    DirectoryReader ireader = DirectoryReader.open(directory);
                    IndexSearcher isearcher = new IndexSearcher(ireader);
                    QueryParser parser = new QueryParser(CONTENT_FIELD, analyzer);

                    Iterator<Relationship> Iter = node.getRelationships().iterator();
                    while (Iter.hasNext()) {
                        Relationship relation = Iter.next();
                        if (relation.isType(JavaExtractor.HAVE_METHOD)) {
                            methodNum++;
                            Node methodNode = relation.getOtherNode(node);

                            String name = (String) methodNode.getProperty(JavaExtractor.NAME);
                            String fullName= (String) methodNode.getProperty(JavaExtractor.FULLNAME);
                            String q=name;
                            Query query = parser.parse(q);

                            ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

                            if (hits.length > 0) {
                                if(hits.length<=10){
                                    hitnumber[hits.length]++;
                                }else{
                                    hitnumber[11]++;
                                }
                                System.out.println(fullName+":"+hits.length);
                                bw.write(fullName+":"+hits.length+"\r\n");
                                bw.write("\r\n");
                                bw.write("\r\n");
                                for(int i=0;i<hits.length;++i){
                                    Document doc = ireader.document(hits[i].doc);
                                    long id = Long.parseLong(doc.getField(ID_FIELD).stringValue());
                                    Node result = graphDb.getNodeById(id);


                                    System.out.println("NO"+(i+1));
                                    bw.write("NO"+(i+1)+"\r\n");

                                    System.out.println("email:"+result.getProperty("body").toString());
                                    bw.write("email:"+result.getProperty("body").toString()+"\r\n");
                                    bw.write("\r\n");

                                }

                                bw.write("-------------------------------");
                                bw.write("\r\n");
                                System.out.println("-------------------------------");

                            }else{
                                hitnumber[0]++;
                            }
                        }
                    }
                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }

    public static void TestEmailUseAPIcomment() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));

        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APIcommentForEmail.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Class")) {

                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);


                    List<Node>emails=new ArrayList();

                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        if (relation.isType(CodeMentionExtractor.CODE_MENTION)) {
                            Node otherNode = relation.getOtherNode(node);
                            if(otherNode.hasLabel(MailExtractor.MAIL)){
                                if(!emails.contains(otherNode)){
                                    emails.add(otherNode);
                                }
                            }
                        }
                    }

                    for(int i=0;i<emails.size();++i){
                        Node no=emails.get(i);
                        Document document = new Document();
                        document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                        String content =no.getProperty("body").toString();
                        document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                        iwriter.addDocument(document);
                    }



                    iwriter.close();

                    DirectoryReader ireader = DirectoryReader.open(directory);
                    IndexSearcher isearcher = new IndexSearcher(ireader);
                    QueryParser parser = new QueryParser(CONTENT_FIELD, analyzer);

                    Iterator<Relationship> Iter = node.getRelationships().iterator();
                    while (Iter.hasNext()) {
                        Relationship relation = Iter.next();
                        if (relation.isType(JavaExtractor.HAVE_METHOD)) {
                            methodNum++;
                            Node methodNode = relation.getOtherNode(node);

                            String name = (String) methodNode.getProperty(JavaExtractor.NAME);
                            String fullName= (String) methodNode.getProperty(JavaExtractor.FULLNAME);
                            List<String> r=camelSplit(name);

                            String q = "method ";
                            if(r.size()>0){
                                q+="AND (";
                            }
                            for(int i=0;i<r.size();++i){
                                if(i==0){
                                    q+=r.get(i);
                                } else{
                                    q+=" AND "+r.get(i);
                                }

                            }

                            if(r.size()>0){
                                q+=" )";
                            }
                            if(!methodNode.getProperty("returnType").toString().equals("void")){
                                q+=" AND ( return OR "+methodNode.getProperty("returnType").toString().split("\\[")[0].split("<")[0]+")";
                            }else{
                                //   q+=")";
                            }
                            Query query = parser.parse(q);

                            ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

                            if (hits.length > 0) {
                                if(hits.length<=10){
                                    hitnumber[hits.length]++;
                                }else{
                                    hitnumber[11]++;
                                }
                                System.out.println(fullName+":"+hits.length);
                                bw.write(fullName+":"+hits.length+"\r\n");
                                bw.write("\r\n");
                                bw.write("\r\n");
                                for(int i=0;i<hits.length;++i){
                                    Document doc = ireader.document(hits[i].doc);
                                    long id = Long.parseLong(doc.getField(ID_FIELD).stringValue());
                                    Node result = graphDb.getNodeById(id);


                                    System.out.println("NO"+(i+1));
                                    bw.write("NO"+(i+1)+"\r\n");

                                    System.out.println("email:"+result.getProperty("body").toString());
                                    bw.write("email:"+result.getProperty("body").toString()+"\r\n");
                                    bw.write("\r\n");

                                }

                                bw.write("-------------------------------");
                                bw.write("\r\n");
                                System.out.println("-------------------------------");

                            }else{
                                hitnumber[0]++;
                            }
                        }
                    }
                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }




    public static void TestStackOverflowUseAPIName() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));

        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APInameForStackOverflow.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Method")) {
                    methodNum++;
                    List<Node>result=new ArrayList();
                    Iterator<Relationship> Iter = node.getRelationships().iterator();
                    while(Iter.hasNext()){
                        Relationship rel=Iter.next();
                        if(rel.isType(CodeMentionExtractor.CODE_MENTION)){
                            Node otherNode=rel.getOtherNode(node);
                            if(otherNode.hasLabel(StackOverflowExtractor.ANSWER)||otherNode.hasLabel(StackOverflowExtractor.COMMENT)){
                                result.add(otherNode);
                            }
                        }
                    }

                    if(result.size()<=10){
                        hitnumber[result.size()]++;
                    }else{
                        hitnumber[11]++;
                    }

                    String name = (String) node.getProperty(JavaExtractor.NAME);
                    String fullName = (String) node.getProperty(JavaExtractor.FULLNAME);


                    System.out.println(fullName+":"+result.size());
                    bw.write(fullName+":"+result.size()+"\r\n");
                    bw.write("\r\n");
                    bw.write("\r\n");

                    for(int i=0;i<result.size();++i){
                        Node r = result.get(i);


                        System.out.println("NO"+(i+1));
                        bw.write("NO"+(i+1)+"\r\n");

                        if(r.hasLabel(StackOverflowExtractor.ANSWER)){
                            System.out.println("stackoverflowAnswer:"+r.getProperty("body").toString());
                            bw.write("stackoverflowAnswer:"+r.getProperty("body").toString()+"\r\n");
                            bw.write("\r\n");
                        }else if(r.hasLabel(StackOverflowExtractor.COMMENT)){
                            System.out.println("stackoverflowComments:"+r.getProperty("text").toString());
                            bw.write("stackoverflowComments:"+r.getProperty("text").toString()+"\r\n");
                            bw.write("\r\n");
                        }


                    }

                    bw.write("-------------------------------");
                    bw.write("\r\n");
                    System.out.println("-------------------------------");


                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }


    public static void TestStackOverflowUseAPIcomment() throws IOException ,ParseException{
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\lucene"));

        final String CONTENT_FIELD = "content";
        final String ID_FIELD = "id";

        int methodNum=0;
        int hitnumber[]=new int[12];

        FileOutputStream fos=new FileOutputStream(new File("D:\\Work\\APIcommentForStackOverflow.txt"));
        OutputStreamWriter osw=new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter  bw=new BufferedWriter(osw);
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getLabels().iterator().next().name().equals("Method")) {
                    methodNum++;
                    List<Node>result=new ArrayList();
                    Iterator<Relationship> Iter = node.getRelationships().iterator();
                    while(Iter.hasNext()){
                        Relationship rel=Iter.next();
                        if(rel.isType(CodeMentionExtractor.CODE_MENTION)){
                            Node otherNode=rel.getOtherNode(node);
                            if(otherNode.hasLabel(StackOverflowExtractor.ANSWER)||otherNode.hasLabel(StackOverflowExtractor.COMMENT)){
                                result.add(otherNode);
                            }
                        }
                    }


                    Analyzer analyzer = new StandardAnalyzer();
                    Directory directory = new RAMDirectory();
                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    IndexWriter iwriter = new IndexWriter(directory, config);



                    for(int i=0;i<result.size();++i){
                        Node no=result.get(i);
                        if(no.hasLabel(StackOverflowExtractor.ANSWER)){
                            Document document = new Document();
                            document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                            String content =no.getProperty("body").toString();
                            document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                            iwriter.addDocument(document);
                        }else if(no.hasLabel(StackOverflowExtractor.COMMENT)){
                            Document document = new Document();
                            document.add(new StringField(ID_FIELD, "" + no.getId(), Field.Store.YES));
                            String content =no.getProperty("text").toString();
                            document.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
                            iwriter.addDocument(document);
                        }

                    }

                    iwriter.close();

                    DirectoryReader ireader = DirectoryReader.open(directory);
                    IndexSearcher isearcher = new IndexSearcher(ireader);
                    QueryParser parser = new QueryParser(CONTENT_FIELD, analyzer);


                    String name = (String) node.getProperty(JavaExtractor.NAME);
                    String fullName= (String) node.getProperty(JavaExtractor.FULLNAME);
                    List<String> r=camelSplit(name);

                    String q = "method ";
                    if(r.size()>0){
                        q+="AND (";
                    }
                    for(int i=0;i<r.size();++i){
                        if(i==0){
                            q+=r.get(i);
                        } else{
                            q+=" AND "+r.get(i);
                        }

                    }

                    if(r.size()>0){
                        q+=" )";
                    }
                    if(!node.getProperty("returnType").toString().equals("void")){
                        q+=" AND ( return OR "+node.getProperty("returnType").toString().split("\\[")[0].split("<")[0]+")";
                    }else{
                        //   q+=")";
                    }
                    Query query = parser.parse(q);

                    ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;


                    if (hits.length > 0) {
                        if(hits.length<=10){
                            hitnumber[hits.length]++;
                        }else{
                            hitnumber[11]++;
                        }
                        System.out.println(fullName+":"+hits.length);
                        bw.write(fullName+":"+hits.length+"\r\n");
                        bw.write("\r\n");
                        bw.write("\r\n");
                        for(int i=0;i<hits.length;++i){
                            Document doc = ireader.document(hits[i].doc);
                            long id = Long.parseLong(doc.getField(ID_FIELD).stringValue());
                            Node re = graphDb.getNodeById(id);


                            System.out.println("NO"+(i+1));
                            bw.write("NO"+(i+1)+"\r\n");


                            if(re.hasLabel(StackOverflowExtractor.ANSWER)){
                                System.out.println("StackoverflowAnswer:"+re.getProperty("body").toString());
                                bw.write("StackoverflowAnswer:"+re.getProperty("body").toString()+"\r\n");
                                bw.write("\r\n");

                            }else if(re.hasLabel(StackOverflowExtractor.COMMENT)){
                                System.out.println("stackoverflowComments:"+re.getProperty("text").toString());
                                bw.write("stackoverflowComments:"+re.getProperty("text").toString()+"\r\n");
                                bw.write("\r\n");
                            }

                        }

                        bw.write("-------------------------------");
                        bw.write("\r\n");
                        System.out.println("-------------------------------");

                    }else{
                        hitnumber[0]++;
                    }




                }
            }

            tx.success();
        }

        bw.close();
        osw.close();
        fos.close();

        System.out.println("total method:"+methodNum);
        for(int i=0;i<12;++i){
            System.out.println("hits-"+i+" "+hitnumber[i]);
        }
    }


    public static void main(String args[])throws IOException ,ParseException{
     //     TestIssueUseAPIName();
      //    TestIssueAndCommentsUseAPIName();
       //   TestEmailUseAPIName();
      //    TestStackOverflowUseAPIName();
      //    TestIssueUseAPIComment();
      //    TestIssueAndCommentsUseAPIComment();
      //    TestEmailUseAPIcomment();
          TestStackOverflowUseAPIcomment();
    }




}