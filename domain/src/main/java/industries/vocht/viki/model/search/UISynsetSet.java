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

package industries.vocht.viki.model.search;

import java.util.List;

/**
 * Created by peter on 24/05/16.
 *
 * a single collection of synset items
 *
 */
public class UISynsetSet {

    private String word;
    private int selectedSynsetId;

    private List<UISynset> synset_list;

    public UISynsetSet() {

    }

    public UISynsetSet( String word, List<UISynset> synset_list ) {
        this.word = word;
        this.synset_list = synset_list;
        this.selectedSynsetId = -1;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getSelectedSynsetId() {
        return selectedSynsetId;
    }

    public void setSelectedSynsetId(int selectedSynsetId) {
        this.selectedSynsetId = selectedSynsetId;
    }

    public List<UISynset> getSynset_list() {
        return synset_list;
    }

    public void setSynset_list(List<UISynset> synset_list) {
        this.synset_list = synset_list;
    }
}
