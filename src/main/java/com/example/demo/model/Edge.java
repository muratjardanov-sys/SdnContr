package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "edges")
public class Edge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int source;
    private int target;
    private String graphName;

    public Edge() {}

    public Edge(int source, int target, String graphName) {
        this.source = source;
        this.target = target;
        this.graphName = graphName;
    }

    // Геттеры
    public int getSource() { return source; }
    public int getTarget() { return target; }
    public String getGraphName() { return graphName; }
}