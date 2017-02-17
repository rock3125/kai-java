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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 17/04/16.
 *
 * collection of summarisation items
 *
 */
public class SummarisationItemList {

    private String url;
    private List<SummarisationItem> summarisationItemList;

    public SummarisationItemList() {
        summarisationItemList = new ArrayList<>();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<SummarisationItem> getSummarisationItemList() {
        return summarisationItemList;
    }

    public void setSummarisationItemList(List<SummarisationItem> summarisationItemList) {
        this.summarisationItemList = summarisationItemList;
    }


}


