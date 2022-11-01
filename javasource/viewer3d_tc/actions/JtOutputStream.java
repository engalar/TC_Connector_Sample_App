package viewer3d_tc.actions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class JtOutputStream extends ByteArrayOutputStream {
    public ByteBuffer getBuffer() {
        return ByteBuffer.wrap(buf);
    }
}
