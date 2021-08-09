package de.rub.nds.tlsscanner.serverscanner.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import de.rub.nds.tlsscanner.serverscanner.probe.stats.ExtractedValueContainer;
import de.rub.nds.tlsscanner.serverscanner.probe.stats.TrackableValueType;

import java.io.IOException;

public class ExtractedValueContainerDeserializer extends StdDeserializer<ExtractedValueContainer> {

    public ExtractedValueContainerDeserializer() {
        super(ExtractedValueContainer.class);
    }

    @Override
    public ExtractedValueContainer deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        TrackableValueType type = TrackableValueType.valueOf(node.get("type").asText());
        //System.out.println(socketState);
        //TODO THIS HAS TO HAVE A FULL IMPLEMENTATION
        return new ExtractedValueContainer(type);

    }
}
