import Indexer.YELPIndexer;
import Indexer.YELPLocationIndexer;
import Indexer.YELPReviewIndexer;
import org.apache.lucene.benchmark.quality.*;
import org.apache.lucene.benchmark.quality.trec.TrecJudge;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    public static final String DATA_FILE = "yelp/yelp_academic_dataset_user.json";
    public static final String REVIEW_DATA_FILE = "yelp/yelp_academic_dataset_review.json";
    public static final String LOCATION_DATA_FILE = "yelp/yelp_academic_dataset_business.json";

    public static final String INDEX_PATH = "index/luceneIndex";
    public static final String TFIDF_INDEX_PATH = "index/luceneReviewCosIndex";
    public static final String BM25_INDEX_PATH = "index/luceneReviewBM25Index";
    public static final String LOCATION_INDEX_PATH = "index/luceneLocationIndex";

    public static void main(String[] arg) throws Exception {
        boolean performIndexForUser = false; //set to true if indexing for the first time. true;
        boolean performIndexForReview = false; //set to true if indexing for the first time. true;
        boolean performIndexForLocation = false; //set to true if indexing for the first time. true;
        // To perform indexing. If there is no change to the data file, index only need to be created once

        if (performIndexForUser) {
            YELPIndexer indexer = new YELPIndexer(INDEX_PATH, new ClassicSimilarity());
            indexer.indexYelps(DATA_FILE);
        }

        if (performIndexForReview) {
            YELPReviewIndexer classicIndexer = new YELPReviewIndexer(TFIDF_INDEX_PATH, new ClassicSimilarity());
            classicIndexer.indexYelps(REVIEW_DATA_FILE);

            YELPReviewIndexer indexer = new YELPReviewIndexer(BM25_INDEX_PATH, new BM25Similarity());
            indexer.indexYelps(REVIEW_DATA_FILE);
        }


        if (performIndexForLocation) {
            YELPLocationIndexer locationIndexer = new YELPLocationIndexer(LOCATION_INDEX_PATH);
            locationIndexer.indexYelpsResterauntLocation(LOCATION_DATA_FILE);
        }

        //search index
        YELPSearcher searcherQ1 = new YELPSearcher(INDEX_PATH);

        printQuestionNumber(1);
        // Question 1 - top 20 documents
        ScoreDoc[] hits = searcherQ1.search("friends", "zSJC0xAwdkWXQw3XTUOYqQ", 20);
        System.out.println("\n=================Results for friends search=============\n");
        ArrayList<String> result1 = new ArrayList<String>();
        result1.add("name");
        result1.add("friends");
        searcherQ1.printResult(hits, result1);


        printQuestionNumber(2);
        // Question 2
        YELPSearcher searcherQ2a = new YELPSearcher(TFIDF_INDEX_PATH);
        YELPSearcher searcherQ2b = new YELPSearcher(BM25_INDEX_PATH);

        // -- Create 20 queries, and retrieve top 10 results. You should use two retrieval models, and evaluation
        //their performance. You need to design the experiments.

        String[] queries = {
                "review:good", //food reviews
                "review:bad", //bad reviews - checking to see if possibly have mixed results ('not bad' is considered)
                "review:food AND review:nice", //nice food reviews
                "review:food OR review:nice", //either is nice or has food in reviews
                "review:clean AND review:good", //clean and good reviews
                "review:clean OR review:good", //clean or good reviews
                "review:food AND review:good AND review:nice", //both good and nice with food reviews
                "review:clean AND review:good OR review:food",  //both good and clean with food reviews
                "review:clean AND review:good AND review:food", //clean and good food reviews
                "review:clean AND review:good AND NOT review:food", //non-food reviews that is clean and is good
                "review:clean OR review:good AND NOT review:food", //non-food reviews that either is clean or is good
                "review:clean OR review:chicken OR NOT review:bad", //non-bad reviews that either serves chicken or is clean
                "review:clean AND review:chicken AND NOT review:bad", //non-bad reviews that serves chicken and is clean
                "review:clean AND review:drink AND NOT review:disappointed", //non-disappointing reviews that serves drinks and is clean
                "review:Atlanta AND NOT review:bad", //non-bad reviews in atlanta
                "review:Atlanta OR review:Atlantic AND review:coffee", //coffee in atlanta/atlantic
                "review:Vietnamese OR review:bahn",  //vietnamese or bahn mi
                "review:Burlington AND review:nice", //nice reviews in burlington
                "review:Burlington AND review:Vietnamese", //vietnamese food in burlington
                "review:Burlington AND review:Vietnamese AND review:good AND review:chicken" //vietnamese food in burlington that serves chicken with good reviews
        };

        for (String it : queries) {
            System.out.printf("\nExecuting for Query: %s", it);
            ScoreDoc[] hitsQ2a = searcherQ2a.searchPhraseQuery(it, 10);
            System.out.println("\n=================Results for TFIDF review search=============\n");
            ArrayList<String> result2a = new ArrayList<String>();
            result2a.add("user_id");
            result2a.add("business_id");
            result2a.add("review");
            try {
                searcherQ2a.printResult(hitsQ2a, result2a);
            } catch (Exception e) {
                e.printStackTrace();
            }

            ScoreDoc[] hitsQ2b = searcherQ2b.searchPhraseQuery(it, 10);
            System.out.println("\n=================Results for BM25 review search=============\n");
            ArrayList<String> result2b = new ArrayList<String>();
            result2b.add("user_id");
            result2b.add("business_id");
            result2b.add("review");
            try {
                searcherQ2b.printResult(hitsQ2b, result2b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ;

        // Evaluation using Trec on documents
        // Recall at 1/2 of the documents that are relevant matches
        File qrelsFile = new File("trec/qrels.txt");
        File topicsFile = new File("trec/topics.txt");

        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get(TFIDF_INDEX_PATH)));
        IndexSearcher searcher = new IndexSearcher(ir);
        String docNameField = "filename";

        PrintWriter logger = new PrintWriter(System.out, true);

        TrecTopicsReader qReader = new TrecTopicsReader();

        QualityQuery[] qqs = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));

        Judge judge = new TrecJudge(new BufferedReader(new FileReader(qrelsFile)));

        boolean judgeValidation = judge.validateData(qqs, logger);
        System.out.println("Validating data:" + judgeValidation);

        QualityQueryParser qqParser = new SimpleQQParser("title", "review");

        System.out.println("Quality Query: " + qqParser.parse(qqs[0]).toString());

        QualityBenchmark qrun = new QualityBenchmark(qqs, qqParser, searcher, docNameField);

        SubmissionReport submitLog = null;

        QualityStats[] stats = qrun.execute(judge, submitLog, logger);

        QualityStats avg = QualityStats.average(stats);
        avg.log("SUMMARY", 2, logger, "  ");


        printQuestionNumber(3);
        // Question 3 - Query Level Boosting
        //TODO: Improvement - Document level boosting - while indexing - by calling document.setBoost() before a document is added to the index.
        //TODO: Improvement - Document's Field level boosting - while indexing - by calling field.setBoost() before adding a field to the document (and before adding the document to the index).

        // Boosting for regular query - sample
        ScoreDoc[] hitsQ3 = searcherQ1.boostedSearch("friends", "wXyx23jwrL-O2kvw8hrA7g", 10, 5);
        System.out.println("\n=================Results for boosted friends search=============\n");
        ArrayList<String> result3 = new ArrayList<String>();
        result3.add("name");
        result3.add("friends");
        searcherQ1.printResult(hitsQ3, result3);

        // Boosting for phrase query - COS_INDEX_PATH
        ScoreDoc[] hitsQ3a = searcherQ2a.searchBoostedPhraseQuery("review:food AND review:nice", 10, 20);
        System.out.println("\n=================Results for boosted TFIDF review search=============\n");
        ArrayList<String> result3a = new ArrayList<String>();
        result3a.add("user_id");
        result3a.add("business_id");
        result3a.add("review");
        searcherQ2a.printResult(hitsQ3a, result3a);

        // Boosting for phrase query - BM25_INDEX_PATH
        ScoreDoc[] hitsQ3b = searcherQ2b.searchBoostedPhraseQuery("review:food AND review:nice", 10, 20);
        System.out.println("\n=================Results for boosted TFIDF review search=============\n");
        ArrayList<String> result3b = new ArrayList<String>();
        result3b.add("user_id");
        result3b.add("business_id");
        result3b.add("review");
        searcherQ2a.printResult(hitsQ3b, result3b);


        printQuestionNumber(4);
        //Question 4 extractive summary for review
        System.out.println("\n=================Results for summary review search: Get the summary of a business=============\n");
        String businessId = "R1KeQwYWkHczmZjSbfY2XA";
        ScoreDoc[] hitsQ4 = searcherQ2a.search("business_id", businessId, 100);
        String review = searcherQ2a.generateReviewSummary(hitsQ4, 10);
        System.out.println(" for business " + businessId + " \n" + review);


        printQuestionNumber(5);
        // Q5 search a place in atlanta knowing atlanta is 33 and -84
        YELPSearcher searcherLocation = new YELPSearcher(LOCATION_INDEX_PATH);

        ScoreDoc[] docs = searcherLocation.searchLocationQueryWithDistance(33, -84, 300000, 5);
        ArrayList<String> result5 = new ArrayList<String>();
        result5.add("business_id");
        result5.add("name");
        result5.add("category");
        result5.add("isOpen");
        searcherLocation.printResult(docs, result5);

        ScoreDoc[] docsNear = searcherLocation.searchNearestBusiness(33, -84, 5);
        searcherLocation.printResult(docsNear, result5);

        ScoreDoc[] burgersInAtlanta = searcherLocation.searchNearestBusinessInACateory(33, -84, "Car Wash", 300000, 5);
        System.out.println();
        System.out.println("====find all open car wash near atlanta====");
        searcherLocation.printResult(burgersInAtlanta, result5);

    }

    public static void printQuestionNumber(int questionNumber) {
        System.out.printf("\n\n==============================\n\t\tQuestion %d\n==============================\n%n", questionNumber);
    }
}

