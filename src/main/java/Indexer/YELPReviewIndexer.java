package Indexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Field.Store;
import org.json.JSONObject;

public class YELPReviewIndexer {
    private IndexWriter writer = null;

    public static final String FIELD_NAME = "filename";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public YELPReviewIndexer(String dir, Similarity similarity) throws IOException {
        //specify the directory to store the Lucene index
        Directory indexDir = FSDirectory.open(Paths.get(dir));

        //specify the analyzer used in indexing
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(OpenMode.CREATE);
        cfg.setSimilarity(similarity);

        //create the IndexWriter
        writer = new IndexWriter(indexDir, cfg);
    }


    public void indexYelps(String fileName) throws Exception {

        System.out.println("Start indexing "+fileName+" "+sdf.format(new Date()));

        //read a JSON file
        Scanner in = new Scanner(new File(fileName));
        int lineNumber = 1;
        String jLine = "";

        while (in.hasNextLine()) {
            try {
                jLine = in.nextLine().trim();
                //parse the JSON file and extract the values for "question" and "answer"
                JSONObject jObj = new JSONObject(jLine);

                String userId = jObj.getString("user_id");
                String business = jObj.getString("business_id");
                double ranking = Double.parseDouble(jObj.getString("stars"));
                String review = jObj.getString("text");

                //create a document for each JSON record
                Document doc=getDocument(fileName, userId, business,ranking, review);
                //index the document
                writer.addDocument(doc);

                lineNumber++;

            } catch (Exception e) {
                System.out.println("Error at: " + lineNumber + "\t" + jLine);
                e.printStackTrace();
            }
        }
        //close the file reader
        in.close();
        System.out.println("Index completed at " + sdf.format(new Date()));
        System.out.println("Total number of documents indexed: " + writer.maxDoc());

        //close the index writer.
        writer.close();

    }


    //specify what is a document, and how its fields are indexed
    protected Document getDocument(String fileName, String userId, String business, double star, String review) throws Exception {
        Document doc = new Document();

        FieldType ft = new FieldType(TextField.TYPE_STORED);
        //ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        ft.setStoreTermVectors(true);


        doc.add(new StringField("path", fileName, Field.Store.YES));
        doc.add(new Field("user_id", userId, ft));
        doc.add(new Field("business_id", business, ft));
        doc.add(new DoubleDocValuesField("star",star));
        doc.add(new TextField("review", review , Store.YES));
        doc.add(new TextField(FIELD_NAME, fileName, Store.YES));

        return doc;
    }
}
