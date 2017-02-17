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

package industries.vocht.viki.relationship;


import java.util.List;

/**
 * Created by peter on 1/11/15.
 *
 * holder for a list of related words
 * used by the UI to return values
 * namespace must be in WebServer
 *
 */
public class RelatedWordSet {

    private List<RelatedWord> relatedWordList;

    public RelatedWordSet() {
    }

    public RelatedWordSet(List<RelatedWord> relatedWordList) {
        this.relatedWordList = relatedWordList;
    }

    public List<RelatedWord> getRelatedWordList() {
        return relatedWordList;
    }

    public void setRelatedWordList(List<RelatedWord> relatedWordList) {
        this.relatedWordList = relatedWordList;
    }
}
