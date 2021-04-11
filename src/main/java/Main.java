import Indexer.YELPIndexer;
import Indexer.YELPReviewIndexer;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.util.ArrayList;

public class Main {
    public static final String DATA_FILE="yelp/yelp_academic_dataset_user.json";
    public static final String REVIEW_DATA_FILE="yelp/yelp_academic_dataset_review.json";
    public static final String INDEX_PATH="yelp/luceneIndex";
    public static final String COS_INDEX_PATH="yelp/luceneReviewCosIndex";
    public static final String BM25_INDEX_PATH="yelp/luceneReviewBM25Index";


    public static void main (String [] arg) throws Exception {
        boolean preformIndexForUser=false;
        boolean preformIndexForReview=false;
        // To perform indexing. If there i no change to the data file, index only need to be created once

        if(preformIndexForUser){
           YELPIndexer indexer = new YELPIndexer(INDEX_PATH, new ClassicSimilarity());
           indexer.indexYelps(DATA_FILE);
        }

        if(preformIndexForReview){
            YELPReviewIndexer classicIndexer = new YELPReviewIndexer(COS_INDEX_PATH, new ClassicSimilarity());
            classicIndexer.indexYelps(REVIEW_DATA_FILE);

            YELPReviewIndexer indexer = new YELPReviewIndexer(BM25_INDEX_PATH, new BM25Similarity());
            indexer.indexYelps(REVIEW_DATA_FILE);
        }


        //search index
        YELPSearcher  searcherQ1=new YELPSearcher(INDEX_PATH);

        // Question 1
        ScoreDoc[] hits=searcherQ1.search("friends", "wXyx23jwrL-O2kvw8hrA7g", 20);
        System.out.println("\n=================Results for friends search=============\n");
        ArrayList<String> result1 = new ArrayList<String>();
        result1.add("name");
        result1.add("friends");
        searcherQ1.printResult(hits, result1);




        //search index
        YELPSearcher  searcherQ2a=new YELPSearcher(COS_INDEX_PATH);

        // Question 2
        ScoreDoc[] hitsQ2=searcherQ2a.searchPhraseQuery("business_id: lPkRneUrVwfJotHOVry36g AND review: nice", 20);
        System.out.println("\n=================Results for Cosine review search=============\n");
        ArrayList<String> result2 = new ArrayList<String>();
        result2.add("user_id");
        result2.add("business_id");
        result2.add("review");

        searcherQ2a.printResult(hitsQ2,result2);


        //search index
        YELPSearcher  searcherQ2b=new YELPSearcher(BM25_INDEX_PATH);

        // Question 2
        ScoreDoc[] hitsQ2b=searcherQ2b.searchPhraseQuery("business_id: lPkRneUrVwfJotHOVry36g AND review: nice", 20);
        System.out.println("\n=================Results for BM25 review search=============\n");
        searcherQ2b.printResult(hitsQ2b,result2);


    }
    }

