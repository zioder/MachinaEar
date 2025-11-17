package MachinaEar.iam.security;

import MachinaEar.iam.entities.Identity;

public final class IdentityUtility {
    private IdentityUtility() {}
    public static String subject(Identity id) {
        // Tu peux passer à id.getId().toHexString() plus tard si tu préfères
        return id.getEmail();
    }
}
