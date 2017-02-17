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

package industries.vocht.viki.model.k_means;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by peter on 4/05/16.
 *
 * a set of cluster items
 *
 */
public class kMeansClusterSet {

    private List<kMeansCluster> kMeansClusterList;

    public kMeansClusterSet() {
        this.setkMeansClusterList(new ArrayList<>());
    }

    public List<kMeansCluster> getkMeansClusterList() {
        return kMeansClusterList;
    }

    public void setkMeansClusterList(List<kMeansCluster> kMeansClusterList) {
        this.kMeansClusterList = kMeansClusterList;
    }

    /**
     * sort cluster items by num-members in each cluster descending
     */
    public void sort() {
        Collections.sort( kMeansClusterList );
    }

}


