
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team;

/**
 *
 * @author cheryl
 */
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Random;
import java.util.TreeMap;


public class ScoreGrayCalculation {

    private static final String SAMPLES_PATH = "put sample files path"; // <-- use the path where you put the sample files.
    private static final int FEATURE_LENGTH = 5; // Tokens per window.
    
    private static final double THICKNESS_THRESHOLD = 0.05; // Winnow thickness threshold.
    
    private TreeMap<String, cFeature> mDatabase = null;
    
    private int mFeaturesCreated = 0;
    
    public ScoreGrayCalculation()
    {
        this.mDatabase = new TreeMap<String, cFeature>();
        this.processSamples();
        return;
    }
    
    private void processSamples()
    {
        File dir = new File(SAMPLES_PATH);
        String[] filesFound = this.randomizeListOrder(dir.list());
        
        for(String aFile : filesFound)
        {
            if(aFile.endsWith("spam") || aFile.endsWith("good")){
                
                String text = this.extractTextFromFile(SAMPLES_PATH + "\\" + aFile);
                ArrayList<String> tokens = this.tokenize(text);
                // Arbitrarily designating "spam" and "good" in the file name facilitates the supervised learning.
                String sampleType = aFile.substring(0, 4);
                
                if(sampleType.compareToIgnoreCase("spam") == 0){
                    this.test(tokens, true);
                }else{
                    this.test(tokens, false);
                }
                this.train(aFile);
            }
        }
        return;
    }
    
    public int test(final ArrayList<String> tokens, final boolean isSpam)
    {
        TreeMap<String, cFeature> featureMap = this.createFeatureMap(tokens);
        ArrayList<String> activeFeatures = this.getActiveFeatures(featureMap);
        
        if(isSpam){
            int detect = this.calculateWeights(true, activeFeatures);
            if(detect == 0)
                return 2;
            else
                return 1;
        }else{
            this.calculateWeights(false, activeFeatures);
            return 0;
        }
        
        
    }
    
    public void train(final String fileName)
    {
        String text = this.extractTextFromFile(SAMPLES_PATH + "\\" + fileName);
        ArrayList<String> tokens = this.tokenize(text);
        this.storeFeatures(tokens);
        
        return;
    }
    
    //In this example, the weights of all active features are subject to adjustment, not just the ones that are equal to 1.0.
    //This means that features can have their weights repeatedly adjusted, and to virtually any size.
    private int calculateWeights(final boolean isSpam, ArrayList<String> activeFeatures)
    {
        double goodSum = 0.0;
        double spamSum = 0.0;
        int totalFeatures = activeFeatures.size();
        cFeature feature = null;
        
        for(int i = 0; i < totalFeatures; i++)
        {
            feature = this.mDatabase.get(activeFeatures.get(i));
            goodSum += feature.getGoodWeight(); // sum the good weights.
            spamSum += feature.getSpamWeight(); // sum the spam weights.
        }
        
        
        double rGood = (2*goodSum)/(goodSum+spamSum);
        double rBad = (2*spamSum)/(goodSum+spamSum);
        
        if(rBad+rGood>0){
            double pSpam = spamSum/(goodSum+spamSum);
            if(pSpam<0.01)
                pSpam = 0.01;
            else if(pSpam>0.99)
                pSpam = 0.99;
            
            double pX = 1/(1+(1-pSpam));
            double gX = 0.5 - Math.abs(pX-0.5);
            if(gX<0.2){
                return 0;
            }
            else{
                return 1;
            }   
        }
                
        return 0;
    }
    
       
    private ArrayList<String> getActiveFeatures(TreeMap<String, cFeature> featureMap)
    {
        ArrayList<String> activeFeatures = new ArrayList<String>();
        
        for(String key : featureMap.keySet())
        {
            if(this.mDatabase.containsKey(key)){
                activeFeatures.add(key);
            }
        }
        
        return activeFeatures;
    }
    
    private void storeFeatures(final ArrayList<String> tokens)
    {
        boolean done = false;
        int index = FEATURE_LENGTH - 1;
        cFeature feature = null;
        
        while(!done)
        {
            if(index <= tokens.size()){
                for(int i = 1; i < FEATURE_LENGTH; i++)
                {
                    feature = new cFeature();
                    feature.setData(FEATURE_LENGTH - 1, tokens.get(index));
                    feature.setData((FEATURE_LENGTH - 1) - i, tokens.get(index - i));
                    
                    // Each key represents the feature data concatenated together into one string.
                    String key = "";
                    for(int j = 0; j < FEATURE_LENGTH; j++)
                    {
                        if(feature.getData(j) == ""){
                            key += " ";
                        }else{
                            key += feature.getData(j);
                        }
                    }
                    
                    if(!this.mDatabase.containsKey(key)){
                        this.mDatabase.put(key, feature);
                        this.mFeaturesCreated++;
                    }
                }
            }

            index += 1;
            if(index > tokens.size() - 1){
                done = true;
            }
        }
        return;
    }
    
    public TreeMap<String, cFeature> createFeatureMap(ArrayList<String> tokens)
    {
        boolean done = false;
        int index = FEATURE_LENGTH - 1;
        TreeMap<String, cFeature> featureMap = new TreeMap<String, cFeature>();
        cFeature feature = null;

        while(!done)
        {
            if(index <= tokens.size()){
                for(int i = 1; i < FEATURE_LENGTH; i++)
                {
                    feature = new cFeature();
                    feature.setData(FEATURE_LENGTH - 1, tokens.get(index));
                    feature.setData((FEATURE_LENGTH - 1) - i, tokens.get(index - i));
                    
                    // Each key represents the feature data concatenated together into one string.
                    String key = "";
                    for(int j = 0; j < FEATURE_LENGTH; j++)
                    {
                        if(feature.getData(j) == ""){
                            key += " ";
                        }else{
                            key += feature.getData(j);
                        }
                    }
                    
                    if(!featureMap.containsKey(key)){
                        featureMap.put(key, feature);
                    }
                }
            }

            index += 1;
            if(index > tokens.size() - 1){
                done = true;
            }
        }
        return featureMap;
    }
    
    public ArrayList<String> tokenize(final String document)
    {
                
        Pattern pattern = Pattern.compile("[\\p{Cntrl}\\p{Space}]"); 
        
        ArrayList<String> tokens = new ArrayList<String>();

        String items[] = pattern.split(document);
        for(String item : items)
        {
            if(item != null && !item.isEmpty()){
                tokens.add(item);
            }
        }
        return tokens;
    }
    
    private String extractTextFromFile(final String filePath)
    {
        BufferedReader reader = null;
        StringBuffer buffer = null;
        try{
            reader = new BufferedReader(new FileReader(filePath));
            
            buffer = this.fillStringBuffer(reader);
            
            reader.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        return buffer.toString();
    }
    
    private StringBuffer fillStringBuffer(BufferedReader reader)
    {
        StringBuffer buffer = new StringBuffer();
        boolean done = false;
        while(!done){
            try{
                String line = reader.readLine();
                if(line != null){
                    buffer.append(line);
                    buffer.append("\n");
                }else{
                    done = true;
                }
            }catch(IOException ioe){
                ioe.printStackTrace();
                done = true;
            }
        }
        return buffer;
    }
    
    private String[] randomizeListOrder(String[] aList)
    {
        Random rand = new Random();
        int listSize = aList.length;
        int shuffleCount = 0;
        boolean done = false;
        
        while(!done)
        {
            int index1 = rand.nextInt(listSize);
            int index2 = rand.nextInt(listSize);
            if(index1 == index2){ continue; } // The random numbers need to differ.
            
            String temp = aList[index1];
            aList[index1] = aList[index2];
            aList[index2] = temp;
            
            shuffleCount++;
            if(shuffleCount >= listSize){
                done = true;
            }
        }
        
        return aList;
    }

    
    
    private class cFeature
    {
        
        private String mData[] = new String[FEATURE_LENGTH];
        
        
        double mSpamWeight = 1.0;
        double mGoodWeight = 1.0;
    
        public cFeature() {    return;    }
    
        public void setSpamWeight(double weightValue)
        {
            this.mSpamWeight = weightValue;
            return;
        }
        
        public double getSpamWeight()
        {
            return this.mSpamWeight;
        }
        
        public void setGoodWeight(double weightValue)
        {
            this.mGoodWeight = weightValue;
            return;
        }
        
        public double getGoodWeight()
        {
            return this.mGoodWeight;
        }
    
        public void setData(final int index, final String token)
        {
            this.mData[index] = token;
            return;
        }
        
        public String getData(int index)
        {
            return this.mData[index];
        }

    } 
    
    public static void main(String[] args) 
    {
        ScoreGrayCalculation winnow1 = new ScoreGrayCalculation();
        return;
    }
    
}
