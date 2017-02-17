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
 * @author G.V.Sekhar
 * 
 * @version $Revision: 1.0 $
 */
public class AdProperties {

	// Add the required user attributes toretrieve
	// User MailIid
	public static final String USER_ATTRIBUTES_MAIL = "mail";

	// User Group : A user may be assigned to multiple groups
	public static final String USER_ATTRIBUTES_MEMBEROF = "memberOf";

	// User Account Control: User status whether he is active or disabled
	public static final String USER_ATTRIBUTES_USERACCOUNTCONTROL = "userAccountControl";

	// distinguishedName: Unique name of the user, user id
	public static final String USER_ATTRIBUTES_DISTINGUISHED_NAME = "distinguishedName";

	// givenname: Given name of the user, this may not be unique among users.
	public static final String USER_ATTRIBUTES_GIVEN_NAME = "givenname";

	// telephonenumber: Given phone number of the user
	public static final String USER_ATTRIBUTES_TELEPHONE_NUMBER = "telephonenumber";

	// Defining all the search filters to apply
	public static final String FILTER_OBJECT_CLASS = "User";
	public static final String FILTER_OBJECT_CATEGORY = "Person";
}

