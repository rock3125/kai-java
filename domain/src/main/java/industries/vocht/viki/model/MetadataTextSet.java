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

package industries.vocht.viki.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 18/04/16.
 *
 * a set of name / value metadata items (All text)
 *
 */
public class MetadataTextSet {

    private Map<String, String> map;

    public MetadataTextSet() {
        map = new HashMap<>();
    }

    public void addValue( String name, String value ) {
        map.put(name, value);
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

}



