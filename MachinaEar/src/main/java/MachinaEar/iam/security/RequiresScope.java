package MachinaEar.iam.security;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.*;

/**
 * Marks endpoints that require specific OAuth scopes
 * Applied in addition to @Secured for fine-grained access control
 * Uses OR logic - any one of the specified scopes is sufficient
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresScope {
    String[] value();  // Required scopes (OR logic - any one required)
}
