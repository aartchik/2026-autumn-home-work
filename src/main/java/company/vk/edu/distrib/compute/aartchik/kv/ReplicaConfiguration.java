package company.vk.edu.distrib.compute.aartchik.kv;

import java.util.Arrays;

record ReplicaConfiguration(int[] ports, int factor) {
    static ReplicaConfiguration load(int localPort) {
        String cluster = System.getProperty("CLUSTER", "");
        int[] nodes = cluster.isBlank() ? new int[]{localPort}
                : Arrays.stream(cluster.split(",", -1)).map(String::trim).mapToInt(Integer::parseInt).toArray();
        Arrays.sort(nodes);
        for (int index = 0; index < nodes.length; index++) {
            if (nodes[index] <= 0 || nodes[index] > 65535
                    || index > 0 && nodes[index] == nodes[index - 1]) {
                throw new IllegalArgumentException("Cluster ports must be valid and distinct");
            }
        }
        if (Arrays.binarySearch(nodes, localPort) < 0) {
            throw new IllegalArgumentException("Cluster must contain this node");
        }
        int replication = Integer.parseInt(System.getProperty("REPLICATION_FACTOR", Integer.toString(nodes.length)));
        if (replication < 1 || replication > nodes.length) {
            throw new IllegalArgumentException("Invalid replication factor");
        }
        return new ReplicaConfiguration(nodes, replication);
    }

    int[] replicas(String key) {
        int primary = Arrays.binarySearch(ports, HttpDao.owner(key, ports));
        int[] selected = new int[factor];
        for (int index = 0; index < factor; index++) {
            selected[index] = ports[(primary + index) % ports.length];
        }
        return selected;
    }
}
