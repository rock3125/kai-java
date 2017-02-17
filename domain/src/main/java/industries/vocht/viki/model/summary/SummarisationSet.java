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

package industries.vocht.viki.model.summary;

import java.util.List;

/**
 * Created by peter on 3/04/16.
 *
 * a set of summarisation fragments
 *
 */
public class SummarisationSet {

    private List<SummarisationFragment> summarisationFragmentList;

    public SummarisationSet() {
    }

    public SummarisationSet( List<SummarisationFragment> summarisationFragmentList ) {
        this.summarisationFragmentList = summarisationFragmentList;
    }

    public List<SummarisationFragment> getSummarisationFragmentList() {
        return summarisationFragmentList;
    }

    public void setSummarisationFragmentList(List<SummarisationFragment> summarisationFragmentList) {
        this.summarisationFragmentList = summarisationFragmentList;
    }

}

