package viewer3d_tc.actions.teamcenter;

import java.util.ArrayList;
import java.util.HashMap;

public final class ItemRevision {
	public String getUid()
	{
		return metadata.get("TC_UID");
	};

	public void setUid(String value)
	{
		metadata.put("TC_UID", value);
	};
	
	public String getObjectString()
	{
		return metadata.get("TC_OBJECT_STRING");
	}

	public void setObjectString(String value)
	{
		metadata.put("TC_OBJECT_STRING", value);
	};
	
	public String getItemId()
	{
		return metadata.get("TC_ITEM_ID");
	}

	public void setItemId(String value)
	{
		metadata.put("TC_ITEM_ID", value);
	};
	
	public String getItemRevisionId()
	{
		return metadata.get("TC_ITEM_REVISION_ID");
	}

	public void setItemRevisionId(String value)
	{
		metadata.put("TC_ITEM_REVISION_ID", value);
	};
	
	public String getRevSequenceId()
	{
		return metadata.get("TC_REVISION_SEQUENCE_ID");
	}

	public void setRevSequenceId(String value)
	{
		metadata.put("TC_REVISION_SEQUENCE_ID", value);
	};

	public String getObjectName()
	{
		return metadata.get("TC_OBJECT_NAME");
	}

	public void setObjectName(String value)
	{
		metadata.put("TC_OBJECT_NAME", value);
	};
	
	protected HashMap<String, String> metadata = new HashMap<String, String>();
	
	public HashMap<String, String> getMetadata()
	{
		return metadata;
	}
	
	private ArrayList<Occurrence> children = new ArrayList<Occurrence>();
	
	public String modelRef = "";
	
	public ItemRevision(String uid)
	{
		setUid(uid);
		metadata.put("CAD_LENGTH_UNITS", "meters");
		metadata.put("CAD_MASS_UNITS", "kilograms");
		// metadata.put("JT_PROP_MEASUREMENT_UNITS", "meters");
	}
	
	public ArrayList<Occurrence> getChildren()
	{
		return children;
	}
	
	public void addChild(Occurrence occu)
	{
		if (children.indexOf(occu) < 0)
		{
			children.add(occu);
		}
	}
}
