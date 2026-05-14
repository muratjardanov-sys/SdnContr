package com.example.demo.model;

import jakarta.persistence.*;

@Entity // Говорит Spring, что это таблица в БД
public class Edge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int source;
    private int target;
    private String graphName; // Позволит хранить несколько разных графов

    // Конструкторы
    public Edge() {}
    public Edge(int source, int target, String graphName) {
        this.source = source;
        this.target = target;
        this.graphName = graphName;
    }

    // Геттеры (важны для Spring!)
    public int getSource() { return source; }
    public int getTarget() { return target; }
    public String getGraphName() { return graphName; }
}