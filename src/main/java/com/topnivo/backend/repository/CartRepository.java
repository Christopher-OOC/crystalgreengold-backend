package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Cart;
import com.topnivo.backend.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    Cart findById(int id);

    Cart findByMember(Member member);
}
