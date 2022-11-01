package viewer3d_tc.actions.controller;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.*;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.thirdparty.org.json.JSONObject;
import com.mendix.viewer3d.jtreader.JtReader;
import com.mendix.viewer3d.jtreader.JtUtils;
import com.mendix.viewer3d.jtreader.StreamType;

import viewer3d.actions.Controller;
import viewer3d.actions.JtResponseWriter;
import viewer3d.actions.controller.Util;
import viewer3d.actions.httprouter.annotation.*;
import viewer3d.actions.websocket.IWebSocketController;
import viewer3d.actions.websocket.IWebSocketResponder;
import viewer3d_tc.actions.ByteBufferDataProvider;
import viewer3d_tc.actions.Constants;
import viewer3d_tc.actions.JtOutputStream;
import viewer3d_tc.actions.VisTcConnection;

@Controller("tc")
public class TcController implements IWebSocketController {

    private ILogNode logger;
    
    final private Base64.Decoder decoder = Base64.getDecoder();
    
    private String decodeBase64(String text) {
        try {
			return new String(decoder.decode(text), "UTF-8");
		} catch (Exception e) {
			return "";
		}
    }

    public TcController() {
        this.logger = Core.getLogger(TcController.class.getSimpleName());
    }

    @GetMapping("{modelId}/ps")
    public void getPs(@Context IContext ctx, @Request IMxRuntimeRequest request, @Response IMxRuntimeResponse response,
            @PathVariable("modelId") String modelId) throws Exception {
        this.processJtRequest(ctx, request, response, modelId, StreamType.ProductStructure);
    }

    @PostMapping("{modelId}/shapes")
    public void getShapes(@Context IContext ctx, @Request IMxRuntimeRequest request,
            @Response IMxRuntimeResponse response, @PathVariable("modelId") String modelId) throws Exception {
        this.processJtRequest(ctx, request, response, modelId, StreamType.Shapes);
    }

    @PostMapping("{modelId}/pmi")
    public void getPmi(@Context IContext ctx, @Request IMxRuntimeRequest request, @Response IMxRuntimeResponse response,
            @PathVariable("modelId") String modelId) throws Exception {
        this.processJtRequest(ctx, request, response, modelId, StreamType.Pmi);
    }

    @PostMapping("{modelId}/metadata")
    public void getMetadata(@Context IContext ctx, @Request IMxRuntimeRequest request,
            @Response IMxRuntimeResponse response, @PathVariable("modelId") String modelId) throws Exception {
        this.processJtRequest(ctx, request, response, modelId, StreamType.Metadata);
    }

    @Override
    public void handleRequest(IContext context, Object request, Map<String, String> headers, String method,
            int messageId, IWebSocketResponder responder) {
        try {
            var action = headers.get("action");
            var modelId = headers.get("modelName");
            switch (action) {
            case "ps":
                this.processJtRequest(context, null, modelId, StreamType.ProductStructure, headers, responder);
                break;
            case "pmi":
                this.processJtRequest(context, request, modelId, StreamType.Pmi, headers, responder);
                break;
            case "shapes":
                this.processJtRequest(context, request, modelId, StreamType.Shapes, headers, responder);
                break;
            case "metadata":
                this.processJtRequest(context, request, modelId, StreamType.Metadata, headers, responder);
                break;
            }

            this.logger.info("Controller " + this.getClass().getSimpleName() + " handling request: (action = " + action
                    + ", modelId = " + modelId + ")");

        } catch (Exception exception) {
            responder.respond(exception, -1, -1, headers, false);
            this.logger.error(exception);
        }
    }

    private void processJtRequest(IContext ctx, Object request, String modelId, StreamType st,
            Map<String, String> headers, IWebSocketResponder responder) throws Exception {
        String bomLineId = null;
        JSONObject bomLineQuery = null;
        if (st == StreamType.ProductStructure) {
            int pos = modelId.indexOf("&psref&");
            if (pos >= 0) {
                String bomLineQueryBase64 = modelId.substring(pos + 7);
                String decodedBomLineQuery = decodeBase64(bomLineQueryBase64);
                bomLineQuery = VisTcConnection.getBOMLineQueryInput(decodedBomLineQuery);
                bomLineId = VisTcConnection.getBOMLine(ctx, bomLineQuery);
                if (bomLineId == null || bomLineId.length() == 0) {
                    responder.respond(null, -1, -1, headers, false);
                    return;
                }
            }
        }
        if (bomLineId != null && bomLineId.length() > 0) {
            // get PS from BOMLines
            var buffer = VisTcConnection.getProductStructure(ctx, bomLineId, bomLineQuery);
            if (buffer != null) {
                int count = buffer.limit() - buffer.position();
                headers.put("content-type", Constants.APPLICATION_JTX_STREAM_TYPE);
                headers.put("segment-count", "1");
                responder.respond(buffer, count, count, headers, false);
            }
        } else {
            // get other things rather than PS,
            // or get PS directly from JT file
            JtOutputStream oStream;
            // check if a file reference is specified,
            // else, modelId is treated as an item revision uid
            String ref = "";
            if (modelId.startsWith("ref&")) {
                ref = modelId.substring(4);
            }
            if (!isBlank(ref)) {
                oStream = VisTcConnection.GetJTFileStream(ctx, ref);
            } else {
                oStream = VisTcConnection.GetItemRevisionJTFileStream(ctx, modelId);
            }

            if (oStream != null) {
                Set<UUID> segmentIds = null;
                if (request instanceof ByteBuffer) {
                    segmentIds = JtUtils.INSTANCE.parseSegmentIds((ByteBuffer) request);
                }
                var dp = new ByteBufferDataProvider(oStream.getBuffer());
                var jtReader = new JtReader(dp);
                var chunks = jtReader.readChunked(st, segmentIds);
                var totalLength = 0;
                if (chunks != null && chunks.size() > 0) {
                    for (var chunk : chunks) {
                        for (var entry : chunk) {
                            totalLength += entry.getLength();
                        }
                    }

                    for (int i = 0, end = chunks.size() - 1; i <= end; i++) {
                        var chunk = chunks.get(i);
                        var contentLength = 0;
                        var buffers = new ArrayList<ByteBuffer>();
                        for (var entry : chunk) {
                            contentLength += entry.getLength();
                            var b = entry.getBuffer();
                            var buff = ByteBuffer.wrap(b.array(), entry.getOffset(), entry.getLength());
                            buffers.add(buff);
                        }

                        var segmentCount = chunk.size();
                        if (st == StreamType.ProductStructure) {
                            segmentCount = chunk.size() - 2;
                        } else {
                            segmentCount = chunk.size() - 1;
                        }
                        headers.put("segment-count", Integer.toString(segmentCount));
                        responder.respond(buffers, totalLength, contentLength, headers, i < end);
                    }
                } else {
                    responder.respond(null, -1, -1, headers, false);
                }
            }
        }
    }

    private void processJtRequest(IContext ctx, IMxRuntimeRequest request, IMxRuntimeResponse response, String modelId,
            StreamType st) throws Exception {
        var outStream = response.getOutputStream();

        String bomLineId = null;
        JSONObject bomLineQuery = null;
        if (st == StreamType.ProductStructure) {
            int pos = modelId.indexOf("&psref&");
            if (pos >= 0) {
                String bomLineQueryBase64 = modelId.substring(pos + 7);
                String decodedBomLineQuery = decodeBase64(bomLineQueryBase64);
                bomLineQuery = VisTcConnection.getBOMLineQueryInput(decodedBomLineQuery);
                bomLineId = VisTcConnection.getBOMLine(ctx, bomLineQuery);
                if (bomLineId == null || bomLineId.length() == 0) {
                    response.setStatus(IMxRuntimeResponse.NOT_FOUND);
                    return;
                }
            }
        }
        if (bomLineId != null && bomLineId.length() > 0) {
            // get PS from BOMLines
            var buffer = VisTcConnection.getProductStructure(ctx, bomLineId, bomLineQuery);
            if (buffer != null) {
                int start = buffer.position();
                int count = buffer.limit() - start;
                // For unknown reason,
                // sometimes oStream.writeTo(outStream) may commit the response,
                // which will prevent setContentType.
                // So do it first.
                response.setContentType(Constants.APPLICATION_JTX_STREAM_TYPE);
                response.addHeader("Content-Length", Integer.toString(count));
                var array = buffer.array();
                outStream.write(array, start, count);
            }
        } else {
            // get other things rather than PS,
            // or get PS directly from JT file
            JtOutputStream oStream;
            // check if a file reference is specified,
            // else, modelId is treated as an item revision uid
            String ref = "";
            if (modelId.startsWith("ref&")) {
                ref = modelId.substring(4);
            }
            if (!isBlank(ref)) {
                oStream = VisTcConnection.GetJTFileStream(ctx, ref);
            } else {
                oStream = VisTcConnection.GetItemRevisionJTFileStream(ctx, modelId);
            }

            if (oStream != null) {
                var ids = Util.getSegmentIds(request, st);
                var dp = new ByteBufferDataProvider(oStream.getBuffer());
                var jtReader = new JtReader(dp);
                var writer = new JtResponseWriter(response, modelId);
                jtReader.pipe(st, writer, ids);
                response.setContentType("application/octet-stream");
            }
        }
        outStream.close();
    }

    private boolean isBlank(final CharSequence cs) {
        final int strLen = cs == null ? 0 : cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
