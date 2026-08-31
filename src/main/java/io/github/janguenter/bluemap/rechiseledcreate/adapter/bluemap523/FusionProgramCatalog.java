/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.resource.fusion.model.FusionDirection;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable programs parsed from operator-installed exact model JSON. */
public final class FusionProgramCatalog {

    static final int EXPECTED_CUSTOM_MODELS = 432;
    private static final int MAX_AST_EDGE_DEPTH = 4;
    private static final int MAX_AST_NODES = 14;
    private static final FusionPredicate NEVER = new FusionPredicate.Never();

    private final Map<Key, Program> programs;

    private FusionProgramCatalog(Map<Key, Program> programs) {
        this.programs = Map.copyOf(programs);
    }

    static FusionProgramCatalog parse(Map<String, byte[]> rawModels) {
        Map<Key, RawProgram> rawPrograms = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : rawModels.entrySet()) {
            JsonObject object = parseObject(entry.getValue());
            if (object == null || !object.has("type")) {
                continue;
            }
            Key key = modelKey(entry.getKey());
            RawProgram program = parseProgram(key, object);
            if (rawPrograms.put(key, program) != null) {
                throw new IllegalArgumentException("duplicate Fusion program");
            }
        }
        if (rawPrograms.size() != EXPECTED_CUSTOM_MODELS) {
            throw new IllegalArgumentException("Fusion custom-model roster changed");
        }
        Map<Key, Program> resolved = new LinkedHashMap<>();
        for (Key key : rawPrograms.keySet()) {
            resolve(key, rawPrograms, resolved, new LinkedHashSet<>());
        }
        return new FusionProgramCatalog(resolved);
    }

    Program get(Key model) {
        return programs.get(model);
    }

    int size() {
        return programs.size();
    }

    Map<Key, Program> programs() {
        return programs;
    }

    private static Program resolve(
            Key key,
            Map<Key, RawProgram> raw,
            Map<Key, Program> resolved,
            Set<Key> visiting
    ) {
        Program existing = resolved.get(key);
        if (existing != null) {
            return existing;
        }
        RawProgram own = raw.get(key);
        if (own == null || !visiting.add(key)) {
            throw new IllegalArgumentException("missing or cyclic Fusion model parent");
        }
        LinkedHashMap<String, String> textures = new LinkedHashMap<>();
        LinkedHashMap<String, Connection> connections = new LinkedHashMap<>();
        if (own.parent() != null && raw.containsKey(own.parent())) {
            Program parent = resolve(own.parent(), raw, resolved, visiting);
            textures.putAll(parent.textures());
            connections.putAll(parent.connections());
        }
        textures.putAll(own.textures());
        connections.putAll(own.connections());
        visiting.remove(key);
        Program result = new Program(key, Map.copyOf(textures), Map.copyOf(connections));
        resolved.put(key, result);
        return result;
    }

    private static RawProgram parseProgram(Key key, JsonObject object) {
        Set<String> permitted = Set.of("type", "loader", "parent", "connections", "textures");
        if (!permitted.containsAll(object.keySet())
                || !"fusion:connecting".equals(string(object.get("type")))
                || !"fusion:model".equals(string(object.get("loader")))) {
            throw new IllegalArgumentException("unsupported Fusion model schema: " + key);
        }
        Key parent = object.has("parent") ? Key.parse(string(object.get("parent"))) : null;
        Map<String, String> textures = new LinkedHashMap<>();
        JsonObject textureObject = objectValue(object.get("textures"));
        if (textureObject != null) {
            textureObject.entrySet().forEach(entry ->
                    textures.put(entry.getKey(), requiredString(entry.getValue())));
        }
        Map<String, Connection> connections = new LinkedHashMap<>();
        JsonObject connectionObject = objectValue(object.get("connections"));
        if (connectionObject == null || connectionObject.size() == 0) {
            throw new IllegalArgumentException("Fusion model has no connections: " + key);
        }
        for (Map.Entry<String, JsonElement> entry : connectionObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String alias = value.getAsString();
                if (!alias.startsWith("#") || alias.length() == 1) {
                    throw new IllegalArgumentException("malformed connection alias");
                }
                connections.put(entry.getKey(), new Connection.Alias(alias.substring(1)));
            } else {
                int[] nodes = {0};
                connections.put(entry.getKey(), new Connection.Predicate(
                        parsePredicate(value, 0, nodes)
                ));
            }
        }
        return new RawProgram(key, parent, textures, connections);
    }

    private static FusionPredicate parsePredicate(JsonElement value, int depth, int[] nodes) {
        if (depth > MAX_AST_EDGE_DEPTH
                || ++nodes[0] > MAX_AST_NODES
                || !value.isJsonObject()) {
            throw new IllegalArgumentException("Fusion predicate exceeds bounds");
        }
        JsonObject object = value.getAsJsonObject();
        return switch (string(object.get("type"))) {
            case "fusion:or" -> new FusionPredicate.Any(
                    parsePredicates(object, depth, nodes)
            );
            case "fusion:and" -> new FusionPredicate.All(
                    parsePredicates(object, depth, nodes)
            );
            case "fusion:is_direction" -> new FusionPredicate.DirectionIn(
                    parseDirections(object)
            );
            case "fusion:match_block" -> new FusionPredicate.MatchBlock(
                    parseRechiseledBlock(object)
            );
            case "fusion:match_state" -> new FusionPredicate.MatchState(
                    parseRechiseledBlock(object), parseProperties(object)
            );
            case "fusion:is_same_state" -> new FusionPredicate.SameState();
            default -> throw new IllegalArgumentException("unsupported Fusion predicate");
        };
    }

    private static List<FusionPredicate> parsePredicates(
            JsonObject object,
            int depth,
            int[] nodes
    ) {
        JsonArray array = arrayValue(object.get("predicates"));
        if (array == null || array.size() > 64) {
            throw new IllegalArgumentException("malformed Fusion predicate list");
        }
        List<FusionPredicate> predicates = new ArrayList<>();
        for (JsonElement element : array) {
            predicates.add(parsePredicate(element, depth + 1, nodes));
        }
        return predicates;
    }

    private static Set<FusionDirection> parseDirections(JsonObject object) {
        JsonArray array = arrayValue(object.get("directions"));
        if (array == null || array.size() == 0 || array.size() > 8) {
            throw new IllegalArgumentException("malformed direction predicate");
        }
        Set<FusionDirection> directions = new HashSet<>();
        for (JsonElement element : array) {
            directions.add(FusionDirection.parse(requiredString(element)));
        }
        return directions;
    }

    private static Map<String, Set<String>> parseProperties(JsonObject object) {
        JsonObject properties = objectValue(object.get("properties"));
        if (properties == null || properties.size() == 0 || properties.size() > 8) {
            throw new IllegalArgumentException("malformed state predicate properties");
        }
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            JsonArray values = arrayValue(entry.getValue());
            if (values == null || values.size() == 0 || values.size() > 16) {
                throw new IllegalArgumentException("malformed state predicate values");
            }
            Set<String> parsed = new HashSet<>();
            for (JsonElement value : values) {
                parsed.add(requiredString(value));
            }
            result.put(entry.getKey(), Set.copyOf(parsed));
        }
        return result;
    }

    private static String parseRechiseledBlock(JsonObject object) {
        String block = requiredString(object.get("block"));
        if (!block.startsWith("rechiseledcreate:")) {
            throw new IllegalArgumentException("predicate leaves exact native namespace");
        }
        return block;
    }

    private static JsonObject parseObject(byte[] raw) {
        try {
            JsonElement value = JsonParser.parseReader(new StringReader(
                    new String(raw, StandardCharsets.UTF_8)
            ));
            return value.isJsonObject() ? value.getAsJsonObject() : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static JsonObject objectValue(JsonElement value) {
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonArray arrayValue(JsonElement value) {
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static String requiredString(JsonElement value) {
        String result = string(value);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("required Fusion string is absent");
        }
        return result;
    }

    private static String string(JsonElement value) {
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString() ? value.getAsString() : "";
    }

    private static Key modelKey(String path) {
        String prefix = "assets/rechiseledcreate/models/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("model path leaves exact namespace");
        }
        return Key.parse(
                "rechiseledcreate:" + path.substring(prefix.length(), path.length() - 5)
        );
    }

    sealed interface Connection permits Connection.Alias, Connection.Predicate {
        record Alias(String key) implements Connection {
        }

        record Predicate(FusionPredicate value) implements Connection {
        }
    }

    record Program(Key model, Map<String, String> textures, Map<String, Connection> connections) {
        FusionPredicate predicate(String materialKey) {
            String key = materialKey;
            Set<String> visited = new HashSet<>();
            while (key != null && visited.add(key)) {
                Connection connection = connections.get(key);
                if (connection instanceof Connection.Predicate predicate) {
                    return predicate.value();
                }
                if (connection instanceof Connection.Alias alias) {
                    key = alias.key();
                    continue;
                }
                String material = textures.get(key);
                key = material != null && material.startsWith("#")
                        ? material.substring(1) : null;
            }
            Connection fallback = connections.get("default");
            return fallback instanceof Connection.Predicate predicate
                    ? predicate.value() : NEVER;
        }
    }

    private record RawProgram(
            Key model,
            Key parent,
            Map<String, String> textures,
            Map<String, Connection> connections
    ) {
    }
}
