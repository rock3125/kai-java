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

package industries.vocht.viki.wordcloud;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.PixelBoundryBackground;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;

import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.utility.SentenceFromBinary;
import net.didion.jwnl.data.Exc;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static ucar.nc2.grib.GribResourceReader.getInputStream;

/**
 * Created by peter on 15/12/16.
 *
 * create an image word cloud for a document:  https://github.com/kennycason/kumo
 *
 */
public class DocumentWordCloudToImage {

    public DocumentWordCloudToImage() {
    }

    /**
     * Convert the text in a document body to a PNG image and return its byte[]
     * @param binaryDocument the binary KAI document whose body to process
     * @param minWordLength the min word size allowed (default 4)
     * @param numWords the number of words to include (default 300)
     * @param padding the padding between words
     * @param width the width of the image generated
     * @param height the height of the image generated
     * @param cloudBackgroundImage the image to use in the background (can be null)
     * @return a byte[] of a PNG image, or null
     */
    public byte[] doc2FreqPng(Map<String, byte[]> binaryDocument,
                              int minWordLength, int numWords, int padding,
                              int width, int height,
                              String cloudBackgroundImage) throws IOException {

        if (binaryDocument != null && binaryDocument.containsKey(Document.META_BODY)) {
            byte[] dataBody = binaryDocument.get(Document.META_BODY);
            SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
            List<Sentence> sentenceList = sentenceFromBinary.convert(dataBody);
            if (sentenceList != null) {

                // convert the document to a text string for rendering
                StringBuilder sb = new StringBuilder();
                for (Sentence sentence : sentenceList) {
                    sb.append(sentence.toString());
                    sb.append("\n");
                }
                return doc2FreqPng(sb.toString(), minWordLength, numWords, padding,
                        width, height, cloudBackgroundImage);
            }
        }
        return null;
    }

    /**
     * Convert the text in a document body to a PNG image and return its byte[]
     * @param text the text to render
     * @param minWordLength the min word size allowed (default 4)
     * @param numWords the number of words to include (default 200)
     * @param padding the padding between words
     * @param width the width of the image generated
     * @param height the height of the image generated
     * @param cloudBackgroundImage the image to use in the background (can be null)
     * @return a byte[] of a PNG image, or null
     */
    public byte[] doc2FreqPng(String text,
                              int minWordLength, int numWords, int padding,
                              int width, int height, String cloudBackgroundImage) throws IOException {

        if (text != null && text.length() > 0) {
            // setup: frequency analyse the words
            FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
            frequencyAnalyzer.setWordFrequenciesToReturn(numWords);
            frequencyAnalyzer.setMinWordLength(minWordLength);
            Undesirables undesirables = new Undesirables();
            frequencyAnalyzer.setStopWords(undesirables.getAsList());

            // feed the words into the system and set dimensions
            InputStream stringStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(stringStream);
            Dimension dimension = new Dimension(width, height);
            WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
            wordCloud.setBackgroundColor(Color.WHITE);
            wordCloud.setPadding(padding);
            if ( cloudBackgroundImage != null && cloudBackgroundImage.length() > 0 && !cloudBackgroundImage.equals("null") ) {
                InputStream bg_image = getInputStream(cloudBackgroundImage);
                wordCloud.setBackground(new PixelBoundryBackground(bg_image));
            } else {
                wordCloud.setBackground(new RectangleBackground(dimension));
            }
            Color[] colors = new Color[] { Color.RED, Color.BLACK };
            wordCloud.setColorPalette(new ColorPalette(colors));
            wordCloud.build(wordFrequencies);

            // write the solution
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            wordCloud.writeToStreamAsPNG(outputStream);
            return outputStream.toByteArray(); // png

//            // convert it to a JPEG or leave it as a PNG?
//            if (jpeg) {
//                ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
//                ImageIO.write(ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray())), "JPEG", jpegStream);
//                return jpegStream.toByteArray();
//            } else {
//                return outputStream.toByteArray(); // png
//            }
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        DocumentWordCloudToImage dwc = new DocumentWordCloudToImage();
        String text = "Mr. Spaceship\n" +
                "\n" +
                "By  Philip K. Dick\n" +
                "\n" +
                "Kramer leaned back. “You can see the situation. How can we deal with a factor like this? The perfect variable.”\n" +
                "“Perfect? Prediction should still be possible. A living thing still acts from necessity, the same as inanimate material. But the cause-effect chain is more subtle; there are more factors to be considered. The difference is quantitative, I think. The reaction of the living organism parallels natural causation, but with greater complexity.”\n" +
                "Gross and Kramer looked up at the board plates, suspended on the wall, still dripping, the images hardening into place. Kramer traced a line with his pencil.\n" +
                "“See that? It’s a pseudopodium. They’re alive, and so far, a weapon we can’t beat. No mechanical system can compete with that, simple or intricate. We’ll have to scrap the Johnson Control and find something else.”\n" +
                "“Meanwhile the war continues as it is. Stalemate. Checkmate. They can’t get to us, and we can’t get through their living minefield.”\n" +
                "Kramer nodded. “It’s a perfect defense, for them. But there still might be one answer.”\n" +
                "“What’s that?”\n";
        byte[] png = dwc.doc2FreqPng(text, 4, 200, 2, 640, 480,
                "data/wordcloud_backgrounds/kai_bg_640_480.png");
        if (png != null) {
            FileUtils.writeByteArrayToFile(new File("/home/peter/test.png"), png);
        }
    }

}


