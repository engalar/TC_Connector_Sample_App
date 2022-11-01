package tcconnector.internal.servicehelper;

import java.util.List;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import tcconnector.actions.CallTeamcenterService;
import tcconnector.foundation.JModelObject;
import tcconnector.internal.foundation.Constants;
import tcconnector.proxies.FindSavedQueryInput;

public class AdvancedSearchHelper {
	private static final String KEY_SAVED_QUERIES = "TcConnector.savedQueries";
	private static final String KEY_QUERY_UID = "queryUID";
	private static final String KEY_SEARCH_CRITERIA = "TcConnector.searchCriteria";
	private static final String KEY_OPERATION_MAPPING = "OperationMapping/Query/2010-04/SavedQuery/findSavedQueries.json";
	private static final String KEY_SEARCH_MAPPING = "OperationMapping/Query/2014-11/Finder/performSearch.json";
	private static final String KEY_SAVED_QUERY_OBJECT_MAPPING = "ImanQuery=TcConnector.ImanQuery";
	private static final String KEY_QUERY_NAME_PROP = "query_name";

	/**
	 * performs the advance search operation based on query input reagrding
	 * 
	 * @param targetObject The service operation response, may be a ServiceData or
	 *                     custom data structure.
	 * @throws Exception
	 */
	public static IMendixObject performAdvanceSearch(IContext context, String queryName,
			IMendixObject specialisedSearchInput, String businessObjectMapping, String configurationName)
			throws Exception {
		// Find Saved Query SOA call
		String newQueryUidVal = getSavedQueryUID(context, queryName, configurationName);
		if (newQueryUidVal == null)
			return Core.instantiate(context, "TcConnector.FindSavedQueryResponse");

		// Set queryUid value from findSavedQuery SOA to 'queryUID' attribute of Saved
		// Query
		IMendixObject searchCriteriaInput = Core.retrieveByPath(context, specialisedSearchInput, KEY_SEARCH_CRITERIA)
				.get(0);
		searchCriteriaInput.setValue(context, KEY_QUERY_UID, newQueryUidVal);

		// Perform Search SOA call
		IMendixObject searchResponse = Core.instantiate(context, "TcConnector.SearchResponse");
		CallTeamcenterService performSearch = new CallTeamcenterService(context, Constants.OPERATION_PERFORMSEARCH,
				specialisedSearchInput, searchResponse, KEY_SEARCH_MAPPING, businessObjectMapping, configurationName);
		return performSearch.executeAction();
	}

	public static String getSavedQueryUID(IContext context, String queryName, String ConfigurationName)
			throws Exception {
		FindSavedQueryInput findSavedQueryInput = new FindSavedQueryInput(context);
		findSavedQueryInput.setqueryNames(queryName);
		IMendixObject queryResponse = Core.instantiate(context, "TcConnector.FindSavedQueryResponse");
		CallTeamcenterService findSavedQuery = new CallTeamcenterService(context, Constants.OPERATION_FINDSAVEDQUERIES,
				findSavedQueryInput.getMendixObject(), queryResponse, KEY_OPERATION_MAPPING,
				KEY_SAVED_QUERY_OBJECT_MAPPING, ConfigurationName);
		findSavedQuery.executeAction();
		List<IMendixObject> savedQueries = Core.retrieveByPath(context, queryResponse, KEY_SAVED_QUERIES);
		if (savedQueries.isEmpty()) {
			return null;
		}

		IMendixObject imanQuery = savedQueries.get(0);
		if (queryName != null)
			for (IMendixObject savedQuery : savedQueries)
				if (queryName.equals(savedQuery.getValue(context, KEY_QUERY_NAME_PROP)))
					imanQuery = savedQuery;

		JModelObject imanQueryObj = new JModelObject(context, imanQuery);
		return imanQueryObj.getUID();
	}
}
