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
 * Created by peter on 12/06/16.
 *
 * list of name value items
 *
 */
public class NameValueList {

    private List<NameValue> nameValueList;

    public NameValueList() {
        nameValueList = new ArrayList<>();
    }

    public NameValueList( List<NameValue> nameValueList ) {
        this.nameValueList = nameValueList;
    }

    public List<NameValue> getNameValueList() {
        return nameValueList;
    }

    public void setNameValueList(List<NameValue> nameValueList) {
        this.nameValueList = nameValueList;
    }

}
