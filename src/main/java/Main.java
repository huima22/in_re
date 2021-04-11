import Indexer.YELPIndexer;
import Indexer.YELPReviewIndexer;
import org.apache.lucene.benchmark.quality.trec.TrecJudge;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.benchmark.quality.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;

import static jdk.nashorn.internal.objects.Global.print;

public class Main {
    public static final String DATA_FILE = "yelp/yelp_academic_dataset_user.json";
    public static final String REVIEW_DATA_FILE = "yelp/yelp_academic_dataset_review.json";
    public static final String INDEX_PATH = "yelp/luceneIndex";
    public static final String COS_INDEX_PATH = "yelp/luceneReviewCosIndex";
    public static final String BM25_INDEX_PATH = "yelp/luceneReviewBM25Index";


    public static void main(String[] arg) throws Exception {
        boolean preformIndexForUser = false; //set to true if indexing for the first time. true;
        boolean preformIndexForReview = false; //set to true if indexing for the first time. true;
        // To perform indexing. If there is no change to the data file, index only need to be created once

        if (preformIndexForUser) {
            YELPIndexer indexer = new YELPIndexer(INDEX_PATH, new ClassicSimilarity());
            indexer.indexYelps(DATA_FILE);
        }

        if (preformIndexForReview) {
            YELPReviewIndexer classicIndexer = new YELPReviewIndexer(COS_INDEX_PATH, new ClassicSimilarity());
            classicIndexer.indexYelps(REVIEW_DATA_FILE);

            YELPReviewIndexer indexer = new YELPReviewIndexer(BM25_INDEX_PATH, new BM25Similarity());
            indexer.indexYelps(REVIEW_DATA_FILE);
        }

        //search index
        YELPSearcher searcherQ1 = new YELPSearcher(INDEX_PATH);

        // Question 1
        ScoreDoc[] hits = searcherQ1.search("friends", "wXyx23jwrL-O2kvw8hrA7g", 20);
        System.out.println("\n=================Results for friends search=============\n");
        ArrayList<String> result1 = new ArrayList<String>();
        result1.add("name");
        result1.add("friends");
        searcherQ1.printResult(hits, result1);


        //search index
        YELPSearcher searcherQ2a = new YELPSearcher(COS_INDEX_PATH);

        // Question 2
        ScoreDoc[] hitsQ2 = searcherQ2a.searchPhraseQuery("review: food AND review: nice", 20);
        System.out.println("\n=================Results for Cosine review search=============\n");
        ArrayList<String> result2 = new ArrayList<String>();
        result2.add("user_id");
        result2.add("business_id");
        result2.add("review");

        searcherQ2a.printResult(hitsQ2, result2);

        //search index
        YELPSearcher searcherQ2b = new YELPSearcher(BM25_INDEX_PATH);

        // Question 2
        ScoreDoc[] hitsQ2b = searcherQ2b.searchPhraseQuery("business_id: lPkRneUrVwfJotHOVry36g AND review: nice", 20);
        System.out.println("\n=================Results for BM25 review search=============\n");
        searcherQ2b.printResult(hitsQ2b, result2);

        // -- Create 20 queries, and retrieve top 10 results. You should use two retrieval models, and evaluation
        //their performance. You need to design the experiments.

        // Evaluation - WIP
        File qrelsFile = new File("yelp/qrels.txt");
        File queriesFile = new File("yelp/queries.txt");
        String docNameField = "filename";
        PrintWriter logger = new PrintWriter(System.out, true);
        TrecTopicsReader qReader = new TrecTopicsReader();
        Directory dir = FSDirectory.open(Paths.get(COS_INDEX_PATH));
        IndexSearcher searcher = new IndexSearcher(searcherQ2a.getReader());

        QualityQuery[] qqs = qReader.readQueries(new BufferedReader(new FileReader(queriesFile)));
        Judge judge = new TrecJudge(new BufferedReader(new FileReader(qrelsFile)));
        boolean judgeValidation = judge.validateData(qqs, logger);
        System.out.println("Validating data:" + judgeValidation);
        QualityQueryParser qqParser = new SimpleQQParser("title", "review");
        QualityBenchmark qrun = new QualityBenchmark(qqs, qqParser, searcher, docNameField);
        SubmissionReport submitLog = null;
        QualityStats[] stats = qrun.execute(judge, submitLog, logger);

        QualityStats avg = QualityStats.average(stats);
        avg.log("SUMMARY", 2, logger, "  ");
        dir.close();

        // Question 3 - Query Level Boosting
        //TODO: Document level boosting - while indexing - by calling document.setBoost() before a document is added to the index.
        //TODO: Document's Field level boosting - while indexing - by calling field.setBoost() before adding a field to the document (and before adding the document to the index).

        // Boosting for regular query
        ScoreDoc[] hitsQ3 = searcherQ1.boostedSearch("friends", "wXyx23jwrL-O2kvw8hrA7g", 20, 5);
        System.out.println("\n=================Results for boosted friends search=============\n");
        ArrayList<String> result3 = new ArrayList<String>();
        result3.add("name");
        result3.add("friends");
        searcherQ1.printResult(hitsQ3, result3);

        // Boosting for phrase query
        ScoreDoc[] hitsQ3a = searcherQ2a.searchBoostedPhraseQuery("review: food AND review: nice", 20, 20);
        System.out.println("\n=================Results for boosted Cosine review search=============\n");
        ArrayList<String> result3a = new ArrayList<String>();
        result3a.add("user_id");
        result3a.add("business_id");
        result3a.add("review");
        searcherQ2a.printResult(hitsQ3a, result3a);

        //Question 4

    }
}

