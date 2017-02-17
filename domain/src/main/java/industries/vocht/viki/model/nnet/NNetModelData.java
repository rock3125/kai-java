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

package industries.vocht.viki.model.nnet;

/**
 * Created by peter on 27/05/16.
 *
 * data for storing a neural network model
 *
 */
public class NNetModelData {

    private byte[] data;
    private String jsonConf;
    private long last_updated;

    private NNetModelData() {
    }

    public NNetModelData( String jsonConf, byte[] data, long last_updated ) {
        this.jsonConf = jsonConf;
        this.data = data;
        this.last_updated = last_updated;
    }


    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getJsonConf() {
        return jsonConf;
    }

    public void setJsonConf(String jsonConf) {
        this.jsonConf = jsonConf;
    }

    public long getLast_updated() {
        return last_updated;
    }

    public void setLast_updated(long last_updated) {
        this.last_updated = last_updated;
    }

}


