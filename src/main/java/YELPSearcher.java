import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    public ScoreDoc[] searchBoostedPhraseQuery(String phrase, int numHits, float boostAmount ) {
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
    public ScoreDoc[] searchBoostedPhraseQueryWithHighlighter(String phrase, int numHits, float boostAmount ) {
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new QueryParser(phrase, analyzer);
        ScoreDoc[] hits = null;
        try {
            //Create a TopScoreDocCollector
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits);

            //search index
            BoostQuery query = new BoostQuery(queryParser.parse(phrase), boostAmount);
            Formatter formatter = new SimpleHTMLFormatter();
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            Fragmenter fragmenter = new SimpleFragmenter();
            highlighter.setTextFragmenter(fragmenter);
            lSearcher.search(query, collector);

            //collect results
            hits = collector.topDocs().scoreDocs;

            for (int i = 0; i < hits.length; i++)
            {
                int docid = hits[i].doc;
                Document doc = lSearcher.doc(docid);

                //Get stored text from found document
                String text = doc.get("review");

                //Create token stream
                TokenStream stream = TokenSources.getTokenStream(lReader, docid, "review", analyzer);

                //Get highlighted text fragments
                String[] frags = highlighter.getBestFragments(stream, text, 10);
                for (String frag : frags)
                {
                    System.out.println("=======================");
                    System.out.println(frag);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }



    public static void main(String[] arg) throws Exception {
    Summarizer summarizer = new Summarizer();
    String tosummarize = "Very <B>nice</B> place and good <B>food</B> in the chinatown of Vancouver.\n" +
            "Typical chinese <B>food</B> with a modern touch.\n" +
            "<B>Nice</B> music and <B>nice</B> place.\n" +
            "Will go back.";

        System.out.println(summarizer.Summarize(tosummarize,20));
    }


    //search for keywords in specified field, with the number of top results
    public ScoreDoc[] searchLocationQueryWithDistance(double lat, double longt, int milimeter, int numHits) {
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


    //Search for nearest resteraunt given location
    public ScoreDoc[] searchNearestBusiness(double lat, double longt, int numHits) {


        ScoreDoc[] hits = null;
        try {
            TopDocs docs =LatLonPoint.nearest(lSearcher,"geo_point", lat,longt, numHits);
            hits =  docs.scoreDocs;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }

    //search for resteraunt nearby in a category

    public ScoreDoc[] searchNearestBusinessInACateory(double lat, double longt, String cat, int milimeter, int numHits) {
        ScoreDoc[] hits = null;
        try {
            Query locationQuery =LatLonPoint.newDistanceQuery("geo_point", lat,longt, milimeter);
            QueryBuilder queryBuilder = new QueryBuilder(new StandardAnalyzer());
            Query category = queryBuilder.createBooleanQuery("category",cat);
            Query isOpen = queryBuilder.createBooleanQuery("isOpen","1");

            BooleanQuery.Builder combinedQuery = new BooleanQuery.Builder();
            combinedQuery.add(category, BooleanClause.Occur.SHOULD);
            combinedQuery.add(locationQuery, BooleanClause.Occur.SHOULD);
            combinedQuery.add(isOpen, BooleanClause.Occur.SHOULD);
            combinedQuery.setMinimumNumberShouldMatch(1);
            BooleanQuery query = combinedQuery.build();
            TopDocs docs = lSearcher.search(query, numHits);
            hits =  docs.scoreDocs;

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

    public String generateReviewSummary(ScoreDoc[] docs, int lineToKeep) throws IOException {

        String rawSummary = " ";
        for (ScoreDoc hit : docs) {
            rawSummary = String.join(rawSummary," ", lReader.document(hit.doc).get("review"));

        }

        Summarizer summarizer = new Summarizer();
        String resultString =  summarizer.Summarize( rawSummary,lineToKeep);
      //  System.out.println("Raw review \n " + rawSummary);
        return resultString;
    }
}
