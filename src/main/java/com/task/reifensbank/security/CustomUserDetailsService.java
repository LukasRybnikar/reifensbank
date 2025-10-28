package com.task.reifensbank.security;

import com.task.reifensbank.entity.User;
import com.task.reifensbank.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.stream.Stream;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities =
                Stream.concat(
                        safeStream(user.getRoles())
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + resolveRoleName(r))),
                        safeStream(user.getRoles())
                                .flatMap(r -> safeStream(r.getAuthorities()))
                                .map(a -> new SimpleGrantedAuthority(resolveAuthorityName(a)))
                ).toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(resolveUsername(user))
                .password(resolvePassword(user))
                .authorities(authorities)
                .disabled(!resolveEnabled(user))
                .accountLocked(resolveLocked(user))
                .build();
    }

    /* =========================
       Resolvers – tolerant getter names
       ========================= */

    private static String resolveUsername(Object u) {
        return firstNonNullString(u, "getUsername");
    }

    private static String resolvePassword(Object u) {
        return firstNonNullString(u, "getPasswordHash");
    }

    private static boolean resolveEnabled(Object u) {
        return firstBoolean(u, true, "isEnabled");
    }

    private static boolean resolveLocked(Object u) {
        Boolean nonLocked = invokeBoolean(u, "isAccountNonLocked");
        if (nonLocked != null) return !nonLocked;

        Boolean locked = firstBoolean(u, null, "isLocked");
        if (locked != null) return locked;

        return false;
    }

    private static String resolveRoleName(Object role) {
        return firstNonNullString(role, "getCodeName");
    }

    private static String resolveAuthorityName(Object authority) {
        return firstNonNullString(authority, "getCodeName");
    }

    /* =========================
       Generic reflective utils
       ========================= */

    private static <T> Stream<T> safeStream(Iterable<T> iterable) {
        if (iterable == null) {
            return Stream.empty();
        }
        var spliterator = iterable.spliterator();
        return java.util.stream.StreamSupport.stream(spliterator, false);
    }

    private static String firstNonNullString(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            String value = invokeString(target, methodName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalStateException(
                "No suitable string getter found on " + target.getClass().getSimpleName()
        );
    }

    private static Boolean firstBoolean(Object target, Boolean defaultVal, String... methodNames) {
        for (String methodName : methodNames) {
            Boolean value = invokeBoolean(target, methodName);
            if (value != null) {
                return value;
            }
        }
        return defaultVal;
    }

    private static String invokeString(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return (value != null) ? value.toString() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Boolean invokeBoolean(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);

            if (value == null) {
                return false;
            }

            if (value instanceof Boolean boolVal) {
                return boolVal;
            }

            return Boolean.valueOf(value.toString());
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
