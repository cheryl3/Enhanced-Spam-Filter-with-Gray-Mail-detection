/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author cheryl
 */
public class Detect {
    
    int spamCount = 0;
    int hamCount = 0;
    Map<String, Node> map = new HashMap<String, Node>();
    List<String> commonWords = new ArrayList<String>();
    
    public void init() throws Exception {
	BufferedReader br = new BufferedReader( new FileReader( new File("put file path for common words")));
	String line = br.readLine();
	while(line != null) {
            commonWords.add(line);
            line = br.readLine();
	}
	String spam_dataset = "put file path for spam dataset";
        
        File spamFolder = new File(spam_dataset);
        File[] spamList = spamFolder.listFiles();
                
        for(File file : spamList){
            br = new BufferedReader(new FileReader(file));
            line = br.readLine();
            while(line != null) {
                line = line.toLowerCase();
		String[] words = line.split(" ");
		for( String s: words ) {
                    if(s.length() > 3 && !commonWords.contains(s)) {
			spamCount++;
			if( map.containsKey(s)) {
                            map.get(s).spamCount++;
			} 
                        else {
                            map.put(s, new Node( 1, 0));
			}
                    }
		}
		line = br.readLine();
            }
        }
                
                
                
        String ham_dataset = "put file path for ham dataset";
        
        File hamFolder = new File(ham_dataset);
        File[] hamList = hamFolder.listFiles();
                
        for(File file : hamList){
            br = new BufferedReader(new FileReader(file));
            line = br.readLine();
            while(line != null) {
		line = line.toLowerCase();
		String[] words = line.split(" ");
		for( String s: words ) {
                    if(s.length() > 3 && !commonWords.contains(s)) {
			hamCount++;
			if( map.containsKey(s)) {
                            map.get(s).hamCount++;
			} 
                        else {
                            map.put(s, new Node( 0, 1));
			}
                    }
		}
		line = br.readLine();
            }
		
        }
                
                
		
	Set<String> keys = map.keySet();
	for( String key: keys ) {
            Node node = map.get(key);
            double res = ((node.spamCount)/(double)(spamCount))/(double)(((node.spamCount)/(double)(spamCount)) + (node.hamCount)/(double)(hamCount));
            node.probability = res;
	}
		
	br.close();
    }
    
    
    public void detect(String s) {

        boolean result = false;
	s = s.toLowerCase();
	String[] sArr = s.split(" ");
	TreeMap<Double, List<Double>> interestMap = new TreeMap<Double, List<Double>>(Collections.reverseOrder());
        
	for( String x: sArr ) {
            
	    if(x.length()> 3 && !commonWords.contains(x)) {
		double i = 0.5;
		double p = 0.5;
		if(map.containsKey(x)) {
                    p = map.get(x).probability;					
		} 
		
                i = Math.abs(i - p);
		if( !interestMap.containsKey(i) ) {
                    List<Double> values = new ArrayList<Double>();
                    values.add(p);						
                    interestMap.put(i, values);
		} 
                else {
                    interestMap.get(i).add(p);
		}
            }
	}
		
	List<Double> probabilities = new ArrayList<Double>();
	int count = 0;
	Set<Double> set = interestMap.keySet();
	for( Double d: set ) {
            List<Double> list = interestMap.get(d);
	
            for(Double x: list) {
		count++;
		probabilities.add(x);
		if(count == 15) {
                	break;
		}
            }
            if(count == 15) {
		break;
            }
	}
		
		double res = 1;
		double numerator = 1;
		double denominator = 1;
		for( Double d: probabilities ) {
			numerator = numerator * d;
			denominator = denominator * (1- d);
		}
		res = numerator/(double)(numerator +denominator);
                
		if(res >= 0.9) {
			result = true;
		}
		
                if( result ) {
                    
                    ScoreGrayCalculation adjoint = new ScoreGrayCalculation();
                    
                    ArrayList<String> tokens = adjoint.tokenize(s);
                    int detect = adjoint.test(tokens, result);
                    if(detect == 1)
                        System.out.println("'" +s+ "' \n spam");
                    if(detect == 2)
                        System.out.println("'" +s+ "'\n gray");
                    
                        
		} 
                else{
                    System.out.println("'" +s+ "'\n is not a spam");
                }
                        
            	
    }
    
    
    public static void main(String args[]) throws Exception{
        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        Detect winnow1 = new Detect();
	winnow1.init();
        
        String s = null;
                do{
                    System.out.println("Enter your message(press 0 to stop)");
                    StringBuilder everything = new StringBuilder();
                    String line;
                    while( !(line = br.readLine()).isEmpty()) {
                        everything.append(line);
                    }
                    s = everything.toString();
                    
                    winnow1.detect(s);
                }while(!"0".equals(s));
                
    }

    
}

class Node {
	int spamCount;
	int hamCount;
	double probability;
	
	public Node( int spamCount, int hamCount ) {
		this.spamCount = spamCount;
		this.hamCount = hamCount;
	}
	
	public String toString() {
		return String.valueOf(probability);
	}
}
