package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.code_mention.CodeMentionExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.java.JavaExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.tokenization.TokenExtractor;
import cn.edu.pku.sei.intellide.graph.qa.code_search.MyNode;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

public class DataAnalysis {
    public static void main(String args[]){
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("D:\\Work\\Lucene"));

/*
        int Methodcnt=0;
        int Classcnt=0;
        int Commitcnt=0;
        double commitperClass = 0;
        double classperCommit = 0;
        double issueperClass = 0;

        double classnoissue = 0;

        double classoneissue = 0;

        double classtwoissue = 0;

        double classthreeissue = 0;

        double classfourissue = 0;

        double classfiveissue = 0;

        double class6issue = 0;

        double class7issue = 0;

        double class8issue = 0;

        double class9issue = 0;

        double class10issue = 0;

        double classlarge10issue = 0;



        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if(node.getLabels().iterator().next().name().equals("Method")){
                    Methodcnt++;
                }


                if(node.getLabels().iterator().next().name().equals("Class")){

                    int cnt=0;
                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        Node otherNode = relation.getOtherNode(node);
                        if (otherNode.getLabels().iterator().next().name().equals("Commit")) {
                            commitperClass = commitperClass + 1;
                        }else if(otherNode.getLabels().iterator().next().name().equals("JiraIssue")){
                            issueperClass = issueperClass + 1;
                            cnt++;
                        }

                    }

                    if(cnt==0){
                        classnoissue++;
                    }else if(cnt==1){
                        classoneissue++;
                    }else if(cnt==2){
                        classtwoissue++;
                    }else if(cnt==3){
                        classthreeissue++;
                    }else if(cnt==4){
                        classfourissue++;
                    }else if(cnt==5){
                        classfiveissue++;
                    }else if(cnt==6){
                        class6issue++;
                    }else if(cnt==7){
                        class7issue++;
                    }else if(cnt==8){
                        class8issue++;
                    }else if(cnt==9){
                        class9issue++;
                    }else if(cnt==10){
                        class10issue++;
                    }else {
                        classlarge10issue++;
                    }
                    Classcnt++;
                }



                if(node.getLabels().iterator().next().name().equals("Commit")){
                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        Node otherNode = relation.getOtherNode(node);
                        if (otherNode.getLabels().iterator().next().name().equals("Class")) {
                            classperCommit = classperCommit + 1;
                        }
                    }
                    Commitcnt++;
                }


            }
            tx.success();
        }



        System.out.println(Methodcnt);
        System.out.println(Classcnt);
        System.out.println(Commitcnt);
        System.out.println(commitperClass/Classcnt);
        System.out.println(classperCommit/Commitcnt);

        System.out.println(issueperClass/Classcnt);


        System.out.println("0 :"+classnoissue);
        System.out.println("1 :"+classoneissue);
        System.out.println("2 :"+classtwoissue);
        System.out.println("3 :"+classthreeissue);
        System.out.println("4 :"+classfourissue);
        System.out.println("5 :"+classfiveissue);
        System.out.println("6 :"+class6issue);
        System.out.println("7 :"+class7issue);
        System.out.println("8 :"+class8issue);
        System.out.println("9 :"+class9issue);
        System.out.println("10 :"+class10issue);

        System.out.println(">10 :"+classlarge10issue);
*/

        /*try {
            FileWriter fw = new FileWriter(new File("D:\\Work\\luceneClassName1.txt"));
            BufferedWriter bw = new BufferedWriter(fw);





            try (Transaction tx = graphDb.beginTx()) {

                ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
                while (iterator.hasNext()) {
                    Node node = iterator.next();
                    if (node.getLabels().iterator().next().name().equals("Class")) {
                        bw.write(node.getProperty("fullName").toString() + "\t\n");
                    }

                }
                bw.close();
                fw.close();
                tx.success();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
*/


        int Methodcnt=0;
        int Classcnt=0;
        int Commitcnt=0;
        double commitperClass = 0;
        double classperCommit = 0;
        double issueperClass = 0;

        double classnoissue = 0;

        double classoneissue = 0;

        double classtwoissue = 0;

        double classthreeissue = 0;

        double classfourissue = 0;

        double classfiveissue = 0;

        double class6issue = 0;

        double class7issue = 0;

        double class8issue = 0;

        double class9issue = 0;

        double class10issue = 0;

        double classlarge10issue = 0;



        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> iterator = graphDb.getAllNodes().iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if(node.getLabels().iterator().next().name().equals("JiraIssue")){
                    int cnt=0;
                    Iterator<Relationship> relationIter = node.getRelationships().iterator();
                    while (relationIter.hasNext()) {
                        Relationship relation = relationIter.next();
                        Node otherNode = relation.getOtherNode(node);
                        if (otherNode.getLabels().iterator().next().name().equals("Class")) {
                            cnt++;
                        }
                    }

                    if(cnt==0){
                        classnoissue++;
                    }else if(cnt==1){
                        classoneissue++;
                    }else if(cnt==2){
                        classtwoissue++;
                    }else if(cnt==3){
                        classthreeissue++;
                    }else if(cnt==4){
                        classfourissue++;
                    }else if(cnt==5){
                        classfiveissue++;
                    }else if(cnt==6){
                        class6issue++;
                    }else if(cnt==7){
                        class7issue++;
                    }else if(cnt==8){
                        class8issue++;
                    }else if(cnt==9){
                        class9issue++;
                    }else if(cnt==10){
                        class10issue++;
                    }else {
                        classlarge10issue++;
                    }
                    Classcnt++;
                }
            }
            tx.success();
        }



   //     System.out.println(Methodcnt);
        System.out.println(Classcnt);
   /*     System.out.println(Commitcnt);
        System.out.println(commitperClass/Classcnt);
        System.out.println(classperCommit/Commitcnt);

        System.out.println(issueperClass/Classcnt);
*/

        System.out.println("0 :"+classnoissue);
        System.out.println("1 :"+classoneissue);
        System.out.println("2 :"+classtwoissue);
        System.out.println("3 :"+classthreeissue);
        System.out.println("4 :"+classfourissue);
        System.out.println("5 :"+classfiveissue);
        System.out.println("6 :"+class6issue);
        System.out.println("7 :"+class7issue);
        System.out.println("8 :"+class8issue);
        System.out.println("9 :"+class9issue);
        System.out.println("10 :"+class10issue);

        System.out.println(">10 :"+classlarge10issue);


    }
}
