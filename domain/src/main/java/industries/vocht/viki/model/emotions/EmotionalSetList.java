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

package industries.vocht.viki.model.emotions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 16/04/16.
 *
 * list of emotional sets
 *
 */
public class EmotionalSetList {

    private List<EmotionalSet> emotionalSetList;

    public EmotionalSetList() {
        emotionalSetList = new ArrayList<>();
    }

    public List<EmotionalSet> getEmotionalSetList() {
        return emotionalSetList;
    }

    public void setEmotionalSetList(List<EmotionalSet> emotionalSetList) {
        this.emotionalSetList = emotionalSetList;
    }


}
