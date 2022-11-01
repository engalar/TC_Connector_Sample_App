package tcconnector.internal.servicehelper;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.TcConnection;
import tcconnector.internal.foundation.Constants;
import tcconnector.proxies.ModelObject;

public class DeepCopyDataHelper {
	private static final String KEY_DEEP_COPY_DATA_INPUT = "deepCopyDataInput";
	private static final String KEY_OPERATION = "operation";
	private static final String KEY_TARGET_OBJECT = "targetObject";
	private static final String KEY_DEEP_COPY_DATAS = "deepCopyDatas";
	private static final String KEY_PROPERTY_VALUE_MAP = "propertyValuesMap";
	private static final String KEY_ATTACHED_OBJECT = "attachedObject";
	private static final String KEY_CHILD_DEEP_COPY_DATA = "childDeepCopyData";
	private static final String KEY_OPERATION_INPUT_TYPE_NAME = "operationInputTypeName";
	private static final String KEY_OPERATION_INPUTS = "operationInputs";
	private static final String KEY_SELECTED_BO = "selectedBO";
	private static final String KEY_PARENT_DEEP_COPY_DATA = "parentDeepCopyData";
	private static final String KEY_UID = "uid";
	private static final String KEY_TYPE = "type";
	private static final String KEY_PROPERTY_NAME = "propertyName";
	private static final String KEY_PROPERTY_TYPE = "propertyType";
	private static final String KEY_COPY_ACTION = "copyAction";
	private static final String KEY_IS_TARGET_PRIMARY = "isTargetPrimary";
	private static final String KEY_IS_REQUIRED = "isRequired";
	private static final String KEY_COPY_RELATIONS = "copy_relations";
	private static final String KEY_COPYRELATIONS = "copyRelations";

	/**
	 * call the getDeepCopyData SOA.
	 * 
	 * @param targetObject The service operation response, may be a ServiceData or
	 *                     custom data structure.
	 * @throws Exception
	 */
	public static JSONObject callGetDeepCopyDataSOA(IContext context, ModelObject targetObject, String operationName,
			ModelObject parentObject, String configurationName) throws Exception {
		JSONObject deepCopyDataInput = new JSONObject();
		deepCopyDataInput.put(KEY_OPERATION, operationName);

		JSONObject tgtObject = new JSONObject();
		if (targetObject != null) {
			tgtObject.put(KEY_UID, targetObject.getUID());
			tgtObject.put(KEY_TYPE, targetObject.get_Type());
		}
		deepCopyDataInput.put(KEY_TARGET_OBJECT, tgtObject);
		deepCopyDataInput.put(KEY_SELECTED_BO, tgtObject);

		JSONObject deepCopyData = new JSONObject();
		deepCopyData.put(KEY_ATTACHED_OBJECT, tgtObject);
		JSONObject propertyValuesMap = new JSONObject();
		deepCopyData.put(KEY_PROPERTY_VALUE_MAP, propertyValuesMap);
		deepCopyData.put(KEY_OPERATION_INPUT_TYPE_NAME, "");
		JSONArray childDeepCopyData = new JSONArray();
		deepCopyData.put(KEY_CHILD_DEEP_COPY_DATA, childDeepCopyData);
		JSONObject operationInputs = new JSONObject();
		deepCopyData.put(KEY_OPERATION_INPUTS, operationInputs);
		JSONArray deepCopyDatas = new JSONArray();
		deepCopyDatas.put(deepCopyData);
		deepCopyDataInput.put(KEY_DEEP_COPY_DATAS, deepCopyDatas);

		JSONObject parentDeepCopyData = new JSONObject();

		JSONObject parentObj = new JSONObject("{'uid': 'AAAAAAAAAAAAAA', 'type': 'unknownType'}");
		if (parentObject != null) {
			parentObj.put(KEY_UID, parentObject.getUID());
			parentObj.put(KEY_TYPE, parentObject.get_Type());
		}
		parentDeepCopyData.put(KEY_ATTACHED_OBJECT, parentObj);
		JSONObject parentPropertyValuesMap = new JSONObject();
		parentDeepCopyData.put(KEY_PROPERTY_VALUE_MAP, parentPropertyValuesMap);
		parentDeepCopyData.put(KEY_OPERATION_INPUT_TYPE_NAME, "");
		JSONArray parentChildDeepCopyData = new JSONArray();
		parentDeepCopyData.put(KEY_CHILD_DEEP_COPY_DATA, parentChildDeepCopyData);
		JSONObject parentOperationInputs = new JSONObject();
		parentDeepCopyData.put(KEY_OPERATION_INPUTS, parentOperationInputs);
		deepCopyDataInput.put(KEY_PARENT_DEEP_COPY_DATA, parentDeepCopyData);

		JSONObject getDeepCopyDataInput = new JSONObject();
		getDeepCopyDataInput.put(KEY_DEEP_COPY_DATA_INPUT, deepCopyDataInput);

		return TcConnection.callTeamcenterService(context, Constants.OPERATION_GETDEEPCOPYDATA, getDeepCopyDataInput,
				null, configurationName);
	}

	/**
	 * call the getDeepCopyData SOA.
	 * 
	 * @param targetObject The service operation response, may be a ServiceData or
	 *                     custom data structure.
	 */
	public static JSONObject processDeepCopyDatas(JSONObject responseDeepCopyData) {
		JSONObject deepCopyData = new JSONObject();
		JSONObject propertyValuesMap = responseDeepCopyData.getJSONObject(KEY_PROPERTY_VALUE_MAP);

		String propertyName = ((JSONArray) propertyValuesMap.get(KEY_PROPERTY_NAME)).getString(0);
		deepCopyData.put(KEY_PROPERTY_NAME, propertyName);

		String propertyType = ((JSONArray) propertyValuesMap.get(KEY_PROPERTY_TYPE)).getString(0);
		deepCopyData.put(KEY_PROPERTY_TYPE, propertyType);

		String copyAction = ((JSONArray) propertyValuesMap.get(KEY_COPY_ACTION)).getString(0);
		deepCopyData.put(KEY_COPY_ACTION, copyAction);

		String isTargetPrimaryStr = ((JSONArray) propertyValuesMap.get(KEY_IS_TARGET_PRIMARY)).getString(0);
		Boolean isTargetPrimary = getStringToBoolean(isTargetPrimaryStr);
		deepCopyData.put(KEY_IS_TARGET_PRIMARY, isTargetPrimary);

		String isRequiredStr = ((JSONArray) propertyValuesMap.get(KEY_IS_REQUIRED)).getString(0);
		Boolean isRequired = getStringToBoolean(isRequiredStr);
		deepCopyData.put(KEY_IS_REQUIRED, isRequired);

		String copyRelationsStr = ((JSONArray) propertyValuesMap.get(KEY_COPY_RELATIONS)).getString(0);
		Boolean copyRelations = getStringToBoolean(copyRelationsStr);
		deepCopyData.put(KEY_COPYRELATIONS, copyRelations);

		JSONObject operationInputs = new JSONObject();
		deepCopyData.put(KEY_OPERATION_INPUTS, operationInputs);

		deepCopyData.put(KEY_OPERATION_INPUT_TYPE_NAME, "");

		JSONArray childDeepCopyDatasInput = new JSONArray();
		JSONArray childDeepCopyDatas = responseDeepCopyData.getJSONArray(KEY_CHILD_DEEP_COPY_DATA);
		for (int index = 0; index < childDeepCopyDatas.length(); index++) {
			JSONObject childDeepCopyData = childDeepCopyDatas.getJSONObject(index);
			JSONObject childDeepCopyDataChild = processDeepCopyDatas(childDeepCopyData);
			childDeepCopyDatasInput.put(childDeepCopyDataChild);
		}

		deepCopyData.put(KEY_CHILD_DEEP_COPY_DATA, childDeepCopyDatasInput);

		JSONObject responseAttachedObject = responseDeepCopyData.getJSONObject(KEY_ATTACHED_OBJECT);
		JSONObject attachedObject = new JSONObject();
		attachedObject.put(KEY_UID, responseAttachedObject.get(KEY_UID));
		attachedObject.put(KEY_TYPE, responseAttachedObject.get(KEY_TYPE));
		deepCopyData.put(KEY_ATTACHED_OBJECT, attachedObject);

		return deepCopyData;
	}

	private static boolean getStringToBoolean(String inputStr) {
		if (inputStr.equals("1")) {
			return true;
		}
		return false;
	}
}
