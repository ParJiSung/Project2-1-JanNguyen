package Game;

import java.util.Arrays;

/**
 * A Union-Find (Disjoint Set Union) data structure implementation.
 * This class efficiently manages a collection of disjoint sets and supports
 * union and find operations with path compression and union by rank optimizations.
 * 
 * <p>This implementation is commonly used for connectivity problems, such as
 * determining whether stones on a game board are connected.
 * 
 * <p>Time Complexity:
 * <ul>
 *   <li>Find: O(α(n)) amortized, where α is the inverse Ackermann function</li>
 *   <li>Union: O(α(n)) amortized</li>
 *   <li>Connected: O(α(n)) amortized</li>
 * </ul>
 */

public class UnionFind {
    /**
     * Array where parent[i] represents the parent of element i.
     * If parent[i] == i, then i is a root of its set.
     */
    private int[] parent;
    
    /**
     * Array where rank[i] represents an upper bound on the height of the tree
     * rooted at i. Used for union by rank optimization.
     */
    private int[] rank;

    /**
     * Constructs a Union-Find data structure with the specified number of elements.
     * Initially, each element is in its own set.
     * 
     * @param size the total number of elements (typically positions on a game board)
     * @throws NegativeArraySizeException if size is negative
     */
    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        // Initially, every node is its own parent, and ranks are zero.
        for (int i = 0; i < size; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    /**
     * Finds the root representative of the set containing the specified element.
     * Uses path compression to flatten the tree structure, improving performance
     * of future operations.
     * 
     * @param x the element whose set representative is to be found
     * @return the root representative of the set containing x
     */
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // Recursively find the root and compress the path
        }
        return parent[x];
    }

    /**
     * Merges the sets containing the two specified elements.
     * Uses union by rank to keep the tree shallow by attaching the shorter
     * tree under the root of the taller tree.
     * 
     * <p>If the elements are already in the same set, no action is taken.
     * 
     * @param a the first element
     * @param b the second element
     */
    public void union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) return; // They are already in the same set

        // Union by rank: attach the smaller tree under the larger tree
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;
        } else if (rank[rootA] > rank[rootB]) {
            parent[rootB] = rootA;
        } else {
            parent[rootB] = rootA;
            rank[rootA]++;
        }
    }

    /**
     * Determines whether two elements are in the same set.
     * 
     * @param a the first element
     * @param b the second element
     * @return {@code true} if a and b are in the same set, {@code false} otherwise
    */
    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    public void reset() {
        Arrays.fill(rank, 0);
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
    }

    public UnionFind copy() {
    UnionFind newParams = new UnionFind(this.parent.length); // Create empty
    // RAW COPY: extremely fast
    System.arraycopy(this.parent, 0, newParams.parent, 0, this.parent.length);
    System.arraycopy(this.rank, 0, newParams.rank, 0, this.rank.length);
    return newParams;
}

    /**
     * Gets the parent array for undo/redo support.
     * @return copy of the parent array
     */
    public int[] getParentArray() {
        return Arrays.copyOf(parent, parent.length);
    }
    
    /**
     * Gets the rank array for undo/redo support.
     * @return copy of the rank array
     */
    public int[] getRankArray() {
        return Arrays.copyOf(rank, rank.length);
    }
    
    /**
     * Restores UnionFind state from arrays (for undo).
     */
    public void restore(int[] parentArray, int[] rankArray) {
        System.arraycopy(parentArray, 0, this.parent, 0, this.parent.length);
        System.arraycopy(rankArray, 0, this.rank, 0, this.rank.length);
    }
}

