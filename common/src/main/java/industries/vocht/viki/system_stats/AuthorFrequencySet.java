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

package industries.vocht.viki.system_stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 19/12/16.
 *
 * collection of author frequency items
 *
 */
public class AuthorFrequencySet {

    private String query;
    private List<AuthorFrequency> authorList;

    public AuthorFrequencySet() {
        this.authorList = new ArrayList<>();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<AuthorFrequency> getAuthorList() {
        return authorList;
    }

    public void setAuthorList(List<AuthorFrequency> authorList) {
        this.authorList = authorList;
    }

}

