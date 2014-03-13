package myLucene;


import java.awt.TextField;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

public class PharseQuery {
	static Map<String, Float> sortResult = new HashMap<String, Float>();
	static ArrayList arrayList = new ArrayList();

	public static void main(String[] args) throws IOException, ParseException {
		// preparing to index
		Analyzer analyzer =  new EnglishAnalyzer(Version.LUCENE_46); 
		Directory directory = new RAMDirectory(); // store the index in main memory
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,analyzer);
		IndexWriter writer = new IndexWriter(directory, config);
		
		FieldType ft=new FieldType();
		ft.setTokenized(true);
		
		ft.setIndexed(true); 
		ft.setStored(false);
	    ft.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        //readFile,adding data into field
        readFile(writer,ft);
	 	
		// done with indexing
	 	writer.close();
	    //prepare for retrieving...
	    IndexReader reader = DirectoryReader.open(directory);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    String Term1 = "lymphoid";
	    String Term2 ="cells" ;
	    Term1 = TokenTerm(Term1,analyzer);
	    Term2 = TokenTerm(Term2,analyzer);
	   
	    
	   // QueryParser parser = new QueryParser(Version.LUCENE_46, "content", analyzer);
	    PhraseQuery query = new PhraseQuery();
	    query.add(new Term("content",Term1),0);
	    query.add(new Term("content",Term2),1);
	   
	    TopDocs hits = searcher.search(query,reader.maxDoc()); // similarity is based on Lucene conceptual scoring
	    System.out.println("Document score:");
	
	    for(ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			String DocID = doc.get("ID");
			sortResult.put(DocID, scoreDoc.score);
		    arrayList.add(Integer.parseInt(DocID.substring(3)));  
		}
	   
	    //sort according the DocID
	    Collections.sort(arrayList);
	    
	   for(int i=0;i<arrayList.size(); i++){
		   
		   System.out.println(".I "+arrayList.get(i)+ ": " + sortResult.get(".I "+arrayList.get(i)) );
        }
	    analyzer.close();
	    reader.close();
	    directory.close();
	}
	public static String TokenTerm(String term,Analyzer a) {
		String tokenStr="";
		try {
			
			TokenStream stream = a.tokenStream("content",new StringReader(term));
		    stream.reset();
			CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class);
			while(stream.incrementToken()) {
				tokenStr=cta.toString();				
			}
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tokenStr;
		
	}
	
	   public static void readFile(IndexWriter writer, FieldType ft) throws IOException{
	    
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
					

}		
