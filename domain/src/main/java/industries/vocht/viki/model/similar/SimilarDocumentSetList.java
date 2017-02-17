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

package industries.vocht.viki.model.similar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 17/04/16.
 *
 * list of similar document-sets - each item is a url with a list of similars
 *
 */
public class SimilarDocumentSetList {

    private List<SimilarDocumentSet> similarDocumentSetList;

    public SimilarDocumentSetList() {
        similarDocumentSetList = new ArrayList<>();
    }

    public SimilarDocumentSetList(List<SimilarDocumentSet> similarDocumentSetList) {
        this.similarDocumentSetList = similarDocumentSetList;
    }

    public List<SimilarDocumentSet> getSimilarDocumentSetList() {
        return similarDocumentSetList;
    }

    public void setSimilarDocumentSetList(List<SimilarDocumentSet> similarDocumentSetList) {
        this.similarDocumentSetList = similarDocumentSetList;
    }

}



