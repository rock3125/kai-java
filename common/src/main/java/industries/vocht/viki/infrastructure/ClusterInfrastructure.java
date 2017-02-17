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

package industries.vocht.viki.infrastructure;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.client.*;
import industries.vocht.viki.model.cluster.ClusterAddress;
import industries.vocht.viki.model.cluster.KAIActionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by peter on 29/05/16.
 *
 * what does the cluster look like?  how many nodes and where,
 * also manage a round robin system for each item - this is a proto-type bean
 *
 */
@Component
public class ClusterInfrastructure {

    /////////////////////////////////////////////////
    // internal node configurations for INTRA node communication

    @Value("${host.name}")
    private String hostName;

    @Value("${web.port}")
    private int port;

    /////////////////////////////////////////////////
    // the services that are active on this node

    @Value("${sl.cluster.activate}")
    private boolean slClusterActive;

    @Value("${sl.converter.activate}")
    private boolean slConverterActive;

    @Value("${sl.document.activate}")
    private boolean slDocumentActive;

    @Value("${sl.group.activate}")
    private boolean slGroupActive;

    @Value("${sl.security.activate}")
    private boolean slSecurityActive;

    @Value("${sl.time.activate}")
    private boolean slTimeActive;

    @Value("${sl.speech.activate}")
    private boolean slSpeechActive;

    @Value("${sl.report.activate}")
    private boolean slReportActive;

    @Value("${sl.summarisation.activate}")
    private boolean slSummarisationActive;

    @Value("${sl.analysis.activate}")
    private boolean slAnalysisActive;

    @Value("${sl.knowledge.activate}")
    private boolean slKnowledgeActive;

    @Value("${sl.document.comparison.activate}")
    private boolean slDocumentComparisonActive;

    @Value("${sl.parser.activate}")
    private boolean slParserActive;

    @Value("${sl.index.activate}")
    private boolean slIndexActive;

    @Value("${sl.nnet.activate}")
    private boolean slNNetActive;

    @Value("${sl.search.activate}")
    private boolean slSearchActive;

    @Value("${sl.rule.activate}")
    private boolean slRuleActive;

    @Value("${sl.stats.activate}")
    private boolean slStatsActive;

    @Value("${sl.vectorize.activate}")
    private boolean slVectorizeActive;

    @Value("${sl.kb.activate:true}")
    private boolean slKBActive;

    ////////////////////////////////////////////////////////////////////////////////////

    // the map of all item addresses by type
    private Map<KAIActionType, List<ClusterAddress>> nodeAddressByType;

    // roound robin dish out system
    private int[] roundRobinCounters;

    public ClusterInfrastructure() {
    }

    public void init() throws ApplicationException {

        // intra node communication
        nodeAddressByType = new HashMap<>();

        setup( KAIActionType.Analysis, hostName, port, slAnalysisActive );
        setup( KAIActionType.Clustering, hostName, port, slClusterActive );
        setup( KAIActionType.Converter, hostName, port, slConverterActive );
        setup( KAIActionType.Document, hostName, port, slDocumentActive);
        setup( KAIActionType.Group, hostName, port, slGroupActive );
        setup( KAIActionType.Index, hostName, port, slIndexActive );
        setup( KAIActionType.Knowledge, hostName, port, slKnowledgeActive );
        setup( KAIActionType.Parser, hostName, port, slParserActive );
        setup( KAIActionType.Report, hostName, port, slReportActive );
        setup( KAIActionType.Rule, hostName, port, slRuleActive );
        setup( KAIActionType.Statistics, hostName, port, slStatsActive );
        setup( KAIActionType.Search, hostName, port, slSearchActive );
        setup( KAIActionType.Security, hostName, port, slSecurityActive );
        setup( KAIActionType.Summary, hostName, port, slSummarisationActive );
        setup( KAIActionType.Time, hostName, port, slTimeActive );
        setup( KAIActionType.Vectorize, hostName, port, slVectorizeActive );
        setup( KAIActionType.Speech, hostName, port, slSpeechActive);
        setup( KAIActionType.DocumentComparison, hostName, port, slDocumentComparisonActive );
        setup( KAIActionType.KBEntry, hostName, port, slKBActive );

        // setup first round-robins
        roundRobinCounters = new int[KAIActionType.LAST_ITEM.ordinal()];
    }

    /**
     * setup a node in the map of available system items
     * @param type the type of node
     * @param active the active flag for this node
     */
    private void setup(KAIActionType type, String hostname1, int port1, boolean active ) {
        if ( active ) {
            List<ClusterAddress> list = nodeAddressByType.get(type);
            if( list == null ) {
                list = new ArrayList<>();
            }
            if ( !contains(list, hostname1, port1) ) {
                list.add(new ClusterAddress(type, hostname1, port1));
                nodeAddressByType.put(type, list);
            }
        }
    }

    /**
     * find hostname/port in the list
     * @param list the list to check
     * @param hostname the name of the host to check for
     * @param port the port of the service
     * @return true if the items are in list
     */
    private boolean contains( List<ClusterAddress> list, String hostname, int port ) {
        if ( list != null && list.size() > 0 ) {
            for ( ClusterAddress address : list ) {
                if ( address.getHost().equals(hostname) && address.getPort() == port ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return all nodes in the cluster of this system with external addresses
     *         to be used by clients connecting to the cluster
     */
    public List<ClusterAddress> getAllExternalNodes() {
        List<ClusterAddress> list = new ArrayList<>();
        for ( Collection<ClusterAddress> typeList : nodeAddressByType.values() ) {
            list.addAll(typeList);
        }
        return list;
    }

    /**
     * receive an update message - make changes to the infrastructure according to list
     * @param clusterAddressList the list of items to register on this node (if not already registered)
     */
    public void updateClusterSetup( List<ClusterAddress> clusterAddressList ) {
        if ( clusterAddressList != null ) {
            for ( ClusterAddress clusterAddress : clusterAddressList ) {
                setup( clusterAddress.getType(), clusterAddress.getHost(), clusterAddress.getPort(), true );
            }
        }
    }

    /**
     * @return the next node round robin style for intra node communications
     */
    public ClientInterfaceCommon getNextClientRoundRobin(KAIActionType type) {
        if ( type != null ) {
            List<ClientInterfaceCommon> list = getClientList(type);
            if ( list != null ) {
                // get current
                int current_count = roundRobinCounters[type.ordinal()];
                // increment
                current_count = (current_count + 1) % list.size();
                // store back
                roundRobinCounters[type.ordinal()] = current_count;
                // return the selected item
                return list.get(current_count);
            }
        }
        return null;
    }

    /**
     * create a client list for a given type (if it has a client)
     * @param type the type of the client address list
     * @return a list of clients
     */
    private List<ClientInterfaceCommon> getClientList( KAIActionType type ) {
        if ( type != null ) {

            switch ( type ) {

                case Search: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new SearchClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Converter: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new ConverterClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Document: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new DocumentClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Analysis: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new EmotionAnalyseClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Index: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new IndexClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Parser: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new ParserClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Report: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new ReportClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Group: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new GroupClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Rule: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new RuleClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Security: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new SecurityClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Summary: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for (ClusterAddress clusterAddress : nodeAddressByType.get(type)) {
                        list.add(new SummariseClientInterface(clusterAddress.getHost(), clusterAddress.getPort()));
                    }
                    return list;
                }

                case Vectorize: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new VectorizeClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case Speech: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new SpeechClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case DocumentComparison: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new DocumentComparisonClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

                case KBEntry: {
                    List<ClientInterfaceCommon> list = new ArrayList<>();
                    for ( ClusterAddress clusterAddress : nodeAddressByType.get(type) ) {
                        list.add( new KBClientInterface(clusterAddress.getHost(), clusterAddress.getPort()) );
                    }
                    return list;
                }

            } // switch

        }
        return null;
    }



}



