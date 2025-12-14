package ru.mentee.power.dao;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.graph.GraphSemantic;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderItem;
import ru.mentee.power.entity.relationship.OrderStatus;

/**
 * DAO с оптимизированными запросами для избежания N+1.
 */
@Slf4j
@RequiredArgsConstructor
public class OptimizedOrderDAO {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Загрузка заказов с использованием JOIN FETCH.
     */
    public List<Order> findOrdersWithItemsJoinFetch() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            // JOIN FETCH загружает связанные данные одним запросом
            return em.createQuery(
                            """
				SELECT DISTINCT o FROM RelationshipOrder o
				LEFT JOIN FETCH o.orderItems oi
				LEFT JOIN FETCH oi.product
				WHERE o.status = :status
				ORDER BY o.orderDate DESC
				""",
                            Order.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Загрузка с использованием Entity Graph.
     */
    public List<Order> findOrdersWithEntityGraph(Long customerId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityGraph<Order> graph = em.createEntityGraph(Order.class);
            graph.addAttributeNodes("customer");
            var itemGraph = graph.addSubgraph("orderItems", OrderItem.class);
            itemGraph.addAttributeNodes("product");

            return em.createQuery(
                            "FROM RelationshipOrder o WHERE o.customer.id = :customerId",
                            Order.class)
                    .setParameter("customerId", customerId)
                    .setHint(GraphSemantic.LOAD.getJpaHintName(), graph)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Загрузка с использованием Criteria API и Fetch.
     */
    public List<Customer> findCustomersWithOrdersCriteria() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customer> query = cb.createQuery(Customer.class);
            Root<Customer> customer = query.from(Customer.class);

            // Fetch для избежания N+1
            Fetch<Customer, Order> orderFetch = customer.fetch("orders", JoinType.LEFT);
            orderFetch.fetch("orderItems", JoinType.LEFT);

            query.select(customer).distinct(true);

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Batch загрузка для коллекций.
     */
    public void demonstrateBatchLoading() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            // Благодаря @BatchSize(size = 25) на Order.orderItems
            // Hibernate загрузит items батчами по 25, а не по одному
            List<Order> orders =
                    em.createQuery(
                                    "FROM RelationshipOrder o WHERE o.orderDate > :date",
                                    Order.class)
                            .setParameter("date", java.time.LocalDateTime.now().minusDays(7))
                            .getResultList();

            // Это вызовет batch загрузку вместо N запросов
            for (Order order : orders) {
                log.info(
                        "Order {} has {} items",
                        order.getOrderNumber(),
                        order.getOrderItems().size());
            }
        } finally {
            em.close();
        }
    }

    /**
     * Использование субселектов для загрузки коллекций.
     */
    public List<Customer> findCustomersWithSubselect() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            // Благодаря @Fetch(FetchMode.SUBSELECT)
            // Hibernate использует подзапрос для загрузки всех заказов
            List<Customer> customers =
                    em.createQuery(
                                    "FROM RelationshipCustomer c WHERE c.email LIKE :pattern",
                                    Customer.class)
                            .setParameter("pattern", "%@example.com")
                            .getResultList();

            // Один подзапрос загрузит все заказы для всех клиентов
            customers.forEach(
                    c -> log.info("Customer {} has {} orders", c.getEmail(), c.getOrders().size()));

            return customers;
        } finally {
            em.close();
        }
    }

    /**
     * Инициализация lazy коллекций перед закрытием сессии.
     */
    public Order findOrderWithInitializedCollections(Long orderId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Order order = em.find(Order.class, orderId);
            if (order != null) {
                // Явная инициализация lazy коллекций
                org.hibernate.Hibernate.initialize(order.getOrderItems());

                // Или через обращение к коллекции
                order.getOrderItems().size();

                // Для вложенных объектов
                order.getOrderItems()
                        .forEach(item -> org.hibernate.Hibernate.initialize(item.getProduct()));
            }
            return order;
        } finally {
            em.close();
        }
    }
}
