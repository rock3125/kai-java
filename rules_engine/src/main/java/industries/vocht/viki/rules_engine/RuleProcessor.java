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

package industries.vocht.viki.rules_engine;

import com.google.common.util.concurrent.RateLimiter;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.client.DocumentClientInterface;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.export.ExportSftp;
import industries.vocht.viki.model.cluster.KAIActionType;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.indexes.IIndex;
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.model.super_search.ISSearchItem;
import industries.vocht.viki.model.super_search.SSearchParser;
import industries.vocht.viki.model.super_search.SSearchParserException;
import industries.vocht.viki.rules_engine.action.*;
import industries.vocht.viki.rules_engine.model.ExecutableRule;
import industries.vocht.viki.rules_engine.model.OrganisationRuleName;
import industries.vocht.viki.rules_engine.model.RuleConversionException;
import industries.vocht.viki.semantic_search.SSearchExecutor;
import industries.vocht.viki.utility.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 15/05/16.
 *
 * execute a rule
 *
 */
@Component
public class RuleProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RuleProcessor.class);

    @Autowired
    private IDao dao;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private RuleOrchestrator ruleOrchestrator;

    // access to the super search system
    @Autowired
    private SSearchExecutor ssExecutor;

    @Autowired
    private ClusterInfrastructure clusterInfrastructure;

    @Autowired
    private Mailer mailer;

    @Autowired
    private ExportSftp exportSftp;

    // shared rate limiter
    private RateLimiter rateLimiter;

    // how much time the thread sleeps after each peek of the queue
    private final int threadSleepInMS = 100;

    // if the thread is active - keep looping
    private boolean active = true;

    public RuleProcessor() {
    }

    public void init() {
        this.rateLimiter = ruleOrchestrator.getSharedRateLimiter();
    }

    /**
     * get the next document processor round-robin style
     * @return the document processor to use
     */
    private DocumentClientInterface getNextDocumentProcessor() {
        return (DocumentClientInterface)clusterInfrastructure.getNextClientRoundRobin(KAIActionType.Document);
    }

    /**
     * stop the thread
     */
    public void stopRuleProcessor() {
        this.active = false;
    }

    /**
     * run the queue snooping thread
     */
    @Override
    public void run() {

        while ( active ) {

            try {

                // blocking call - grab the next document
                OrganisationRuleName rule = ruleOrchestrator.getNextMessageFromQueue();
                if (rule != null) {

                    UUID sessionID = ruleOrchestrator.getSessionForOrganisation(rule.getOrganisation_id());
                    if (sessionID == null) {

                        logger.error("RuleProcessor.run()", "session id cannot be null, invalid system user");

                    } else {

                        // get the next rule to execute by name
                        try {
                            RuleItem ruleItem = dao.getRuleDao().loadRuleByName(rule.getOrganisation_id(), rule.getRule_name());
                            if (ruleItem != null) {

                                // do the hard work
                                // todo: make maxDistanceAllowed a parameter of the rule object
                                int maxDistanceAllowed = 0;
                                exec(new ConvertFromRuleItem().convert(ruleItem), rule.getUrl(), maxDistanceAllowed);

                            } else {
                                logger.error("RuleProcessor: rule \"" + rule.getRule_name() + "\" does not exist (null)");
                            }

                        } catch (RuleConversionException ex) {
                            logger.error("RuleProcessor: rule \"" + rule.getRule_name() + "\" conversion exception", ex);
                        } catch (SSearchParserException ex) {
                            logger.error("RuleProcessor: rule \"" + rule.getRule_name() + "\" super-search query exception", ex);
                        } catch (IOException ex) {
                            logger.error("RuleProcessor: db \"" + rule.getRule_name() + "\"", ex);
                        }

                    } // end of if session id valid

                } else {

                    // nothing - wait
                    try {
                        Thread.sleep(threadSleepInMS);
                    } catch (InterruptedException ex) {
                        logger.error("RuleProcessor", ex);
                    }

                }

            } catch (ApplicationException ex) {
                logger.error("rule-processor", ex);
            }
        }
    }

    /**
     * execute a rule in the system
     * @param rule the rule to execute
     * @param url an optional (can be null) url to execute the rule against
     */
    public void exec( ExecutableRule rule, String url, int maxDistanceAllowed ) throws RuleConversionException,
            SSearchParserException, IOException, ApplicationException {
        if ( rule != null ) {
            if ( url != null ) {
                logger.debug("executing rule \"" + rule.getRule_name() + "\" for \"" + url + "\"");
            } else {
                logger.debug("executing rule \"" + rule.getRule_name() + "\"");
            }

            // construct a super query for the condition to execute against all indexes
            String sQuery = new ConditionProcessor().conditionListToSuperQuery(rule.getConditionList());
            logger.info("super-query: " + sQuery);

            // execute a query
            if ( sQuery.length() > 0 ) {

                // get the access set for security purposes
                HashSet<Integer> accessSet = hazelcast.getUserAclMap(rule.getOrganisation_id()).get(rule.getCreator());
                if ( accessSet == null ) {
                    throw new ApplicationException("no acls found for user " + rule.getCreator() + " in rule " + rule.getRule_name());
                }

                ISSearchItem searchItem = new SSearchParser().parse(sQuery);
                // todo: can't have nulls fix
                Map<String, DocumentIndexSet> combinedSet = ssExecutor.doSearch(rule.getOrganisation_id(), searchItem, accessSet, null, null);
                if ( combinedSet != null && combinedSet.size() > 0 ) {
                    if ( url != null ) {
                        Map<String, DocumentIndexSet> filterSet = new HashMap<>();
                        if ( combinedSet.containsKey(url) ) {
                            filterSet.put( url, combinedSet.get(url) );
                        }
                        combinedSet = filterSet;
                    }
                    if ( combinedSet.size() > 0 ) {
                        executeActions( rule, combinedSet );
                    }
                } else {
                    logger.info("rule \"" + rule.getRule_name() + "\" zero results, not executing");
                }

            } // if has query

        }
    }

    /**
     * execute any actions associated with this rule
     * @param rule the rule whose actions to execute
     * @param combinedSet the set of indexes combined by url
     */
    private void executeActions( ExecutableRule rule, Map<String, DocumentIndexSet> combinedSet ) throws IOException, ApplicationException {
        if ( rule != null && combinedSet != null && combinedSet.size() > 0 && rule.getActionList() != null ) {
            for ( IAction actionIterator : rule.getActionList() ) {

                if ( actionIterator instanceof ActionPutMetadata) {

                    ActionPutMetadata action = (ActionPutMetadata)actionIterator;
                    String name = action.getName();
                    String value = action.getValue();

                    for ( String url : combinedSet.keySet() ) {
                        Document document = dao.getDocumentDao().read(rule.getOrganisation_id(), url);
                        boolean changed = false; // only process the document if it has changed
                        if ( !document.getName_value_set().containsKey(name) ) {
                            document.getName_value_set().put(name, value);
                            changed = true;
                        } else if ( !document.getName_value_set().get(name).equals(value) ) {
                            document.getName_value_set().put(name, value);
                            changed = true;
                        }
                        if ( changed ) {
                            dao.getDocumentDao().update(rule.getOrganisation_id(), document);
                            // start re-processing the document as it has changed - put it through the document pipe-line
                            getNextDocumentProcessor().start(
                                    ruleOrchestrator.getSessionForOrganisation(rule.getOrganisation_id()).toString(), url);
                        }
                        rateLimiter.acquire();
                    }

                } else if ( actionIterator instanceof ActionRemoveMetadata ) {

                    ActionRemoveMetadata action = (ActionRemoveMetadata)actionIterator;
                    String name = action.getName();

                    for ( String url : combinedSet.keySet() ) {
                        Document document = dao.getDocumentDao().read(rule.getOrganisation_id(), url);
                        if ( document.getName_value_set().containsKey(name) ) {
                            document.getName_value_set().remove(name);
                            dao.getDocumentDao().update(rule.getOrganisation_id(), document);
                            // start re-processing the document as it has changed - put it through the document pipe-line
                            getNextDocumentProcessor().start(
                                    ruleOrchestrator.getSessionForOrganisation(rule.getOrganisation_id()).toString(), url);
                        }
                        rateLimiter.acquire();
                    }

                } else if ( actionIterator instanceof ActionChangeDocumentClassification ) {

                    ActionChangeDocumentClassification action = (ActionChangeDocumentClassification)actionIterator;
                    String classification = action.getClassification();

                    for ( String url : combinedSet.keySet() ) {
                        Document document = dao.getDocumentDao().read(rule.getOrganisation_id(), url);
                        document.getName_value_set().put( Document.META_CLASSIFICATION, classification );
                        dao.getDocumentDao().update( rule.getOrganisation_id(), document );
                        // start re-processing the document as it has changed - put it through the document pipe-line
                        getNextDocumentProcessor().start(
                                ruleOrchestrator.getSessionForOrganisation(rule.getOrganisation_id()).toString(), url );
                        rateLimiter.acquire();
                    }

                } else if ( actionIterator instanceof ActionEmail ) {

                    ActionEmail action = (ActionEmail)actionIterator;
                    String contents = generateReport( rule, combinedSet );
                    mailer.email( action.getTo(), action.getSubject(), contents );

                } else if ( actionIterator instanceof ActionExport) {

                    ActionExport action = (ActionExport)actionIterator;
                    if ( action.getProtocol().compareToIgnoreCase("sftp") == 0 ) {
                        String contents = generateReport(rule, combinedSet);
                        exportSftp.export(action.getUsername(), action.getPassword(),
                                action.getUrl(), action.getPath(), contents);
                    } else {
                        logger.error("unsupported protocol: " + action.getProtocol());
                    }

                } else if ( actionIterator instanceof ActionChangeDocumentSecurity) {

                    ActionChangeDocumentSecurity action = (ActionChangeDocumentSecurity)actionIterator;
                    String aclList = action.getAcl_csv();

                    for ( String url : combinedSet.keySet() ) {
                        Document document = dao.getDocumentDao().read(rule.getOrganisation_id(), url);
                        document.getName_value_set().put( Document.META_ACLS, aclList );
                        dao.getDocumentDao().update( rule.getOrganisation_id(), document );
                        // start re-processing the document as it has changed - put it through the document pipe-line
                        getNextDocumentProcessor().start(
                                ruleOrchestrator.getSessionForOrganisation(rule.getOrganisation_id()).toString(), url );
                        rateLimiter.acquire();
                    }

                } else if ( actionIterator instanceof ActionRemoveDocument) {

                    for ( String url : combinedSet.keySet() ) {
                        getNextDocumentProcessor().delete(
                                ruleOrchestrator.getSessionForOrganisation(rule.getOrganisation_id()).toString(), url );
                        rateLimiter.acquire();
                    }

                }

            }
        }
    }

    /**
     * generate a text report from the search results
     * @param rule the rule executing
     * @param combinedSet the combined set of indexes for the results
     * @return the text of a report
     * @throws IOException
     * @throws ApplicationException
     */
    private String generateReport( ExecutableRule rule, Map<String, DocumentIndexSet> combinedSet ) throws IOException, ApplicationException {
        // create a body content for the email report
        StringBuilder sb = new StringBuilder();
        sb.append("rule \"").append(rule.getRule_name()).append("\" found ").append(combinedSet.size()).append(" result(s)\n\n");
        int count = 1;
        for ( String url : combinedSet.keySet() ) {
            sb.append("result ").append(count).append(", document ").append(url).append("\n");

            // get the document set

            count = count + 1;
        }

        return sb.toString();
    }

    /**
     * group indexes by url - phase I
     * @param indexList the list of indexes to combine
     * @return the list of combined indexes - but no scoring applied yet
     */
    private Map<String, List<IIndex>> combineIndexesByUrl(List<IIndex> indexList ) {
        Map<String, List<IIndex>> combinedSetMap = new HashMap<>();
        if ( indexList != null ) {
            for (IIndex index : indexList) {
                List<IIndex> set = combinedSetMap.get(index.getUrl());
                if (set == null) {
                    set = new ArrayList<>();
                    combinedSetMap.put(index.getUrl(), set);
                }
                set.add( index );
            } // for each index
        } // if list valid
        return combinedSetMap;
    }

}

