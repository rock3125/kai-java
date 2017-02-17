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

package industries.vocht.viki.model.indexes;

/**
 * Created by peter on 14/08/16.
 *
 * an offset, score, and keyword indicator for an index in a document
 *
 */
public class DocumentIndex implements Comparable<DocumentIndex> {

    public int offset;
    public float score;
    public int keyword_index;

    public DocumentIndex() {
    }

    public DocumentIndex(int keyword_index, float score, int offset) {
        this.keyword_index = keyword_index;
        this.score = score;
        this.offset = offset;
    }

    @Override
    public int compareTo(DocumentIndex documentIndex) {
        if ( offset < documentIndex.offset ) return -1;
        if ( offset > documentIndex.offset ) return 1;
        return 0;
    }

}

