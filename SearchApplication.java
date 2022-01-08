/**
Copyright [2022] [Victor Bialek]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 **/


import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SearchApplication {
    private final static String indexPath = "/home/hb/IdeaProjects/lucene-core/index";
    private final static String pathOfFiles = "/home/hb/Documents/books/";

    public static List<Document> searchDocuments(String queryString) {
        QueryParser parser = new QueryParser("searchTitle", new StandardAnalyzer());
        try {
            List<Document> result = new ArrayList<>();

            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = parser.parse(queryString);
            TopDocs topDocs = searcher.search(query, 10);
            var docs = topDocs.scoreDocs;
            for (var scoreDoc : docs) {
                Document doc = searcher.doc(scoreDoc.doc);
                if (doc != null)
                    result.add(doc);
            }
            reader.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static boolean indexDirectory() {
        File[] inputFiles = new File(pathOfFiles).listFiles();
        if (inputFiles == null) {
            System.out.println("This directory does not exist or does not contain files");
            System.out.println("Exiting index command");
            return false;
        }
        System.out.println("Indexing files ... ");
        try {
            Directory indexDirectory = FSDirectory.open(Paths.get(indexPath));
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(indexDirectory, indexWriterConfig);
            for (File inputFile : inputFiles) {
                String inputPath = inputFile.getAbsolutePath();
                if (!inputPath.endsWith("pdf"))
                    continue;
                // needs to be repeated for every file
                Document document = new Document();
                String fileName = inputPath.substring(pathOfFiles.length());
                System.out.print(fileName);

                // index pdf files
                document.add(new StringField("title", inputFile.getName(), Field.Store.YES));
                TokenStream stream = analyzer.tokenStream("", inputFile.getName());
                stream.reset();
                var termAttr = stream.addAttribute(CharTermAttribute.class);
                while (stream.incrementToken())
                    document.add(new StringField("searchTitle", termAttr.toString(), Field.Store.YES));

                stream.close();
                writer.addDocument(document);

                System.out.println(" ... done");
            }

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Give Lucilla documents to index: 1");
                System.out.println("Have Lucilla search documents for you: 2");
                System.out.println("Leave: 0");
                String command = scanner.next();
                if (command.equals("0")) {
                    System.out.println("Have a nice day!\n");
                    return;
                }
                if (command.equals("1")) {
                    System.out.println("Indexing directory now");
                    if (indexDirectory())
                        System.out.println("I managed to index everything! That was great, thank you!");
                    else {
                        System.out.println("Something went wrong... I couldn't find anything here... Sorry about that.");
                    }
                }
                if (command.equals("2")) {
                    System.out.println("Sure! What would you like to search for?:");
                    String queryString = scanner.next();
                    List<Document> documents = searchDocuments(queryString);
                    if (documents.isEmpty())
                        System.out.println("Sorry, but I couldn't find anything related to that");
                    else {
                        for (var document : documents)
                            System.out.println(document.get("title"));
                    }
                }
                System.out.println("Anything else, I can do for you?");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
