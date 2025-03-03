package com.vending;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry per i servizi dell'applicazione
 */
public class ServiceRegistry {
    private static final Map<String, Object> services = new HashMap<>();

    public static void register(String name, Object service) {
        services.put(name, service);
    }

    public static <T> T get(Class<T> serviceClass) {
        return services.values().stream()
                .filter(service -> serviceClass.isInstance(service))
                .map(serviceClass::cast)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceClass.getName()));
    }

    public static Object get(String name) {
        return services.get(name);
    }
}