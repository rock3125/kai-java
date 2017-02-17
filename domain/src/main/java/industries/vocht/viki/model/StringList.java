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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 16/04/16.
 *
 * a list of strings
 *
 */
public class StringList {

    private List<String> string_list;

    public StringList() {
        string_list = new ArrayList<>();
    }

    public StringList(List<String> string_list) {
        this.string_list = string_list;
    }


    public List<String> getString_list() {
        return string_list;
    }

    public void setString_list(List<String> string_list) {
        this.string_list = string_list;
    }


}

