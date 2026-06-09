package com.topnivo.backend.repository;

import com.topnivo.backend.model.constant.OrderStatus;
import com.topnivo.backend.model.constant.OrderType;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String>, PagingAndSortingRepository<Order, String> {

    Page<Order> findByOrderByOrderDateDesc(Pageable pageable);

    Order findByOrderId(String orderId);

    Page<Order> findByMemberOrderByOrderDateDesc(Member member,
                                                 Pageable pageable);

    Page<Order> findByMemberAndOrderStatusOrderByOrderDateDesc(Member member,
                                                               OrderStatus orderStatus,
                                                               Pageable pageable);

    Page<Order> findByMemberAndOrderDateBetweenOrderByOrderDateDesc(Member member,
                                                                    LocalDateTime from,
                                                                    LocalDateTime to,
                                                                    Pageable pageable);

    Page<Order> findByMemberAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(Member member,
                                                                                  OrderStatus orderStatus,
                                                                                  LocalDateTime from,
                                                                                  LocalDateTime to,
                                                                                  Pageable pageable);


    Page<Order> findByStoreOrderByOrderDateDesc(Member store,
                                                Pageable pageable);

    Page<Order> findByStoreAndOrderStatusOrderByOrderDateDesc(Member store,
                                                              OrderStatus orderStatus,
                                                              Pageable pageable);

    Page<Order> findByStoreAndOrderDateBetweenOrderByOrderDateDesc(Member store,
                                                                   LocalDateTime from,
                                                                   LocalDateTime to,
                                                                   Pageable pageable);

    Page<Order> findByStoreAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(Member store,
                                                                                 OrderStatus orderStatus,
                                                                                 LocalDateTime from,
                                                                                 LocalDateTime to,
                                                                                 Pageable pageable);

    List<Order> findByMemberAndOrderType(Member member, OrderType orderType);

}
