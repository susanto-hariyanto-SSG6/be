package com.bankdki.jakone_be.repository;

import com.bankdki.jakone_be.entity.Mutation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MutationRepository extends JpaRepository<Mutation, Long> {
    List<Mutation> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}