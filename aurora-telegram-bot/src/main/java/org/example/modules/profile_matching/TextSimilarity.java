package org.example.modules.profile_matching;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.example.models.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TextSimilarity {

    private static final Logger logger = LoggerFactory.getLogger(TextSimilarity.class);

    public static List<SimilarityPair> processUserInfos(UserInfo[] userInfos) throws IOException, ParseException {
        logger.info("Starting processUserInfos with {} users", userInfos.length);

        try (Directory directory = new RAMDirectory(); Analyzer analyzer = new StandardAnalyzer()) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                indexDocuments(userInfos, writer);
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                List<SimilarityPair> similarities = findSimilarities(userInfos, analyzer, reader);
                logger.info("Found {} similarity pairs", similarities.size());
                return similarities;
            }
        }
    }

    private static void indexDocuments(UserInfo[] userInfos, IndexWriter writer) throws IOException {
        logger.info("Indexing documents...");
        for (UserInfo userInfo : userInfos) {
            Document doc = new Document();
            doc.add(new TextField("content", userInfo.getDiscussionTopic(), Field.Store.YES));
            writer.addDocument(doc);
            logger.debug("Indexed document for user: {}", userInfo.getUserId());
        }
        logger.info("Indexing completed.");
    }

    private static List<SimilarityPair> findSimilarities(UserInfo[] userInfos, Analyzer analyzer, IndexReader reader) throws IOException, ParseException {
        IndexSearcher searcher = new IndexSearcher(reader);
        List<SimilarityPair> similarityPairs = new ArrayList<>();

        logger.info("Finding similarities...");
        for (int i = 0; i < userInfos.length; i++) {
            Document doc = reader.document(i);
            String content = doc.get("content");
            Query query = new QueryParser("content", analyzer).parse(QueryParser.escape(content));
            TopDocs results = searcher.search(query, userInfos.length);

            for (ScoreDoc scoreDoc : results.scoreDocs) {
                if (scoreDoc.doc != i) {
                    similarityPairs.add(new SimilarityPair(userInfos[i].getUserId(), userInfos[scoreDoc.doc].getUserId(), scoreDoc.score));
                    logger.debug("Similarity found: {} <-> {} with score {}", userInfos[i].getUserId(), userInfos[scoreDoc.doc].getUserId(), scoreDoc.score);
                }
            }
        }

        similarityPairs.sort(Comparator.comparingDouble(SimilarityPair::score).reversed());
        logger.info("Similarity finding completed.");
        return similarityPairs;
    }

    public record SimilarityPair(Long userId1, Long userId2, float score) {
    }
}
