package com.example.demo.controller;

import com.example.demo.service.GraphService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin // Разрешает запросы с твоего будущего JS-фронтенда
public class GraphController {
    private final GraphService service;

    public GraphController(GraphService service) {
        this.service = service;
    }

    @PostMapping("/add")
    public String addEdge(@RequestParam int src, @RequestParam int dst, @RequestParam String name) {
        service.addEdge(src, dst, name);
        return "Ребро добавлено!";
    }

    @GetMapping("/paths")
    public List<List<Integer>> getPaths(@RequestParam String name, @RequestParam int k) {
        return service.findPaths(name, k);
    }
}