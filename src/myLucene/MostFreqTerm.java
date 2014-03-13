package myLucene;
import java.awt.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;


public class MostFreqTerm {
	static Map<String, Integer> freq = new HashMap<String, Integer>();
	public static void main(String[] args) throws IOException, ParseException {
		// preparing to index
		Analyzer analyzer =  new EnglishAnalyzer(Version.LUCENE_46);
		Directory directory = new RAMDirectory(); 
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,analyzer);
		IndexWriter writer = new IndexWriter(directory, config);
		
		FieldType ft=new FieldType();
		ft.setTokenized(true);
		ft.setIndexed(true);
		ft.setStored(false);
		ft.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS);
        //readFile,adding data into field
        readFile(writer,ft);
	 	
		// done with indexing
	 	writer.close();
		//System.out.println("Finishing indexing " + id + " documents.");
		    
	    
	    //prepare for retrieving...
	    IndexReader reader = DirectoryReader.open(directory);
	    IndexSearcher searcher = new IndexSearcher(reader);
        //get frequence of all term
	    getFreq(reader, "content");
	    //get the two most frequent terms through sorting
        mostFreqTerm();
        
	    reader.close();
	    directory.close();
	    
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
	    	
	public static  void getFreq(IndexReader reader, String searchfield) throws IOException { 
		Fields fields=MultiFields.getFields(reader);
		Bits livedocs=MultiFields.getLiveDocs(reader);
		for(String field:fields){
			if(field.equals(searchfield)){
				Terms terms = fields.terms(searchfield);
				TermsEnum termsEnum = terms.iterator(null);
				BytesRef text=null;
				while((text=termsEnum.next())!=null){
					String term=text.utf8ToString();
					int docfreq=termsEnum.docFreq();
					freq.put(term,docfreq);
				}
			}
		}
	}
    public static void  mostFreqTerm (){
    	ArrayList arrayList = new ArrayList(freq.entrySet()); 
    	Collections.sort(arrayList, new Comparator(){
    		public int compare(Object o1, Object o2) {  
                Map.Entry obj1 = (Map.Entry) o1;  
                Map.Entry obj2 = (Map.Entry) o2;  
                return ((Integer) obj2.getValue()).compareTo((Integer)obj1.getValue());  
                }  
        }); 
    	int len = arrayList.size();
    	System.out.println("The most frequent term is "+arrayList.get(0)+" and The second most frequent term is "+arrayList.get(1));
    	}
    	
    	
    
}
