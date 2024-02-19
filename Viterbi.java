/**
 *use POS tagging to take a sequence of words and produce
 * the corresponding sequence of tags using HMM and Viterbi
 *
 *
 * @author Dylan Lawler, Rebecca Liu , Dartmouth CS 10 Spring 2021
 */

import java.io.*;
import java.util.*;

public class Viterbi {
    public String trainingTagFile;
    public String trainingWordFile;
    public Map<String, Map<String, Integer>> observationfrequency; // map of tags and the frequency of words of each tag
    public Map<String, Map<String, Integer>> transitionfrequency; // map of tag to tag  frequencies
    public Map<String, Map<String, Double>> observationscores;
    public Map<String, Map<String, Double>> transitionscores;
    public ArrayList<String> path;
    public int unseenWordPenalty = -100; // set observation score for an unseen word

    public Viterbi(String trainingFile, String trainingWord){
        this.transitionfrequency = new HashMap<>();
        this.observationfrequency = new HashMap<>();
        this.observationscores = new HashMap<>();
        this.transitionscores = new HashMap<>();
        this.trainingTagFile = trainingFile;
        this.trainingWordFile = trainingWord;
        this.path = new ArrayList<>();
    }

    public Viterbi(){
        this.transitionfrequency = new HashMap<>();
        this.observationfrequency = new HashMap<>();
        this.observationscores = new HashMap<>();
        this.transitionscores = new HashMap<>();
        this.path = new ArrayList<>();
    }

    public void Train(){
        try{
            // read files
            BufferedReader tagInput = new BufferedReader(new FileReader(trainingTagFile));
            BufferedReader wordInput = new BufferedReader(new FileReader(trainingWordFile));
            String wordLine;
            String tagLine;
            transitionfrequency.put("#", new HashMap<>());
            //while there's still a line to read
            while ((tagLine = tagInput.readLine())!= null && (wordLine = wordInput.readLine())!= null){
                String[] tagSplit = tagLine.split(" ");
                String[] wordSplit = wordLine.split(" ");
                for(int i = 0; i< wordSplit.length; i++){
                    wordSplit[i] = wordSplit[i].toLowerCase();
                }
                for(int i = 0; i < tagSplit.length; i++){
                    // if reach the end of the line, add starting value and set transition from #
                    if(i == tagSplit.length - 1){
                        if(transitionfrequency.get("#").containsKey(tagSplit[0])){
                            int frequency = transitionfrequency.get("#").get(tagSplit[0]) + 1;
                            transitionfrequency.get("#").put(tagSplit[0], frequency);
                        }
                        else{
                            transitionfrequency.get("#").put(tagSplit[0], 1);
                        }
                    }
                }
                for(int i = 0; i< tagSplit.length; i++){
                    // creating words as tags frequency map
                    //if tag exists
                    if(observationfrequency.containsKey(tagSplit[i])){
                        // if tag and word exist
                        if(observationfrequency.get(tagSplit[i]).containsKey(wordSplit[i])){
                            Map<String, Integer> words = observationfrequency.get(tagSplit[i]);
                            int freq = observationfrequency.get(tagSplit[i]).get(wordSplit[i]) + 1;
                            words.put(wordSplit[i], freq);
                            observationfrequency.put(tagSplit[i], words);

                        }
                        else{
                            observationfrequency.get(tagSplit[i]).put((wordSplit[i]), 1);
                        }
                    }
                    // if neither exist
                    else{
                        observationfrequency.put(tagSplit[i], new HashMap<>());
                        observationfrequency.get(tagSplit[i]).put(wordSplit[i], 1);
                    }
                    // creating map of the frequency of diff tag transitions
                    //if tag exists
                    if(i< tagSplit.length - 1){
                        if(transitionfrequency.containsKey(tagSplit[i])){
                            //if transition exist
                            if(transitionfrequency.get(tagSplit[i]).containsKey(tagSplit[i+1])){
                                Map<String, Integer> tags = transitionfrequency.get(tagSplit[i]);
                                int freq = transitionfrequency.get(tagSplit[i]).get(tagSplit[i+1]) +1;
                                tags.put(tagSplit[i+1], freq);
                                transitionfrequency.put(tagSplit[i], tags);
                            }
                            else{
                                transitionfrequency.get(tagSplit[i]).put(tagSplit[i+1], 1);
                            }
                        }
                        // if tag does not exist
                        else{
                            transitionfrequency.put(tagSplit[i], new HashMap<>());
                            transitionfrequency.get(tagSplit[i]).put(tagSplit[i+1], 1);
                        }
                    }
                }
            }

            // creating observation scores
            for(String tag: observationfrequency.keySet()){
                observationscores.put(tag, new HashMap<>());
                int counter = 0;// keep track of overall total
                for(String word: observationfrequency.get(tag).keySet()){
                    counter += observationfrequency.get(tag).get(word);
                }
                for(String word: observationfrequency.get(tag).keySet()){
                    double score = (double)observationfrequency.get(tag).get(word)/counter;
                    observationscores.get(tag).put(word, Math.log(score));
                }
            }
            //creating transition scores
            for(String tag: transitionfrequency.keySet()){
                transitionscores.put(tag, new HashMap<>());
                int counter = 0; // keep track of overall total
                for(String tag2: transitionfrequency.get(tag).keySet()){
                    counter += transitionfrequency.get(tag).get(tag2);
                }
                for(String tag2: transitionfrequency.get(tag).keySet()){
                    double score = (double)transitionfrequency.get(tag).get(tag2)/counter;
                    transitionscores.get(tag).put(tag2, Math.log(score));
                }
            }
            //close files
            tagInput.close();
            wordInput.close();
        }
        catch(IOException e){
            System.out.println("IO Exception");

        }
    }

    // return the most likely path
    public ArrayList<String> viterbiDecodingPath(String line){
        Map<String, Double> currentScores = new HashMap<>();
        ArrayList<Map<String, String>> backTraceList = new ArrayList<>();
        String start = "#";
        List<String> currStates = new ArrayList<String>();
        currStates.add(start);
        currentScores.put(start, 0.0);
        HashMap<String, Double> scores = new HashMap<>();
        String[] lineSplit = line.split(" ");
        path.clear(); // reset path
        int observations = lineSplit.length ; // number of words to read
        for(int i = 0; i< observations; i ++){
            Map<String, Double> nextScores = new HashMap<>();
            backTraceList.add(new HashMap<>());
            double nextScore;
            for(String current: currentScores.keySet()){
                if(transitionscores.get(current)!= null){
                    for(String next: transitionscores.get(current).keySet()){
                        // calculating scores
                        if(!observationscores.get(next).containsKey(lineSplit[i])){ // if the observation score does not exist
                            nextScore = currentScores.get(current) + transitionscores.get(current).get(next) + unseenWordPenalty;
                        }
                        else{
                            nextScore = currentScores.get(current) + transitionscores.get(current).get(next) + observationscores.get(next).get(lineSplit[i]);
                        }
                        if(!nextScores.containsKey(next) || nextScore > nextScores.get(next)){
                            nextScores.put(next, nextScore);
                            backTraceList.get(i).put(next, current); // update backtrace
                            if(i == observations - 1){ // if reached the end of observations
                                scores.put(next, nextScore); // keep track of end scores to compare
                            }
                        }
                    }
                }

            }
            // update current current scores
            currentScores = nextScores;
        }
        Double highestscore = -100000000.0;
        String finaltag = "";
        for(String tag: scores.keySet()){
            // finding highest final score
            if (scores.get(tag)>highestscore){
                highestscore = scores.get(tag);
                finaltag = tag;
            }
        }
        // setting initial current tag to the final tag with the highest score
        String currenttag = finaltag;
        // backtracking and creating path
        for(int i = observations -1; i>=0; i--){
            path.add(0,currenttag);
            currenttag = backTraceList.get(i).get(currenttag); // update current tag
        }
        return path;
    }

    public void accuracy(ArrayList path, String[] correctpath){
        int correct = 0;
        int error = 0;
        for(int i = 0; i< path.size(); i++){
            // if two tags are equal, increment correct value
            if(path.get(i).equals(correctpath[i])){
                correct +=1;
            }
            //increment error value if tags are unequal
            else{
                error+=1;
            }
        }
        System.out.println("correct: " + correct + " error: " + error);
    }

    public void consoleTest(){
        //train the text files
        this.Train();
        Scanner in = new Scanner(System.in);
        String line = "";
        while (!line.equals("q")){
            line = in.nextLine();
            // quit when q is pressed
            if (line.equals("q")){
                System.out.println("Thanks for testing!");
            }
            else { // find the best path for the input line
                System.out.println(viterbiDecodingPath(line));
            }
        }
    }

    public void viterbiDecoding(String testTagFile, String testWordFile){
        // train the file
        this.Train();
        try{
            BufferedReader testWordInput = new BufferedReader(new FileReader(testWordFile));
            BufferedReader testTagInput = new BufferedReader(new FileReader(testTagFile));
            String tags;
            String line;
            int correct = 0;
            int error = 0;
            while((tags = testTagInput.readLine())!= null && (line = testWordInput.readLine())!=null){
                String[] tagsSplit = tags.split(" ");
                path = this.viterbiDecodingPath(line);
                for(int i = 0; i< path.size(); i++){
                    // if tag is correct add 1 to correct value
                    if(path.get(i).equals(tagsSplit[i])){
                        correct+=1;
                    }
                    // if tag is different, add 1 to error value
                    else{
                        error +=1;
                    }
                }
                // reset path
                path.clear();
            }
            System.out.println("correct: " + correct + " error: " + error);
            //close files
            testTagInput.close();
            testWordInput.close();
        }
        catch (IOException e){
            System.out.println("IO exception");
        }
    }

    public static void main(String[] args) {
        // testing a hard coded graph
        System.out.println("Test 1");
        Viterbi test = new Viterbi();
        test.transitionscores.put("#", new HashMap<>());
        test.transitionscores.get("#").put("Cold", 5.0);
        test.transitionscores.get("#").put("Hot", 5.0);
        test.transitionscores.put("Cold", new HashMap<>());
        test.transitionscores.get("Cold").put("Cold", 7.0);
        test.transitionscores.get("Cold").put("Hot", 3.0);
        test.transitionscores.put("Hot", new HashMap<>());
        test.transitionscores.get("Hot").put("Hot", 7.0);
        test.transitionscores.get("Hot").put("Cold", 3.0);
        test.observationscores.put("Cold", new HashMap<>());
        test.observationscores.get("Cold").put("one", 7.0);
        test.observationscores.get("Cold").put("two", 2.0);
        test.observationscores.get("Cold").put("three", 1.0);
        test.observationscores.put("Hot", new HashMap<>());
        test.observationscores.get("Hot").put("one", 2.0);
        test.observationscores.get("Hot").put("two", 3.0);
        test.observationscores.get("Hot").put("three", 5.0);
        String line = "two three two one";
        System.out.println(test.viterbiDecodingPath(line));
        String[] correctpath1 = {"Hot", "Hot", "Hot", "Cold"};
        test.accuracy(test.path, correctpath1);

        //testing one sentence
        System.out.println("test 1");
        Viterbi test1 = new Viterbi("PS5/simple-train-tags.txt", "PS5/simple-train-sentences.txt");
        test1.Train();
        String testline = "he walks the dog .";
        System.out.println(testline);
        System.out.println(test1.viterbiDecodingPath(testline));

        //testing and training simple text
        System.out.println("simple test 1");
        Viterbi test2 = new Viterbi("PS5/simple-train-tags.txt", "PS5/simple-train-sentences.txt");
        test2.viterbiDecoding("PS5/simple-test-tags.txt", "PS5/simple-test-sentences.txt");

        //testing and training with the same text file
        System.out.println("simple test 2");
        Viterbi test3 = new Viterbi("PS5/simple-train-tags.txt", "PS5/simple-train-sentences.txt");
        test3.viterbiDecoding("PS5/simple-train-tags.txt", "PS5/simple-train-sentences.txt");

        // training and testing brown text
        System.out.println("brown test");
        Viterbi test4 = new Viterbi("PS5/brown-train-tags.txt", "PS5/brown-train-sentences.txt");
        test4.viterbiDecoding("PS5/brown-test-tags.txt", "PS5/brown-test-sentences.txt");

        // testing console method
        Viterbi test5 = new Viterbi("PS5/brown-train-tags.txt", "PS5/brown-train-sentences.txt");
        System.out.println('\n' + "Test 5: Console Test" + '\n' + "Test a sentence below: ");
        test5.consoleTest();
    }
}
