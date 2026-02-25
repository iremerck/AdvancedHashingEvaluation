import java.util.*;

// LINEAR PROBING HASH TABLE 
  
class LinearProbingHashTable<K, V> {

    static class Entry<K, V> {
        K key;
        V value;
        boolean occupied;
    }

    private Entry<K, V>[] table;
    private int elementCount;
    private int totalProbes;
    private int maxProbes;

    LinearProbingHashTable(int capacity) {
    table = (Entry<K, V>[]) new Entry[capacity];
    for (int i = 0; i < capacity; i++) {
        table[i] = new Entry<>();
    }
}

    private int hashIndex(K key) {
        return Math.abs(key.hashCode()) % table.length;
    }

    void put(K key, V value) {
        int index = hashIndex(key);
        int probe = 0;

        while (table[index].occupied) {
            index = (index + 1) % table.length;
            probe++;
        }

        table[index].key = key;
        table[index].value = value;
        table[index].occupied = true;

        recordProbe(probe);
        elementCount++;
    }

    V get(K key) {
        int index = hashIndex(key);
        int probe = 0;

        while (table[index].occupied) {
            if (table[index].key.equals(key)) {
                recordProbe(probe);
                return table[index].value;
            }
            index = (index + 1) % table.length;
            probe++;
        }

        recordProbe(probe);
        return null;
    }

    void remove(K key) {
        int index = hashIndex(key);
        while (table[index].occupied) {
            if (table[index].key.equals(key)) {
                table[index].occupied = false;
                elementCount--;
                return;
            }
            index = (index + 1) % table.length;
        }
    }

    private void recordProbe(int probeCount) {
        totalProbes += probeCount;
        maxProbes = Math.max(maxProbes, probeCount);
    }

    double averageProbe() {
        return elementCount == 0 ? 0.0 : (double) totalProbes / elementCount;
    }

    int maxProbe() {
        return maxProbes;
    }
}

// ROBIN HOOD HASH TABLE
  
class RobinHoodHashTable<K, V> {

    static class Entry<K, V> {
        K key;
        V value;
        int displacement = -1;
        boolean occupied;
    }

    private Entry<K, V>[] table;
    private int elementCount;
    private int totalProbes;
    private int maxProbes;

    RobinHoodHashTable(int capacity) {
    table = (Entry<K, V>[]) new Entry[capacity]; 
    for (int i = 0; i < capacity; i++) {
        table[i] = new Entry<>();
    }
}

    private int hashIndex(K key) {
        return Math.abs(key.hashCode()) % table.length;
    }

    void put(K key, V value) {
        int index = hashIndex(key);
        int probeDistance = 0;
        
        while (table[index].occupied) {
            if (table[index].displacement < probeDistance) {
                Entry<K, V> temp = table[index];

                table[index] = new Entry<>();
                table[index].key = key;
                table[index].value = value;
                table[index].displacement = probeDistance;
                table[index].occupied = true;

                key = temp.key;
                value = temp.value;
                probeDistance = temp.displacement;
            }
            index = (index + 1) % table.length;
            probeDistance++;
        }

        table[index].key = key;
        table[index].value = value;
        table[index].displacement = probeDistance;
        table[index].occupied = true;

        recordProbe(probeDistance);
        elementCount++;
    }

    V get(K key) {
        int index = hashIndex(key);
        int probeDistance = 0;

        while (table[index].occupied && table[index].displacement >= probeDistance) {
            if (table[index].key.equals(key)) {
                recordProbe(probeDistance);
                return table[index].value;
            }
            index = (index + 1) % table.length;
            probeDistance++;
        }

        recordProbe(probeDistance);
        return null;
    }

    void remove(K key) {
        int index = hashIndex(key);
        while (table[index].occupied) {
            if (table[index].key.equals(key)) {
                backwardShiftDelete(index);
                elementCount--;
                return;
            }
            index = (index + 1) % table.length;
        }
    }

    private void backwardShiftDelete(int startIndex) {
        int index = startIndex;

        while (true) {
            int next = (index + 1) % table.length;
            if (!table[next].occupied || table[next].displacement == 0) {
                table[index].occupied = false;
                return;
            }
            table[index] = table[next];
            table[index].displacement--;
            index = next;
        }
    }

    private void recordProbe(int probeCount) {
        totalProbes += probeCount;
        maxProbes = Math.max(maxProbes, probeCount);
    }

    double averageProbe() {
        return elementCount == 0 ? 0.0 : (double) totalProbes / elementCount;
    }

    int maxProbe() {
        return maxProbes;
    }
}

// MAIN – EXPERIMENTAL EVALUATION

public class AdvancedHashingEvaluation {

    private static final Random RANDOM = new Random();

    private static final int ENTRY_SIZE_BYTES = 40;

    public static void main(String[] args) {
        executeBenchmark();
    }

    private static void executeBenchmark() {
        double[] loadFactors = {0.5, 0.8, 0.9};
        int tableCapacity = 20_000;
        int operationCount = 30_000;

        for (double loadFactor : loadFactors) {
            printLoadFactorHeader(loadFactor);

            evaluateWorkload("Random", loadFactor, tableCapacity, operationCount);
            evaluateWorkload("Clustered", loadFactor, tableCapacity, operationCount);
            evaluateWorkload("Mixed", loadFactor, tableCapacity, operationCount);
        }
    }

    private static void evaluateWorkload(String workloadType,
                                         double loadFactor,
                                         int capacity,
                                         int operations) {

        int insertions = (int) (capacity * loadFactor);
        int[] keys = generateKeysForWorkload(workloadType, insertions);

        printWorkloadHeader(workloadType);

        LinearProbingHashTable<Integer, Integer> linearTable =
                new LinearProbingHashTable<>(capacity);
        RobinHoodHashTable<Integer, Integer> robinHoodTable =
                new RobinHoodHashTable<>(capacity);

        long startTime = System.currentTimeMillis();
        for (int key : keys) linearTable.put(key, key);
        for (int i = 0; i < operations; i++)
            linearTable.get(keys[RANDOM.nextInt(capacity / 4)]);
        long linearTime = System.currentTimeMillis() - startTime;

        printResultRow("Linear",
                linearTable.averageProbe(),
                linearTable.maxProbe(),
                linearTime,
                capacity);

        startTime = System.currentTimeMillis();
        for (int key : keys) robinHoodTable.put(key, key);
        for (int i = 0; i < operations; i++)
            robinHoodTable.get(keys[RANDOM.nextInt(capacity / 4)]);
        long robinTime = System.currentTimeMillis() - startTime;

        printResultRow("RobinHood",
                robinHoodTable.averageProbe(),
                robinHoodTable.maxProbe(),
                robinTime,
                capacity);

        System.out.println();
    }

    // ---------------- OUTPUT HELPERS ---------------- 

    private static void printLoadFactorHeader(double loadFactor) {
        System.out.println("\n============= Load Factor: " + loadFactor + " =============");
    }

    private static void printWorkloadHeader(String workload) {
        System.out.println("\nWorkload Type : " + workload);
        System.out.println("--------------------------------------------------");
        System.out.println("Method      AvgProbe   MaxProbe   Time(ms)  ApproxMem(KB)");
    }

    private static void printResultRow(String method,
                                       double avgProbe,
                                       int maxProbe,
                                       long timeMs,
                                       int capacity) {

        double approxMemoryKB = (capacity * ENTRY_SIZE_BYTES) / 1024.0;

        System.out.printf("%-11s %-10.2f %-10d %-9d %.1f%n",
                method, avgProbe, maxProbe, timeMs, approxMemoryKB);
    }

    // ---------------- WORKLOAD GENERATION ---------------- 

    private static int[] generateKeysForWorkload(String workload, int count) {
        int[] keys = new int[count];

        switch (workload) {
            case "Clustered":
                for (int i = 0; i < count; i++) {
                    keys[i] = (i % 2 == 0)
                            ? RANDOM.nextInt(300)
                            : RANDOM.nextInt(600);
                }
                break;

            case "Mixed":
                for (int i = 0; i < count; i++) {
                    keys[i] = (i % 3 == 0)
                            ? RANDOM.nextInt(800)
                            : RANDOM.nextInt();
                }
                break;

            default: 
                for (int i = 0; i < count; i++) {
                    keys[i] = RANDOM.nextInt();
                }
        }
        return keys;
    }
}
