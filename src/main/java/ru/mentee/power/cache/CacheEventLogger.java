package ru.mentee.power.cache;

import lombok.extern.slf4j.Slf4j;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

/**
 * Слушатель событий кэша EhCache.
 */
@Slf4j
public class CacheEventLogger implements CacheEventListener<Object, Object> {

    @Override
    public void onEvent(CacheEvent<?, ?> event) {
        log.debug(
                "Cache event: type={}, key={}, old={}, new={}",
                event.getType(),
                event.getKey(),
                event.getOldValue(),
                event.getNewValue());

        // Можем собирать метрики по событиям
        switch (event.getType()) {
            case CREATED:
                // Счетчик новых записей
                break;
            case UPDATED:
                // Счетчик обновлений
                break;
            case EVICTED:
                // Счетчик вытеснений (важно для tuning)
                log.warn("Cache eviction: key={}", event.getKey());
                break;
            case EXPIRED:
                // Счетчик истечений
                break;
            case REMOVED:
                // Счетчик удалений
                break;
        }
    }
}
