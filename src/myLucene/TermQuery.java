package myLucene;

import java.awt.TextField;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class TermQuery {

	public static void main(String[] args) throws IOException, ParseException {
		// preparing to index
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_46); 
																	// tokenization
		Directory directory = new RAMDirectory(); // store the index in main
													// memory
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
				analyzer);
		IndexWriter writer = new IndexWriter(directory, config);

		FieldType ft = new FieldType();
		ft.setTokenized(true);
		ft.setIndexed(true);
		ft.setStored(false);
		ft.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS);
		// readFile,adding data into field
		readFile(writer, ft);

		// done with indexing
		writer.close();
		// prepare for retrieving...
		IndexReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);

		QueryParser parser = new QueryParser(Version.LUCENE_46, "content",
				analyzer);
		Query query1 = parser.parse("lymphoid");
		Query query2 = parser.parse("cells");
		BooleanQuery query = new BooleanQuery();
		query.add(query1, Occur.MUST);
		query.add(query2, Occur.MUST);
		

		TopDocs hits = searcher.search(query, 5); // similarity is based on
													// Lucene conceptual scoring
		System.out.println("Document score:");
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			System.out.println(doc.get("ID") + ": " + scoreDoc.score);
		}

		reader.close();
		directory.close();

	}

	public static void readFile(IndexWriter writer, FieldType ft)
			throws IOException {

		FileReader fr = new FileReader("./all/MED.ALL");

		BufferedReader br = new BufferedReader(fr);

		String s = "";
		String docName = "";
		boolean flag = true;
		boolean isFirst = true;
		String page = "";

		do {
			s = br.readLine();
			if (s != null) {
				if (s.contains(".I ")) {
					flag = false;
					if (isFirst) {
						docName = s;
						continue;
					}
					// System.out.println(docName);
					// System.out.print(page);
					Document doc = new Document();
					doc.add(new Field("content", page, ft));
					doc.add(new StringField("ID", docName, Field.Store.YES));
					writer.addDocument(doc);
					page = "";
					docName = s;

				}
				// get .W do nothing, just continue
				else if (s.contains(".W")) {
					continue;
				}
				if (flag) {
					page = page + s + "\r\n";
				}
				flag = true;
			} else { // The last loop
				// System.out.println(docName);
				// System.out.print(page);
				Document doc = new Document();
				doc.add(new Field("content", page, ft));
				doc.add(new StringField("ID", docName, Field.Store.YES));
				writer.addDocument(doc);
			}
			isFirst = false;
		} while (s != null);

		br.close();
	}

	// Return the number of distinct terms in an index reader
	public static long getDistinctTerm(IndexReader reader, String searchfield)
			throws IOException {
		long numTerm = 0;

		Fields fields = MultiFields.getFields(reader);
		for (String field : fields) {
			if (field.equals(searchfield)) {
				Terms terms = fields.terms(searchfield);
				TermsEnum termsEnum = terms.iterator(null);
				BytesRef text = null;
				while ((text = termsEnum.next()) != null) {
					numTerm++;
				}
			}
		}

		return numTerm;
	}

}
