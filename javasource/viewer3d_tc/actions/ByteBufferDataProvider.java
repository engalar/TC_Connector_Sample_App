package viewer3d_tc.actions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.mendix.viewer3d.jtreader.BufferEntry;
import com.mendix.viewer3d.jtreader.DataProvider;
import com.mendix.viewer3d.jtreader.Span;

public class ByteBufferDataProvider implements DataProvider {

	private ByteBuffer buf;

	public ByteBufferDataProvider(ByteBuffer buf) {
		this.buf = buf;
	}

	@Override
	public long getSize() {
		return this.buf.array().length;
	}

	@Override
	public ByteBuffer getChunk(Span span) {
		var bytes = new byte[span.getCount()];
		var oldPos = this.buf.position();
		this.buf.position((int) span.getOffset());
		this.buf.get(bytes);
		this.buf.position(oldPos);
		return ByteBuffer.wrap(bytes);
	}

	@Override
	public List<BufferEntry> getChunks(List<Span> spans) {
		var ret = new ArrayList<BufferEntry>();
		for (var span : spans) {
			var buf = this.getChunk(span);
			ret.add(new BufferEntry(buf, 0, buf.array().length));
		}
		return ret;
	}

	@Override
	public ByteBuffer getSourceMap(ByteOrder byteOrder) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
