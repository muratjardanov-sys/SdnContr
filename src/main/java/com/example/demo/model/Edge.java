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
    private int latency;   // Задержка (пинг) в мс
    private int bandwidth; // Пропускная способность в Гбит/с
    private String graphName;

    public Edge() {}

    public Edge(int source, int target, int latency, int bandwidth, String graphName) {
        this.source = source;
        this.target = target;
        this.latency = latency;
        this.bandwidth = bandwidth;
        this.graphName = graphName;
    }

    public Long getId() { return id; }
    public int getSource() { return source; }
    public int getTarget() { return target; }
    public int getLatency() { return latency; }
    public int getBandwidth() { return bandwidth; }
    public String getGraphName() { return graphName; }
}