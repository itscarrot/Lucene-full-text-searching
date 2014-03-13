package myLucene;

import java.awt.TextField;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.apache.lucene.index.DocsAndPositionsEnum;
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

public class ShowPostion {
	static Map<String, Float> sortResult = new HashMap<String, Float>();
	static ArrayList arrayList = new ArrayList();
	static Map<String, String> phrasePosition = new HashMap<String, String>();
	
	public static void main(String[] args) throws IOException, ParseException {
		// preparing to index
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_46); 
		Directory directory = new RAMDirectory(); // store the index in mainmemory
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
				analyzer);
		IndexWriter writer = new IndexWriter(directory, config);

		FieldType ft = new FieldType();
		ft.setTokenized(true);
		ft.setIndexed(true);
		// ft.setStored(true);
		ft.setStoreTermVectors(true);
		ft.setStoreTermVectorOffsets(true);
		ft.setStoreTermVectorPositions(true);
		ft.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        //readFile,adding data into field
        readFile(writer,ft);

		// done with indexing
		writer.close();
		// prepare for retrieving...
		IndexReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);
		
	    String Term1 = "lymphoid";
	    String Term2 ="cells" ;
	    Term1 = TokenTerm(Term1,analyzer);
	    Term2 = TokenTerm(Term2,analyzer);
		// building a PhraseQuery
		PhraseQuery query = new PhraseQuery();

		query.add(new Term("content", Term1));
		query.add(new Term("content", Term2));
		TopDocs hits = searcher.search(query, reader.maxDoc()); // similarity is based on
		System.out.println("List the beginning positions of phrase");										
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			int docid = scoreDoc.doc;
			String DocID = doc.get("ID");
		    arrayList.add(Integer.parseInt(DocID.substring(3))); 
		    //get the postion of phrase
			getpostion(reader, docid,DocID);		
			
		}

	    //sort according the DocID
	    Collections.sort(arrayList);
	    
	   for(int i=0;i<arrayList.size(); i++){
		   System.out.println(".I "+arrayList.get(i)+ ": " +phrasePosition.get(".I "+arrayList.get(i)));
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
    
	public static void getpostion(IndexReader reader, int docid,String DocID)
			throws IOException {
		int freq_1;
		int freq_2;
		int []lymphoid = null;
		int []cell = null;
		Terms terms = reader.getTermVector(docid, "content");
		TermsEnum termsEnum = terms.iterator(null);
		BytesRef term;
		while ((term = termsEnum.next()) != null) {
			String docTerm = term.utf8ToString();
			// retrieve all positions
			if (docTerm.equals("lymphoid")) {
				DocsAndPositionsEnum docPosEnum = termsEnum.docsAndPositions(
						null, null, DocsAndPositionsEnum.FLAG_OFFSETS);
				docPosEnum.nextDoc();
				// Retrieve the term frequency in the current document
			   freq_1 = docPosEnum.freq();
			   lymphoid = new int[freq_1]; 
				for (int i = 0; i < freq_1; i++) {
					lymphoid [i]= docPosEnum.nextPosition();
				}
				
			}
			if (docTerm.equals("cell")) {
				DocsAndPositionsEnum docPosEnum = termsEnum.docsAndPositions(
						null, null, DocsAndPositionsEnum.FLAG_OFFSETS);
				docPosEnum.nextDoc();
				// Retrieve the term frequency in the current document
			    freq_2 = docPosEnum.freq();
			    cell = new int[freq_2];
				for (int i = 0; i < freq_2; i++) {
					cell[i] = docPosEnum.nextPosition();
				}
				
			}
		}
		//process the phrase position after getting the position of botj
		processPhrasePostion(lymphoid,cell,DocID);
	}
	
	public static void processPhrasePostion(int []lymphoid,int []cell,String DocID){
		String postionStr="";
		for(int i=0;i<lymphoid.length;i++){
			for(int j=0;j<cell.length;j++){
				if(cell[j]-lymphoid[i]==1){			
					postionStr+=lymphoid[i]+1+",";	
				}
					
				}
			}
		//System.out.print(postionStr);
		phrasePosition.put(DocID, postionStr.substring(0, postionStr.length()-1));
		
	}

}
