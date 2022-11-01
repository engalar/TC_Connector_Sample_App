package tcconnector.internal.foundation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Map;

import com.mendix.core.Core;
import com.mendix.thirdparty.org.json.JSONObject;

public class OperationMappingCache {
	private static final Map<String, JSONObject> CACHED_MAPS = new Hashtable<>();
	private static final String OPERATION_MAPPING = "OperationMapping/";

	private static final String[] VALID_KEYS = new String[] { OperationMapper.KEY_SERVICE_OPERATION,
			OperationMapper.KEY_INPUT_TYPE, OperationMapper.KEY_RESPONSE_TYPE, OperationMapper.KEY_OBJECT_MAPPING,
			OperationMapper.KEY_OPERATION_INPUT, OperationMapper.KEY_OPERATION_RESPONSE };

	/**
	 * Get the cached mapping template
	 * 
	 * @param mappingSource A file path relative to resources/OperationMapping
	 *                      folder, or an actual JSON string
	 * @return
	 */
	public synchronized static JSONObject getMapping(String mappingSource) {
		JSONObject map = CACHED_MAPS.get(mappingSource);
		if (map == null) {
			if (mappingSource.startsWith("{")) {
				OperationMappingCache cache = new OperationMappingCache();
				map = cache.createTemplate(mappingSource);
			} else {
				OperationMappingCache cache = new OperationMappingCache();
				map = cache.loadFile(mappingSource);
				CACHED_MAPS.put(mappingSource, map);
			}
		}
		return map;
	}

	private String fileName;

	private OperationMappingCache() {
	}

	private JSONObject loadFile(String mappingFile) {
		fileName = mappingFile;
		try {
			InputStream is = getInputStream(mappingFile);
			String mappingJsonDoc = readSteamToEnd(is);
			JSONObject mappingObj = createTemplate(mappingJsonDoc);
			return mappingObj;
		} catch (IOException e) {
			String message = "Failed to load the service operation mapping file (" + fileName + ").";
			Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message + " " + e.getMessage());
			throw new IllegalArgumentException(message);
		}
	}

	private JSONObject createTemplate(String mappingJsonDoc) {
		JSONObject mappingObj = new JSONObject(mappingJsonDoc);
		validateKeys(mappingObj);
		return mappingObj;
	}

	private InputStream getInputStream(String mappingFile) throws IOException {
		File resourcesFolder = Core.getConfiguration().getResourcesPath();
		File mapFile = new File(resourcesFolder, OPERATION_MAPPING + mappingFile);
		if (!mapFile.exists()) {
			mapFile = new File(resourcesFolder, mappingFile);
		}
		fileName = mapFile.getName();

		InputStream is;
		if (mapFile.exists())
			is = new FileInputStream(mapFile);
		else
			is = OperationMapper.class.getClassLoader().getResourceAsStream(mappingFile);

		if (is != null)
			return is;

		throw new IOException("The file does exist in the Resource folder (" + mapFile.getAbsolutePath()
				+ ") or in the ClassPath (" + mappingFile + ").");
	}

	private String readSteamToEnd(InputStream is) throws IOException {
		char[] data = new char[4096];
		StringBuilder document = new StringBuilder();
		InputStreamReader streamReader = new InputStreamReader(is, "UTF-8");

		int numRead = streamReader.read(data, 0, 4095);
		while (numRead != -1) {
			document.append(data, 0, numRead);
			numRead = streamReader.read(data, 0, 4095);
		}
		return document.toString();
	}

	public void validateKeys(JSONObject operationMapObj) {
		for (String key : VALID_KEYS) {
			if (!operationMapObj.has(key)) {
				Constants.LOGGER.error(LogCorrelationID.getId()
						+ ": The operation mapping is missing the required key '" + key + "'.");
				throw new IllegalArgumentException("The ServiceData does not represent a ServiceData.");
			}
		}
	}
}
