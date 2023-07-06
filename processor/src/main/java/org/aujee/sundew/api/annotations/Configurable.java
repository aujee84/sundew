package shared.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//Does nothing useful - read ConfigurableSupport doc.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Configurable {
}
