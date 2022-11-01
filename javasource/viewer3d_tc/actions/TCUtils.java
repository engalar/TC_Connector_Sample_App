package viewer3d_tc.actions;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.flatbuffers.FlatBufferBuilder;

import viewer3d.actions.productstructureschema.Instance;
import viewer3d.actions.productstructureschema.Model;
import viewer3d.actions.productstructureschema.Part;
import viewer3d.actions.productstructureschema.Property;
import viewer3d_tc.actions.teamcenter.ItemRevision;
import viewer3d_tc.actions.teamcenter.Occurrence;

public class TCUtils {
	public static ByteBuffer exportToFlatBuffer(ItemRevision itemRev)
	{
		HashMap<String, Integer> itemRevOffset = new HashMap<String, Integer>();
		FlatBufferBuilder builder = new FlatBufferBuilder();
		int root = writeFlatBuffer(builder, itemRev, itemRevOffset);
		int source = builder.createString(Constants.TEAM_CENTER);
		Model.startModel(builder);
		Model.addSource(builder, source);
		Model.addRoot(builder, root);
		int model = Model.endModel(builder);
		builder.finish(model);
		return builder.dataBuffer();
	}
	
	private static int writeFlatBuffer(FlatBufferBuilder builder, ItemRevision itemRev, HashMap<String, Integer> itemRevOffset)
	{
		if (itemRevOffset.containsKey(itemRev.getUid()))
		{
			return itemRevOffset.get(itemRev.getUid());
		}

		int name = builder.createString(itemRev.getObjectString());
		String modelRefStr = "";
		if (itemRev.modelRef.length() > 0)
		{
			modelRefStr = "ref&" + itemRev.modelRef;
		}
		int modelRef = builder.createString(modelRefStr);
		
		ArrayList<Occurrence> occurrences = itemRev.getChildren();
		int[] occus = new int[occurrences.size()];
		for (int i = 0; i < occurrences.size(); ++i)
		{
			int occu = writeFlatBuffer(builder, occurrences.get(i), itemRevOffset);
			occus[i] = occu;
		}
		int children = Part.createChildrenVector(builder, occus);
		
		int[] items = writeFlatBuffer(builder, itemRev.getMetadata());
		int properties = Part.createPropertiesVector(builder, items);
		
		int part = Part.createPart(builder, name, children, modelRef, properties);
		
		itemRevOffset.put(itemRev.getUid(), part);
		return part;
	}
	
	private static int writeFlatBuffer(FlatBufferBuilder builder, Occurrence occurrence, HashMap<String, Integer> itemRevOffset)
	{
		int part = writeFlatBuffer(builder, occurrence.itemRevision, itemRevOffset);
		
		int matrix = Instance.createMatrixVector(builder, occurrence.matrix);

		int[] items = writeFlatBuffer(builder, occurrence.getMetadata());
		int properties = Instance.createPropertiesVector(builder, items);

		Instance.startInstance(builder);
		Instance.addPart(builder, part);
		Instance.addMatrix(builder, matrix);
		Instance.addProperties(builder, properties);
		int instance = Instance.endInstance(builder);

		return instance;
	}
	
	private static int[] writeFlatBuffer(FlatBufferBuilder builder, Object properties)
	{
		ArrayList<Integer> props = new ArrayList<Integer>();
		try
		{
			if (properties instanceof Map)
			{
				@SuppressWarnings("rawtypes")
				Map<?, ?> m = (Map)properties;
				for (Map.Entry<?,?> entry : m.entrySet())
				{
				    var k = entry.getKey();
				    var v = entry.getValue();
				    if (k instanceof String && v instanceof String)
					{
				    	int key = builder.createString((String)k);
						int value = builder.createString((String)v);
						int prop = Property.createProperty(builder, key, value);
						props.add(prop);
					}
				    else
				    {
				    	break;
				    }
				}
			}
			else
			{
				Class<? extends Object> cls = properties.getClass();
				Field[] fields = cls.getDeclaredFields();
				for (int i = 0; i < fields.length; ++i)
				{
					var obj = fields[i].get(properties);
					if (obj instanceof String)
					{
						int key = builder.createString(fields[i].getName());
						int value = builder.createString((String)obj);
						int prop = Property.createProperty(builder, key, value);
						props.add(prop);
					}
					else if (obj instanceof Map)
					{
						@SuppressWarnings("rawtypes")
						Map<?, ?> m = (Map)obj;
						for (Map.Entry<?,?> entry : m.entrySet())
						{
						    var k = entry.getKey();
						    var v = entry.getValue();
						    if (k instanceof String && v instanceof String)
							{
						    	int key = builder.createString((String)k);
								int value = builder.createString((String)v);
								int prop = Property.createProperty(builder, key, value);
								props.add(prop);
							}
						    else
						    {
						    	break;
						    }
						}
					}
				}
			}
		}
		catch (Exception e)
		{
		}

		int[] result = new int[props.size()];
		for (int i = 0; i < props.size(); ++i)
		{
			result[i] = props.get(i);
		}
		return result;
	}
}
