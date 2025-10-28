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
        User u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities =
                Stream.concat(
                        safeStream(u.getRoles())
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + resolveRoleName(r))),
                        safeStream(u.getRoles())
                                .flatMap(r -> safeStream(r.getAuthorities()))
                                .map(a -> new SimpleGrantedAuthority(resolveAuthorityName(a)))
                ).toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(resolveUsername(u))
                .password(resolvePassword(u))
                .authorities(authorities)
                .disabled(!resolveEnabled(u))
                .accountLocked(resolveLocked(u))
                .build();
    }

    /* =========================
       Helpers – for mapping entity names to springsecurity names
       ========================= */

    private static String resolveUsername(Object u) {
        return firstNonNullString(u, "getUsername", "getLogin", "getEmail", "getUserName");
    }

    private static String resolvePassword(Object u) {
        return firstNonNullString(u, "getPassword", "getPasswordHash", "getHashedPassword", "getPwdHash");
    }

    private static boolean resolveEnabled(Object u) {
        return firstBoolean(u, true, "isEnabled", "getEnabled", "isActive", "getActive", "isAccountEnabled");
    }

    private static boolean resolveLocked(Object u) {
        Boolean nonLocked = invokeBoolean(u, "isAccountNonLocked");
        if (nonLocked != null) return !nonLocked;
        Boolean locked = firstBoolean(u, null, "isLocked", "getLocked", "isAccountLocked");
        if (locked != null) return locked;
        return false;
    }

    private static String resolveRoleName(Object role) {

        return firstNonNullString(role, "getCodeName", "getCode", "getKey", "getRoleName", "getName");
    }

    private static String resolveAuthorityName(Object authority) {
        return firstNonNullString(authority, "getCodeName", "getCode", "getKey", "getAuthority", "getValue", "getName");
    }

    /* =========================
       Generic reflective utils
       ========================= */

    private static <T> Stream<T> safeStream(Iterable<T> it) {
        return it == null ? Stream.empty() : Stream.of(it).flatMap(i -> {
            var spl = i.spliterator();
            return java.util.stream.StreamSupport.stream(spl, false);
        });
    }

    private static String firstNonNullString(Object target, String... methodNames) {
        for (String m : methodNames) {
            String val = invokeString(target, m);
            if (val != null && !val.isBlank()) return val;
        }
        throw new IllegalStateException("No suitable string getter found on " + target.getClass().getSimpleName());
    }

    private static Boolean firstBoolean(Object target, Boolean defaultVal, String... methodNames) {
        for (String m : methodNames) {
            Boolean val = invokeBoolean(target, m);
            if (val != null) return val;
        }
        return defaultVal;
    }

    private static String invokeString(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return (v != null) ? v.toString() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Boolean invokeBoolean(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return (v instanceof Boolean b) ? b : (v != null ? Boolean.valueOf(v.toString()) : null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
