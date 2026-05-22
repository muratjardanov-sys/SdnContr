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

    public void addEdge(int src, int dst, String name) {
        repository.save(new Edge(src, dst, name));
        repository.save(new Edge(dst, src, name));
    }

    public List<List<Integer>> findPaths(String graphName, int k) {
        List<Edge> edges = repository.findByGraphName(graphName);
        if (edges.isEmpty()) return new ArrayList<>();

        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (Edge e : edges) {
            adj.computeIfAbsent(e.getSource(), v -> new ArrayList<>()).add(e.getTarget());
        }

        List<List<Integer>> currentPaths = new ArrayList<>();
        for (Edge e : edges) {
            List<Integer> path = new ArrayList<>();
            path.add(e.getSource());
            path.add(e.getTarget());
            currentPaths.add(path);
        }

        for (int i = 2; i < k; i++) {
            List<List<Integer>> nextPaths = new ArrayList<>();
            for (List<Integer> path : currentPaths) {
                int lastNode = path.get(path.size() - 1);
                List<Integer> neighbors = adj.getOrDefault(lastNode, new ArrayList<>());

                for (Integer neighbor : neighbors) {
                    if (!path.contains(neighbor)) {
                        List<Integer> newPath = new ArrayList<>(path);
                        newPath.add(neighbor);
                        nextPaths.add(newPath);
                    }
                }
            }
            currentPaths = nextPaths;
        }

        return currentPaths;
    }
}