package shared.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class UtilBucket {

    private static final Map<String, Class<?>> SUPPORTED_TYPE_MAPPER = HashMap.newHashMap(16);

    private UtilBucket() {}

    static {
        SUPPORTED_TYPE_MAPPER.put("short", Short.TYPE);
        SUPPORTED_TYPE_MAPPER.put("Short", Short.class);
        SUPPORTED_TYPE_MAPPER.put("byte", Byte.TYPE);
        SUPPORTED_TYPE_MAPPER.put("Byte", Byte.class);
        SUPPORTED_TYPE_MAPPER.put("int", Integer.TYPE);
        SUPPORTED_TYPE_MAPPER.put("Integer", Integer.class);
        SUPPORTED_TYPE_MAPPER.put("long", Long.TYPE);
        SUPPORTED_TYPE_MAPPER.put("Long", Long.class);
        SUPPORTED_TYPE_MAPPER.put("float", Float.TYPE);
        SUPPORTED_TYPE_MAPPER.put("Float", Float.class);
        SUPPORTED_TYPE_MAPPER.put("double", Double.TYPE);
        SUPPORTED_TYPE_MAPPER.put("Double", Double.class);
        SUPPORTED_TYPE_MAPPER.put("boolean", Boolean.TYPE);
        SUPPORTED_TYPE_MAPPER.put("Boolean", Boolean.class);
        SUPPORTED_TYPE_MAPPER.put("char", Character.TYPE);
        SUPPORTED_TYPE_MAPPER.put("char[]", char.class.arrayType());
        SUPPORTED_TYPE_MAPPER.put("Character", Character.class);
        SUPPORTED_TYPE_MAPPER.put("Character[]", Character.class.arrayType());
    }

    public static Throwable getRootCause(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    public static Object getValue(String type, String value) {

        return switch (type) {
            case "Short", "short" -> Short.parseShort(value);
            case "Byte", "byte" -> Byte.parseByte(value);
            case "Integer", "int" -> Integer.parseInt(value);
            case "Long", "long" -> Long.parseLong(value);
            case "Float", "float" -> Float.parseFloat(value);
            case "Double", "double" -> Double.parseDouble(value);
            case "Boolean", "boolean" -> Boolean.parseBoolean(value);
            case "Character", "char" -> value.charAt(0);
            case "Character[]" -> value.codePoints().mapToObj(cp -> (char) cp).toArray(Character[]::new);
            case "char[]" -> value.toCharArray();
            default -> throw new IllegalStateException(
                    "Unexpected value for: " + type);
        };
    }

    public static Class<?> getPrimOrWrapClass(String primOrWrap) {
        return SUPPORTED_TYPE_MAPPER.get(primOrWrap);
    }

    public static boolean isString(String typeName) {
        return "String".equals(typeName);
    }
}
