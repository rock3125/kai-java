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

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Created by peter on 5/12/15.
 *
 * simple object for wrapping a simple message as an html return message
 *
 */
public class HtmlWrapper {

    public HtmlWrapper() {
    }

    /**
     * wrap a message as an <h1></h1> item (single line)
     * @param msg the message to wrap
     * @return the wrapped string
     */
    public String wrapH1(String msg) {
        return "<html><body><h1>" + StringEscapeUtils.escapeHtml(msg) + "</h1></body></html>";
    }

    /**
     * wrap a message as an <body></body> item over multiple lines using <br/> to replace \n
     * @param msg the message to wrap
     * @return the wrapped string
     */
    public String wrap(String msg) {
        return "<html><body>" + StringEscapeUtils.escapeHtml(msg).replace("\n","<br/>") + "</body></html>";
    }
}

