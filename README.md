# Yelp's Information Retrievel
This is a search engine about the Yelp dataset that can be downloaded from https://www.yelp.com/dataset/

## Prerequisites

### Basic steps:
1. Create a `yelp` folder with the unzipped json files from the downloaded dataset and place them there
2. Build maven project with a desired IDE like IntelliJ
	
## About Lucene
Apache Lucene™ is a high-performance, full-featured text search engine library written entirely in Java. It is a technology suitable for nearly any application that requires full-text search, especially cross-platform.

### Objectives
1. Lucene index was created that takes in a query (including field information) from the user and returns a list of top 20 documents (for a ranking query). 
	The index includes fields from the data supporting both Boolean query, and ranking query
2. 20 sample queries are created with top 10 results returned under 2 retrival models (TF-IDF and BM25)
3. Accuracy is improved by Lucene Scoring
4. Reuslt summaries implemented; Extractive and Abstractive
5. Geospatial/Location and keyword queries are supported


## Documentation and References

https://lucene.apache.org/core/7_1_0/index.html

https://www.manning.com/books/lucene-in-action-second-edition

https://github.com/apache/lucene
