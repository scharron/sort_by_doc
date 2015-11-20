package org.elasticsearch.search.query.sortbydoc.utils;

import org.elasticsearch.common.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * samuel
 * 22/10/15, 18:56
 */
public class XContentGetScoreMap {
    public static Map<String, Float> extractMap(Object part, String rootPath, String key, String val) {
        String[] pathElements = Strings.splitStringToArray(rootPath, '.');

        // We expect only one
        for (int i = 0; i < pathElements.length; ++i) {
            if (!(part instanceof Map))
                return null;
            part = ((Map)part).get(pathElements[i]);
            if (i == pathElements.length - 1)
                break;
        }

        if (!(part instanceof List)) {
            return null;
        }

        Map<String, Float> values = new HashMap<>();
        for (Object o: (List)part) {
            if (!(o instanceof Map)) {
                return null;
            }
            Map item = (Map)o;
            Object itemKey = item.get(key);
            Object itemVal = item.get(val);

            if ((itemKey != null && itemKey instanceof String) && (itemVal != null || itemVal instanceof Number)) {
                values.put((String) itemKey, ((Number)itemVal).floatValue());
            }
        }

        return values;
    }
}
