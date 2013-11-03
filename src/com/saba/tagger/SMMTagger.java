/**
 * 
 */
package com.saba.tagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



import com.wcohen.ss.Jaccard;
import com.wcohen.ss.JaroWinkler;
import com.wcohen.ss.SoftTFIDF;



/**
 * @author ratish
 *
 */
public class SMMTagger {

	private static final String TAB = "\t";
	private static final String GENMODELAVG = "/home/ratish/project/u23/model/genmodelavg";
	private static final String GENMODEL = "/home/ratish/project/u23/model/genmodel";
	private static final String TRAINING_DIR = "/home/ratish/Downloads/skype/Shobit/train/";
	private static final String TESTFILE = "/home/ratish/Downloads/skype/Shobit/test/Essay2.txt";
	private static final String TESTFILEOUT = "/home/ratish/Downloads/skype/Shobit/test/Essay2.tsvout";
	private static final String SEPARATOR = ":";
	private static final String SPACE = " ";
	private static final String STAR = "*";
	private static final String STOP = "STOP";
	private static final String CITY = "CITY";
	private static final String STATE = "STATE";
	private static final String PERSONNAME = "PERSONNAME";
	private static final String OTHER = "OTHER";
	private static final String TAG = "TAG";
	private static final String WINDOWLEFT = "WINDOWLEFT";
	private static final String WINDOWRIGHT = "WINDOWRIGHT";
	private static final String SEGMENTLENGTH = "SEGMENTLENGTH";
	private static final String SEGMENTTAG = "SEGMENTTAG";
	private static final String SEGMENTSHAPE1 = "SEGMENTSHAPE1";
	private static final String SEGMENTSHAPE2 = "SEGMENTSHAPE2";
	private static final String TAGBEFORE = "TAGBEFORE";
	private static final String BINARYDISTANCE = "BINARYDISTANCE";
	private static final String JAROWINKLERDISTANCE = "JAROWINKLERDISTANCE";
	private static final String SOFTTFIDF = "SOFTTFIDF";
	private static final String JACCARD = "JACCARD";
			
	private static int L = 3;
	private static final String JOBTITLE = "JOBTITLE";
	
	private Map<String,Object> gazetteMap = new HashMap<String,Object>();
	private List<String> gazetteList = new ArrayList<String>();
	private Map<String, Double> jaccardDistance = new HashMap<String, Double>();
	private Map<String, Double> jwDistance = new HashMap<String, Double>();
	private Map<String, Double> softTFIDFDistance = new HashMap<String, Double>();
	
	public static void main(String[] args) {
		SMMTagger smmTagger = new SMMTagger();
		smmTagger.loadGazette();
//		smmTagger.train();
//		smmTagger.train("dummy");
		Map<String,Double> vMap = new HashMap<String,Double>();
		smmTagger.loadV(vMap);
		smmTagger.decode(vMap);
		
	}
	
	
	
	private void loadV(Map<String, Double> vMap) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(GENMODELAVG)));
			String readLine = null;
			while((readLine = br.readLine())!= null){
				String [] args = readLine.split(TAB);
				vMap.put(args[0], Double.parseDouble(args[1]));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void decode(Map<String, Double> vMap){
		try {
			BufferedReader br1 = new BufferedReader(new FileReader(new File(TESTFILE)));
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(new File(TESTFILEOUT)));
			String readLine = null;
			List<String> input = new ArrayList<String>();
			List<String>  tags = null;
//			while((readLine = br1.readLine())!= null){
//				input.add(readLine);
//			}
			String sentence = "I hereby appoint you as the chief manager of this company";
			tags = viterbi(Arrays.asList(sentence.split(SPACE)), vMap);
			writeDevTags(bw1, input, tags);
			br1.close();
			bw1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeDevTags(BufferedWriter bw1, List<String> input,
			List<String> tags) throws IOException {
		for(int i = 0; i< input.size(); i++){
			bw1.write(input.get(i)+SPACE+ tags.get(i));
			bw1.newLine();
		}
		bw1.newLine();
	}
	
	private void loadGazette() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("/home/ratish/Downloads/skype/jobTitles.txt")));
			String  readLine = null;
			while((readLine = br.readLine())!=null){
				gazetteMap.put(readLine, new Object());
				gazetteList.add(readLine);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	private void train(String sentence){
		try {
			Map<String, Double> vMap = new HashMap<String, Double>();
			Map<String, AvgValue> vMapAvg = new HashMap<String, AvgValue>();
			sentence = "Fred you are my manager this saturday";
			for(int i=0; i<5; i++){
				
				String [] args = sentence.split(SPACE);
				List<String> input = Arrays.asList(args);
				
				String tagSentence = "O O O O JOBTITLE O O";
				String [] goldtagsArray = tagSentence.split(SPACE);
				List<String> goldTags = Arrays.asList(goldtagsArray);
				List<String> tags = viterbi(input, vMap);
				Map<String, Double> fxizi = new HashMap<String, Double>();
				Map<String, Double> fxiyi = new HashMap<String, Double>();
				updateF(fxizi, tags, input);
				updateF(fxiyi, goldTags, input);
				updateV(vMap, fxiyi, fxizi);
				updateVmapAvg(vMap, vMapAvg, fxiyi, fxizi);
				
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(GENMODEL)));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File(GENMODELAVG)));
			Set<Map.Entry<String, Double>> entrySet = vMap.entrySet();
			for (Map.Entry<String, Double> entry : entrySet) {
				bw.write(entry.getKey()+TAB+entry.getValue());
				bw.newLine();
			}
			
			Set<Map.Entry<String, AvgValue>>entrySet2 = vMapAvg.entrySet();
			for (Map.Entry<String, AvgValue> entry : entrySet2) {
				AvgValue value = entry.getValue();
				bw2.write(entry.getKey()+TAB+(value.getD()/value.getCount()));
				bw2.newLine();
			}
			
			bw.close();
			bw2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void train(){
		try{
			Map<String, Double> vMap = new HashMap<String, Double>();
			Map<String, AvgValue> vMapAvg = new HashMap<String, AvgValue>();
			for(int i=0; i<6; i++){
				String dirname = TRAINING_DIR;
				File dir = new File(dirname);
				for(File child: dir.listFiles()){
					BufferedReader br = new BufferedReader(new FileReader(child));
					String readLine = null;
					List<String> input = new ArrayList<String>();
					List<String> goldTags = new ArrayList<String>();
					while((readLine = br.readLine())!= null){
						if(readLine.trim().equals("O") || readLine.trim().length()==0){
							continue;
						}
						input.add(readLine.split(TAB)[0]);
						goldTags.add(readLine.split(TAB)[1]);
					}
					List<String> tags = viterbi(input, vMap);
					Map<String, Double> fxizi = new HashMap<String, Double>();
					Map<String, Double> fxiyi = new HashMap<String, Double>();
					updateF(fxizi, tags, input);
					updateF(fxiyi, goldTags, input);
					updateV(vMap, fxiyi, fxizi);
					updateVmapAvg(vMap, vMapAvg, fxiyi, fxizi);
					br.close();
		
				}
				
				System.out.println(i);
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(GENMODEL)));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File(GENMODELAVG)));
			Set<Map.Entry<String, Double>> entrySet = vMap.entrySet();
			for (Map.Entry<String, Double> entry : entrySet) {
				bw.write(entry.getKey()+TAB+entry.getValue());
				bw.newLine();
			}
			
			Set<Map.Entry<String, AvgValue>>entrySet2 = vMapAvg.entrySet();
			for (Map.Entry<String, AvgValue> entry : entrySet2) {
				AvgValue value = entry.getValue();
				bw2.write(entry.getKey()+TAB+(value.getD()/value.getCount()));
				bw2.newLine();
			}
			
			bw.close();
			bw2.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	private void updateVmapAvg(Map<String, Double> vMap,
			Map<String, AvgValue> vMapAvg, Map<String, Double> fxiyi,
			Map<String, Double> fxizi) {
		Set<Map.Entry<String, Double>> entrySet = fxiyi.entrySet();
		Map<String,Object> changed = new HashMap<String, Object>();
		for(Map.Entry<String, Double> entry:entrySet){
			String key = entry.getKey();
			changed.put(key, new Object());
		}
		
		entrySet = fxizi.entrySet();
		for(Map.Entry<String, Double> entry:entrySet){
			String key = entry.getKey();
			changed.put(key, new Object());
		}
		
		Set<Map.Entry<String, Object>> changedSet =  changed.entrySet();
		for(Map.Entry<String, Object> changedSetEntry: changedSet){
			String key = changedSetEntry.getKey();
			if(vMapAvg.containsKey(key)){
				AvgValue avg = vMapAvg.get(key);
				Double sum = avg.getD() + vMap.get(key);
				int count = avg.getCount() +1;
				vMapAvg.put(key, new AvgValue(sum,count));
			}else{
				vMapAvg.put(key, new AvgValue(vMap.get(key), 1));
			}
		}
		
	}
	
	private Map<String, Double> updateV(Map<String, Double> vMap,
			Map<String, Double> fxiyi, Map<String, Double> fxizi) {

		Set<Map.Entry<String, Double>> entrySet = fxiyi.entrySet();
		for(Map.Entry<String, Double> entry:entrySet){
			String key = entry.getKey();
			Double value = entry.getValue();
			if(vMap.containsKey(key)){
				vMap.put(key, vMap.get(key)+value);
			}else{
				vMap.put(key, value);
			}
		}
		entrySet = fxizi.entrySet();
		for(Map.Entry<String, Double> entry:entrySet){
			String key = entry.getKey();
			Double value = entry.getValue();
			if(vMap.containsKey(key)){
				vMap.put(key, vMap.get(key)-value);
			}else{
				vMap.put(key, -1*value);
			}
		}
		return vMap;
	}



	private void updateF(Map<String, Double> fxizi, List<String> tags, List<String> input) {
		int begin = 0;
		List<String> segments = new ArrayList<String>();
		List<String> processedTags = new ArrayList<String>();
		List<String> segment = new ArrayList<String>();
		for(int i=0; i<input.size(); i++){
			String inputEntry = input.get(i);
			String tag = tags.get(i);
			if(tag.equals(JOBTITLE)){
				if(begin == 0){
					begin = 1;
					segment = new ArrayList<String>(); 
				}
				segment.add(inputEntry);
				if(segment.size() >=3){
					processSegment(segments, processedTags, segment);
					segment = new ArrayList<String>();
				}
			}else{
				begin = 0;
				processSegment(segments, processedTags, segment);
				segment = new ArrayList<String>();
				segments.add(inputEntry);
				processedTags.add(OTHER);
			}
			
		}
		processSegment(segments, processedTags, segment);
		
		
		for(int i=0; i<processedTags.size(); i++){
			String w = processedTags.get(i);
			String segmentEntry = segments.get(i);
			setOrIncrement(fxizi, getSegmentTag(w, segmentEntry));
//			setOrIncrement(fxizi, getSegmentLengthFeature(w, segmentEntry.split(SPACE).length));
//			setOrIncrement(fxizi, getSegmentShapeS1Feature(w, segmentEntry));
//			setOrIncrement(fxizi, getSegmentShapeS2Feature(w, segmentEntry));
			if(gazetteMap.containsKey(segment)){
				setOrIncrement(fxizi, getBinaryDistanceFeature(w));
			}
			setOrIncrement(fxizi, getJaccardDistanceFeature(w), jaccardDistance.get(segmentEntry));
//			setOrIncrement(fxizi, getJaroWinklerDistanceFeature(w), jwDistance.get(segmentEntry));
//			setOrIncrement(fxizi, getSoftTFIDFDistanceFeature(w), softTFIDFDistance.get(segmentEntry));
		}
		
	}
	
	private void setOrIncrement(Map<String, Double> f, String key) {
		if(f.containsKey(key)){
			f.put(key, f.get(key)+1);	
		}else{
			f.put(key, 1d);
		}
	}

	private void setOrIncrement(Map<String, Double> f, String key, Double incrementFactor) {
		if(f.containsKey(key)){
			f.put(key, f.get(key)+incrementFactor);	
		}else{
			f.put(key, incrementFactor);
		}
	}


	private void processSegment(List<String> segments, List<String> processedTags,
			List<String> segment) {
		if(segment.size() >0){
			StringBuilder sb = new StringBuilder();
			for(int index = 0; index< segment.size() -1; index++){
				sb.append(segment.get(index));
				sb.append(SPACE);
			}
			sb.append(segment.get(segment.size()-1));
			segments.add(sb.toString());
			processedTags.add(JOBTITLE);
			segment = new ArrayList<String>();
		}
	}



	private List<String> viterbi(List<String> input, Map<String, Double> vMap){
		int n = input.size();

		String [] tags = new String []{JOBTITLE, OTHER};
		String [] tagsPlusStar = new String []{JOBTITLE,OTHER, STAR};
		Map<Kv, Double> pi = new HashMap<Kv, Double>();
		Map<Kv, Kv> bp = new HashMap<Kv, Kv>();
		
		pi.put(new Kv(STAR, 0), 0d);
		for(int h=0; h<tags.length; h++){
			pi.put(new Kv(tags[h],0), Double.NEGATIVE_INFINITY);
		}
		
		for(int k=0; k<n; k++){
			for(String v: tags){
				int i = k+1;
				Kv kv = new Kv(v, i);
//				pi.put(kv, Double.NEGATIVE_INFINITY);
				
				Double maxValue = Double.NEGATIVE_INFINITY;
				Kv maxKv = null;				
				for(String w: tagsPlusStar){
					if(w == JOBTITLE){
						L = 3;
					}else {
						L = 1;
					}
					for(int idash = i-L; idash <i; idash++){
						if(idash <0){
							continue;
						}
						if(i == 1 && !w.equals(STAR)){
							continue;
						}
						
						Kv kw = new Kv(w, idash);
						
						double piIdash = Double.NEGATIVE_INFINITY;
						if(pi.containsKey(kw)){
							piIdash = pi.get(kw);
						}
						
						
						StringBuilder sb = new StringBuilder();
						
						for(int index = idash; index< i-1; index++){
							sb.append(input.get(index));
							sb.append(SPACE);
						}
						sb.append(input.get(i-1));
						String segment = sb.toString();
						String segmentTag = getSegmentTag(v, segment);
						
						Double segmentTagValue = 0d;
						if(vMap.containsKey(segmentTag)){
							segmentTagValue = vMap.get(segmentTag);
						}
						
						int length = i-idash;
						String segmentLengthTag = getSegmentLengthFeature(v, length);
						Double segmentLengthValue = 0d;
						if(vMap.containsKey(segmentLengthTag)){
							segmentLengthValue = vMap.get(segmentLengthTag);
						}

//						String s1 = getShapeS1(segment);
//						String s2 = getShapeS2(s1);
						
						String segmentShape1Tag = getSegmentShapeS1Feature(v, segment);
						Double segmentShape1Value = 0d;
						if(vMap.containsKey(segmentShape1Tag)){
							segmentShape1Value = vMap.get(segmentShape1Tag);
						}
						
						String segmentShape2Tag = getSegmentShapeS2Feature(v, segment);
						Double segmentShape2Value = 0d;
						if(vMap.containsKey(segmentShape2Tag)){
							segmentShape2Value = vMap.get(segmentShape2Tag);
						}
								
						Double binaryDistanceValue = 0d;
						String binaryDistanceTag = getBinaryDistanceFeature(v);
						if(gazetteMap.containsKey(segment) && vMap.containsKey(binaryDistanceTag)){
							binaryDistanceValue = vMap.get(binaryDistanceTag);
						}
						
						/*Double jwdDistanceValue = 0d;
						String jwdDistanceTag = getJaroWinklerDistanceFeature(w);
						JaroWinkler jaroWinkler = new JaroWinkler();
						Double maxjwDistance = 0d;
						if(jwDistance.containsKey(segment)){
							maxjwDistance = jwDistance.get(segment);
						}else{
							for(String gazetteEntry: gazetteList){
								double score = jaroWinkler.score(segment, gazetteEntry);
								if(maxjwDistance < score){
									maxjwDistance = score;
								}
							}
							jwDistance.put(segment, maxjwDistance);
						}
						
						if(vMap.containsKey(jwdDistanceTag)){
							jwdDistanceValue = maxjwDistance * vMap.get(jwdDistanceTag);
						}*/
						
						
						Double maxJaccardDistance = 0d;
						String jaccardDistanceTag = getJaccardDistanceFeature(v);
						Double jaccardDistanceValue = 0d;
						
						if(jaccardDistance.containsKey(segment)){
							maxJaccardDistance = jaccardDistance.get(segment);	
						}else{
							Jaccard jaccard = new Jaccard();
							for(String gazetteEntry:gazetteList){
								double score = jaccard.score(segment, gazetteEntry);
								if(maxJaccardDistance < score){
									maxJaccardDistance = score;
								}
							}
							jaccardDistance.put(segment, maxJaccardDistance);
						}						
						if(vMap.containsKey(jaccardDistanceTag)){
							jaccardDistanceValue = maxJaccardDistance*vMap.get(jaccardDistanceTag);
						}
						
						/*SoftTFIDF softTFIDF = new SoftTFIDF();
						Double softTFIDFValue = 0d;
						Double maxSoftTFIDFValue = 0d;
						String softTFIDFTag = getSoftTFIDFDistanceFeature(w);
						
						if(softTFIDFDistance.containsKey(segment)){
							maxSoftTFIDFValue = softTFIDFDistance.get(segment);
						}else{
							for(String gazetteEntry: gazetteList){
								double score = softTFIDF.score(segment, gazetteEntry);
								if(maxSoftTFIDFValue < score){
									maxSoftTFIDFValue = score;
								}
							}
							softTFIDFDistance.put(segment, maxSoftTFIDFValue);
						}
						
						if(vMap.containsKey(softTFIDFTag)){
							softTFIDFValue = maxSoftTFIDFValue* vMap.get(softTFIDFTag);
						}*/
						
						double piValue = piIdash +segmentTagValue  
								+binaryDistanceValue+jaccardDistanceValue; 
//								jwdDistanceValue  + softTFIDFValue+segmentLengthValue + segmentShape2Value+  segmentShape1Value;
						if(maxValue <= piValue){
							maxValue = piValue;
							maxKv = kw;
						}
						
						System.out.println("pi "+piValue+" piIdash "+piIdash +" i "+i+ " segment "+segment + " w "+w +" v "+v +" segmenttagvalue "+segmentTagValue+ " segmentlength "+segmentLengthValue + " ss1 "+segmentShape1Value+ " ss2 "+segmentShape2Value+ " jaccard "+jaccardDistanceValue + " binary "+binaryDistanceValue);
					}
				}
				
				pi.put(kv, maxValue);
				bp.put(kv, maxKv);
			}
			System.out.println(k);
		}
		
		
		double maxPiValue = Double.NEGATIVE_INFINITY;
		Kv maxPi = null;
		for(String v: tags){
			Kv kv = new Kv(v, n);
			if(maxPiValue <= pi.get(kv)){
				maxPiValue = pi.get(kv);
				maxPi = kv;
			}
		}
		
		List<Kv> kvs = new ArrayList<Kv>();
		kvs.add(maxPi);
		while(bp.get(maxPi) != null){
			maxPi = bp.get(maxPi);
			kvs.add(maxPi);
		}
		Collections.reverse(kvs);
		List<String> tagsOutput = new ArrayList<String>();
		for(int k=0; k<input.size(); k++){
			tagsOutput.add(kvs.get(k+1).getV());
		}
		System.out.println(kvs);
		System.out.println(tagsOutput);
		return tagsOutput;
	}



	private String getSoftTFIDFDistanceFeature(String w) {
		return SOFTTFIDF + SEPARATOR +w;
	}



	private String getJaccardDistanceFeature(String w) {
		return JACCARD+SEPARATOR+w;
	}



	private String getJaroWinklerDistanceFeature(String w) {
		return JAROWINKLERDISTANCE +SEPARATOR +w;
	}



	private String getBinaryDistanceFeature(String w) {
		return BINARYDISTANCE+SEPARATOR+w;
	}



	private String getSegmentShapeS2Feature(String w, String segment) {
		return SEGMENTSHAPE2+SEPARATOR+getShapeS2(getShapeS1(segment)) +SEPARATOR+w;
	}



	private String getSegmentShapeS1Feature(String w, String segment) {
		return SEGMENTSHAPE1+SEPARATOR+getShapeS1(segment) +SEPARATOR+w;
	}



	private String getShapeS2(String s1) {
		return s1.replaceAll("XX+", "X+").replaceAll("xx+", "x+").replaceAll("dd+", "d+");
	}



	private String getShapeS1(String segment) {
		return segment.replaceAll("[A-Z]", "X").replaceAll("[a-z]", "x").replaceAll("[0-9]", "d");
	}



	private String getSegmentLengthFeature(String w, int length) {
		return SEGMENTLENGTH+SEPARATOR+length+SEPARATOR+w;
	}



	private String getSegmentTag(String w, String segment) {
		return SEGMENTTAG+SEPARATOR+segment+SEPARATOR+w;
	}
	
}

class Kv{
	private String v;
	private int k;
	
	public Kv() {
		// TODO Auto-generated constructor stub
	}
	
	
	public Kv(String v, int k) {
		super();
		this.v = v;
		this.k = k;
	}


	public void setK(int k) {
		this.k = k;
	}
	
	public void setV(String v) {
		this.v = v;
	}
	
	public int getK() {
		return k;
	}
	
	public String getV() {
		return v;
	}
	
	


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + k;
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Kv other = (Kv) obj;
		if (k != other.k)
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Kv [v=" + v + ", k=" + k + "]";
	}
	
	
}

class AvgValue{
	private Double d;
	private int count;
	
	public AvgValue() {
		// TODO Auto-generated constructor stub
	}
	
	
	public AvgValue(Double d, int count) {
		super();
		this.d = d;
		this.count = count;
	}


	public Double getD() {
		return d;
	}
	public void setD(Double d) {
		this.d = d;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	
	
}
