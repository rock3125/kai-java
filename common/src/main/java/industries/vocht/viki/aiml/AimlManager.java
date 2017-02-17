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

package industries.vocht.viki.aiml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.IDao;
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.services.KBService;
import industries.vocht.viki.system_stats.DocumentWordCount;
import industries.vocht.viki.system_stats.GeneralStatistics;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 24/07/16.
 *
 * manage the library of AIML files
 *
 * usage: get spring to autoinject you one of these, then use the
 * AimlSession and AimlPatternMatcher to match your input strings to AIML responses (or null if dne)
 *
 *
 */
@Component
public class AimlManager {

    private static Logger logger = LoggerFactory.getLogger(AimlManager.class);

    @Value("${aiml.folder.location:/opt/kai/data/aiml}")
    private String aimlFolderBase;

    // this service layer is required for AIML to be loaded
    @Value("${sl.search.activate:true}")
    private boolean slSearchActive;

    @Autowired
    private IDao dao;

    private boolean loadAim;

    // local schema items to template (key= type + '-' + field)
    private Map<String, String> schemaTemplateSet;

    // date and time formats
    private Format formatDate = new SimpleDateFormat("yyyy-MM-dd");
    private Format formatTime = new SimpleDateFormat("HH:mm:ss");
    private Format formatMonth = new SimpleDateFormat("MMMM");
    private Format formatDay = new SimpleDateFormat("EEEEEEE");
    private String[] dateTimeNames = new String[] {"{day}", "{year}", "{month}", "{time}", "{date}"};

    // the total set of possible conversations
    private Map<String, AimlPattern> nodeSet;

    public AimlManager() {
        this.nodeSet = new HashMap<>();
        this.schemaTemplateSet = new HashMap<>();
    }

    /**
     * load all AIML files located @ aiml folder base
     * @throws IOException wrong
     */
    public void init() throws IOException {
        int numFiles = 0;
        int numTemplates = 0;
        int numPatterns = 0;
        loadAim = slSearchActive;
        if ( loadAim ) {
            logger.info("loading AI/ML files from file db");
            File folder = new File(aimlFolderBase);
            File[] fileList = folder.listFiles();
            if (fileList != null) {
                for (File aimlFile : fileList) {
                    if (aimlFile != null && aimlFile.getAbsolutePath().endsWith(".aiml")) {
                        try {
                            AimlXmlProcessor xmlProcessor = new AimlXmlProcessor(nodeSet);
                            xmlProcessor.processFile(aimlFile.getAbsolutePath());
                            numFiles = numFiles + 1;
                            numTemplates = numTemplates + xmlProcessor.getTemplateCount();
                            numPatterns = numPatterns + xmlProcessor.getPatternCount();
                        } catch (ParserConfigurationException | SAXException ex) {
                            String errMsg = "failed to load AIML file \"" + aimlFile.getAbsolutePath() + "\", xml error: " + ex.getMessage();
                            logger.error(errMsg);
                            throw new IOException(errMsg);
                        }
                    }
                }
            }

            logger.info("setting up user defined AI/ML");
            if ( dao != null ) { // unit tests that don't use spring
                List<Organisation> organisationList = dao.getOrganisationDao().getOrganisationList();
                if ( organisationList != null ) {
                    ObjectMapper mapper = new ObjectMapper();
                    for (Organisation organisation : organisationList) {
                        // read them 10 at a time
                        int pageSize = 10;
                        UUID prev = null;
                        List<KBEntry> entryList = dao.getKBDao().getEntityList(organisation.getId(), "schema", prev, pageSize);
                        do {
                            if ( entryList != null ) {
                                for ( KBEntry entry : entryList ) {
                                    updateAiml(entry);
                                    prev = entry.getId();
                                }
                                entryList = dao.getKBDao().getEntityList(organisation.getId(), "schema", prev, pageSize);
                            }
                        } while (entryList != null && entryList.size() == 10);
                    }
                }
            }

        }
        logger.info("loaded " + numFiles + " AIML files, with " + nodeSet.size() + " root items, " + numTemplates + " templates and " + numPatterns + " patterns.");
    }

    /**
     * evaluate magic parameters inside AimlTemplate items and generate proper text for return to the user
     * @param templateList the list of templates to evaluate
     * @param kbService a reference to the kbService system for kb access
     * @param user the user's name etc
     * @param documentWordCount the stats system access
     */
    public List<String> evaluate(List<AimlTemplate> templateList, KBService kbService, User user,
                                 DocumentWordCount documentWordCount, int pageSize) throws IOException {
        List<String> templateAnswerSet = new ArrayList<>();
        if (templateList != null && user != null && user.getOrganisation_id() != null && kbService != null && documentWordCount != null ) {
            Random r = new Random();
            for (AimlTemplate template : templateList) {
                if (template.getKb_field() == null && template.getTextList() != null && template.getTextList().size() > 0) {
                    // are there more than one responses?  this means there is a set of responses to pick from
                    // pick a random one
                    int index = Math.abs(r.nextInt(template.getTextList().size()));
                    String str = template.getTextList().get(index);
                    if (str.contains("{name}")) {
                        str = str.replace("{name}", user.getFirst_name());
                    }
                    if (str.contains("{email}")) {
                        str = str.replace("{email}", user.getEmail());
                    }
                    if (str.contains("{fullname}")) {
                        str = str.replace("{fullname}", user.getFullname());
                    }
                    if (str.contains("{stats}")) {
                        str = str.replace("{stats}", getStats(user.getOrganisation_id(), documentWordCount));
                    }
                    for (String typeStr : dateTimeNames) {
                        if (str.contains(typeStr)) {
                            str = str.replace(typeStr, getDateTime(typeStr));
                        }
                    }
                    templateAnswerSet.add(str);

                } else if ( template.getKb_field() != null && template.getKb_type() != null &&
                        template.getStarList() != null && template.getStarList().size() > 0) {  // we use the KB system

                    Tokenizer tokenizer = new Tokenizer();
                    ObjectMapper mapper = new ObjectMapper();
                    String query = tokenizer.toString(template.getStarList());
                    List<KBEntry> entryList = kbService.findPaginated(user.getOrganisation_id(), template.getKb_field(), template.getKb_type(),
                            query, null, pageSize);

                    if (entryList != null) {
                        for (KBEntry entry : entryList) {

                            if (entry.getJson_data() != null) {
                                Map<String, Object> protoMap = mapper.readValue(entry.getJson_data(), new TypeReference<Map<String, Object>>() {});
                                String html_template = schemaTemplateSet.get(template.getKb_type() + "-" + template.getKb_field());
                                String str = "";
                                if (protoMap != null) {
                                    for (String key : protoMap.keySet()) {
                                        if (!"id".equals(key) && (protoMap.get(key) instanceof String)) {
                                            String value = (String) protoMap.get(key);
                                            if ( html_template == null ) {
                                                if (str.length() > 0) {
                                                    str += ", ";
                                                }
                                                str += value;
                                            } else {
                                                String t_key = "<" + key + ">";
                                                if ( html_template.contains(t_key) ) {
                                                    html_template = html_template.replace(t_key, value);
                                                }
                                            }
                                        }
                                    }
                                } // if protoMap != null

                                // add the result
                                if (html_template == null && str.length() > 0) {
                                    templateAnswerSet.add(str);
                                } else if (html_template != null) {
                                    templateAnswerSet.add(html_template);
                                }

                            } // if has json

                        } // for each entry found
                    } // if entryList != null
                } // else if kb item
            } // for each template in the list
        }
        return templateAnswerSet;
    }

    /**
     * access the system's statistics when asked for
     * @return the statistics string
     */
    private String getStats(UUID organisation_id, DocumentWordCount documentWordCount) {
        if ( documentWordCount != null && organisation_id != null ) {
            GeneralStatistics generalStatistics = documentWordCount.getGeneralStatistics(organisation_id);
            StringBuilder sb = new StringBuilder();
            sb.append("I have collected: ");
            sb.append(generalStatistics.getDocument_count()).append(" documents, ");
            sb.append(generalStatistics.getTotal_index_count()).append(" indexes, ");
            sb.append(generalStatistics.getNoun()).append(" nouns, ").append(generalStatistics.getVerb());
            sb.append(" verbs, and ").append(generalStatistics.getAdjective()).append(" adjectives.");
            return sb.toString();
        }
        return "no statistics available.";
    }

    /**
     * turn {date} and other tags into strings based on now()
     * @param name the name of the item to get
     * @return a formatted string with the object or empty string if dne
     */
    private String getDateTime(String name) {
        if (name != null) {
            DateTime dt = DateTime.now();
            Date date = dt.toDate();
            switch (name) {
                case "{year}": return "" + dt.getYear();
                case "{month}": return formatMonth.format(date);
                case "{time}": return formatTime.format(date);
                case "{date}": return formatDate.format(date);
                case "{day}": return formatDay.format(date);
                default: return "";
            }
        }
        return "";
    }

    /**
     * load all AIML files located @ aiml folder base
     * @throws IOException wrong
     */
    public void initFromXmlString(String xmlString) throws IOException {
        if ( xmlString != null ) {
            try {
                AimlXmlProcessor xmlProcessor = new AimlXmlProcessor(nodeSet);
                xmlProcessor.processXmlString(xmlString);
            } catch (ParserConfigurationException | SAXException ex ) {
                String errMsg = "failed to load AIML string, xml error: " + ex.getMessage();
                logger.error(errMsg);
                throw new IOException(errMsg);
            }
        }
        logger.info("loaded XML string, with " + nodeSet.size() + " root items");
    }

    /**
     * register AI/ML from the KB system with this system
     * @param type the type of the KB system
     * @param language_list the language list of possible template queries
     * @param field the field in question for searching
     */
    public void register_kb_aiml(String type, String[] language_list, String field) throws IOException {
        if (type != null && field != null && language_list != null && language_list.length > 0) {
            AimlKBProcessor processor = new AimlKBProcessor(nodeSet);
            processor.addPattern(language_list, new AimlTemplate(type, field));
        }
    }

    /**
     * if this entry / entity is AI/ML capable, add it to the AI/ML system
     * @param entry the entry whose json to look at
     */
    public void updateAiml(KBEntry entry) throws IOException {
        if (entry != null && entry.getJson_data() != null) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> protoMap = mapper.readValue(entry.getJson_data(), new TypeReference<Map<String, Object>>() {});
            if (protoMap != null && protoMap.containsKey("aiml_list") && protoMap.containsKey("name")) {
                Object aimlListObj = protoMap.get("aiml_list");
                if (aimlListObj instanceof List) {

                    String type = (String)protoMap.get("name");
                    String field = null;
                    String[] language = null;
                    String html_template = null;

                    List aimlList = (List)aimlListObj;
                    for (Object obj : aimlList) {
                        if (obj instanceof Map) {
                            Map map = (Map)obj;
                            if (map.containsKey("field") && (map.get("field") instanceof String)) {
                                field = (String)map.get("field");
                            }
                            if (map.containsKey("language") && (map.get("language") instanceof String)) {
                                language = ((String) map.get("language")).split("\n");
                            }
                            if (map.containsKey("html_template") && (map.get("html_template") instanceof String)) {
                                html_template = ((String) map.get("html_template"));
                            }
                        }
                    }
                    if (field != null && language != null && language.length > 0 && type != null) {
                        register_kb_aiml(type, language, field);
                        if ( html_template != null ) {
                            schemaTemplateSet.put(type + "-" + field, html_template);
                        }
                    }
                }
            }
        }
    }

    public void setAimlFolderBase( String aimlFolderBase ) {
        this.aimlFolderBase = aimlFolderBase;
    }

    public Map<String, AimlPattern> getNodeSet() {
        return nodeSet;
    }



}

