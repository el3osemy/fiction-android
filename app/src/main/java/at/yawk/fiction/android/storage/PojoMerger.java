package at.yawk.fiction.android.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class PojoMerger {
    private final ObjectMapper mapper;

    /**
     * Merge two pojos. Values from the first parameter will take priority.
     */
    @SuppressWarnings("unchecked")
    public <T> T merge(T a, T b) {
        JsonNode ja = mapper.valueToTree(a);
        JsonNode jb = mapper.valueToTree(b);

        JsonNode merged = merge(ja, jb);
        try {
            return mapper.treeToValue(merged, (Class<? extends T>) a.getClass());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonNode merge(JsonNode a, JsonNode b) {
        if (a.getNodeType() != b.getNodeType()) {
            return a;
        }

        if (a.isArray()) {
            ArrayNode joined = new ArrayNode(JsonNodeFactory.instance);
            for (int i = 0; ; i++) {
                boolean la = i < a.size();
                boolean lb = i < b.size();
                if (la) {
                    if (lb) {
                        joined.add(merge(a.get(i), b.get(i)));
                    } else {
                        joined.add(a.get(i));
                    }
                } else {
                    if (lb) {
                        joined.add(b.get(i));
                    } else {
                        break;
                    }
                }
            }
            return joined;
        }

        if (a.isObject()) {
            ObjectNode joined = new ObjectNode(JsonNodeFactory.instance);

            Iterator<Map.Entry<String, JsonNode>> fieldIterator = a.fields();
            while (fieldIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldIterator.next();
                joined.set(entry.getKey(), entry.getValue());
            }

            fieldIterator = b.fields();
            while (fieldIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldIterator.next();
                JsonNode existing = joined.get(entry.getKey());
                joined.set(entry.getKey(), existing == null ? entry.getValue() : merge(existing, entry.getValue()));
            }

            return joined;
        }

        return a;
    }
}
