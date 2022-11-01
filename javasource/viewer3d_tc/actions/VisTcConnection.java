package viewer3d_tc.actions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;
import com.teamcenter.fms.servercache.FSCException;
import com.teamcenter.fms.servercache.proxy.CommonsFSCWholeFileIOImpl;

import tcconnector.foundation.JModelObject;
import tcconnector.foundation.JServiceData;
import tcconnector.foundation.TcConnection;
import tcconnector.foundation.exceptions.NotLoadedExcpetion;
import tcconnector.internal.foundation.Constants;
import tcconnector.internal.foundation.Messages;
import tcconnector.proxies.TeamcenterConfiguration;

import viewer3d_tc.actions.teamcenter.ItemRevision;
import viewer3d_tc.actions.teamcenter.Occurrence;

public final class VisTcConnection {
	
	private static String[][] massPropNames = {
		{"mass", "CAD_MASS"},
		{"area", "CAD_SURFACE_AREA"},
		{"volume", "CAD_VOLUME"},
		{"density", "CAD_DENSITY"},
		{"com_x", "CAD_CENTER_OF_GRAVITY_X"},
		{"com_y", "CAD_CENTER_OF_GRAVITY_Y"},
		{"com_z", "CAD_CENTER_OF_GRAVITY_Z"},
		{"moi_xx", "CAD_MOMENT_OF_INERTIA_XX"},
		{"moi_yy", "CAD_MOMENT_OF_INERTIA_YY"},
		{"moi_zz", "CAD_MOMENT_OF_INERTIA_ZZ"},
		{"poi_xy", "CAD_PRODUCT_OF_INERTIA_XY"},
		{"poi_xz", "CAD_PRODUCT_OF_INERTIA_XZ"},
		{"poi_yz", "CAD_PRODUCT_OF_INERTIA_YZ"},
	};

	private IContext 	context;
	static private java.lang.String configurationName;
	
	public VisTcConnection(IContext context) {
		this.context 		= context;
		configurationName =  tcconnector.proxies.microflows.Microflows.retrieveConfigNameFromSingleActiveConfiguration(context);
	}
	
	public static JtOutputStream GetItemRevisionJTFileStream(IContext context, java.lang.String itemRevisionUID) throws Exception {
		// Create expandGRMRelationsForPrimary JSON template
		String getDatasetJT = "\r\n" + 
				"{\r\n" + 
				"    \"primaryObjects\": [{\r\n" + 
				"        \"uid\": \"{1}\"\r\n" + 
				"    }],\r\n" + 
				"    \"pref\": {\r\n" +
				"        \"expItemRev\": false,\r\n" +
				"        \"returnRelations\": true,\r\n" +
				"        \"info\": [{\r\n" +
				"            \"relationTypeName\": \"TC_Attaches\"\r\n" +
				"        },{\r\n" +
				"		     \"relationTypeName\": \"IMAN_specification\"\r\n" + 
				"		 },{\r\n" +
				"		     \"relationTypeName\": \"IMAN_Rendering\"\r\n" + 
				"		 }]\r\n" +
				"    }\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"";
		
		// substitutions for createDatasets
		ArrayList<String> Substitutions = createSubstitutionsFor_getProperties(itemRevisionUID);
		getDatasetJT = createServiceInput( getDatasetJT, Substitutions );
		
		JSONObject dsList = TcConnection.callTeamcenterService(context, "Core-2007-09-DataManagement/expandGRMRelationsForPrimary", getDatasetJT, new JSONObject(), configurationName);
		JSONObject data = dsList.getJSONObject("ServiceData");
		if (data != null)
		{
			JServiceData serviceData = (JServiceData)data;
			List<JModelObject> datasets = serviceData.getPlainObjects();
			for (int i = 0; i < datasets.size(); ++i)
			{
				JModelObject ds = datasets.get(i);
				if (ds.getType().equals("DirectModel"))
				{
					java.lang.String datasetUID = ds.getUID();
					JtOutputStream os = GetDatasetJTFileStream(context, datasetUID);
					return os;
				}
			}
		}
		return null;
	}
	
	public static JtOutputStream GetDatasetJTFileStream(IContext context, java.lang.String datasetUID) throws Exception {
		
		try {
			VisTcConnection visTcConnection = new VisTcConnection(context);
			
			JSONObject refList = visTcConnection.RetrieveDatasetRefList(datasetUID);
			
			List<JSONObject> files = visTcConnection.RetrieveFileListUIDs( refList, datasetUID, false );
	
			if( files.size() > 0)
			{
				JSONObject body = visTcConnection.generateBody_getFileReadTickets(files);

				JSONObject getFileReadTicketsResponse = visTcConnection.getFileReadTickets( body.toString(), configurationName );
				
				return visTcConnection.downloadFilesFromFMS(getFileReadTicketsResponse);
			}
			else
			{
				Constants.LOGGER.info( Messages.Dataset.NoFilesAvailableToDownload );
			}
		} catch (Exception e) {
			Constants.LOGGER.error( Messages.Dataset.DownloadFilesError + e.getMessage());
			throw e;
		}
		return null;
	}
	
	public static JtOutputStream GetJTFileStream(IContext context, java.lang.String refUID) throws Exception {
		
		try {
			VisTcConnection visTcConnection = new VisTcConnection(context);
			
			JSONObject datasetType = new JSONObject();
			//datasetType.put("modelName", uiValues.get(j));
			datasetType.put("fileUID", refUID);
			List<JSONObject> files = new ArrayList<JSONObject>();
			files.add(datasetType);
			JSONObject body = visTcConnection.generateBody_getFileReadTickets(files);

			JSONObject getFileReadTicketsResponse = visTcConnection.getFileReadTickets( body.toString(), configurationName );
			
			JtOutputStream os = visTcConnection.downloadFilesFromFMS(getFileReadTicketsResponse);
			return os;
		} catch (Exception e) {
			Constants.LOGGER.error( Messages.Dataset.DownloadFilesError + e.getMessage());
			throw e;
		}
	}
	
	/*
	 * Call TC Service and get ref_list for dataset.
	 */
	public JSONObject RetrieveDatasetRefList(java.lang.String datasetUID) throws Exception {
		// Create getProperties ref_list JSON template
		String getPropertiesJT = "\r\n" + 
				"{\r\n" + 
				"    \"objects\": [\r\n" + 
				"        \"{1}\"\r\n" + 
				"    ],\r\n" + 
				"    \"attributes\": [\r\n" + 
				"        \"ref_list\"\r\n" + 
				"    ]\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"";
		
		// substitutions for createDatasets
		ArrayList<String> Substitutions = createSubstitutionsFor_getProperties(datasetUID);
		getPropertiesJT = createServiceInput( getPropertiesJT, Substitutions );
		
		return TcConnection.callTeamcenterService(context, Constants.OPERATION_GETPROPERTIES , getPropertiesJT, new JSONObject(), configurationName);
	}

	/*
	 * Retrieve ImanFile UID & File name
	 */
	public List<JSONObject> RetrieveFileListUIDs(JSONObject file_listResponse, java.lang.String datasetUID, boolean detailsRequired) throws Exception {
		List<JSONObject> iMendixObjectList = new ArrayList<JSONObject>();

		JServiceData object = (JServiceData)file_listResponse;
		List<JModelObject> plainObjects = object.getPlainObjects();
		
		for ( int i = 0 ; i < plainObjects.size() ; i ++ )
		{
			JModelObject dataSet = plainObjects.get(i);
			
			if( dataSet != null && dataSet.getUID().equals( datasetUID ))
			{
				try {
					List<String> uiValues = dataSet.getPropertyValues("ref_list");
					List<JModelObject> dbValues = dataSet.getPropertyValueAsModelObjects("ref_list");
					for( int j = 0 ; j < dbValues.size() ; j ++ )
					{
						JSONObject datasetType = new JSONObject();
						datasetType.put("modelName", uiValues.get(j));
						datasetType.put("fileUID", dbValues.get(j).getUID());

						if (detailsRequired) {
							JSONObject details = getFileDetails(dbValues.get(j).getUID());
							JModelObject detailObj = ((JServiceData)details).getPlainObjects().get(0);
							
							datasetType.put("fileSize", detailObj.getPropertyValues("file_size").get(0));
							datasetType.put("owner", detailObj.getPropertyValues("owning_user").get(0));
							datasetType.put("creationDate", detailObj.getPropertyValueAsStrings("creation_date").get(0));
							datasetType.put("lastModDate", detailObj.getPropertyValueAsStrings("last_mod_date").get(0));
						}

						iMendixObjectList.add(datasetType);
					}
				} catch (NotLoadedExcpetion e) {
					/*
					 * In catch means there is empty ref_list. This is valid scenario and ignore this exception 
					 */
				}
			}
		}
		
		return iMendixObjectList;
	}
	
	private static String checkForNull(String input)
	{
		return input.length()>0 ? input : "";
	}

	private static ArrayList<String> createSubstitutionsFor_getProperties(java.lang.String datasetUID) throws CoreException {
		ArrayList<String> Substitutions = new java.util.ArrayList<String>();
		Substitutions.add(checkForNull(datasetUID));
		return Substitutions;
	}

	private JtOutputStream downloadFilesFromFMS(JSONObject getFileReadTicketsResponse) 
			throws FSCException, IOException {
		/*
		 * Initialize FMS
		 */
		String[] fmsURLs = retrieveFMSURLs();
		CommonsFSCWholeFileIOImpl fscFileIOImpl = initializeFMS( fmsURLs );
		
		/*
		 * Parse getFileTicketsResponse and get ticket(s) information from that.
		 */

		JSONArray tickets = getFileReadTicketsResponse.getJSONArray("tickets");
		
		
		/*
		 *  getFileReadTickets returns response having 2 arrays under 'tickets'.
		 *  First array would contain ImanFile objects
		 *  Second array contains list of corresponding tickets
		 *  --------------------------------------------------------------------
		 * 	{
		 *		tickets:
		 *		[
		 *			["ImanFileObjects"],
		 *			["Tickets"]
		 *		],
		 *		serviceData: "IServiceData"
		 *	}
		 *	--------------------------------------------------------------------
		 */
		for( int i = 0 ; i < tickets.getJSONArray(1).length() ; i++ )
		{
			//TODO, Need change for Assembly JT data
			/*
			 * Get ticket & other required information
			 */
			String fileTicket = tickets.getJSONArray(1).getString( i );
			
			/*
			 * Create stream to get file from FMS
			 */
			JtOutputStream os = new JtOutputStream();

			/*
			 * Get stream from FMS
			 */
			fscFileIOImpl.download("TCM", fmsURLs, fileTicket, os);
			os.close();

			return os;
		}
		return null;
	}

	private CommonsFSCWholeFileIOImpl initializeFMS(String[] fmsURLs) throws UnknownHostException, FSCException {
		CommonsFSCWholeFileIOImpl fscFileIOImpl;
		fscFileIOImpl = new CommonsFSCWholeFileIOImpl();
		final InetAddress clientIP = InetAddress.getLocalHost();
		fscFileIOImpl.init(clientIP.getHostAddress(), fmsURLs, fmsURLs);		
		return fscFileIOImpl;
	}

	/*
	 * retrieve FMS URL from active TC configuration
	 */
	private String[] retrieveFMSURLs() {
		TeamcenterConfiguration config = tcconnector.proxies.microflows.Microflows.retrieveTeamcenterConifgurationByName(context, configurationName); 
		String FMSURL = config.getFMSURL(context);
		String[] bootstrapURLs = FMSURL.split(",");
		return bootstrapURLs;
	}

	/*
	 * get ImanFile UIDs from dataset response
	 */
	private JSONObject generateBody_getFileReadTickets(List<JSONObject> ref_list) {
		JSONObject body = new JSONObject();
		JSONArray filesVec = new JSONArray();
		for(int cnt=0; cnt < ref_list.size(); ++cnt)
		{
			JSONObject file = new JSONObject();
			JSONObject ImanFileObject = ref_list.get(cnt);
			file.put("uid",ImanFileObject.getString("fileUID"));
			filesVec.put(file);
		}
		body.put("files",filesVec);
		return body;
	}
	
	private static String createServiceInput(String jsonTemplate, ArrayList<String> substitutions)
	{
		for(int i=0; i<substitutions.size(); i++)
		{
			String replacement 	= substitutions.get(i);
			String target       = "{"+(i+1)+"}";
			jsonTemplate 		= jsonTemplate.replace(target, replacement);
		}
		return jsonTemplate;
	}
	
	private static String createServiceInput(String jsonTemplate, String objs) throws CoreException
	{
		ArrayList<String> allSubstitutions = createSubstitutionsFor_getProperties(objs);
		return createServiceInput( jsonTemplate, allSubstitutions );
	}
	
	/*
	 * Call TC Service and get tickets of ImanFile(s)
	 */
	private JSONObject getFileReadTickets(String getFileReadTicketsJT, String configurationName) throws Exception {
		JSONObject emptyPolicy = new JSONObject("{\r\n" + 
				"    \"types\": [\r\n" + 
				"        {\r\n" + 
				"            \"name\": \"ImanFile\",\r\n" + 
				"            \"properties\": [\r\n" + 
				"                {\r\n" + 
				"                    \"name\": \"original_file_name\"\r\n" + 
				"                }\r\n" + 
				"            ]\r\n" + 
				"        }\r\n" + 
				"    ]\r\n" + 
				"}");
		return TcConnection.callTeamcenterService(context, Constants.OPERATION_GETFILEREADTICKETS , getFileReadTicketsJT, emptyPolicy, configurationName);
	}

	private JSONObject getFileDetails(String fileUID) throws Exception {
		String getPropertiesJT = "\r\n" + 
				"{\r\n" + 
				"    \"objects\": [\r\n" + 
				"        \"{1}\"\r\n" + 
				"    ],\r\n" + 
				"    \"attributes\": [\r\n" + 
	            "        \"file_size\",\r\n" + 
	            "        \"owning_user\",\r\n" + 
	            "        \"creation_date\",\r\n" + 
                "        \"last_mod_date\",\r\n" + 
	            "        \"status_flag\"\r\n" + 
				"    ]\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"";
		
		// substitutions for createDatasets
		ArrayList<String> Substitutions = createSubstitutionsFor_getProperties(fileUID);
		getPropertiesJT = createServiceInput( getPropertiesJT, Substitutions );
		return TcConnection.callTeamcenterService(context, Constants.OPERATION_GETPROPERTIES , getPropertiesJT, new JSONObject(), configurationName);
	}
	
	public static ByteBuffer getProductStructure(IContext context, String bomLineId, JSONObject bomLineQuery) throws Exception {
		// expand PS with default minimum amount of properties to minimize the duplicated data
		JSONObject blList = expandPSAllLevels(context, bomLineId);

		// collect and initialize the PS nodes
		HashMap<String, ItemRevision> itemRevisionsMap = new HashMap<String, ItemRevision>();
		HashMap<String, Occurrence> occurrencesMap = new HashMap<String, Occurrence>();
		HashMap<String, ItemRevision> modelDataset2ItemRevisionMap = new HashMap<String, ItemRevision>();
		HashMap<String, ItemRevision> massProps2ItemRevisionMap = new HashMap<String, ItemRevision>();

        HashSet<String> uids = new HashSet<String>();
		ItemRevision root = collectPSNodes(context, blList, itemRevisionsMap, occurrencesMap, modelDataset2ItemRevisionMap, massProps2ItemRevisionMap, uids);
		if (root == null)
		{
			return null;
		}
		
		// get properties for the previously collected PS nodes
		JSONObject propList = getTCObjectsProperties(context, uids);
        populatePSNodesProperties(occurrencesMap, itemRevisionsMap, modelDataset2ItemRevisionMap, massProps2ItemRevisionMap, propList);

        // get pack info if is_packed_by_default == true
        getPackInfo(bomLineQuery, itemRevisionsMap);

        // output as flatbuffer
		ByteBuffer fb = TCUtils.exportToFlatBuffer(root);
		return fb;
	}
	

	private static JSONObject expandPSAllLevels(IContext context, String bomLineId) throws CoreException, Exception {
		// Create ExpandPSAllLevels JSON template
		String expandPSAll = "\r\n" + 
				"{\r\n" +
				"    \"input\": {\r\n" + 
				"        \"excludeFilter\": \"None\",\r\n" +
				"        \"parentBomLines\": [{1}]\r\n" + 
				"    },\r\n" + 
				"    \"pref\": {\r\n" +
				"        \"expItemRev\": true,\r\n" +
				"        \"info\": []\r\n" +
				"    }\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"";
		
		expandPSAll = createServiceInput( expandPSAll, "{\"uid\":\"" + bomLineId + "\",\"className\":\"BOMLine\",\"type\":\"BOMLine\"}");
		
		// expand PS all levels
		JSONObject blList = TcConnection.callTeamcenterService(context, "Cad-2007-01-StructureManagement/expandPSAllLevels", expandPSAll, new JSONObject(), configurationName);
		return blList;
	}

	private static ItemRevision collectPSNodes(IContext context, JSONObject blList,
			HashMap<String, ItemRevision> itemRevisionsMap, HashMap<String, Occurrence> occurrencesMap,
			HashMap<String, ItemRevision> modelDataset2ItemRevisionMap, HashMap<String, ItemRevision> massProps2ItemRevisionMap,
			HashSet<String> uids) throws CoreException, Exception {
		if (blList != null)
		{
			JSONArray bls = blList.getJSONArray("output");
			if (bls != null)
			{
				HashMap<String, ItemRevision> masterDataset2ItemRevisionMap = new HashMap<String, ItemRevision>();
				HashSet<String> masterDataSetUids = new HashSet<String>();
				 
				String uid;
				for (int i = 0; i < bls.length(); ++i)
				{
					JSONObject parent = bls.getJSONObject(i).has("parent") ? bls.getJSONObject(i).getJSONObject("parent") : null;
					if (parent == null)
						continue;
					
					Occurrence newOccu = null;
					ItemRevision itemRev = null;
					
					// collect occurrences referred by BOM lines
					JModelObject bl = parent.has("bomLine") ? (JModelObject)parent.get("bomLine") : null;
					if (bl != null)
					{
						if (!parent.has("itemRevOfBOMLine"))
						{
							continue;
						}
						
						uid = bl.getUID();
						uids.add(uid);
						
						if (occurrencesMap.get(uid) == null)
						{
							newOccu = new Occurrence(uid);
							occurrencesMap.put(uid, newOccu);
						}
					}

					// collect item revisions
					JModelObject ir = parent.has("itemRevOfBOMLine") ? (JModelObject)parent.get("itemRevOfBOMLine") : null;
					if (ir != null)
					{
						uid = ir.getUID();
						uids.add(uid);
						
						itemRev = itemRevisionsMap.get(uid);
						if (itemRev == null)
						{
							itemRev = new ItemRevision(uid);
							itemRevisionsMap.put(uid, itemRev);
						}
						
						if (newOccu != null)
						{
							newOccu.itemRevision = itemRev;
						}
					}
					
					// collect datasets containing JT models
					JSONArray datasets = parent.has("parentDatasets") ? parent.getJSONArray("parentDatasets") : null;
					if (datasets != null && itemRev != null)
					{
						for (int j = 0; j < datasets.length(); ++j)
						{
							JModelObject ds = (JModelObject)datasets.getJSONObject(j);
							if (ds != null && ds.getType().equalsIgnoreCase("DirectModel"))
							{
								uid = ds.getUID();
								uids.add(uid);
								modelDataset2ItemRevisionMap.put(uid, itemRev);
							}
							if (ds != null && ds.getType().equalsIgnoreCase("UGMASTER"))
							{
								uid = ds.getUID();
								masterDataSetUids.add(uid);
								masterDataset2ItemRevisionMap.put(uid, itemRev);
							}
						}
					}
					
					// collect children occurrences belonging to the item revision
					// only collect once for each item revision
					// Although different occurrences of this item revision have different child occurrences,
					// these child occurrences should have same properties.
					if (itemRev != null && itemRev.getChildren().isEmpty())
					{
						JSONArray children = bls.getJSONObject(i).has("children") ? bls.getJSONObject(i).getJSONArray("children") : null;
						for (int j = 0; j < children.length(); ++j)
						{
							JModelObject childBl = children.getJSONObject(j).has("bomLine") ? (JModelObject)children.getJSONObject(j).get("bomLine") : null;
							JModelObject childIr = children.getJSONObject(j).has("itemRevOfBOMLine") ? (JModelObject)children.getJSONObject(j).get("itemRevOfBOMLine") : null;
							if (childBl != null && childIr != null)
							{
								uid = childBl.getUID();
								var childOccu = occurrencesMap.get(uid);
								if (childOccu == null)
								{
									childOccu = new Occurrence(uid);
									occurrencesMap.put(uid, childOccu);
									
									uid = childIr.getUID();
									var childItemRev = itemRevisionsMap.get(uid);
									if (childItemRev == null)
									{
										childItemRev = new ItemRevision(uid);
										itemRevisionsMap.put(uid, childItemRev);
									}								
									childOccu.itemRevision = childItemRev;
								}
								itemRev.addChild(childOccu);
							}	
						}
					}
				}
				
				// collect UGPartMassPropsForm from UGMASTER datasets
				if (!masterDataSetUids.isEmpty())
				{
					TCGetPropertiesParam param = new TCGetPropertiesParam();
					param.add("ref_list", false, true);
					var params = new HashMap<String, TCGetPropertiesParam>();
					params.put("Dataset", param);
					JSONObject propList = getTCObjectsProperties(context, masterDataSetUids, params);
			        JServiceData serviceData = (JServiceData)propList;
					if (serviceData != null)
					{
						List<JModelObject> objs = serviceData.getPlainObjects();
						for (int i = 0; i < objs.size(); ++i)
						{
							JModelObject obj = objs.get(i);
							String dsuid = obj.getUID();
							ItemRevision itemRev = masterDataset2ItemRevisionMap.get(dsuid);
							if (itemRev != null)
							{
								try {
									List<JModelObject> refs = obj.getPropertyValueAsModelObjects("ref_list");
									Iterator<JModelObject> itRef = refs.iterator();
									while (itRef.hasNext())
									{
										JModelObject ref = itRef.next();
										if (ref.getType().equalsIgnoreCase("UGPartMassPropsForm"))
										{
											uid = ref.getUID();
											uids.add(uid);
											massProps2ItemRevisionMap.put(uid, itemRev);
										}
									}
								} catch (Exception e) {}
							}
						}
					}
				}
			}
		}
		
		var root = findRoot(itemRevisionsMap, occurrencesMap);
		return root;
	}
	
	private static ItemRevision findRoot(HashMap<String, ItemRevision> itemRevisionsMap, HashMap<String, Occurrence> occurrencesMap)
	{
		var subItemRevisions = new HashMap<String, ItemRevision>();
		Iterator<ItemRevision> itItemRevisions = itemRevisionsMap.values().iterator();
		while (itItemRevisions.hasNext())
		{
			var ir = itItemRevisions.next();
			var children = ir.getChildren();
			for (int i = 0; i < children.size(); ++i)
			{
				subItemRevisions.put(children.get(i).itemRevision.getUid(), children.get(i).itemRevision);
			}
		}
		itItemRevisions = itemRevisionsMap.values().iterator();
		while (itItemRevisions.hasNext())
		{
			var ir = itItemRevisions.next();
			if (!subItemRevisions.containsKey(ir.getUid()))
			{
				return ir;
			}
		}
		
		return null;
	}

	private static JSONObject getTCObjectsProperties(IContext context, HashSet<String> uids)
			throws CoreException, Exception {
		var params = new HashMap<String, TCGetPropertiesParam>();
		
		TCGetPropertiesParam param = new TCGetPropertiesParam();
		param.add("object_string", true, false);
		param.add("item_revision_id", true, false);
		param.add("item_id", true, false);
		param.add("sequence_id", true, false);
		param.add("object_name", true, false);
		param.add("creation_date", true, false);
		param.add("last_mod_date", true, false);
		param.add("last_mod_user", true, false);
		param.add("owning_user", true, false);
		params.put("ItemRevision", param);
		
		param = new TCGetPropertiesParam();
		param.add("bl_occurrence_uid", true, false);
		param.add("bl_occ_xform_matrix", true, false); // in model unit
//		param.add("bl_abs_xform_matrix", true, false); // in model unit
		param.add("bl_plmxml_occ_xform", true, false); // in meters
//		param.add("bl_plmxml_abs_xform", true, false); // in meters
		// part bounding box in meters
		// assemblies bounding box only covers its own geometry, not including its children
		// So we don't get the bounding box and let the client compute it.
//		param.add("bl_bounding_boxes", true, false);
		param.add("bl_sequence_no", true, false);
		params.put("BOMLine", param);
		
		param = new TCGetPropertiesParam();
		param.add("ref_list", false, true);
		param.add("ref_names", true, false);
		params.put("Dataset", param);
		
		param = new TCGetPropertiesParam();
		for (int j = 0; j < massPropNames.length; ++j)
		{
			param.add(massPropNames[j][0], true, false);
		}
		params.put("UGPartMassPropsForm", param);
				
		JSONObject propList = getTCObjectsProperties(context, uids, params);
		return propList;
	}

	private static JSONObject getTCObjectsProperties(IContext context, HashSet<String> uids,
			HashMap<String, TCGetPropertiesParam> params) throws CoreException, Exception
	{
		// Create getProperties JSON template
		String getProperties = "\r\n" + 
				"{\r\n" +
				"    \"objects\": {1},\r\n" + 
				"    \"attributes\": []\r\n" +
				"}\r\n" + 
				"\r\n" + 
				"";
		
		JSONArray uidArray = new JSONArray();
		Iterator<String> it = uids.iterator();
		while(it.hasNext())
		{
			JSONObject uidObj = new JSONObject();
			uidObj.put("uid", it.next());
			uidArray.put(uidObj);
		}
		String strObjs = uidArray.toString();
		getProperties = createServiceInput( getProperties, strObjs );

		String policy = 
					"{\r\n" + 
					"	types: [\r\n";
		var entries = params.entrySet();
		var itEntry = entries.iterator();
		while (itEntry.hasNext())
		{
			var entry = itEntry.next();

			policy +=
					"		{\r\n" +
					"			name: \"" + entry.getKey() + "\",\r\n" + 
					"			properties: [\r\n";
			TCGetPropertiesParam param = entry.getValue();
			for (int j = 0; j < param.size(); ++j)
			{
				boolean uiValue = param.isUiValueRequired(j);
				boolean dbValue = param.isDbValueRequired(j);
				policy += 			
					"				{\r\n" + 
					"					name: \"" + param.getPropName(j) + "\",\r\n";
				if (uiValue != dbValue)
				{
					policy += 			
					"					modifiers: [{ name: \"" + (uiValue ? "uIValueOnly" : "excludeUiValues") + "\", Value: \"true\" }],\r\n";
				}
				policy += 			
					"				},\r\n"; 

			}
			policy += 			
					"			],\r\n" + 
					"		},\r\n";
		}
		policy += 
					"	],\r\n" +
					"}";
		
		JSONObject propList = TcConnection.callTeamcenterService(context, "Core-2006-03-DataManagement/getProperties", getProperties, new JSONObject(policy), configurationName);
		return propList;
	}

	// set the properties for item revisions and occurrences
	private static void populatePSNodesProperties(HashMap<String, Occurrence> occurrencesMap,
			HashMap<String, ItemRevision> itemRevisionsMap, HashMap<String, ItemRevision> modelDataset2ItemRevisionMap,
			HashMap<String, ItemRevision> massProps2ItemRevisionMap, JSONObject propList) {
        JServiceData serviceData = (JServiceData)propList;
		if (serviceData != null)
		{
			List<JModelObject> objs = serviceData.getPlainObjects();
			for (int i = 0; i < objs.size(); ++i)
			{
				JModelObject obj = objs.get(i);
				String uid = obj.getUID();
				
				Occurrence occu = occurrencesMap.get(uid);
				if (occu != null)
				{
					// xform values are stored as one array element in obj's property
					String[] values = null;
					values = readProperty(obj, "bl_plmxml_occ_xform").split(" ");
					if (values != null && values.length >= 16)
					{
						double dValues[] = new double[16];
						for (int j = 0; j < 16; ++j)
						{
							dValues[j] = Double.parseDouble(values[j]);
							occu.matrix[j] = dValues[j];
						}
					}
					
					values = null;
					values = readProperty(obj, "bl_bounding_boxes").split(",");
					if (values != null && values.length == 6)
					{
						occu.itemRevision.getMetadata().put("CAD_X_MIN", values[0].trim());
						occu.itemRevision.getMetadata().put("CAD_Y_MIN", values[1].trim());
						occu.itemRevision.getMetadata().put("CAD_Z_MIN", values[2].trim());
						occu.itemRevision.getMetadata().put("CAD_X_MAX", values[3].trim());
						occu.itemRevision.getMetadata().put("CAD_Y_MAX", values[4].trim());
						occu.itemRevision.getMetadata().put("CAD_Z_MAX", values[5].trim());
					}
				
					occu.sequenceNumber = readProperty(obj, "bl_sequence_no");
				}

				ItemRevision itemRev = itemRevisionsMap.get(uid);
				if (itemRev != null)
				{
					itemRev.setObjectString(readProperty(obj, "object_string"));
					itemRev.setItemId(readProperty(obj, "item_id"));
					itemRev.setItemRevisionId(readProperty(obj, "item_revision_id"));
					itemRev.setRevSequenceId(readProperty(obj, "sequence_id"));
					itemRev.setObjectName(readProperty(obj, "object_name"));
					itemRev.getMetadata().put("TC_CREATION_DATE", readProperty(obj, "creation_date"));
					itemRev.getMetadata().put("TC_LAST_MODIFYING_DATE", readProperty(obj, "last_mod_date"));
					itemRev.getMetadata().put("TC_LAST_MODIFYING_USER", readProperty(obj, "last_mod_user"));
					itemRev.getMetadata().put("TC_OWNING_USER", readProperty(obj, "owning_user"));
				}
				
				itemRev = modelDataset2ItemRevisionMap.get(uid);
				if (itemRev != null && (itemRev.modelRef == null || itemRev.modelRef.length() == 0))
				{
					if (obj.getType().equalsIgnoreCase("DirectModel"))
					{
						List<JModelObject> refs = null;
						List<String> _refNames = null;
						try {
							refs = obj.getPropertyValueAsModelObjects("ref_list");
							_refNames = obj.getPropertyValues("ref_names");
						} catch (Exception e) {
						}
						
						if (refs != null && _refNames != null) {
							String[] names = _refNames.get(0).split(",");
							List<String> refNames = new ArrayList<String>();
							for (int n = 0; n < names.length; ++n)
							{
								refNames.add(names[n].trim());
							}
							
							Iterator<JModelObject> itRef = refs.iterator();
							Iterator<String> itRefName = refNames.iterator();
							while (itRef.hasNext() && itRefName.hasNext())
							{
								String refName = itRefName.next();
								JModelObject ref = itRef.next();
								if (refName.equalsIgnoreCase("JTPART"))
								{
									itemRev.modelRef = ref.getUID();
									break;
								}
							}
						}
					}
				}
				
				itemRev = massProps2ItemRevisionMap.get(uid);
				if (itemRev != null)
				{
					if (obj.getType().equalsIgnoreCase("UGPartMassPropsForm"))
					{
						for (int j = 0; j < massPropNames.length; ++j)
						{
							if (filterProperties(massPropNames[j][0]))
							{
								itemRev.getMetadata().put(massPropNames[j][1], readProperty(obj, massPropNames[j][0]));
							}
						}
						
						// special handling:
						// combine the following groups of properties:
						//	{
						//		"com_x", "CAD_CENTER_OF_GRAVITY_X"
						//		"com_y", "CAD_CENTER_OF_GRAVITY_Y"
						//		"com_z", "CAD_CENTER_OF_GRAVITY_Z"
						//	},
						// 	{
						//		"moi_xx", "CAD_MOMENT_OF_INERTIA_XX"
						//		"moi_yy", "CAD_MOMENT_OF_INERTIA_YY"
						//		"moi_zz", "CAD_MOMENT_OF_INERTIA_ZZ"
						//		"poi_xy", "CAD_PRODUCT_OF_INERTIA_XY"
						//		"poi_xz", "CAD_PRODUCT_OF_INERTIA_XZ"
						//		"poi_yz", "CAD_PRODUCT_OF_INERTIA_YZ"
						//	}
						// to these single properties (space separated) respectively:
						//	CAD_CENTER_OF_GRAVITY
						//	CAD_MOMENT_OF_INERTIA
						try {
							String x = readProperty(obj, "com_x");
							String y = readProperty(obj, "com_y");
							String z = readProperty(obj, "com_z");
							itemRev.getMetadata().put("CAD_CENTER_OF_GRAVITY",
								x + " " + y + " " + z);
						} catch (Exception e) {}
						try {
							String xx = readProperty(obj, "moi_xx");
							String yy = readProperty(obj, "moi_yy");
							String zz = readProperty(obj, "moi_zz");
							String xy = readProperty(obj, "poi_xy");
							String xz = readProperty(obj, "poi_xz");
							String yz = readProperty(obj, "poi_yz");
							itemRev.getMetadata().put("CAD_MOMENT_OF_INERTIA",
								xx + " " + yy + " " + zz + " " + xy + " " + xz + " " + yz);
						} catch (Exception e) {}
					}
				}
			}
		}
	}
	
	private static boolean filterProperties(String propName)
	{
		// special handling:
		// following properties should be combined:
		if (propName.equalsIgnoreCase("com_x") || 
			propName.equalsIgnoreCase("com_y") || 
			propName.equalsIgnoreCase("com_z") || 
			propName.equalsIgnoreCase("moi_xx") || 
			propName.equalsIgnoreCase("moi_yy") || 
			propName.equalsIgnoreCase("moi_zz") || 
			propName.equalsIgnoreCase("poi_xy") || 
			propName.equalsIgnoreCase("poi_xz") || 
			propName.equalsIgnoreCase("poi_yz"))
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	private static String readProperty(JModelObject obj, String propName)
	{
		try {
			return obj.getPropertyValue(propName);
		} catch (Exception e) {}
		return "";
	}
	
	public static String getBOMLine(IContext ctx, JSONObject bomLineQuery) throws Exception
	{
		if (bomLineQuery != null && bomLineQuery.length() > 0) {
			// always get the unpacked BOM line
			var bomLineQuery2 = new JSONObject(bomLineQuery.toString());
			JSONObject info = bomLineQuery2.getJSONArray("info").getJSONObject(0);
			if (!info.has("bomWinPropFlagMap")) {
				info.put("bomWinPropFlagMap", new JSONObject());
			}
			var bomWinPropFlagMap = info.getJSONObject("bomWinPropFlagMap");
			bomWinPropFlagMap.put("is_packed_by_default", "false");

			JSONObject jsonPolicy      = new JSONObject();
			JSONObject jsonResponseObj = TcConnection.callTeamcenterService(ctx, Constants.OPERATION_CREATEBOMWINDOWS2, bomLineQuery2, jsonPolicy, configurationName);
			JSONArray output = jsonResponseObj.getJSONArray("output");
			if (output.length() > 0)
			{
				var bl = (JModelObject)output.getJSONObject(0).get("bomLine");
				String bomLineId = bl.getUID();
				return bomLineId;
			}
		}
		return "";
	}
			
	public static JSONObject getBOMLineQueryInput(String bomLineQuery)
	{
		if (bomLineQuery == null || bomLineQuery.isEmpty()) {
			return null;
		}
	
		var flagMap = Core.getMetaObject("TcConnector.BomWindowPropFlagMap");
		JSONObject query = new JSONObject("{\r\n" +
			"\"info\": [{\r\n" + 
			"	\"bomView\": \"\",\r\n" + 
			"	\"item\": \"\",\r\n" + 
			"	\"itemRev\": {\r\n" + 
			"		\"uid\": \"AAAAAAAAAAAAAA\",\r\n" + 
			"		\"className\": \"ItemRevision\",\r\n" + 
			"		\"type\": \"ItemRevision\"\r\n" + 
			"	},\r\n" + 
			"	\"clientId\": \"CreateBOMWindows\",\r\n" + 
			"	\"revRuleConfigInfo\": {\r\n" + 
			"		\"clientId\": \"\",\r\n" + 
			"		\"revRule\": {\r\n" + 
			"			\"uid\": \"AAAAAAAAAAAAAA\",\r\n" + 
			"			\"className\": \"RevisionRule\",\r\n" + 
			"			\"type\": \"RevisionRule\"\r\n" + 
			"		},\r\n" + 
			"		\"props\": {\r\n" + 
			"			\"date\": \"\",\r\n" + 
			"			\"endItem\": \"\",\r\n" + 
			"			\"unitNo\": -1,\r\n" + 
			"			\"today\": true,\r\n" + 
			"			\"endItemRevision\": \"\",\r\n" + 
			"			\"overrideFolders\": [{\r\n" + 
			"				\"folder\": \"\",\r\n" + 
			"				\"ruleEntry\": \"\"\r\n" + 
			"			}]\r\n" + 
			"		}\r\n" + 
			"	},\r\n" + 
			"	\"bomWinPropFlagMap\": {\r\n" + 
			"		\"show_suppressed_occurrences\": \"" + flagMap.getMetaPrimitive("show_suppressed_occurrences").getDefaultValue() + "\",\r\n" + 
			"		\"show_out_of_context_lines\": \"" + flagMap.getMetaPrimitive("show_out_of_context_lines").getDefaultValue() + "\",\r\n" + 
			"		\"is_packed_by_default\": \"" + flagMap.getMetaPrimitive("is_packed_by_default").getDefaultValue() + "\",\r\n" + 
			"		\"fnd0bw_in_cv_cfg_to_load_md\": \"" + flagMap.getMetaPrimitive("fnd0bw_in_cv_cfg_to_load_md").getDefaultValue() + "\",\r\n" + 
			"		\"fnd0show_uncnf_occ_eff\": \"" + flagMap.getMetaPrimitive("fnd0show_uncnf_occ_eff").getDefaultValue() + "\",\r\n" + 
			"		\"show_unconfigured_variants\": \"" + flagMap.getMetaPrimitive("show_unconfigured_variants").getDefaultValue() + "\",\r\n" + 
			"		\"show_unconfigured_changes\": \"" + flagMap.getMetaPrimitive("show_unconfigured_changes").getDefaultValue() + "\"\r\n" + 
			"	},\r\n" + 
			"	\"configContext\": {\r\n" + 
			"		\"uid\": \"AAAAAAAAAAAAAA\",\r\n" + 
			"		\"className\": \"ConfigurationContext\",\r\n" + 
			"		\"type\": \"ConfigurationContext\"\r\n" + 
			"	},\r\n" + 
			"	\"activeAssemblyArrangement\": \"\",\r\n" + 
			"	\"objectsForConfigure\": []\r\n" + 
			"}]\r\n" + 
		"}");

		JSONObject rawQuery = new JSONObject(bomLineQuery);
		JSONObject info = query.getJSONArray("info").getJSONObject(0);

		JSONObject itemRev = info.getJSONObject("itemRev");
		if (rawQuery.has("itemRev"))
		{
			itemRev.put("uid", rawQuery.getJSONObject("itemRev").getString("uid"));
		}
		else
		{
			itemRev.remove("className");
			itemRev.put("type", "UnKnown Type");
		}

		JSONObject revRule = info.getJSONObject("revRuleConfigInfo").getJSONObject("revRule");
		boolean hasRevRule = false;
		if (rawQuery.has("revRuleConfigInfo"))
		{
			JSONObject rawRevRuleConfigInfo = rawQuery.getJSONObject("revRuleConfigInfo");
			if (rawRevRuleConfigInfo.has("revRule"))
			{
				String uid = rawRevRuleConfigInfo.getJSONObject("revRule").getString("uid");
				if (uid.length() > 0)
				{
					revRule.put("uid", uid);
					hasRevRule = true;
				}
			}
		}
		if (!hasRevRule)
		{
			revRule.remove("className");
			revRule.put("type", "UnKnown Type");
		}

		JSONObject bomWinPropFlagMap = info.getJSONObject("bomWinPropFlagMap");
		if (rawQuery.has("bomWinPropFlagMap"))
		{
			JSONObject rawBomWinPropFlagMap = rawQuery.getJSONObject("bomWinPropFlagMap");
			if (rawBomWinPropFlagMap.has("show_suppressed_occurrences"))
			{
				bomWinPropFlagMap.put("show_suppressed_occurrences", rawBomWinPropFlagMap.getString("show_suppressed_occurrences"));
			}
			if (rawBomWinPropFlagMap.has("show_out_of_context_lines"))
			{
				bomWinPropFlagMap.put("show_out_of_context_lines", rawBomWinPropFlagMap.getString("show_out_of_context_lines"));
			}
			if (rawBomWinPropFlagMap.has("is_packed_by_default"))
			{
				bomWinPropFlagMap.put("is_packed_by_default", rawBomWinPropFlagMap.getString("is_packed_by_default"));
			}
			if (rawBomWinPropFlagMap.has("fnd0bw_in_cv_cfg_to_load_md"))
			{
				bomWinPropFlagMap.put("fnd0bw_in_cv_cfg_to_load_md", rawBomWinPropFlagMap.getString("fnd0bw_in_cv_cfg_to_load_md"));
			}
			if (rawBomWinPropFlagMap.has("fnd0show_uncnf_occ_eff"))
			{
				bomWinPropFlagMap.put("fnd0show_uncnf_occ_eff", rawBomWinPropFlagMap.getString("fnd0show_uncnf_occ_eff"));
			}
			if (rawBomWinPropFlagMap.has("show_unconfigured_variants"))
			{
				bomWinPropFlagMap.put("show_unconfigured_variants", rawBomWinPropFlagMap.getString("show_unconfigured_variants"));
			}
			if (rawBomWinPropFlagMap.has("show_unconfigured_changes"))
			{
				bomWinPropFlagMap.put("show_unconfigured_changes", rawBomWinPropFlagMap.getString("show_unconfigured_changes"));
			}
		}

		JSONObject configContext = info.getJSONObject("configContext");
		if (rawQuery.has("configContext"))
		{
			configContext.put("uid", rawQuery.getJSONObject("configContext").getString("uid"));
		}
		else
		{
			configContext.remove("className");
			configContext.put("type", "UnKnown Type");
		}

		JSONArray objectsForConfigure = info.getJSONArray("objectsForConfigure");
		if (rawQuery.has("objectsForConfigure"))
		{
			JSONArray rawObjectsForConfigure = rawQuery.getJSONArray("objectsForConfigure");
			for (int i = 0; i < rawObjectsForConfigure.length(); ++i)
			{
				JSONObject object = rawObjectsForConfigure.getJSONObject(i);
				object.put("className", object.getString("VariantRule"));
				object.put("type", object.getString("VariantRule"));
				objectsForConfigure.put(object);
			}
		}
		if (objectsForConfigure.length() == 0)
		{
			info.remove("objectsForConfigure");
		}

		return query;
	}
	
	private static void getPackInfo(JSONObject bomLineQuery, HashMap<String, ItemRevision> itemRevisionsMap) {
		boolean isPacked = false;
		JSONObject info = bomLineQuery.getJSONArray("info").getJSONObject(0);
		if (info.has("bomWinPropFlagMap")) {
			JSONObject bomWinPropFlagMap = info.getJSONObject("bomWinPropFlagMap");
			if (bomWinPropFlagMap.has("is_packed_by_default")) {
				isPacked = bomWinPropFlagMap.getBoolean("is_packed_by_default");
			}
		}
		if (isPacked) {
			HashMap<ItemRevision, HashMap<String, ArrayList<Occurrence>>> groups = new HashMap<ItemRevision, HashMap<String, ArrayList<Occurrence>>>();
			Iterator<ItemRevision> itItemRevisions = itemRevisionsMap.values().iterator();
			while (itItemRevisions.hasNext()) {
				var ir = itItemRevisions.next();
				var children = ir.getChildren();
				for (int i = 0; i < children.size(); ++i) {
					var child = children.get(i);
					if (!child.sequenceNumber.isEmpty()) {
						if (!groups.containsKey(child.itemRevision)) {
							groups.put(child.itemRevision, new HashMap<String, ArrayList<Occurrence>>());
						}
						var childItem = groups.get(child.itemRevision);
						if (!childItem.containsKey(child.sequenceNumber)) {
							childItem.put(child.sequenceNumber, new ArrayList<Occurrence>());
						}
						var group = childItem.get(child.sequenceNumber);
						group.add(child);
					}
				}
			}
			
			Iterator<ItemRevision> itChildItem = groups.keySet().iterator();
			while (itChildItem.hasNext()) {
				var childItem = itChildItem.next();
				var seqGroup = groups.get(childItem);
				Iterator<ArrayList<Occurrence>> itGroup = seqGroup.values().iterator();
				while (itGroup.hasNext()) {
					var group = itGroup.next();
					if (group.size() > 1) {
						for (var o: group) {
							o.setGroupId(childItem.getItemId() + "_" + o.sequenceNumber);
						}
					}
				}
			}
		}
	}
}
