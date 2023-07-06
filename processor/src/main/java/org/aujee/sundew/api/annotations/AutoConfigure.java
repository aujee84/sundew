package shared.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//to work on: with/without Maven; yaml, toml, json, conf
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface AutoConfigure {
}
