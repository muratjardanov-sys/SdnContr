package com.example.demo.repository;

import com.example.demo.model.Edge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface EdgeRepository extends JpaRepository<Edge, Long> {
    List<Edge> findByGraphName(String graphName);

    @Transactional
    void deleteByGraphName(String graphName);
}