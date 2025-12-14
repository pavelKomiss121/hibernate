package ru.mentee.power.examples;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderItem;
import ru.mentee.power.entity.relationship.OrderStatus;
import ru.mentee.power.entity.relationship.Product;

/**
 * Демонстрация каскадных операций.
 */
@Slf4j
@RequiredArgsConstructor
public class CascadeExamples {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * CascadeType.ALL - все операции каскадируются.
     */
    public void demonstrateCascadeAll() {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // Создаем заказ с позициями
            Order order = new Order();
            order.setOrderNumber("ORD-001");
            order.setStatus(OrderStatus.PENDING);
            order.setOrderDate(java.time.LocalDateTime.now());

            Product product1 = em.find(Product.class, 1L);
            Product product2 = em.find(Product.class, 2L);

            if (product1 != null && product2 != null) {
                OrderItem item1 = new OrderItem();
                item1.setProduct(product1);
                item1.setQuantity(2);

                OrderItem item2 = new OrderItem();
                item2.setProduct(product2);
                item2.setQuantity(1);

                // Добавляем через вспомогательный метод
                order.addOrderItem(item1);
                order.addOrderItem(item2);

                // Сохраняем только order - items сохранятся автоматически
                em.persist(order);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            log.error("Error in demonstrateCascadeAll", e);
        } finally {
            em.close();
        }
    }

    /**
     * orphanRemoval - удаление осиротевших сущностей.
     */
    public void demonstrateOrphanRemoval() {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Order order = em.find(Order.class, 1L);
            if (order != null && !order.getOrderItems().isEmpty()) {
                // Удаляем item из коллекции
                OrderItem itemToRemove = order.getOrderItems().get(0);
                order.removeOrderItem(itemToRemove);

                // При flush() itemToRemove будет удален из БД
                // благодаря orphanRemoval = true
                em.flush();
            }

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            log.error("Error in demonstrateOrphanRemoval", e);
        } finally {
            em.close();
        }
    }

    /**
     * Разные типы каскадов.
     */
    public void demonstrateDifferentCascades() {
        // CascadeType.PERSIST - только при сохранении
        // CascadeType.MERGE - только при merge
        // CascadeType.REMOVE - только при удалении
        // CascadeType.REFRESH - только при обновлении из БД
        // CascadeType.DETACH - только при отсоединении от сессии

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // PERSIST cascade
            ru.mentee.power.entity.relationship.Customer customer =
                    new ru.mentee.power.entity.relationship.Customer();
            customer.setEmail("new@example.com");
            customer.setFirstName("John");
            customer.setLastName("Doe");

            Order order = new Order();
            order.setCustomer(customer);
            order.setOrderNumber("ORD-002");
            order.setStatus(OrderStatus.PENDING);
            order.setOrderDate(java.time.LocalDateTime.now());

            // Если у Order есть @ManyToOne(cascade = CascadeType.PERSIST)
            // то customer тоже сохранится
            em.persist(order);

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            log.error("Error in demonstrateDifferentCascades", e);
        } finally {
            em.close();
        }
    }
}
