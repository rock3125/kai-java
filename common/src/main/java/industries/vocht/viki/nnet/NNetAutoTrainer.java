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

package industries.vocht.viki.nnet;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.lexicon.AmbiguousLexicon;
import industries.vocht.viki.lexicon.LexiconSynset;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.nnet.NNetTrainingSample;
import industries.vocht.viki.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 27/05/16.
 *
 * learn automatically from documents and store the
 * new learnings to generate new neural networks
 *
 */
@Component
public class NNetAutoTrainer {

    private final Logger logger = LoggerFactory.getLogger(NNetAutoTrainer.class);

    @Autowired
    private AmbiguousLexicon ambiguousLexicon;

    @Autowired
    private Undesirables undesirables;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private IDao dao;

    @Autowired
    private ClusterInfrastructure clusterInfrastructure;

    // the extend of the window for learning
    @Value("${nnet.window.size:25}")
    private int windowSize;

    // the session ids for the organisations submitting documents - their system user accounts
    private Map<UUID, String> organisationSessionID;

    // how many words we need from the windowSize x 2 for a valid learning excercise to happen
    @Value("${nnet.min.training.size:15}")
    private int minTrainingSize;

    // how many training samples to collect maximum for a learning experience (0 = infinite)
    // careful - these must all fit in someones memory when it comes to executing
    @Value("${nnet.max.training.collection.size:5000}")
    private long maxTrainingCollectionSize;

    // how many training samples we need to re-train a neural network, must be less than $nnet.max.training.collection.size
    @Value("${nnet.training.samples.before.retrain:1000}")
    private long trainingSampleBeforeRetrain;


    public NNetAutoTrainer() {
    }

    // setup the round robin for neural network trainers
    public void init() {
        this.organisationSessionID = new HashMap<>();
    }

    /**
     * take the given document and see if there are any items worth learning from
     * using our ambiguous lexicon
     * @param organisation_id the organisation this is happening for
     * @param documentMap the document map / contents
     */
    public void learnFrom(UUID organisation_id, String url, Map<String, List<Sentence>> documentMap ) {
        logger.debug("learnFrom:" + url);
        // retrieve the words to look for in the document
        HashSet<String> wordFocusSet = ambiguousLexicon.getWordFocusSet();

        for ( String metadata : documentMap.keySet() ) {
            List<Sentence> sentenceList = documentMap.get(metadata);
            List<Token> tokenList = Sentence.sentenceListToTokens(sentenceList);

            // scan for the words
            int size = tokenList.size();
            for ( int i = 0; i < size; i++ ) {

                Token token = tokenList.get(i);
                // nouns only
                if ( token.getPennType() == PennType.NN || token.getPennType() == PennType.NNS ) {
                    String wordStr = token.getText().toLowerCase();
                    if ( wordFocusSet.contains(wordStr) ) {

                        // enough room for a good training set?
                        if ( i >= windowSize || i + windowSize < size ) {
                            // gather the tokens
                            int left = Math.max(0, i - windowSize);
                            int right = Math.min(i + windowSize, size -1 );
                            List<Token> tokenSublist = tokenList.subList(left, right);
                            List<LexiconSynset> synsetList = ambiguousLexicon.getSynset(wordStr);
                            int synsetId = validForLearning( tokenSublist, synsetList );
                            if ( synsetId >= 0 ) {
                                try {
                                    storeForLearning(organisation_id, metadata, url, ambiguousLexicon.getSingular(wordStr), synsetId,
                                            synsetList.size(), tokenSublist);
                                } catch (ApplicationException | IOException ex ) {
                                    logger.error("", ex);
                                }
                            }
                        }

                    } // if word is ambigous

                } // if word is noun

            } // for each token

        } // for each metadata item

    }

    /**
     * investigate if the token-list is good enough as a training set for the given synset-list
     * this is when:
     * (1) it is exclusive to one of the training sets
     * (2) it is mutually exclusive to the other training sets
     * @param tokenList the list of tokens to check
     * @param synsetList the synset-lists to verify with
     * @return 0..x for the synset if its good to go, or -1 if it fails to meet the criteria
     */
    private int validForLearning(List<Token> tokenList, List<LexiconSynset> synsetList ) {
        if ( tokenList != null && synsetList != null && synsetList.size() > 1 ) {
            // a hit for each syn is counted, we don't want any crossovers between synsets
            int synsetSize = synsetList.size();
            int[] setHits = new int[synsetSize];
            for ( Token token : tokenList ) {
                // ignore #s and NNP(S)
                if ( token.getPennType() == PennType.CD ||  token.getPennType() == PennType.NNP || token.getPennType() == PennType.NNPS ) {
                    continue;
                }

                String part_j = token.getText().toLowerCase();
                if ( !undesirables.isUndesirable(part_j) ) {
                    // synset hit count
                    for (int k = 0; k < synsetSize; k++) {
                        if (synsetList.get(k).getRelationshipSet().contains(part_j)) {
                            setHits[k] = setHits[k] + 1;
                        }
                    }
                } // if not undesirable

            } // for each token

            // see if this item is suitable as a training item
            // if it has no crossovers and a count
            for ( int k = 0; k < synsetSize; k++ ) {

                // does it have any hits on the OTHER synsets relationships?
                boolean hasCount = false;
                for (int k1 = 0; k1 < synsetSize && !hasCount; k1++) {
                    if (k1 != k) {
                        if (setHits[k1] > 0) {
                            hasCount = true;
                        }
                    }
                }
                // nope - if this is a unique hit and no others - we're in!
                if ( setHits[k] > 0 && !hasCount ) {
                    return k;
                }

            } // for each synset
        }
        return -1;
    }

    /**
     * store a learning example
     * @param organisation_id the organisation to store it for
     * @param wordStr the word that is "learned"
     * @param synsetId the synset-id applicable for this word
     * @param tokenList the list of tokens that provides information on the learning
     */
    private void storeForLearning( UUID organisation_id, String metadata, String url, String wordStr, int synsetId, int synsetSize,
                                   List<Token> tokenList ) throws ApplicationException, IOException {
        if ( organisation_id != null && wordStr != null && synsetId >= 0 && tokenList != null ) {

            logger.debug("storeForLearning:" + url + ",\"" + wordStr + ":" + synsetId + "\"");

            // collect the words to learn
            Map<String, Integer> learningSet = new HashMap<>();
            for ( Token token : tokenList ) {
                // ignore #s and NNP(S)
                if ( token.getPennType() == PennType.CD ||  token.getPennType() == PennType.NNP ||
                     token.getPennType() == PennType.NNPS ) {
                    continue;
                }
                String part_j = token.getText().toLowerCase();
                if ( !undesirables.isUndesirable(part_j) ) {
                    increment( part_j, learningSet );
                } // if not undesirable
            }

            // enough tokens to learn from?
            if ( learningSet.size() >= minTrainingSize ) {

                // hazelcast keeps track of the token counts for the synsets
                Long trainingSampleCount = hazelcast.getWordCountMap(organisation_id, metadata).get(wordStr + ":" + synsetId);
                if ( trainingSampleCount == null ) {
                    trainingSampleCount = 1L;
                }

                // check limits?
                if ( maxTrainingCollectionSize > 0 && maxTrainingCollectionSize < trainingSampleCount ) {

                    // store it in the database
                    dao.getNNetDao().addNNetTrainingSample(organisation_id, wordStr, synsetId,
                            new NNetTrainingSample(UUID.randomUUID(), learningSet)) ;

                    // update hazelcast count for this synset
                    hazelcast.addToWordCount(organisation_id, metadata, wordStr + ":" + synsetId, 1);

                } else if ( maxTrainingCollectionSize <= 0 ) {

                    // store it in the database
                    dao.getNNetDao().addNNetTrainingSample(organisation_id, wordStr, synsetId,
                            new NNetTrainingSample(UUID.randomUUID(), learningSet)) ;

                    // update hazelcast count for this synset
                    hazelcast.addToWordCount(organisation_id, metadata, wordStr + ":" + synsetId, 1);

                } else {
                    // we can't proceed - we've got too many training samples for this syn already
                    logger.debug("storeForLearning:" + url + ",\"" + wordStr + ":" + synsetId + "\" has the maximum allowed samples (" + maxTrainingCollectionSize + ")");
                }

                // see when we last trained this set
                Long lastTraining = hazelcast.getWordCountMap(organisation_id, metadata).get(wordStr + ":last-training");
                if ( lastTraining == null ) {
                    lastTraining = 0L;
                }

                // do we have enough items for all synsets to work?
                boolean readyForTraining = true;
                long totalSamples = 0;
                long nextTraining = lastTraining + trainingSampleBeforeRetrain;
                for ( int i = 0; i < synsetSize; i++ ) {
                    Long trainingCount = hazelcast.getWordCountMap(organisation_id, metadata).get(wordStr + ":" + i);
                    if ( trainingCount == null ) {
                        readyForTraining = false;
                        break;
                    }
                    if ( trainingCount < nextTraining ) {
                        readyForTraining = false;
                        break;
                    }
                    totalSamples = totalSamples + trainingCount;
                }

                // do we have enough data for a full neural network training session?
                if ( readyForTraining ) {
                    logger.info("storeForLearning:" + url + ",\"" + wordStr + "\", start retraining neural network.  Next training at " + nextTraining + " samples if applicable");
                    // setup for the next training to occur later again
                    hazelcast.addToWordCount(organisation_id, metadata, wordStr + ":last-training", nextTraining );

                } else {
                    logger.info("storeForLearning:" + url + ",\"" + wordStr + "\", has " + totalSamples + " samples for " + synsetSize + " syns");
                }

            }

        }
    }


    // helper: update count in map
    private void increment( String str, Map<String, Integer> map ) {
        Integer value = map.get( str );
        if ( value == null ) {
            map.put( str, 1 );
        } else {
            map.put( str, value + 1 );
        }
    }

    /**
     * check this organisation has a valid login for its services account
     * @param organisation_id the organisation to check
     * @throws ApplicationException something is wrong with the system account
     */
    private synchronized String getSessionId( UUID organisation_id ) throws ApplicationException {
        if ( !organisationSessionID.containsKey(organisation_id) ) {
            // login the system user and create a session on their behalf
            UUID session_id = dao.getUserDao().loginSystemUser( organisation_id + UserService.SYSTEM_USER_EMAIL_POSTFIX, UserService.SYSTEM_IP_ADDRESS);
            if ( session_id == null ) {
                throw new ApplicationException("invalid session_id, could not login system user");
            }
            organisationSessionID.put( organisation_id, session_id.toString() );
        }
        return organisationSessionID.get( organisation_id );
    }

}


