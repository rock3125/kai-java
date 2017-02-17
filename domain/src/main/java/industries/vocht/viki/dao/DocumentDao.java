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

package industries.vocht.viki.dao;

import industries.vocht.viki.IDatabase;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.similar.SimilarDocument;
import industries.vocht.viki.model.similar.SimilarDocumentSet;
import industries.vocht.viki.model.summary.SummarisationSet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class DocumentDao {

    private IDatabase db;

    public DocumentDao(IDatabase db) {
        this.db = db;
    }

    /**
     * create a new document in the document system
     * @param organisation_id the id of the organisation
     * @param document the document
     * @return the updated document
     */
    public Document create(UUID organisation_id, Document document) throws IOException {
        return db.createDocument(organisation_id, document);
    }

    /**
     * save a document image into the document store
     * @param organisation_id the organisation of the document
     * @param url the url of the document
     * @param png_data image data for the PNG of the document
     */
    public void saveDocumentImage(UUID organisation_id, String url, byte[] png_data) {
        db.saveDocumentImage(organisation_id, url, png_data);
    }

    /**
     * remove a document image from the document store
     * @param organisation_id the organisation of the document
     * @param url the url of the document
     */
    public void removeDocumentImage(UUID organisation_id, String url) {
        db.removeDocumentImage(organisation_id, url);
    }

    /**
     * retrieve a document's PNG image by org/url
     * @param organisation_id the organisation's id
     * @param url the url of the document
     * @return null, or a valid PNG image (hopefully)
     */
    public byte[] getDocumentImage(UUID organisation_id, String url) {
        return db.getDocumentImage(organisation_id, url);
    }

    /**
     * read a document from the document system
     * @param organisation_id the id of the organisation
     * @param url the url of the document to fetch
     * @return the document
     */
    public Document read(UUID organisation_id, String url) throws IOException {
        return db.readDocument(organisation_id, url);
    }

    /**
     * update an existing document in the document system
     * @param organisation_id the id of the organisation
     * @param document the document
     */
    public void update(UUID organisation_id, Document document) throws IOException {
        db.updateDocument(organisation_id, document);
    }

    /**
     * delete an existing document from the system
     * @param organisation_id the id of the organisation
     * @param url the url of the item/document to delete
     */
    public void delete(UUID organisation_id, String url) {
        db.deleteDocument(organisation_id, url);
    }

    /**
     * return a list of documents
     * @param organisation_id the organisation id
     * @param prevUrl the previous url, null initially
     * @param limit how many to return
     * @return a list of documents
     */
    public List<Document> getDocumentList(UUID organisation_id, String prevUrl, int limit ) throws IOException {
        return db.getDocumentList(organisation_id, prevUrl, limit);
    }

    /**
     * return a list of document urls
     * @param organisation_id the organisation id
     * @param prevUrl the previous url, null initially
     * @param limit how many to return
     * @return a list of document urls
     */
    public List<String> getDocumentUrlList(UUID organisation_id, String prevUrl, int limit ) {
        return db.getDocumentUrlList(organisation_id, prevUrl, limit);
    }

    /**
     * return a list of document binary items
     * @param organisation_id the organisation id
     * @param url the url of the document
     * @return a map of metadata items with byte[] attached
     */
    public Map<String, byte[]> getDocumentParseTreeMap(UUID organisation_id, String url) {
        return db.getDocumentParseTreeMap(organisation_id, url);
    }

    /**
     * save a map of document binary items for a given url
     * @param organisation_id the organisation id
     * @param url the url of the document
     * @param map the map of items to save
     */
    public void saveDocumentParseTreeMap(UUID organisation_id, String url, Map<String, byte[]> map) {
        db.saveDocumentParseTreeMap(organisation_id, url, map);
    }

    /**
     * save the binary of a document
     * @param organisation_id the organisation id
     * @param url the url of the document
     * @param data the data array of the document
     */
    public void updateDocumentBinary(UUID organisation_id, String url, byte[] data) {
        db.uploadDocumentBinary(organisation_id, url, data);
    }

    /**
     * get the binary of a document
     * @param organisation_id the organisation id
     * @param url the url of the document
     * @return the byte[] of the document or null
     */
    public byte[] getDocumentBinary(UUID organisation_id, String url) {
        return db.getDocumentBinary(organisation_id, url);
    }

    /**
     * write a document history to db
     * @param organisation_id the organisation's id
     * @param url the url of the document
     * @param compressedVector the histogram vector
     */
    public void saveDocumentHistogram( UUID organisation_id, String url, CompressedVector compressedVector ) throws IOException {
        db.saveDocumentHistogram( organisation_id, url, compressedVector );
    }

    /**
     * reload a compressed vector of a document history
     * @param organisation_id the organisation's id
     * @param url the url of the document
     * @return a compressed vector or null if dne
     */
    public CompressedVector loadDocumentHistogram(UUID organisation_id, String url) throws IOException {
        return db.loadDocumentHistogram(organisation_id, url);
    }

    // removed along with text rank (using way too much ram)
//    /**
//     * summarization of a set of words representing a larger set of words (TextRank)
//     * @param organisation_id the organisation's id
//     * @param url the url of the document
//     * @param fragmentSet the set of fragments / words representing the summarization
//     */
//    public void saveDocumentSummarizationWordSet(UUID organisation_id, String url, SummarisationSet fragmentSet) throws IOException {
//        db.saveDocumentSummarizationWordSet( organisation_id, url, fragmentSet );
//    }
//
//    /**
//     * load the summarisation word set
//     * @param organisation_id the organisation's id
//     * @param url the url of the document
//     * @return the summarisation set or null
//     */
//    public SummarisationSet loadDocumentSummarizationWordSet(UUID organisation_id, String url) throws IOException {
//        return db.loadDocumentSummarizationWordSet( organisation_id, url );
//    }

    /**
     * summarisation at a "sentence" level (i.e. top sentences representing the document)
     * @param organisation_id the organisation's id
     * @param url the url of the document
     * @param sentence the sentence to summarize
     */
    public void saveDocumentSummarizationSentenceSet(UUID organisation_id, String url, Sentence sentence) throws IOException {
        db.saveDocumentSummarizationSentenceSet( organisation_id, url, sentence );
    }

    /**
     * re-load a sentence level summarisation item
     * @param organisation_id the organisation's id
     * @param url the url of the document
     * @return the summarisation sentence or null
     */
    public Sentence loadDocumentSummarizationSentenceSet(UUID organisation_id, String url) throws IOException {
        return db.loadDocumentSummarizationSentenceSet( organisation_id, url );
    }

    /**
     * save a list of similar documents - does it all ways each document in the list is similar
     * @param organisation_id the organisation
     * @param similarDocumentList list of similar items with values
     */
    public void saveDocumentSimilarityList( UUID organisation_id, List<SimilarDocument> similarDocumentList ) {
        db.saveDocumentSimilarityMap(organisation_id, similarDocumentList);
    }


    /**
     * get a set of documents similar to url
     * @param organisation_id the organisation
     * @param url the url to get similar documents for
     * @return null or a set of similar documents
     */
    public List<SimilarDocument> loadSimilarDocumentList( UUID organisation_id, String url ) {
        return db.loadSimilarDocuments(organisation_id, url);
    }

    /**
     * get a set of documents for all urls
     * @param organisation_id the organisation
     * @return null or a set of similar documents
     */
    public List<SimilarDocumentSet> loadSimilarDocumentList(UUID organisation_id ) {
        return db.loadSimilarDocuments(organisation_id);
    }

    /**
     * return the authors (if known) for the set of URLs
     * @param organisation_id the organisation owner of the URLs
     * @param urlList a list of urls
     * @return a map going from URL to author
     */
    public Map<String, byte[]> getAuthorsForUrlList(UUID organisation_id, List<String> urlList) {
        return db.getAuthorsForUrlList(organisation_id, urlList);
    }


}

