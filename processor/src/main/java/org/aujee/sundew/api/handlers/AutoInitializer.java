package shared.api.handlers;

import org.aujee.com.shared.util.UtilBucket;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public interface AutoInitializer {

    class ConfigurationHandler {

        private ConfigurationHandler(){}

        //to work on - handle private initialize in AutoConfig, delete properties after initialization
        public static void initialize() {
            MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
            InitializerFunc initializer;
            try {
                Class<?> targetClass = Class.forName("org.aujee.com.shared.autogencore.AutoConfig");
                MethodHandles.Lookup privateLockup = MethodHandles.privateLookupIn(targetClass, LOOKUP);
                MethodHandle aStatic = privateLockup.findStatic(targetClass, "initialize", MethodType.methodType(void.class));

                CallSite site = LambdaMetafactory.metafactory(
                        LOOKUP,
                        "initialize",
                        MethodType.methodType(InitializerFunc.class),
                        MethodType.methodType(void.class),
                        aStatic,
                        MethodType.methodType(void.class)
                );
                initializer = (InitializerFunc) site.getTarget().invoke();

            } catch (Throwable e) {
                System.err.println(UtilBucket.getRootCause(e).getMessage());
                throw new RuntimeException(e);
            }
            initializer.initialize();
        }
    }
}
