/*
 * Copyright (c) 2016 by Peter de Vocht
 *
 * All rights reserved. No part of this publication may be reproduced, distributed, or
 * transmitted in any form or by any means, including photocopying, recording, or other
 * electronic or mechanical methods, without the prior written permission of the publisher,
 * except in the case of brief quotations embodied in critical reviews and certain other
 * noncommercial uses permitted by copyright law.
 *
 */

package industries.vocht.viki.nnet_wsd_create;

import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.TokenizerConstants;
import industries.vocht.viki.parser.NLParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Created by peter on 25/05/16.
 *
 * create training data from a large text set using Peter's lexicon
 *
 *
 */
@Component
public class NNetStep1 {

    public NNetStep1() {
    }

    /**
     * load a set of samples from a training set using Peter's lexicon
     * and write any instace of the training words to their files, pre-collecting
     * windows of training data, for both labelled and un-labelled instances
     *
     * @param trainingSetFilename the training set line by line sample set
     * @param output_directories where to write the resulting files
     * @param maxFileSizeInBytes an optional limit (in bytes) to the generated file size (if <=0, ignored)
     * @param windowSize the size of the windows, try 25
     * @param wordArray a list of focus words (all if null)
     */
    public void create( String trainingSetFilename, String output_directories,
                        long maxFileSizeInBytes, int windowSize, String... wordArray )
            throws IOException, InterruptedException {

        System.out.println("step 1: loading pre-parsed test set " + trainingSetFilename);

        Undesirables undesirables = new Undesirables();
        if ( !output_directories.endsWith("/") ) {
            output_directories += "/";
        }

        String nnetUnlabelledDirectory = output_directories + "unlabelled/";
        new File(nnetUnlabelledDirectory).mkdirs();

        // get the ambiguous map sets - from Peter's lexicon
        Map<String, WordnetAmbiguousSet> map = WordnetAmbiguousSet.readFromFile();

        // setup what words to look for
        HashSet<String> focus = new HashSet<>();
        if ( wordArray == null || wordArray.length == 0 ) {
            focus.addAll( map.keySet() ); // all words?
        } else { // or parameters?
            for ( String word : wordArray ) {
                if ( !map.containsKey(word) ) {
                    throw new IOException("unknown focus word \"" + word + "\"");
                }
                focus.add(word);
            }
        }

        // remove words that have already been processed
        List<String> toRemove = new ArrayList<>();
        for ( String word : focus ) {
            if ( map.get(word).getWordPlural() == null || !map.get(word).getWordPlural().equals(word) ) {
                if ( new File(outputFilename(nnetUnlabelledDirectory, word)).exists() ) {
                    toRemove.add(word);
                    if ( map.get(word).getWordPlural() != null ) {
                        toRemove.add(map.get(word).getWordPlural());
                    }
                }
            }
        }
        for ( String str : toRemove ) {
            focus.remove(str);
        }

        // do we have anything left to process?
        if (focus.size() == 0) {
            System.out.println("step 1: all items already processed, skipping step 1.");
            return;
        }

        Map<String, PrintWriter> openFileSet = new HashMap<>();
        Map<String, Long> openFileSize = new HashMap<>();

        int lineCounter = 0;

        int minValidSize = (windowSize + windowSize / 2);

        // open the wiki set (plain text) for reading
        try ( BufferedReader br = new BufferedReader(new FileReader(trainingSetFilename) ) ) {

            // for each line in the wiki training set
            for ( String line; (line = br.readLine()) != null; ) {

                // tokenize the words
                List<Token> tokenList = process(line);
                if ( tokenList == null ) {
                    continue;
                }

                // for each token
                int size = tokenList.size();
                for (int i = 0; i < size; i++) {

                    // is this token / word one of the ambiguous words from Peter's lexicon?
                    Token token = tokenList.get(i);
                    String part = token.getText();
                    String pennType = token.getPennType().toString();
                    if ( focus.contains(part.toLowerCase()) && pennType.startsWith("NN") ) {
                        // get the set
                        WordnetAmbiguousSet set = map.get(part.toLowerCase());

                        // construct a window left and right of the word
                        int left = i - windowSize;
                        if ( left < 0 ) left = 0;
                        for ( int j = left; j < i; j++ ) {
                            String punc = tokenList.get(j).getText();
                            if ( punc.equals(".") ) {
                                left = j + 1;
                            }
                        }
                        int right = i + windowSize;
                        if ( right + 1 >= size ) {
                            right = size - 1;
                        }

                        // 1.5 window size at least
                        if ( Math.abs(left - right) >= minValidSize ) {

                            // get the singular version
                            String wordStr = part.toLowerCase();
                            String plural = set.getWordPlural();
                            if ( plural != null && plural.equals(wordStr) ) {
                                wordStr = set.getWord();
                            }
                            Long fileSize = openFileSize.get(wordStr);
                            PrintWriter writer = openFileSet.get(wordStr);
                            if ( writer == null ) {
                                writer = new PrintWriter(outputFilename(nnetUnlabelledDirectory, wordStr));
                                openFileSet.put( wordStr, writer );
                                fileSize = 0L;
                                openFileSize.put( wordStr, fileSize );
                            }

                            // a hit for each syn is counted, we don't want any crossovers between synsets
                            int count = 0;
                            StringBuilder sb = new StringBuilder();
                            int j;
                            for (j = left; j <= right; j++) {
                                token = tokenList.get(j);
                                if (token.getType() == TokenizerConstants.Type.Text) {
                                    String part_j = token.getText().toLowerCase();
                                    if (!undesirables.isUndesirable(part_j)) {
                                        count = count + 1;
                                        if (sb.length() > 0) {
                                            sb.append(",");
                                        }
                                        sb.append(part_j);
                                    } // if not undesirable
                                } else if ( token.getText().equals(".") ) {
                                    break; // stop collecting at end of sentence events
                                }
                            }
                            // a valid piece of text to collect?
                            if ( count >= windowSize && (maxFileSizeInBytes <= 0 || fileSize < maxFileSizeInBytes) ) {
                                sb.append("\n");
                                writer.write(sb.toString());
                                fileSize = fileSize + sb.length();
                                openFileSize.put(wordStr, fileSize);
                            }

                        } // if window size big enough

                    } // if is focus word

                } // for each word

                lineCounter = lineCounter + 1;

                // display periodic progress
                if ( lineCounter % 100_000 == 0 ) {
                    System.out.println("   lines processed: " + lineCounter);
                } // if lineCounter hit

            } // for each line

        } // io try

        // close all open files
        for ( PrintWriter writer : openFileSet.values() ) {
            writer.close();
        }

    }

    // convert a line into parsed tokens
    private List<Token> process(String line) {
        if ( line != null && line.length() > 0 ) {
            List<Token> tokenList = new ArrayList<>();
            String[] parts = line.split(" ");
            for ( String part : parts ) {
                String[] items = part.split(":");
                if ( items.length == 2 && !items[0].equals("_") ) {
                    tokenList.add( new Token(items[0], PennType.fromString(items[1]) ) );
                }
            }
            return tokenList;
        }
        return null;
    }

    private String outputFilename( String nnetUnlabelledDirectory, String word ) {
        return nnetUnlabelledDirectory + word + "-trainingset.csv";
    }

}


