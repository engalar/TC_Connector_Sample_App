// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

/**
 * Mapping for a single Mendix element to a single Teamcenter element.
 *
 */
public interface TcMapping {
	/** @return The name of the Teamcenter (or JSON key) element. */
	public String getTcName();

	/** @return The name of the Mendix element. */
	public String getMxName();
}
