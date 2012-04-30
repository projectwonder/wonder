/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb.pages;
import com.webobjects.appserver.WOContext;

/**
 * Query page utilizing fetch specifications.<br />
 * @deprecated use ERD2WQueryPage instead
 * @d2wKey entity
 * @d2wKey pageWrapperName
 * @d2wKey border
 * @d2wKey backgroundColorForTable
 * @d2wKey componentName
 * @d2wKey propertyKey
 * @d2wKey findButtonLabel
 */
public class ERD2WQueryPageWithFetchSpecification extends ERD2WQueryPage  {
	/**
	 * Do I need to update serialVersionUID?
	 * See section 5.6 <cite>Type Changes Affecting Serialization</cite> on page 51 of the 
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

    public ERD2WQueryPageWithFetchSpecification(WOContext context) {
        super(context);
    }
}