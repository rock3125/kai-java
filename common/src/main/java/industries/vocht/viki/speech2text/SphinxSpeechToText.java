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

package industries.vocht.viki.speech2text;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.result.WordResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CMU Sphinx 4 model based speech to text (GMM)
 *
 * see unit test: SpeechToTextTest.java for a sample demo
 *
 */
@Component
public class SphinxSpeechToText {

    private final Logger logger = LoggerFactory.getLogger(SphinxSpeechToText.class);


    @Value("${cmu.accoustic.model.path:/opt/kai/data/cmu_models/model_1}")
    private String accousticModelPath;

    @Value("${cmu.cmud.dict.file:/opt/kai/data/cmu_models/model_1/combined_vtl.dic}")
    private String dictionaryFile;

    @Value("${cmu.language.model.file:/opt/kai/data/cmu_models/model_1/combined_vtl.lm.bin}")
    private String languageModelFile;

    private StreamSpeechRecognizer recognizer;


    public SphinxSpeechToText() {
    }

    // setup the recognizer
    public void init() throws IOException {

        String conFile = System.getProperty("java.util.logging.config.file");
        if (conFile == null) {
            System.setProperty("java.util.logging.config.file", "ignoreAllSphinx4LoggingOutput");
        }

        logger.info("setting up speech-to-text system");
        Configuration configuration = new Configuration();
        // Load model from the jar
        configuration.setAcousticModelPath(accousticModelPath);
        configuration.setLanguageModelPath(languageModelFile);
        configuration.setDictionaryPath(dictionaryFile);
        recognizer = new StreamSpeechRecognizer(configuration);
        logger.info("speech-to-text system setup done.");
    }

    /**
     * Convert a wav input stream to text
     * @param waveStream the stream in
     * @return a converted object
     */
    public STTResult wavToText(InputStream waveStream) throws IOException {
        recognizer.startRecognition(waveStream);
        SpeechResult result;

        List<STTWord> wordList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        while ((result = recognizer.getResult()) != null) {
            sb.append(result.getHypothesis());
            for (WordResult r : result.getWords()) {
                wordList.add(new STTWord(r.getWord().getSpelling(),
                        r.getTimeFrame().getStart(), r.getTimeFrame().getEnd(), r.getScore()));
            }
        }
        recognizer.stopRecognition();
        return new STTResult(sb.toString(), wordList);
    }




}

