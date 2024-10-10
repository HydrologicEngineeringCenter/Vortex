package mil.army.usace.hec.vortex.io;

import java.util.*;

final class IntervalTree<T extends Interval> {
    private final Node root;

    /* Constructor */
    private IntervalTree(List<T> intervals) {
        List<T> sortedByStart = intervals.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(T::startEpochSecond))
                .toList();
        root = buildTree(sortedByStart, 0, intervals.size() - 1);
    }

    static <T extends Interval> IntervalTree<T> from(List<T> intervals) {
        return new IntervalTree<>(intervals);
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
        if (intervals.isEmpty() || start > end) {
            return null;
        }

        int mid = (start + end) / 2;

        Node node = new Node(intervals.get(mid));
        node.left = buildTree(intervals, start, mid - 1);
        node.right = buildTree(intervals, mid + 1, end);
        node.maxEnd = intervals.get(mid).endEpochSecond();

        if (node.left != null) {
            node.maxEnd = Math.max(node.maxEnd, node.left.maxEnd);
        }

        if (node.right != null) {
            node.maxEnd = Math.max(node.maxEnd, node.right.maxEnd);
        }

        return node;
    }

    /* Queries */
    public List<T> findOverlaps(T target) {
        if (root == null) {
            return Collections.emptyList();
        }

        List<T> overlaps = new ArrayList<>();
        collectOverlaps(root, target, overlaps);
        return overlaps;
    }

    public T findMinimum() {
        if (root == null) {
            return null;
        }

        Node current = root;
        while (current.left != null) {
            current = current.left;
        }
        return current.interval;
    }

    public T findMaximum() {
        if (root == null) {
            return null;
        }

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
        if (node.left != null && node.left.maxEnd > target.startEpochSecond()) {
            collectOverlaps(node.left, target, overlaps);
        }
        // Continue searching in the right subtree
        if (node.right != null && node.interval.startEpochSecond() < target.endEpochSecond()) {
            collectOverlaps(node.right, target, overlaps);
        }
    }
}
