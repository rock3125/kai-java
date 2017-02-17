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

package industries.vocht.viki.active_directory;

/**
 * This class stores the user objects along with their attributes.
 * 
 * @author G.V.Sekhar
 * @version $Revision: 1.0 $
 */
public class AdUser {
	/**
	 * Field attributeValues.
	 */
	private String[] attributeValues;

	/**
	 * Method getAttributeValues.
	 * 
	 * @return String[]
	 */
	public String[] getAttributeValues()
	{
		return attributeValues;
	}

	/**
	 * Method setAttributeValues.
	 * 
	 * @param attributeValues
	 *            String[]
	 */
	public void setAttributeValues(String[] attributeValues)
	{
		this.attributeValues = attributeValues;
	}
}

