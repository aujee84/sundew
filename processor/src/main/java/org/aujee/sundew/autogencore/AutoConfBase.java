package shared.autogencore;

import org.aujee.com.shared.util.UtilBucket;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.aujee.com.shared.util.UtilBucket.getPrimOrWrapClass;
import static org.aujee.com.shared.util.UtilBucket.getValue;
import static org.aujee.com.shared.util.UtilBucket.isString;

final class AutoConfBase {
    private static List<Map.Entry<Class<?>[], String[]>> entries = new LinkedList<>();

    private AutoConfBase() {}

    static void load(final Map<String, String[]> autoGenMap){
        Properties properties = loadProperties();
        loadEntries(properties, autoGenMap);
    }

    static void initialize() {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        entries.forEach (entry -> {
            Class<?>[] where_VarType = entry.getKey();
            String[] varName_Value_SimpleType = entry.getValue();

            try {
                MethodHandles
                        .privateLookupIn(
                                where_VarType[0],
                                lookup)
                        .findStaticVarHandle(
                                where_VarType[0],
                                varName_Value_SimpleType[0],
                                where_VarType[1])
                        .set(isString(varName_Value_SimpleType[2]) ?
                                varName_Value_SimpleType[1] :
                                getValue(varName_Value_SimpleType[2], varName_Value_SimpleType[1]));

            } catch (IllegalAccessException | NoSuchFieldException e) {
                String mes = "Unable to initialize.";
                throw new RuntimeException(mes, UtilBucket.getRootCause(e));
            }
        });
        entries.clear();
        entries = null;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        String filePath = "application.properties";

        try (InputStream resourceAsStream = ClassLoader.getSystemClassLoader().getResourceAsStream(filePath)) {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            String mes = "Unable to load properties file : " + filePath;
            throw new RuntimeException(mes, UtilBucket.getRootCause(e));
        }

        return properties;
    }

    private static void loadEntries(final Properties properties, final Map<String, String[]> autoGenMap) {
        properties.forEach((k, v) -> {
            Class<?>[] where_VarType = new Class[2];
            String[] varName_Value_SimpleType = new String[3];
            String key = k.toString();
            String[] value = autoGenMap.get(key);

            try {
                where_VarType[0] = Class.forName(value[0]);
            } catch (ClassNotFoundException e) {
                String mes = "Class " + value[0] + " not found.";
                throw new RuntimeException(mes, UtilBucket.getRootCause(e));
            }

            where_VarType[1] = isString(value[2]) ? String.class : getPrimOrWrapClass(value[2]);
            varName_Value_SimpleType[0] = value[1];
            varName_Value_SimpleType[1] = (String) v;
            varName_Value_SimpleType[2] = value[2];

            entries.add(Map.entry(where_VarType, varName_Value_SimpleType));
        });
    }
}
