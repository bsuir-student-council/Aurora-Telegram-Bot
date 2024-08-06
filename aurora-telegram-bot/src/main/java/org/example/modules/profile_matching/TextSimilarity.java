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

import java.io.IOException;
import java.util.*;

public class TextSimilarity {

    public static List<SimilarityPair> processUserInfos(UserInfo[] userInfos) throws IOException, ParseException {
        Directory directory = new RAMDirectory();
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(directory, config);

        for (UserInfo userInfo : userInfos) {
            Document doc = new Document();
            doc.add(new TextField("content", userInfo.getDiscussionTopic(), Field.Store.YES));
            writer.addDocument(doc);
        }
        writer.close();

        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        List<SimilarityPair> similarityPairs = new ArrayList<>();
        for (int i = 0; i < userInfos.length; i++) {
            Document doc = reader.document(i);
            String content = doc.get("content");
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(QueryParser.escape(content));
            TopDocs results = searcher.search(query, userInfos.length);

            for (ScoreDoc scoreDoc : results.scoreDocs) {
                if (scoreDoc.doc != i) {
                    similarityPairs.add(new SimilarityPair(userInfos[i].getUserId(), userInfos[scoreDoc.doc].getUserId(), scoreDoc.score));
                }
            }
        }

        similarityPairs.sort((p1, p2) -> Float.compare(p2.score, p1.score));

        reader.close();
        directory.close();

        return similarityPairs;
    }

    public record SimilarityPair(Long userId1, Long userId2, float score) {
    }
}
