package Indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class YELPLocationIndexer {
    private IndexWriter writer = null;
    private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public YELPLocationIndexer(String dir) throws IOException {
        //specify the directory to store the Lucene index
        Directory indexDir = FSDirectory.open(Paths.get(dir));
        //specify the analyzer used in indexing
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(indexDir,cfg);
    }

    public void indexYelpsResterauntLocation(String fileName) throws Exception {

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


                String businessId = jObj.getString("business_id");
                String name = jObj.getString("name");
                double latitude = Double.parseDouble(jObj.getString("latitude"));
                double longitude = Double.parseDouble(jObj.getString("longitude"));
                int isOpen = Integer.parseInt(jObj.getString("is_open"));
                String category = jObj.getString("categories");

                //create a document for each JSON record
                Document doc=getDocument(latitude,longitude,businessId, name,category, isOpen);

                //index the document
                writer.addDocument(doc);

                lineNumber++;

            } catch (Exception e) {
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

    protected Document getDocument(double latitude, double longitude, String businessid, String name, String category, int isOpen) {
        Document doc = new Document();

        FieldType ft = new FieldType(TextField.TYPE_STORED);
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        //ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        ft.setStoreTermVectors(true);
        doc.add(new StoredField("business_id", businessid, ft));
        doc.add(new StoredField("name",name));
        doc.add(new TextField("category", category, Field.Store.YES));
        doc.add(new StoredField("isOpen", isOpen));
        doc.add(new LatLonPoint("geo_point", latitude,longitude));

        return doc;
    }
}
