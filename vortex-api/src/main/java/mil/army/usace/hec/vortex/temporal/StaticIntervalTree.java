package mil.army.usace.hec.vortex.temporal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implements a static interval tree for efficiently querying overlapping intervals.
 * This tree is immutable after construction, optimized for quick read access and suitable
 * for scenarios where interval data does not change after tree construction. It constructs
 * a balanced binary search tree from a sorted list of intervals, where each node also tracks
 * the maximum end point of all intervals in its subtree to facilitate fast overlap queries.
 * <p>
 * Construction Time Complexity: O(n log n), due to sorting and building the tree.
 * Query Time Complexity: O(log n + k), where k is the number of overlapping intervals found.
 *
 * @param <T> The type of Interval this tree contains, must implement the Interval interface.
 */
public class StaticIntervalTree<T extends Interval> {
    private final Node root;

    /* Constructor */
    public StaticIntervalTree(List<T> intervals) {
        List<T> sortedByStart = new ArrayList<>(intervals);
        sortedByStart.sort(Comparator.comparingLong(T::start));
        root = buildTree(intervals, 0, intervals.size() - 1);
    }

    /* Tree's Node (includes maxEnd for IntervalTree algorithm) */
    private class Node {
        private final T interval;
        private long maxEnd;
        private Node left;
        private Node right;

        Node(T interval) {
            this.interval = interval;
        }
    }

    /* Tree Initialization */
    private Node buildTree(List<T> intervals, int start, int end) {
        if (start > end) {
            return null;
        }

        int mid = (start + end) / 2;

        Node node = new Node(intervals.get(mid));
        node.left = buildTree(intervals, start, mid - 1);
        node.right = buildTree(intervals, mid + 1, end);
        node.maxEnd = intervals.get(mid).end();

        if (node.left != null) {
            node.maxEnd = Math.max(node.maxEnd, node.left.maxEnd);
        }

        if (node.right != null) {
            node.maxEnd = Math.max(node.maxEnd, node.right.maxEnd);
        }

        return node;
    }

    /* Queries */
    /**
     * Queries for all intervals in the tree that overlap with a given target interval.
     *
     * @param target The interval to check for overlap.
     * @return A list of overlapping intervals.
     */
    public List<T> findOverlaps(T target) {
        List<T> overlaps = new ArrayList<>();
        collectOverlaps(root, target, overlaps);
        return overlaps;
    }

    /**
     * Returns the interval with the earliest start time in the tree.
     *
     * @return The interval with the earliest start.
     */
    public T findMinimum() {
        Node current = root;
        while (current.left != null) {
            current = current.left;
        }
        return current.interval;
    }

    /**
     * Returns the interval with the latest end time in the tree.
     *
     * @return The interval with the latest end.
     */
    public T findMaximum() {
        Node current = root;
        long maxEnd = current.maxEnd; // Start with the maxEnd of the root
        while (current != null) {
            if (current.maxEnd == maxEnd) {
                if (current.right != null && current.right.maxEnd == maxEnd) {
                    current = current.right;
                } else {
                    return current.interval; // This node contains the max end
                }
            } else {
                current = current.right; // Continue to node with potentially higher maxEnd
            }
        }
        return null;
    }

    /* Helpers */
    private void collectOverlaps(Node node, T target, List<T> overlaps) {
        if (node == null) return;
        // Check for overlap with the current node's interval
        if (node.interval.overlaps(target)) {
            overlaps.add(node.interval);
        }
        // Continue searching in the left subtree if there's a potential overlap
        if (node.left != null && node.left.maxEnd > target.start()) {
            collectOverlaps(node.left, target, overlaps);
        }
        // Continue searching in the right subtree
        if (node.right != null && node.interval.start() < target.end()) {
            collectOverlaps(node.right, target, overlaps);
        }
    }
}
