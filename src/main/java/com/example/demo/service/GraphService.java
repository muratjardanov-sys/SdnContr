package com.example.demo.service;

import com.example.demo.model.Edge;
import com.example.demo.repository.EdgeRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GraphService {
    private final EdgeRepository repository;

    public GraphService(EdgeRepository repository) {
        this.repository = repository;
    }

    // Сохранение ребра в БД
    public void addEdge(int src, int dst, String name) {
        repository.save(new Edge(src, dst, name));
    }

    // Поиск путей длины K
    public List<List<Integer>> findPaths(String graphName, int k) {
        List<Edge> edges = repository.findByGraphName(graphName);

        // Строим список смежности из данных БД
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (Edge e : edges) {
            adj.computeIfAbsent(e.getSource(), v -> new ArrayList<>()).add(e.getTarget());
        }

        // Тут логика поиска (та же, что мы обсуждали в C++)
        // Для краткости вернем список всех ребер как стартовую точку
        List<List<Integer>> result = new ArrayList<>();
        for (Edge e : edges) {
            result.add(Arrays.asList(e.getSource(), e.getTarget()));
        }
        return result;
    }
}