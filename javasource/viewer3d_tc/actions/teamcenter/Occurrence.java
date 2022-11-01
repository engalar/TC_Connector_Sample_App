package viewer3d_tc.actions.teamcenter;

import java.util.HashMap;

public final class Occurrence {
	public String uid;

	public ItemRevision itemRevision;
	
	public double[] matrix = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
	
	public String sequenceNumber = "";
	
	public void setGroupId(String value)
	{
		metadata.put("PACK_GROUP_ID", value);
	};
	
	protected HashMap<String, String> metadata = new HashMap<String, String>();

	public HashMap<String, String> getMetadata()
	{
		return metadata;
	}
	
	public Occurrence(String uid)
	{
		this.uid = uid;
	}
}
