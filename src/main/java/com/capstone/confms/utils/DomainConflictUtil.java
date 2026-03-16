package com.capstone.confms.utils;

import java.util.Set;

/**
 * Utility class for domain-based conflict of interest detection.
 * Used to automatically detect conflicts when reviewer and paper author
 * share the same institutional email domain.
 */
public final class DomainConflictUtil {

    private DomainConflictUtil() {
    }

    /**
     * Public/free email domains that should NOT trigger domain conflicts.
     */
    private static final Set<String> PUBLIC_DOMAINS = Set.of(
            "gmail.com",
            "yahoo.com",
            "yahoo.com.vn",
            "outlook.com",
            "hotmail.com",
            "live.com",
            "icloud.com",
            "aol.com",
            "protonmail.com",
            "proton.me",
            "mail.com",
            "zoho.com",
            "yandex.com",
            "tutanota.com",
            "gmx.com",
            "gmx.net"
    );

    /**
     * Extract the domain part from an email address.
     * 
     * @param email the email address
     * @return domain (lowercase), or null if email is invalid
     */
    public static String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        return email.substring(email.lastIndexOf('@') + 1).toLowerCase().trim();
    }

    /**
     * Check if a domain is a public/free email provider.
     * These domains should NOT trigger domain conflicts because
     * sharing gmail.com doesn't imply a conflict of interest.
     * 
     * @param domain the domain to check
     * @return true if public domain
     */
    public static boolean isPublicDomain(String domain) {
        if (domain == null) {
            return true; // treat null as public to avoid false positives
        }
        return PUBLIC_DOMAINS.contains(domain.toLowerCase());
    }
}
