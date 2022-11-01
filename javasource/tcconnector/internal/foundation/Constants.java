// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.thirdparty.org.json.JSONObject;

public class Constants {
	public static final ILogNode LOGGER = Core.getLogger("TcConnector");

	public static final String QNAME_SERVER_EXP = "http://teamcenter.com/Schemas/Soa/2006-03/Exceptions.InternalServerException";
	public static final String QNAME_USER_EXP = "http://teamcenter.com/Schemas/Soa/2006-03/Exceptions.InvalidUserException";
	public static final String QNAME_CREDENTIAL_EXP = "http://teamcenter.com/Schemas/Soa/2006-03/Exceptions.InvalidCredentialsException";
	public static final String QNAME_SERVICE_EXP = "http://teamcenter.com/Schemas/Soa/2006-03/Exceptions.ServiceException";
	public static final String QNAME_LOGIN_RESPONSE = "http://teamcenter.com/Schemas/Core/2011-06/Session.LoginResponse";

	public static final String KEY_QNAME = ".QName";
	public static final String TEXT_JSON = "text/json";
	public static final String HTTP_CONTENT_TYPE = "Content-Type";

	public static final String SERVICE_SESSION_0603 = "Core-2006-03-Session";
	public static final String SERVICE_SESSION_1106 = "Core-2011-06-Session";
	public static final String SERVICE_SESSION_1511 = "Core-2015-10-Session";
	public static final String SERVICE_SESSION_0701 = "Core-2007-01-Session";
	public static final String OPERATION_LOGIN = SERVICE_SESSION_1106 + "/login";
	public static final String OPERATION_LOGOUT = SERVICE_SESSION_0603 + "/logout";
	public static final String OPERATION_GETTYPEDESCRIPTION2 = SERVICE_SESSION_1511 + "/getTypeDescriptions2";
	public static final String OPERATION_GETTYPEDESCRIPTION = SERVICE_SESSION_1106 + "/getTypeDescriptions";
	public static final String OPERATION_GET_AVAILABLE_SERVICES = SERVICE_SESSION_0603 + "/getAvailableServices";
	public static final String OPERATION_GETSESSIONINFO = SERVICE_SESSION_0701 + "/getTCSessionInfo";

	public static final String SERVICE_DATAMANAGEMENT_0603 = "Core-2006-03-DataManagement";
	public static final String SERVICE_DATAMANAGEMENT_0701 = "Core-2007-01-DataManagement";
	public static final String SERVICE_DATAMANAGEMENT_0706 = "Core-2007-06-DataManagement";
	public static final String SERVICE_DATAMANAGEMENT_0709 = "Core-2007-09-DataManagement";
	public static final String SERVICE_DATAMANAGEMENT_1004 = "Core-2010-04-DataManagement";
	public static final String SERVICE_DATAMANAGEMENT_1009 = "Core-2010-09-DataManagement";
	public static final String SERVICE_DATAMANAGEMENT_1305 = "Core-2013-05-DataManagement";
	public static final String SERVICE_DATAMANAGEMENT_1507 = "Core-2015-07-DataManagement";
	public static final String OPERATION_CREATERELATIONS = SERVICE_DATAMANAGEMENT_0603 + "/createRelations";
	public static final String OPERATION_GETPROPERTIES = SERVICE_DATAMANAGEMENT_0603 + "/getProperties";
	public static final String OPERATION_GET_ITEM_FROM_ID = SERVICE_DATAMANAGEMENT_0701 + "/getItemFromId";
	public static final String OPERATION_WHERE_USED = SERVICE_DATAMANAGEMENT_0701 + "/whereUsed";
	public static final String OPERATION_GETDATASETTYPEINFO = SERVICE_DATAMANAGEMENT_0706 + "/getDatasetTypeInfo";
	public static final String OPERATION_GETAVAILABLETYPESWITHDISPLAYNAMES = SERVICE_DATAMANAGEMENT_1004
			+ "/getAvailableTypesWithDisplayNames";
	public static final String OPERATION_CREATEDATASETS = SERVICE_DATAMANAGEMENT_1004 + "/createDatasets";
	public static final String OPERATION_SETPROPERTIES = SERVICE_DATAMANAGEMENT_1009 + "/setProperties";
	public static final String OPERATION_REVISEOBJECTS = SERVICE_DATAMANAGEMENT_1305 + "/reviseObjects";
	public static final String OPERATION_CREATEOBJECTS = SERVICE_DATAMANAGEMENT_1507 + "/createRelateAndSubmitObjects2";
	public static final String OPERATION_GETDEEPCOPYDATA = SERVICE_DATAMANAGEMENT_1507 + "/getDeepCopyData";
	public static final String OPERATION_LOAD_OBJECTS = SERVICE_DATAMANAGEMENT_0709 + "/loadObjects";
	public static final String OPERATION_EXPAND_GRM_RELATIONS_PRIMARY = SERVICE_DATAMANAGEMENT_0709
			+ "/expandGRMRelationsForPrimary";
	public static final String OPERATION_EXPAND_GRM_RELATIONS_SECONDARY = SERVICE_DATAMANAGEMENT_0709
			+ "/expandGRMRelationsForSecondary";

	public static final String SERVICE_LOV_1305 = "Core-2013-05-LOV";
	public static final String SERVICE_LOV_0706 = "Core-2007-06-LOV";
	public static final String OPERATION_GET_INITIAL_LOV_VALUES = SERVICE_LOV_1305 + "/getInitialLOVValues";
	public static final String OPERATION_GET_NEXT_LOV_VALUES = SERVICE_LOV_1305 + "/getNextLOVValues";
	public static final String OPERATION_GET_ATTACHED_LOVS = SERVICE_LOV_0706 + "/getAttachedLOVs";

	public static final String SERVICE_FILEMANAGEMENT_0603 = "Core-2006-03-FileManagement";
	public static final String SERVICE_FILEMANAGEMENT_0701 = "Core-2007-01-FileManagement";
	public static final String OPERATION_GETFILEREADTICKETS = SERVICE_FILEMANAGEMENT_0603 + "/getFileReadTickets";
	public static final String OPERATION_COMMITDATASETFILES = SERVICE_FILEMANAGEMENT_0603 + "/commitDatasetFiles";
	public static final String OPERATION_GETTRANSIENTFILETICKETSFORUPLOAD = SERVICE_FILEMANAGEMENT_0701
			+ "/getTransientFileTicketsForUpload";

	public static final String SERVICE_STRUCTUREMANAGEMENT_0701 = "Cad-2007-01-StructureManagement";
	public static final String SERVICE_STRUCTUREMANAGEMENT_0806 = "Cad-2008-06-StructureManagement";
	public static final String OPERATION_CREATEBOMWINDOWS = SERVICE_STRUCTUREMANAGEMENT_0701 + "/createBOMWindows";
	public static final String OPERATION_EXPANDPSONELEVEL = SERVICE_STRUCTUREMANAGEMENT_0701 + "/expandPSOneLevel";
	public static final String OPERATION_EXPANDPSONELEVEL2 = SERVICE_STRUCTUREMANAGEMENT_0806 + "/expandPSOneLevel";
	public static final String OPERATION_GETREVISIONRULES = SERVICE_STRUCTUREMANAGEMENT_0701 + "/getRevisionRules";
	public static final String OPERATION_EXPANDPSALLLEVELS = SERVICE_STRUCTUREMANAGEMENT_0701 + "/expandPSAllLevels";
	public static final String OPERATION_GETVARIANTRULES = SERVICE_STRUCTUREMANAGEMENT_0701 + "/getVariantRules";
	public static final String OPERATION_CLOSEBOMWINDOWS = SERVICE_STRUCTUREMANAGEMENT_0701 + "/closeBOMWindows";

	public static final String SERVICE_STRUCTUREMANAGEMENT_1305 = "Cad-2013-05-StructureManagement";
	public static final String OPERATION_CREATEBOMWINDOWS2 = SERVICE_STRUCTUREMANAGEMENT_1305 + "/createBOMWindows2";

	public static final String SERVICE_SAVEDQUERY_1004 = "Query-2010-04-SavedQuery";
	public static final String SERVICE_QUERY_FINDER_1411 = "Query-2014-11-Finder";
	public static final String SERVICE_SAVEDQUERY_0806 = "Query-2008-06-SavedQuery";
	public static final String OPERATION_FINDSAVEDQUERIES = SERVICE_SAVEDQUERY_1004 + "/findSavedQueries";
	public static final String OPERATION_PERFORMSEARCH = SERVICE_QUERY_FINDER_1411 + "/performSearch";
	public static final String OPERATION_EXECUTESAVEDQUERIES = SERVICE_SAVEDQUERY_0806 + "/executeSavedQueries";

	public static final String SERVICE_WORKFLOW_1410 = "Workflow-2014-10-Workflow";
	public static final String SERVICE_WORKFLOW_0806 = "Workflow-2008-06-Workflow";
	public static final String SERVICE_WORKFLOW_1210 = "Workflow-2012-10-Workflow";

	public static final String OPERATION_CREATEWORKFLOW = SERVICE_WORKFLOW_1410 + "/createWorkflow";
	public static final String OPERATION_GETALLTASK = SERVICE_WORKFLOW_0806 + "/getAllTasks";
	public static final String OPERATION_PERFORMACTION = SERVICE_WORKFLOW_1210 + "/performAction2";
	public static final String OPERATION_GETWORKFLOWTEMPLATES = SERVICE_WORKFLOW_0806 + "/getWorkflowTemplates";

	public static final String KEY_USER = "user";
	public static final String KEY_PASS = "password";
	public static final String KEY_GROUP = "group";
	public static final String KEY_ROLE = "role";
	public static final String KEY_LOCALE = "locale";
	public static final String KEY_DESCRIMINATOR = "descrimator";
	public static final String KEY_CREDENTIALS = "credentials";
	public static final String TCTYPE_DATASET = "Dataset";

	public static final JSONObject InvalidUserException = new JSONObject("{\r\n"
			+ "                 \".QName\": \"http://teamcenter.com/Schemas/Soa/2006-03/Exceptions.InvalidUserException\",\r\n"
			+ "                 \"ssoServerURL\": \"\",\r\n" + "                    \"ssoAppID\": \"\",\r\n"
			+ "                 \"code\": 1000,\r\n" + "                    \"level\": 3,\r\n"
			+ "                 \"tcresponse\": \"Teamcenter\",\r\n"
			+ "                 \"message\": \"User does not have a valid session.\"\r\n" + "               }");

}
