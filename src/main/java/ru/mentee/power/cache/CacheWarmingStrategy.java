package ru.mentee.power.cache;

import org.hibernate.SessionFactory;

/**
 * Стратегия прогрева кэша.
 */
public interface CacheWarmingStrategy {

    /**
     * Выполнить прогрев кэша.
     * @param sessionFactory фабрика сессий
     */
    void warmUp(SessionFactory sessionFactory);

    /**
     * Получить приоритет стратегии.
     * @return приоритет (меньше = выше)
     */
    int getPriority();

    /**
     * Проверить необходимость прогрева.
     * @return true если прогрев нужен
     */
    boolean shouldWarmUp();
}
