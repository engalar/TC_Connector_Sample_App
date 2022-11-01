package viewer3d_tc.actions;

import java.util.ArrayList;

public class TCGetPropertiesParam {
	private ArrayList<String> _name = new ArrayList<String>();
	private ArrayList<Boolean> _requireUiValue = new ArrayList<Boolean>();
	private ArrayList<Boolean> _requireDbValue = new ArrayList<Boolean>();
	
	public void add(String name, boolean requireUiValue, boolean requireDbValue)
	{
		_name.add(name);
		_requireUiValue.add(requireUiValue);
		_requireDbValue.add(requireDbValue);
	}
	
	public int size()
	{
		return _name.size();
	}
	
	public String getPropName(int index)
	{
		return _name.get(index);
	}
	
	public boolean isUiValueRequired(int index)
	{
		return _requireUiValue.get(index);
	}	

	public boolean isDbValueRequired(int index)
	{
		return _requireDbValue.get(index);	
	}
}
