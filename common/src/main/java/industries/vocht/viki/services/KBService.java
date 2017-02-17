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

package industries.vocht.viki.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.IDao;
import industries.vocht.viki.indexer.IndexerFindCommon;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.parser.NLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 4/01/17.
 *
 * kb service layer helper items
 *
 */
public class KBService {

    private final Logger logger = LoggerFactory.getLogger(KBService.class);

    @Autowired
    private IDao dao;

    @Autowired
    private NLParser parser;

    /**
     * return a paginated list of items according to the find criteria
     * @param organisation_id the organisation owner
     * @param json_field the field to look for in json items
     * @param type the type of entity to look for
     * @param query_str the query of the search
     * @param prev the previous GUID (or null)
     * @param page_size page size
     * @return a list with the items (or empty list)
     */
    public List<KBEntry> findPaginated(UUID organisation_id, String json_field, String type, String query_str,
                                       UUID prev, int page_size ) throws IOException {
        IndexerFindCommon simple_index_finder = new IndexerFindCommon();
        Undesirables undesirables = new Undesirables();
        List<Token> tokenList = new ArrayList<>();
        List<Sentence> sentenceList = parser.parseText(query_str);
        if ( sentenceList != null ) {
            for (Sentence sentence : sentenceList) {
                for (Token token : sentence.getTokenList()) {
                    if (!undesirables.isUndesirable(token.getText())) {
                        tokenList.add(token);
                    }
                }
            }
        }
        List<String> entityUrlList = simple_index_finder.readIndexesWithTokens(dao, organisation_id,
                tokenList, 0, "kb:" + type + ":" + json_field);
        List<KBEntry> entryList = new ArrayList<>();
        if (entityUrlList != null) {
            String prevStr = prev != null ? prev.toString() : null;
            boolean startNow = (prev == null);
            int counter = page_size;
            for (String guidStr : entityUrlList) {
                if (prevStr != null && prevStr.equals(guidStr)) {
                    startNow = true;
                } else if (startNow && counter > 0) {
                    UUID id = UUID.fromString(guidStr);
                    KBEntry entry = dao.getKBDao().getKBEntry(organisation_id, type, id);
                    if ( entry != null ) {
                        entryList.add(entry);
                    }
                    counter--;
                } else if (counter <= 0) {
                    break;
                }
            }
        }
        return entryList;
    }


    /**
     * remove any previous indexes of an entity
     * @param entry the entity to index
     */
    public void unindex_entity(KBEntry entry) {
        if (entry != null && entry.getId() != null && entry.getOrganisation_id() != null) {
            // get what it was in the past (i.e. previous version)
            KBEntry previous_entry = dao.getKBDao().getKBEntry(entry.getOrganisation_id(), entry.getType(), entry.getId());
            if ( previous_entry != null && previous_entry.getOrigin() != null ) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> map = mapper.readValue(entry.getJson_data(), new TypeReference<Map<String, Object>>() {});
                    if (map != null) {
                        for (String key : map.keySet()) {
                            String metadata = "kb:" + entry.getType() + ":" + key;
                            dao.getIndexDao().removeIndex(entry.getOrganisation_id(), entry.getId().toString(), metadata);
                        }
                    }
                } catch (IOException ex) {
                    logger.error("unindex_entity: " + ex.getMessage());
                }
            }
        }
    }


    /**
     * make sure a knowledge-base entity can be found again in the future by indexing it
     * @param entry the entity to index (its json parts will be indexed)
     * @param acl_hash security access field for this index
     */
    public void index_entity(KBEntry entry, int acl_hash) {
        if (entry != null && entry.getId() != null && entry.getOrganisation_id() != null && entry.getJson_data() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Undesirables undesirables = new Undesirables();
                Map<String, Object> map = mapper.readValue(entry.getJson_data(), new TypeReference<Map<String, Object>>() {});
                if (map != null) {
                    for (String key : map.keySet()) {
                        Object value = map.get(key);
                        String metadata = "kb:" + entry.getType() + ":" + key;
                        if (value instanceof String) {
                            indexText(entry.getOrganisation_id(), (String)value, undesirables, entry.getId().toString(), metadata, acl_hash);
                        } else if (value instanceof List) {
                            for (Object item : (List)value) {
                                if (item instanceof String) {
                                    indexText(entry.getOrganisation_id(), (String)item, undesirables, entry.getId().toString(), metadata, acl_hash);
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                logger.error("index_entity: " + ex.getMessage());
            }
        }
    }

    /**
     * helper: index a text string
     * @param organisation_id the owner
     * @param text the text to index
     * @param undesirables undesirable detection
     * @param url the url of the owner
     * @param metadata metadata field for indexing
     * @param acl_hash security acl
     */
    private void indexText(UUID organisation_id, String text, Undesirables undesirables, String url, String metadata, int acl_hash) throws IOException {
        List<Sentence> sentenceList = parser.parseText(text);
        if (sentenceList != null) {
            int offset = 0;
            for (Sentence sentence : sentenceList) {
                for (Token token : sentence.getTokenList()) {
                    if (!undesirables.isUndesirable(token.getText())) {
                        Index index = new Index(url, token.getText(), 0, null,
                                token.getSynid(), metadata, acl_hash, 0, token.getPennType().toString(), offset);
                        dao.getIndexDao().addIndex(organisation_id, index);

                        // spaces in the token (multi word index)
                        String[] parts = token.getText().split(" ");
                        if ( parts.length > 1) {
                            for (String part : parts) {
                                if (part.trim().length() > 0) {
                                    Index index2 = new Index(url, part, 0, token.getText(),
                                            token.getSynid(), metadata, acl_hash, 0, token.getPennType().toString(), offset);
                                    dao.getIndexDao().addIndex(organisation_id, index2);
                                }
                            }
                        }

                    }
                    offset += 1;
                }
            }
            dao.getIndexDao().flushIndexes(); // saving done
        }
    }


}

