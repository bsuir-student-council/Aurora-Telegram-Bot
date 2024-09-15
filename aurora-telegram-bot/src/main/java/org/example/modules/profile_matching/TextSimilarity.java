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
import java.util.*;

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

                // Post-process to pair users without a match
                List<SimilarityPair> finalPairs = postProcessPairs(userInfos, similarities);
                logger.info("Final number of pairs: {}", finalPairs.size());
                return finalPairs;
            }
        }
    }

    private static void indexDocuments(UserInfo[] userInfos, IndexWriter writer) throws IOException {
        logger.info("Indexing documents...");
        for (UserInfo userInfo : userInfos) {
            Document doc = new Document();
            String content =
                    "Интересы: " + userInfo.getDiscussionTopic() + "\n" +
                    "Фан-факт: " + userInfo.getFunFact();
            doc.add(new TextField("content", content, Field.Store.YES));
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

    private static List<SimilarityPair> postProcessPairs(UserInfo[] userInfos, List<SimilarityPair> similarityPairs) {
        Set<Long> pairedUsers = new HashSet<>();
        List<SimilarityPair> finalPairs = new ArrayList<>(similarityPairs);

        for (SimilarityPair pair : similarityPairs) {
            pairedUsers.add(pair.userId1());
            pairedUsers.add(pair.userId2());
        }

        List<UserInfo> unpairedUsers = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            if (!pairedUsers.contains(userInfo.getUserId())) {
                unpairedUsers.add(userInfo);
            }
        }

        Collections.shuffle(unpairedUsers);
        for (int i = 0; i < unpairedUsers.size() - 1; i += 2) {
            finalPairs.add(new SimilarityPair(unpairedUsers.get(i).getUserId(), unpairedUsers.get(i + 1).getUserId(), 0.0f));
            logger.debug("Randomly paired unpaired users: {} <-> {}", unpairedUsers.get(i).getUserId(), unpairedUsers.get(i + 1).getUserId());
        }

        // If there's an odd user out, they remain unpaired
        if (unpairedUsers.size() % 2 != 0) {
            logger.warn("User {} remains unpaired.", unpairedUsers.get(unpairedUsers.size() - 1).getUserId());
        }

        return finalPairs;
    }

    public record SimilarityPair(Long userId1, Long userId2, float score) {
    }
}
