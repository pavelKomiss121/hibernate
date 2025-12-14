package ru.mentee.power.relationship;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

/**
 * Реализация менеджера связей.
 */
@Slf4j
@RequiredArgsConstructor
public class RelationshipManagerImpl implements RelationshipManager {

    private final EntityManagerFactory entityManagerFactory;

    @Override
    public <P, C> void linkBidirectional(P parent, C child) {
        if (parent == null || child == null) {
            return;
        }

        try {
            // Пытаемся найти метод setParent или аналогичный
            Method setParentMethod = findSetParentMethod(child.getClass(), parent.getClass());
            if (setParentMethod != null) {
                setParentMethod.invoke(child, parent);
            }

            // Пытаемся найти метод addChild или аналогичный
            Method addChildMethod = findAddChildMethod(parent.getClass(), child.getClass());
            if (addChildMethod != null) {
                addChildMethod.invoke(parent, child);
            }
        } catch (Exception e) {
            log.warn("Failed to link bidirectional relationship", e);
        }
    }

    @Override
    public <P, C> void unlinkBidirectional(P parent, C child) {
        if (parent == null || child == null) {
            return;
        }

        try {
            // Пытаемся найти метод setParent(null)
            Method setParentMethod = findSetParentMethod(child.getClass(), parent.getClass());
            if (setParentMethod != null) {
                setParentMethod.invoke(child, (Object) null);
            }

            // Пытаемся найти метод removeChild
            Method removeChildMethod = findRemoveChildMethod(parent.getClass(), child.getClass());
            if (removeChildMethod != null) {
                removeChildMethod.invoke(parent, child);
            }
        } catch (Exception e) {
            log.warn("Failed to unlink bidirectional relationship", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> initializeCollection(Object entity, String propertyName) {
        if (entity == null || propertyName == null) {
            return null;
        }

        try {
            Method getter = findGetter(entity.getClass(), propertyName);
            if (getter == null) {
                log.warn("Getter not found for property: {}", propertyName);
                return null;
            }

            Object collection = getter.invoke(entity);
            if (collection != null && collection instanceof Collection) {
                Hibernate.initialize(collection);
                return (Collection<T>) collection;
            }
        } catch (Exception e) {
            log.error("Failed to initialize collection: {}", propertyName, e);
        }

        return null;
    }

    @Override
    public <T> T loadWithAssociations(Class<T> entityClass, Object id, String... fetchPaths) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityGraph<T> graph = em.createEntityGraph(entityClass);
            for (String path : fetchPaths) {
                graph.addAttributeNodes(path);
            }

            Map<String, Object> hints = new HashMap<>();
            hints.put("jakarta.persistence.loadgraph", graph);
            return em.find(entityClass, id, hints);
        } finally {
            em.close();
        }
    }

    private Method findSetParentMethod(Class<?> childClass, Class<?> parentClass) {
        for (Method method : childClass.getMethods()) {
            if (method.getName().startsWith("set")
                    && method.getParameterCount() == 1
                    && parentClass.isAssignableFrom(method.getParameterTypes()[0])) {
                return method;
            }
        }
        return null;
    }

    private Method findAddChildMethod(Class<?> parentClass, Class<?> childClass) {
        for (Method method : parentClass.getMethods()) {
            if ((method.getName().startsWith("add") || method.getName().contains("add"))
                    && method.getParameterCount() == 1
                    && childClass.isAssignableFrom(method.getParameterTypes()[0])) {
                return method;
            }
        }
        return null;
    }

    private Method findRemoveChildMethod(Class<?> parentClass, Class<?> childClass) {
        for (Method method : parentClass.getMethods()) {
            if ((method.getName().startsWith("remove") || method.getName().contains("remove"))
                    && method.getParameterCount() == 1
                    && childClass.isAssignableFrom(method.getParameterTypes()[0])) {
                return method;
            }
        }
        return null;
    }

    private Method findGetter(Class<?> clazz, String propertyName) {
        String capitalized = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String getterName = "get" + capitalized;
        String isGetterName = "is" + capitalized;

        for (Method method : clazz.getMethods()) {
            if ((method.getName().equals(getterName) || method.getName().equals(isGetterName))
                    && method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }
}
