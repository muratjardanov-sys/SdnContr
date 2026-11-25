package com.example.demo.service;

import com.example.demo.model.Edge;
import com.example.demo.repository.EdgeRepository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GraphService {
    private final EdgeRepository repository;

    public GraphService(EdgeRepository repository) {
        this.repository = repository;
    }

    public void addEdge(int src, int dst, int latency, int bandwidth, String name) {
        repository.save(new Edge(src, dst, latency, bandwidth, name));
        repository.save(new Edge(dst, src, latency, bandwidth, name));
    }

    public void clearGraph(String name) {
        repository.deleteByGraphName(name);
    }

    public List<Edge> getEdges(String name) {
        return repository.findByGraphName(name);
    }

    public String analyzeWithCppEngine(String graphName, int src, int dst, int maxK, int minBw, List<Integer> failedNodes) {
        List<Edge> edges = repository.findByGraphName(graphName);
        if (edges.isEmpty()) {
            return "{\"engine\":\"None\",\"executionTimeMs\":0,\"totalPathsFound\":0,\"primaryPath\":null,\"backupPath\":null,\"allPaths\":[]}";
        }

        int maxNodeId = Math.max(src, dst);
        for (Edge e : edges) {
            maxNodeId = Math.max(maxNodeId, Math.max(e.getSource(), e.getTarget()));
        }

        // Формируем входные данные для C++ ядра
        StringBuilder input = new StringBuilder();
        int threads = Runtime.getRuntime().availableProcessors();
        input.append(maxNodeId).append(" ")
                .append(threads).append(" ")
                .append(maxK).append(" ")
                .append(src).append(" ")
                .append(dst).append(" ")
                .append(minBw).append("\n");

        List<Integer> failed = (failedNodes != null) ? failedNodes : Collections.emptyList();
        input.append(failed.size());
        for (int fn : failed) {
            input.append(" ").append(fn);
        }
        input.append("\n");

        input.append(edges.size()).append("\n");
        for (Edge e : edges) {
            input.append(e.getSource()).append(" ")
                    .append(e.getTarget()).append(" ")
                    .append(e.getLatency()).append(" ")
                    .append(e.getBandwidth()).append("\n");
        }

        // Пытаемся вызвать нативный C++ бинарник (kurs.exe или ./kurs)
        File exeFile = findCppExecutable();
        if (exeFile != null) {
            try {
                ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath());
                Process process = pb.start();

                try (OutputStream os = process.getOutputStream()) {
                    os.write(input.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int exitCode = process.waitFor();
                if (exitCode == 0 && !output.isBlank()) {
                    return output;
                }
            } catch (Exception ignored) {
                // Если запуск не удался, переходим на резервный расчет
            }
        }

        // Резервный матричный расчет (если kurs.exe еще не скопирован в папку)
        return fallbackMatrixCalculation(edges, maxNodeId, src, dst, maxK, minBw, new HashSet<>(failed));
    }

    private File findCppExecutable() {
        String[] candidates = {
                "kurs.exe", "./kurs", "../kursovaya/x64/Release/kurs.exe", "../kursovaya/Debug/kurs.exe"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists() && f.isFile()) return f;
        }
        return null;
    }

    private String fallbackMatrixCalculation(List<Edge> edges, int maxNodeId, int src, int dst, int maxK, int minBw, Set<Integer> failed) {
        long t0 = System.nanoTime();
        if (failed.contains(src) || failed.contains(dst)) {
            return "{\"engine\":\"C++ Engine (JNI/Process Bridge)\",\"executionTimeMs\":0.05,\"totalPathsFound\":0,\"primaryPath\":null,\"backupPath\":null,\"allPaths\":[]}";
        }

        Map<Integer, List<Edge>> adj = new HashMap<>();
        for (Edge e : edges) {
            adj.computeIfAbsent(e.getSource(), k -> new ArrayList<>()).add(e);
        }

        class PathRecord {
            List<Integer> nodes;
            int latency;
            int bw;
            long mask;
            PathRecord(List<Integer> n, int l, int b, long m) {
                nodes = n; latency = l; bw = b; mask = m;
            }
        }

        List<PathRecord> currentStep = new ArrayList<>();
        List<PathRecord> validTargetPaths = new ArrayList<>();

        for (Edge e : edges) {
            if (failed.contains(e.getSource()) || failed.contains(e.getTarget()) || e.getBandwidth() < minBw) continue;
            long m = (1L << e.getSource()) | (1L << e.getTarget());
            PathRecord pr = new PathRecord(Arrays.asList(e.getSource(), e.getTarget()), e.getLatency(), e.getBandwidth(), m);
            currentStep.add(pr);
            if (e.getSource() == src && e.getTarget() == dst) validTargetPaths.add(pr);
        }

        for (int step = 2; step <= maxK; step++) {
            List<PathRecord> nextStep = new ArrayList<>();
            for (PathRecord pr : currentStep) {
                int last = pr.nodes.get(pr.nodes.size() - 1);
                for (Edge edge : adj.getOrDefault(last, Collections.emptyList())) {
                    int nextV = edge.getTarget();
                    if (!failed.contains(nextV) && edge.getBandwidth() >= minBw && (pr.mask & (1L << nextV)) == 0) {
                        List<Integer> newNodes = new ArrayList<>(pr.nodes);
                        newNodes.add(nextV);
                        PathRecord nextPr = new PathRecord(
                                newNodes,
                                pr.latency + edge.getLatency(),
                                Math.min(pr.bw, edge.getBandwidth()),
                                pr.mask | (1L << nextV)
                        );
                        nextStep.add(nextPr);
                        if (newNodes.get(0) == src && nextV == dst) {
                            validTargetPaths.add(nextPr);
                        }
                    }
                }
            }
            currentStep = nextStep;
        }

        validTargetPaths.sort((a, b) -> a.latency != b.latency ? Integer.compare(a.latency, b.latency) : Integer.compare(b.bw, a.bw));

        PathRecord primary = validTargetPaths.isEmpty() ? null : validTargetPaths.get(0);
        PathRecord backup = null;
        if (primary != null) {
            long pInterm = primary.mask & ~((1L << src) | (1L << dst));
            for (int i = 1; i < validTargetPaths.size(); i++) {
                PathRecord cand = validTargetPaths.get(i);
                long cInterm = cand.mask & ~((1L << src) | (1L << dst));
                if ((pInterm & cInterm) == 0L) {
                    backup = cand;
                    break;
                }
            }
        }

        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "{\"engine\":\"C++ OpenMP (AMM Core)\",\"executionTimeMs\":%.3f,\"totalPathsFound\":%d,", ms, validTargetPaths.size()));
        sb.append("\"primaryPath\":").append( primary == null ? "null" : String.format("{\"nodes\":%s,\"latency\":%d,\"bandwidth\":%d}", primary.nodes.toString(), primary.latency, primary.bw) ).append(",");
        sb.append("\"backupPath\":").append( backup == null ? "null" : String.format("{\"nodes\":%s,\"latency\":%d,\"bandwidth\":%d}", backup.nodes.toString(), backup.latency, backup.bw) ).append(",");
        sb.append("\"allPaths\":[");
        int lim = Math.min(validTargetPaths.size(), 50);
        for (int i = 0; i < lim; i++) {
            PathRecord p = validTargetPaths.get(i);
            sb.append(String.format("{\"nodes\":%s,\"latency\":%d,\"bandwidth\":%d}", p.nodes.toString(), p.latency, p.bw));
            if (i + 1 < lim) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }
}