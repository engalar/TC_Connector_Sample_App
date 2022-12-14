// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package tcconnector.actions;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;
import tcconnector.internal.servicehelper.AdvancedSearchHelper;
import com.mendix.systemwideinterfaces.core.IMendixObject;

/**
 * SOA URL: 
 * Query-2010-04-SavedQuery/findSavedQueries
 * Query-2014-11-Finder/performSearch
 * 
 * Description: 
 * This is a specific action to send the request to and receive the response from a the data provider 'Awp0SavedQuerySearchProvider' for query 'General...'. The input criteria for GeneralSearch query is passed through the generalQuerySearchInput object which is extended from .SearchInput Object.
 * 
 * Returns:
 * An entity of type SearchResponse. Search Results can be retrieved using association TcConnector.searchResultsList. Partial errors can be retrieved using association TcConnector.ResponseData/TcConnector.PartialErrors.
 * 
 * NOTE:
 * This action will work only if the teamcenter enviornment has active workspace installation .
 */
public class PerformGeneralQuerySearchAW extends CustomJavaAction<IMendixObject>
{
	private IMendixObject __generalQuerySearchInput;
	private tcconnector.proxies.SearchInput generalQuerySearchInput;
	private java.lang.String businessObjectMapping;
	private java.lang.String ConfigurationName;

	public PerformGeneralQuerySearchAW(IContext context, IMendixObject generalQuerySearchInput, java.lang.String businessObjectMapping, java.lang.String ConfigurationName)
	{
		super(context);
		this.__generalQuerySearchInput = generalQuerySearchInput;
		this.businessObjectMapping = businessObjectMapping;
		this.ConfigurationName = ConfigurationName;
	}

	@java.lang.Override
	public IMendixObject executeAction() throws Exception
	{
		this.generalQuerySearchInput = __generalQuerySearchInput == null ? null : tcconnector.proxies.SearchInput.initialize(getContext(), __generalQuerySearchInput);

		// BEGIN USER CODE
		return AdvancedSearchHelper.performAdvanceSearch(getContext(), KEY_GENERAL_QUERY_NAME,
				generalQuerySearchInput.getMendixObject(), businessObjectMapping, ConfigurationName);
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "PerformGeneralQuerySearchAW";
	}

	// BEGIN EXTRA CODE
	private static final String KEY_GENERAL_QUERY_NAME = "General...";
	// END EXTRA CODE
}
