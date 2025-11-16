package MachinaEar.iam.security;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.*;

@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Secured {
    String[] value() default {}; // rôles requis
}
