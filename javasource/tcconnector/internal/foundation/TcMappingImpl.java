package tcconnector.internal.foundation;

import tcconnector.foundation.TcMapping;

public class TcMappingImpl implements TcMapping {
	private final String tcName;
	private final String mxName;

	public TcMappingImpl(String tc, String mx) {
		tcName = tc;
		mxName = mx;
	}

	@Override
	public String getTcName() {
		return tcName;
	}

	@Override
	public String getMxName() {
		return mxName;
	}

	@Override
	public String toString() {
		return tcName + "/" + mxName;
	}

	@Override
	public int hashCode() {
		return 31 * tcName.hashCode() + mxName.hashCode();
	}

	@Override
	public boolean equals(Object right) {
		if (!(right instanceof TcMapping))
			return false;

		TcMapping that = (TcMapping) right;
		if (!this.getTcName().equals(that.getTcName()))
			return false;
		if (!this.getMxName().equals(that.getMxName()))
			return false;
		return true;
	}
}