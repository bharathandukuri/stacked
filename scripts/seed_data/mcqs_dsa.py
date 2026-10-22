"""Data Structures & Algorithms MCQs."""

QUESTIONS = [
    {
        "title": "Binary Search Tree Degeneration and Asymptotics",
        "slug": "binary-search-tree-degeneration-asymptotics",
        "content": """Consider a Binary Search Tree (BST) into which $N$ elements are inserted in strictly ascending order without rebalancing:

What is the worst-case time complexity of searching for an element in this tree, compared to a self-balancing AVL or Red-Black Tree?

| Data Structure | Tree Shape | Worst-Case Search |
| :--- | :--- | :--- |
| Unbalanced BST (sorted insertion) | Degenerate Linked List | ? |
| Red-Black Tree / AVL Tree | Balanced ($h \\le 2\\log_2(N+1)$) | ? |
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["bst", "trees", "complexity", "big-o"],
        "options": [
            {
                "id": "opt_a",
                "text": "Unbalanced BST: $\\mathcal{O}(N)$ (linear skew); Balanced Tree: $\\mathcal{O}(\\log N)$",
                "explanation": "Inserting sorted elements into an unbalanced BST creates a linked list of depth N, degrading search from logarithmic to linear time."
            },
            {
                "id": "opt_b",
                "text": "Unbalanced BST: $\\mathcal{O}(\\log N)$; Balanced Tree: $\\mathcal{O}(1)$",
                "explanation": "Balanced trees provide O(log N) search, not O(1)."
            },
            {
                "id": "opt_c",
                "text": "Unbalanced BST: $\\mathcal{O}(N \\log N)$; Balanced Tree: $\\mathcal{O}(\\log N)$",
                "explanation": "Searching an unbalanced tree cannot take N log N time."
            },
            {
                "id": "opt_d",
                "text": "Both structures guarantee $\\mathcal{O}(\\log N)$ worst-case search regardless of insertion order",
                "explanation": "Standard non-balancing BSTs provide no balance guarantees."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "When elements are inserted in sorted order into an unbalanced BST, every node has only a right child, forming a degenerate linked list with height $N$, yielding worst-case $\\mathcal{O}(N)$ search time. Self-balancing trees guarantee height $\\mathcal{O}(\\log N)$ via tree rotations.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Dynamic Array Resizing Amortized Complexity",
        "slug": "dynamic-array-resizing-amortized-complexity",
        "content": """In dynamic array data structures like Java's `ArrayList` or Python's `list`, when internal capacity is exhausted, a new underlying array of double capacity is allocated, and all elements are copied over.

What is the **amortized time complexity** of appending $N$ items sequentially to an initially empty dynamic array?

$$\\text{Total Cost of } N \\text{ appends} = N + \\sum_{i=1}^{\\log_2 N} 2^i < 3N$$
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["amortized-analysis", "arrays", "arraylist", "complexity"],
        "options": [
            {
                "id": "opt_1",
                "text": "$\\mathcal{O}(1)$ amortized per append operation",
                "explanation": "Although occasional resizing takes O(K) copy time, it occurs geometrically infrequently, spreading the total copy cost over all insertions to average O(1) amortized."
            },
            {
                "id": "opt_2",
                "text": "$\\mathcal{O}(N)$ amortized per append operation",
                "explanation": "O(N) is the single worst-case individual resize cost, not the amortized average."
            },
            {
                "id": "opt_3",
                "text": "$\\mathcal{O}(\\log N)$ amortized per append operation",
                "explanation": "Resizing by a multiplicative factor yields constant amortized time, not logarithmic."
            },
            {
                "id": "opt_4",
                "text": "$\\mathcal{O}(N^2)$ amortized per append operation",
                "explanation": "Additive resizing (e.g. +10 capacity) causes O(N) amortized, but doubling capacity keeps total cost linear, O(1) amortized."
            }
        ],
        "correctOptionIds": ["opt_1"],
        "explanation": "By doubling the array size, total copy cost across $N$ insertions is a geometric series bounded by $2N$. Dividing total cost by $N$ insertions gives $\\mathcal{O}(1)$ amortized time per operation.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Stable vs Unstable Sorting Algorithms",
        "slug": "stable-vs-unstable-sorting-algorithms",
        "content": """A **stable sorting algorithm** preserves the relative order of elements that have equal sort keys.

Which of the following sorting algorithms are **stable** by design?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["sorting", "stability", "algorithms"],
        "options": [
            {
                "id": "opt_merge",
                "text": "**Merge Sort**: Divides array in halves and merges maintaining order of identical keys",
                "explanation": "Standard Merge Sort is stable when taking the left element on ties (`left <= right`)."
            },
            {
                "id": "opt_timsort",
                "text": "**TimSort**: Hybrid derived from Merge Sort and Insertion Sort used by Java `Arrays.sort(Object[])` and Python",
                "explanation": "TimSort is strictly stable."
            },
            {
                "id": "opt_heapsort",
                "text": "**Heap Sort**: Builds a binary heap and swaps root with last element",
                "explanation": "Heap Sort is inherently unstable due to long-distance swaps between heap levels."
            },
            {
                "id": "opt_quicksort",
                "text": "**Standard In-Place QuickSort** (Lomuto or Hoare partitioning)",
                "explanation": "Standard in-place QuickSort swaps non-adjacent elements across partitions, violating stability."
            }
        ],
        "correctOptionIds": ["opt_merge", "opt_timsort"],
        "explanation": "Merge Sort and TimSort are stable. Standard in-place QuickSort, HeapSort, and Selection Sort are inherently unstable.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Dijkstra's Algorithm and Negative Edge Weights",
        "slug": "dijkstras-algorithm-negative-edge-weights",
        "content": """Dijkstra's shortest path algorithm with a priority queue **cannot** be guaranteed to find the correct shortest paths in graphs containing edges with negative weights.

Is this statement **True** or **False**?

> [!WARNING]
> Consider graphs with negative edges, even in the absence of negative-weight cycles.""",
        "type": "BOOLEAN",
        "difficulty": "EASY",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["graphs", "dijkstra", "shortest-path", "bellman-ford"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "Dijkstra's greedy choice assumes that once a vertex is marked finalized/visited, its shortest distance cannot be improved. Negative edges violate this assumption."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Adding a large constant to all edges also alters paths with different hop counts, so Dijkstra cannot simply be patched."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. Dijkstra's algorithm relies on the greedy invariant that edge weights are non-negative ($w(u, v) \\ge 0$). When negative edges exist, the Bellman-Ford algorithm or SPFA must be used instead.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Monotonic Stack Next Greater Element Complexity",
        "slug": "monotonic-stack-next-greater-element-complexity",
        "content": """Given an integer array of length $N$, we want to find the next strictly greater element to the right for every element:

```text
Input:  [2, 1, 2, 4, 3]
Output: [4, 2, 4, -1, -1]
```

Using a **monotonic decreasing stack** of indices, what is the optimal time and space complexity?

> [!TIP]
> Notice how many times any single index is pushed and popped from the stack.""",
        "type": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["stack", "monotonic-stack", "complexity"],
        "options": [
            {
                "id": "opt_a",
                "text": "$\\mathcal{O}(N)$ time and $\\mathcal{O}(N)$ auxiliary space",
                "explanation": "Each index is pushed onto the stack exactly once and popped at most once. The total number of stack operations across the loop is bounded by 2N."
            },
            {
                "id": "opt_b",
                "text": "$\\mathcal{O}(N^2)$ time and $\\mathcal{O}(1)$ auxiliary space",
                "explanation": "This describes the brute-force nested loop approach, not monotonic stack."
            },
            {
                "id": "opt_c",
                "text": "$\\mathcal{O}(N \\log N)$ time and $\\mathcal{O}(N)$ auxiliary space",
                "explanation": "No sorting or heap operations are required; stack operations take linear time."
            },
            {
                "id": "opt_d",
                "text": "$\\mathcal{O}(N)$ time and $\\mathcal{O}(1)$ auxiliary space",
                "explanation": "An auxiliary stack of up to N elements is required in the worst-case (descending array)."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "A monotonic stack achieves linear $\\mathcal{O}(N)$ time because every index enters the stack once and leaves at most once. Auxiliary space is $\\mathcal{O}(N)$ to store stack elements and results.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Hash Table Collision Resolution Strategies",
        "slug": "hash-table-collision-resolution-strategies",
        "content": """Which statements regarding **hash table collision resolution techniques** are accurate?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["hash-table", "collisions", "data-structures"],
        "options": [
            {
                "id": "opt_chaining",
                "text": "**Separate Chaining**: Stores colliding elements in a secondary data structure (e.g. linked list or red-black tree) at each bucket",
                "explanation": "Used by Java's HashMap; transforms long linked lists to Red-Black Trees when bucket size exceeds 8."
            },
            {
                "id": "opt_linear_probing",
                "text": "**Linear Probing (Open Addressing)**: Suffers from *primary clustering*, where occupied slots form continuous blocks and degrade lookup performance",
                "explanation": "Primary clustering causes search times to increase as load factor grows."
            },
            {
                "id": "opt_load_factor",
                "text": "Open addressing tables can safely operate with a load factor $\\alpha > 1.0$ without resizing",
                "explanation": "False. In open addressing, all elements live in the array itself. The load factor can never exceed 1.0."
            },
            {
                "id": "opt_quadratic",
                "text": "**Quadratic Probing** and **Double Hashing** reduce primary clustering by using non-linear step intervals",
                "explanation": "Quadratic probing uses step intervals $i^2$, and double hashing uses a second hash function."
            }
        ],
        "correctOptionIds": ["opt_chaining", "opt_linear_probing", "opt_quadratic"],
        "explanation": "Separate chaining stores collisions outside the main array and can exceed load factor 1.0. Open addressing stores all elements inside the table array (load factor must be $< 1.0$) and uses probing (linear, quadratic, double hashing).",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "0/1 Knapsack vs Fractional Knapsack Strategy",
        "slug": "knapsack-problem-dp-vs-greedy",
        "content": """Why does the **Greedy approach** (selecting items with highest value-to-weight ratio $v_i / w_i$) find the optimal solution for the **Fractional Knapsack problem**, but fails for the **0/1 Knapsack problem**?

$$\\text{Fractional Knapsack: } \\text{Items can be broken into arbitrary fractions}$$
$$\\text{0/1 Knapsack: } \\text{Items must either be taken entirely or left (binary } x_i \\in \\{0, 1\\}\\text{)}$$
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["dynamic-programming", "greedy", "knapsack", "algorithms"],
        "options": [
            {
                "id": "opt_a",
                "text": "In 0/1 Knapsack, taking a high-ratio item may leave unusable empty capacity that cannot be filled by smaller high-value items, violating the greedy-choice property",
                "explanation": "0/1 Knapsack requires Dynamic Programming because decisions depend on remaining capacity that cannot be filled partially."
            },
            {
                "id": "opt_b",
                "text": "Fractional knapsack requires solving overlapping subproblems using memoization",
                "explanation": "Fractional knapsack is solved greedily in O(N log N) time without DP."
            },
            {
                "id": "opt_c",
                "text": "The 0/1 Knapsack problem has an optimal solution computable in polynomial time $\\mathcal{O}(N)$ using greedy choice",
                "explanation": "0/1 Knapsack is NP-Complete; pseudo-polynomial DP takes O(N * W)."
            },
            {
                "id": "opt_d",
                "text": "Greedy fails for fractional knapsack when weights are non-integer",
                "explanation": "Greedy works perfectly for fractional knapsack with any positive real weights."
            }
        ],
        "correctOptionIds": ["opt_a"],
        "explanation": "In Fractional Knapsack, the greedy-choice property holds because any leftover capacity can be topped off with a fraction of the next best item. In 0/1 Knapsack, leaving unused capacity causes the greedy approach to produce suboptimal values, necessitating Dynamic Programming.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Binary Search on Rotated Sorted Array",
        "slug": "binary-search-on-rotated-sorted-array",
        "content": """Can an element be found in a rotated sorted array of distinct integers (e.g., `[4, 5, 6, 7, 0, 1, 2]`) in **$\\mathcal{O}(\\log N)$** time using a modified binary search?

Is this statement **True** or **False**?

```text
Key Insight: In any rotated sorted array, dividing at mid guarantees
that at least ONE half ([low..mid] or [mid..high]) is strictly sorted.
```
""",
        "type": "BOOLEAN",
        "difficulty": "MEDIUM",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["binary-search", "arrays", "rotated-array"],
        "options": [
            {
                "id": "opt_true",
                "text": "True",
                "explanation": "By checking whether nums[low] <= nums[mid], one half is guaranteed sorted. We can check if the target falls within the sorted range, halving the search space each step in O(log N)."
            },
            {
                "id": "opt_false",
                "text": "False",
                "explanation": "Linear scan O(N) is not required when elements are distinct."
            }
        ],
        "correctOptionIds": ["opt_true"],
        "explanation": "True. In a rotated sorted array without duplicates, at least one half of any subarray is strictly sorted. We check if target lies within the boundaries of the sorted half to prune half the search space, maintaining $\\mathcal{O}(\\log N)$ time.",
        "defaultPoints": 1.0,
        "negativePoints": 0.0,
        "shuffleOptions": False
    },
    {
        "title": "Topological Sort and Cycle Detection in DAGs",
        "slug": "topological-sort-and-cycle-detection",
        "content": """In **Directed Acyclic Graphs (DAGs)**, topological sorting produces a linear ordering of vertices such that for every directed edge $u \\to v$, vertex $u$ comes before $v$.

Which statements regarding topological sorting algorithms (Kahn's Algorithm and DFS) are correct?

Select all that apply.""",
        "type": "MULTIPLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["graphs", "topological-sort", "kahns-algorithm", "cycle-detection"],
        "options": [
            {
                "id": "opt_kahn",
                "text": "**Kahn's Algorithm (BFS)**: Uses an in-degree array; vertices with in-degree 0 are pushed to a queue. If the final ordered list contains fewer than $|V|$ vertices, the graph contains a directed cycle",
                "explanation": "Vertices in a cycle never reach an in-degree of 0, making Kahn's algorithm an effective cycle detector."
            },
            {
                "id": "opt_dfs",
                "text": "**DFS-Based Sort**: Pushes vertices to a stack/list in post-order traversal and reverses the result; cycles are detected if a back-edge points to an ancestor node currently in the recursion stack",
                "explanation": "Using 3-color node marking (White/Gray/Black), encountering a Gray node identifies a cycle."
            },
            {
                "id": "opt_time",
                "text": "The time complexity for both algorithms is $\\mathcal{O}(V + E)$ where $V$ is vertices and $E$ is edges",
                "explanation": "Each vertex and edge is visited a constant number of times."
            },
            {
                "id": "opt_undirected",
                "text": "Topological sort is equally well-defined and unique on undirected cyclic graphs",
                "explanation": "Topological sorting is strictly defined only for Directed Acyclic Graphs (DAGs)."
            }
        ],
        "correctOptionIds": ["opt_kahn", "opt_dfs", "opt_time"],
        "explanation": "Topological sorting can be implemented via Kahn's in-degree queue or DFS post-order reversal in $\\mathcal{O}(V + E)$ time. If the graph contains a directed cycle, a valid topological sort is mathematically impossible.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    },
    {
        "title": "Disjoint Set Union (Union-Find) Time Complexity",
        "slug": "disjoint-set-union-find-complexity",
        "content": """When implementing a **Disjoint Set Union (DSU / Union-Find)** data structure with both **Path Compression** and **Union by Rank/Size**:

What is the nearly constant amortized time complexity per operation (`find` and `union`) across $M$ operations on $N$ elements?

$$\\mathcal{O}(M \\cdot \\alpha(N))$$

Where $\\alpha$ is the Inverse Ackermann Function ($\\\\alpha(N) \\le 4$ for all practical universe values $N < 10^{80}$).
""",
        "type": "SINGLE_CHOICE",
        "difficulty": "HARD",
        "status": "PUBLISHED",
        "topics": ["Data Structures & Algorithms"],
        "tags": ["dsu", "union-find", "inverse-ackermann", "complexity"],
        "options": [
            {
                "id": "opt_ack",
                "text": "$\\mathcal{O}(\\alpha(N))$ amortized (practically $\\mathcal{O}(1)$ constant time)",
                "explanation": "Path compression flattens the tree on find, and union by rank keeps tree height logarithmic, combining to yield inverse Ackermann complexity."
            },
            {
                "id": "opt_log",
                "text": "$\\mathcal{O}(\\log N)$ strictly",
                "explanation": "Union by rank alone is O(log N); adding path compression reduces it to O(alpha(N))."
            },
            {
                "id": "opt_n",
                "text": "$\\mathcal{O}(N)$ worst-case per operation",
                "explanation": "Linear time only occurs in naive DSU without rank or path compression."
            },
            {
                "id": "opt_n2",
                "text": "$\\mathcal{O}(N^2)$",
                "explanation": "DSU is drastically faster than quadratic."
            }
        ],
        "correctOptionIds": ["opt_ack"],
        "explanation": "With both Path Compression and Union by Rank, any sequence of $M$ operations on $N$ elements runs in $\\mathcal{O}(M \\cdot \\alpha(N))$ time, where $\\alpha$ is the extremely slow-growing inverse Ackermann function, essentially constant in practice.",
        "defaultPoints": 2.0,
        "negativePoints": 0.0,
        "shuffleOptions": True
    }
]
