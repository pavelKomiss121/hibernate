package ru.mentee.power.dao;

import org.hibernate.SessionFactory;
import ru.mentee.power.entity.mp_177.User;

/**
 * Реализация UserDAO через Hibernate.
 */
public class HibernateUserDAO extends HibernateGenericDAO<User, Long> implements UserDAO {
    public HibernateUserDAO(SessionFactory sessionFactory) {
        super(sessionFactory, User.class);
    }
}
