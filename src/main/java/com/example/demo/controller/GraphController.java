package com.example.demo.controller;

import com.example.demo.model.Edge;
import com.example.demo.service.GraphService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GraphController {
    private final GraphService service;

    public GraphController(GraphService service) {
        this.service = service;
    }

    @PostMapping("/add")
    public String addEdge(
            @RequestParam int src,
            @RequestParam int dst,
            @RequestParam(defaultValue = "10") int latency,
            @RequestParam(defaultValue = "40") int bandwidth,
            @RequestParam(defaultValue = "telecom") String name) {
        service.addEdge(src, dst, latency, bandwidth, name);
        return "Канал [" + src + " <-> " + dst + "] (" + latency + " мс, " + bandwidth + " Гбит/с) добавлен";
    }

    @PostMapping("/reset")
    public String resetGraph(@RequestParam(defaultValue = "telecom") String name) {
        service.clearGraph(name);
        return "Топология сети очищена";
    }

    @GetMapping("/edges")
    public List<Edge> getEdges(@RequestParam(defaultValue = "telecom") String name) {
        return service.getEdges(name);
    }

    @GetMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public String analyzeNetwork(
            @RequestParam(defaultValue = "telecom") String name,
            @RequestParam int src,
            @RequestParam int dst,
            @RequestParam(defaultValue = "5") int k,
            @RequestParam(defaultValue = "10") int minBw,
            @RequestParam(required = false) List<Integer> failed) {
        return service.analyzeWithCppEngine(name, src, dst, k, minBw, failed);
    }
}