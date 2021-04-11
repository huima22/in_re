import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class YELPSearcher {
    private IndexSearcher lSearcher;
    private IndexReader lReader;

    public YELPSearcher(String dir) {
        try {
            //create an index reader and index searcher
            lReader = DirectoryReader.open(FSDirectory.open(Paths.get(dir)));
            lSearcher = new IndexSearcher(lReader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IndexSearcher getSearcher() {
        return lSearcher;
    }

    public IndexReader getReader() {
        return lReader;
    }

    //report the number of documents indexed
    public int getCollectionSize() {
        return this.lReader.numDocs();
    }

    //search for keywords in specified field, with the number of top results
    public ScoreDoc[] search(String field, String keywords, int numHits) {

        //the query has to be analyzed the same way as the documents being index
        //using the same Analyzer
        QueryBuilder builder = new QueryBuilder(new StandardAnalyzer());
        Query query = builder.createBooleanQuery(field, keywords);
        ScoreDoc[] hits = null;
        try {
            //Create a TopScoreDocCollector
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits);

            //search index
            lSearcher.search(query, collector);

            //collect results
            hits = collector.topDocs().scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }


    public ScoreDoc[] boostedSearch(String field, String keywords, int numHits, float boostAmount) {

        //the query has to be analyzed the same way as the documents being index
        //using the same Analyzer
        QueryBuilder builder = new QueryBuilder(new StandardAnalyzer());
        BoostQuery query = new BoostQuery(builder.createBooleanQuery(field, keywords), boostAmount);
        ScoreDoc[] hits = null;
        try {
            //Create a TopScoreDocCollector
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits);

            //search index
            lSearcher.search(query, collector);

            //collect results
            hits = collector.topDocs().scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    //search for keywords in specified field, with the number of top results
    public ScoreDoc[] searchPhraseQuery(String phrase, int numHits) {

    /*        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                new string[] {"bodytext", "title"},
                analyzer);*/
        QueryParser queryParser = new QueryParser(phrase, new StandardAnalyzer());
        ScoreDoc[] hits = null;
        try {
            //Create a TopScoreDocCollector
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits);

            //search index
            lSearcher.search(queryParser.parse(phrase), collector);

            //collect results
            hits = collector.topDocs().scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }


    //search for keywords in specified field, with the number of top results
    public ScoreDoc[] searchBoostedPhraseQuery(String phrase, int numHits, float boostAmount) {
        QueryParser queryParser = new QueryParser(phrase, new StandardAnalyzer());
        ScoreDoc[] hits = null;
        try {
            //Create a TopScoreDocCollector
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits);

            //search index
            BoostQuery query = new BoostQuery(queryParser.parse(phrase), boostAmount);
            lSearcher.search(query, collector);

            //collect results
            hits = collector.topDocs().scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }


    //search for keywords in specified field, with the number of top results
    public ScoreDoc[] searchLocationQuery(double lat, double longt, int milimeter,int numHits) {


        ScoreDoc[] hits = null;
        try {

        TopDocs docs = lSearcher.search(LatLonPoint.newDistanceQuery("geo_point", lat,longt, milimeter), numHits);
        hits =  docs.scoreDocs;
       // printResult(docs.scoreDocs, Arrays.asList("geo_point"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }



    //present the search results
    public void printResult(ScoreDoc[] hits, List<String> fieldName) throws Exception {
        int i = 1;
        for (ScoreDoc hit : hits) {
            System.out.println("\nResult " + i + "\tDocID: " + hit.doc + "\t Score: " + hit.score);
            try {
               for(String s : fieldName){
                   System.out.println(s + ": " + lReader.document(hit.doc).get(s));
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

         /*   if (i == 1) {
                Terms terms = getTermVector(hit.doc, "friends");
                System.out.println("doc: " + hit.doc);

                TermsEnum iterator = terms.iterator();
                BytesRef term = null;
                System.out.print("List of Terms: ");
                while ((term = iterator.next()) != null) {
                    String termText = term.utf8ToString();
                    long termFreq = iterator.totalTermFreq(); // term freq in doc with docID
                    System.out.print(termText + ":" + termFreq + "\t");
                }
                System.out.println();
            }*/
            i++;

        }
    }
        //get term vector
        public Terms getTermVector (int docID, String field) throws Exception {
            return lReader.getTermVector(docID, field);
        }

        public void close() {
            try {
                if (lReader != null) {
                    lReader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
