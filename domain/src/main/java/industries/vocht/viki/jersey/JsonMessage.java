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

package industries.vocht.viki.jersey;

/**
 * Created by peter on 5/12/15.
 *
 * an message object bean to return errors and other text with
 *
 */
public class JsonMessage {
    private String error; // an error message
    private String message; // an optional main message
    public JsonMessage() {
    }
    public JsonMessage(String error) {
        this.error = error;
    }
    public JsonMessage(String message, String error)
    {
        this.message = message;
        this.error = error;
    }

    public String toString() {
        if ( message != null ) {
            return message;
        } else {
            return error;
        }
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
