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

package industries.vocht.viki.converter;

import industries.vocht.viki.VikiException;

import java.util.Map;

/*
 * Created by peter on 6/12/14.
 *
 * interface for something that can convert binary objects to text
 *
 */
public interface IDocumentConverter  {

    // get all the metadata for a document from a converter
    Map<String, String> getText(String url, byte[] binaryData) throws VikiException;

    // return true if the this extraction system supports the given url
    boolean isSupported(String url, String mimeType);

    // the fallback converted can always be used for a conversion if all else fails
    boolean isDefaultConverter();
}

