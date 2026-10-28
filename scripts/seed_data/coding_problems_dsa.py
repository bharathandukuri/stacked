"""
NeetCode 150 / Blind 75 - Arrays & Hashing (9 Problems)
Standard LeetCode problem statements, examples, constraints, starter code,
and calibrated testcases (including adversarial stress testcases for TLE/MLE).
"""

CODING_PROBLEMS_DSA = [
    # --------------------------------------------------------------------------
    # 1. Contains Duplicate (LeetCode 217)
    # --------------------------------------------------------------------------
    {
        "title": "Contains Duplicate",
        "slug": "contains-duplicate",
        "difficulty": "EASY",
        "problemType": "GENERIC",
        "topics": ["Array", "Hash Table", "Sorting"],
        "hints": [
            "A brute force approach compares every pair in O(N^2) time, which will exceed the time limit.",
            "Use a Hash Set to track seen numbers in O(N) time and O(N) space."
        ],
        "statement": (
            "Given an integer array `nums`, return `true` if any value appears "
            "**at least twice** in the array, and return `false` if every element is distinct.\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: nums = [1,2,3,1]\n"
            "Output: true\n"
            "Explanation: The element 1 occurs at the indices 0 and 3.\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: nums = [1,2,3,4]\n"
            "Output: false\n"
            "Explanation: All elements are distinct.\n"
            "```\n\n"
            "### Example 3:\n"
            "```\n"
            "Input: nums = [1,1,1,3,3,4,3,2,4,2]\n"
            "Output: true\n"
            "```\n\n"
            "### Constraints:\n"
            "- `1 <= nums.length <= 10^5`\n"
            "- `-10^9 <= nums[i] <= 10^9`\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: An integer `n`, the length of the array.\n"
            "  - Line 2: `n` space-separated integers representing `nums`.\n"
            "- **Output**:\n"
            "  - Print `true` if any element appears at least twice, otherwise `false` (lowercase)."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def contains_duplicate():\n"
                "    raw = sys.stdin.read().split()\n"
                "    if not raw:\n"
                "        return\n"
                "    n = int(raw[0])\n"
                "    nums = [int(x) for x in raw[1:1+n]]\n"
                "    # TODO: Return true if duplicate exists, else false\n"
                "    pass\n\n"
                "contains_duplicate()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNextInt()) return;\n"
                "        int n = sc.nextInt();\n"
                "        int[] nums = new int[n];\n"
                "        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n"
                "        // TODO: Print true or false\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (!tokens[0]) return;\n"
                "    const n = parseInt(tokens[0], 10);\n"
                "    const nums = tokens.slice(1, 1 + n).map(Number);\n"
                "    // TODO: Print true or false\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int n;\n"
                "    if (!(cin >> n)) return 0;\n"
                "    vector<int> nums(n);\n"
                "    for (int i = 0; i < n; i++) cin >> nums[i];\n"
                "    // TODO: Print true or false\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def solve():\n"
            "    raw = sys.stdin.read().split()\n"
            "    if not raw:\n"
            "        return\n"
            "    n = int(raw[0])\n"
            "    seen = set()\n"
            "    for x in raw[1:1+n]:\n"
            "        val = int(x)\n"
            "        if val in seen:\n"
            "            print('true')\n"
            "            return\n"
            "        seen.add(val)\n"
            "    print('false')\n\n"
            "solve()\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "4\n1 2 3 1", "dbInit": None},
            {"stdin": "4\n1 2 3 4", "dbInit": None},
            {"stdin": "10\n1 1 1 3 3 4 3 2 4 2", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "1\n0", "dbInit": None},
            {"stdin": "2\n-1000000000 1000000000", "dbInit": None},
            {"stdin": "2\n-5 -5", "dbInit": None},
            # Adversarial Stress Testcase: N = 10,000 distinct integers.
            # O(N^2) brute force makes 50,000,000 checks (TLE > 2000ms), while O(N) finishes in ~2ms.
            {"stdin": f"10000\n{' '.join(str(i) for i in range(10000))}", "dbInit": None},
            # Adversarial Stress Testcase 2: N = 10,000 with duplicate only at indices 0 and 9999.
            {"stdin": f"10000\n{' '.join(['42'] + [str(i) for i in range(1, 9999)] + ['42'])}", "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 2. Valid Anagram (LeetCode 242)
    # --------------------------------------------------------------------------
    {
        "title": "Valid Anagram",
        "slug": "valid-anagram",
        "difficulty": "EASY",
        "problemType": "GENERIC",
        "topics": ["Hash Table", "String", "Sorting"],
        "hints": [
            "An anagram requires identical character frequencies across all characters.",
            "Use a frequency array or hash map of size 26 for an O(N) time and O(1) space solution."
        ],
        "statement": (
            "Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.\n\n"
            "An **Anagram** is a word or phrase formed by rearranging the letters of a different word or phrase, "
            "typically using all the original letters exactly once.\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: s = \"anagram\", t = \"nagaram\"\n"
            "Output: true\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: s = \"rat\", t = \"car\"\n"
            "Output: false\n"
            "```\n\n"
            "### Constraints:\n"
            "- `1 <= s.length, t.length <= 5 * 10^4`\n"
            "- `s` and `t` consist of lowercase English letters.\n\n"
            "### Follow-up:\n"
            "What if the inputs contain Unicode characters? How would you adapt your solution to such a case?\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: String `s`.\n"
            "  - Line 2: String `t`.\n"
            "- **Output**:\n"
            "  - Print `true` if `t` is an anagram of `s`, otherwise `false` (lowercase)."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def is_anagram():\n"
                "    lines = sys.stdin.read().split()\n"
                "    if len(lines) < 2:\n"
                "        return\n"
                "    s, t = lines[0], lines[1]\n"
                "    # TODO: Print true or false\n"
                "    pass\n\n"
                "is_anagram()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNext()) return;\n"
                "        String s = sc.next();\n"
                "        String t = sc.next();\n"
                "        // TODO: Print true or false\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (tokens.length < 2) return;\n"
                "    const s = tokens[0], t = tokens[1];\n"
                "    // TODO: Print true or false\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <string>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    string s, t;\n"
                "    if (!(cin >> s >> t)) return 0;\n"
                "    // TODO: Print true or false\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def solve():\n"
            "    tokens = sys.stdin.read().split()\n"
            "    if len(tokens) < 2:\n"
            "        return\n"
            "    s, t = tokens[0], tokens[1]\n"
            "    if len(s) != len(t):\n"
            "        print('false')\n"
            "        return\n"
            "    counts = [0] * 26\n"
            "    for ch in s:\n"
            "        counts[ord(ch) - 97] += 1\n"
            "    for ch in t:\n"
            "        counts[ord(ch) - 97] -= 1\n"
            "        if counts[ord(ch) - 97] < 0:\n"
            "            print('false')\n"
            "            return\n"
            "    print('true')\n\n"
            "solve()\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "anagram\nnagaram", "dbInit": None},
            {"stdin": "rat\ncar", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "a\nb", "dbInit": None},
            {"stdin": "a\na", "dbInit": None},
            {"stdin": "ab\na", "dbInit": None},
            {"stdin": "aaabbb\nbbbaaa", "dbInit": None},
            # Stress Case: N = 25,000 chars. Inefficient character replacement runs in O(N^2) (TLE).
            {"stdin": f"{'a' * 24999 + 'b'}\n{'b' + 'a' * 24999}", "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 3. Two Sum (LeetCode 1)
    # --------------------------------------------------------------------------
    {
        "title": "Two Sum",
        "slug": "two-sum",
        "difficulty": "EASY",
        "problemType": "GENERIC",
        "topics": ["Array", "Hash Table"],
        "hints": [
            "A brute force approach checks all pairs in O(N^2) time, which will exceed the 2000ms time limit.",
            "Can you trade space for speed by storing visited numbers in a Hash Map to check complement in O(1)?"
        ],
        "statement": (
            "Given an array of integers `nums` and an integer `target`, return *indices of the two numbers such that they add up to `target`*.\n\n"
            "You may assume that each input would have **exactly one solution**, and you may not use the *same* element twice.\n\n"
            "You can return the answer in any order (print the two indices separated by a space).\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: nums = [2,7,11,15], target = 9\n"
            "Output: 0 1\n"
            "Explanation: Because nums[0] + nums[1] == 9, we return 0 1.\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: nums = [3,2,4], target = 6\n"
            "Output: 1 2\n"
            "```\n\n"
            "### Example 3:\n"
            "```\n"
            "Input: nums = [3,3], target = 6\n"
            "Output: 0 1\n"
            "```\n\n"
            "### Constraints:\n"
            "- `2 <= nums.length <= 10^4`\n"
            "- `-10^9 <= nums[i] <= 10^9`\n"
            "- `-10^9 <= target <= 10^9`\n"
            "- **Only one valid answer exists.**\n\n"
            "### Follow-up:\n"
            "Can you come up with an algorithm that is less than $O(n^2)$ time complexity?\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: Two space-separated integers `n` (array length) and `target`.\n"
            "  - Line 2: `n` space-separated integers representing `nums`.\n"
            "- **Output**:\n"
            "  - Print the two zero-based indices separated by a space."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def two_sum():\n"
                "    raw = sys.stdin.read().split()\n"
                "    if not raw:\n"
                "        return\n"
                "    n, target = int(raw[0]), int(raw[1])\n"
                "    nums = [int(x) for x in raw[2:2+n]]\n"
                "    # TODO: Print the two 0-based indices\n"
                "    pass\n\n"
                "two_sum()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNextInt()) return;\n"
                "        int n = sc.nextInt();\n"
                "        int target = sc.nextInt();\n"
                "        int[] nums = new int[n];\n"
                "        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n"
                "        // TODO: Print the two 0-based indices\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (tokens.length < 2) return;\n"
                "    const n = parseInt(tokens[0], 10);\n"
                "    const target = parseInt(tokens[1], 10);\n"
                "    const nums = tokens.slice(2, 2 + n).map(Number);\n"
                "    // TODO: Print the two 0-based indices\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int n, target;\n"
                "    if (!(cin >> n >> target)) return 0;\n"
                "    vector<int> nums(n);\n"
                "    for (int i = 0; i < n; i++) cin >> nums[i];\n"
                "    // TODO: Print the two 0-based indices\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def solve():\n"
            "    raw = sys.stdin.read().split()\n"
            "    if not raw:\n"
            "        return\n"
            "    n = int(raw[0])\n"
            "    target = int(raw[1])\n"
            "    seen = {}\n"
            "    for i in range(n):\n"
            "        num = int(raw[2 + i])\n"
            "        diff = target - num\n"
            "        if diff in seen:\n"
            "            print(f\"{seen[diff]} {i}\")\n"
            "            return\n"
            "        seen[num] = i\n\n"
            "solve()\n"
        ),
        "requireVerificationScript": True,
        "verificationScriptLanguage": "python-3.12",
        "verificationScript": (
            "import sys\n\n"
            "# argv[1]: input file (contains n target and nums)\n"
            "# argv[2]: candidate output file (contains two space-separated indices)\n"
            "# argv[3]: reference solution output file\n\n"
            "with open(sys.argv[1], 'r') as f:\n"
            "    tokens = [int(x) for x in f.read().split()]\n"
            "    if len(tokens) < 2:\n"
            "        sys.exit(1)\n"
            "    n = tokens[0]\n"
            "    target = tokens[1]\n"
            "    nums = tokens[2:2+n]\n\n"
            "with open(sys.argv[2], 'r') as f:\n"
            "    out_tokens = [int(x) for x in f.read().split()]\n\n"
            "if len(out_tokens) != 2:\n"
            "    print(f'Expected 2 space-separated indices, got {len(out_tokens)}', file=sys.stderr)\n"
            "    sys.exit(1)\n\n"
            "i, j = out_tokens[0], out_tokens[1]\n\n"
            "if i == j:\n"
            "    print(f'Cannot use the same element twice (i={i}, j={j})', file=sys.stderr)\n"
            "    sys.exit(1)\n\n"
            "if not (0 <= i < n and 0 <= j < n):\n"
            "    print(f'Indices out of range [0, {n-1}]: i={i}, j={j}', file=sys.stderr)\n"
            "    sys.exit(1)\n\n"
            "if nums[i] + nums[j] != target:\n"
            "    print(f'nums[{i}] ({nums[i]}) + nums[{j}] ({nums[j]}) = {nums[i] + nums[j]} != target ({target})', file=sys.stderr)\n"
            "    sys.exit(1)\n\n"
            "sys.exit(0)\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "4 9\n2 7 11 15", "dbInit": None},
            {"stdin": "3 6\n3 2 4", "dbInit": None},
            {"stdin": "2 6\n3 3", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "2 -10\n-7 -3", "dbInit": None},
            {"stdin": "4 0\n-5 2 3 5", "dbInit": None},
            # Adversarial Stress Testcase: N = 10,000, target pair at index 9998 and 9999.
            # O(N^2) brute force checks 50,000,000 pairs (~3.8s in Python -> TLE).
            # O(N) hash map completes in ~2.5ms.
            {"stdin": f"10000 19999\n{' '.join(str(i) for i in range(1, 10001))}", "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 4. Group Anagrams (LeetCode 49)
    # --------------------------------------------------------------------------
    {
        "title": "Group Anagrams",
        "slug": "group-anagrams",
        "difficulty": "MEDIUM",
        "problemType": "GENERIC",
        "topics": ["Array", "Hash Table", "String", "Sorting"],
        "hints": [
            "Strings that are anagrams of each other share the same sorted sequence of characters.",
            "Map each sorted string (or tuple of 26 character counts) to a list of its anagrams."
        ],
        "statement": (
            "Given an array of strings `strs`, group the **anagrams** together. You can return the answer in any order.\n\n"
            "An **Anagram** is a word or phrase formed by rearranging the letters of a different word or phrase, "
            "typically using all the original letters exactly once.\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: strs = [\"eat\",\"tea\",\"tan\",\"ate\",\"nat\",\"bat\"]\n"
            "Output: [[\"bat\"],[\"nat\",\"tea\"],[\"ate\",\"eat\"]]\n"
            "Explanation:\n"
            "- \"nat\" and \"tan\" are anagrams.\n"
            "- \"ate\", \"eat\", and \"tea\" are anagrams.\n"
            "- \"bat\" has no other anagrams.\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: strs = [\"\"]\n"
            "Output: [[\"\"]]\n"
            "```\n\n"
            "### Example 3:\n"
            "```\n"
            "Input: strs = [\"a\"]\n"
            "Output: [[\"a\"]]\n"
            "```\n\n"
            "### Constraints:\n"
            "- `1 <= strs.length <= 10^4`\n"
            "- `0 <= strs[i].length <= 100`\n"
            "- `strs[i]` consists of lowercase English letters.\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: An integer `n`, the number of strings.\n"
            "  - Next `n` lines: Each contains a single string `strs[i]`.\n"
            "- **Output**:\n"
            "  - Print each anagram group on a separate line, with words sorted alphabetically and groups sorted alphabetically by their first word."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def group_anagrams():\n"
                "    lines = sys.stdin.read().splitlines()\n"
                "    if not lines:\n"
                "        return\n"
                "    n = int(lines[0].strip())\n"
                "    strs = [lines[i] if i < len(lines) else '' for i in range(1, 1 + n)]\n"
                "    # TODO: Group anagrams\n"
                "    pass\n\n"
                "group_anagrams()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNextInt()) return;\n"
                "        int n = sc.nextInt();\n"
                "        sc.nextLine();\n"
                "        List<String> strs = new ArrayList<>();\n"
                "        for (int i = 0; i < n; i++) {\n"
                "            strs.add(sc.hasNextLine() ? sc.nextLine() : \"\");\n"
                "        }\n"
                "        // TODO: Group anagrams\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const lines = fs.readFileSync(0, 'utf-8').split(/\\r?\\n/);\n"
                "    if (!lines[0]) return;\n"
                "    const n = parseInt(lines[0].trim(), 10);\n"
                "    const strs = lines.slice(1, 1 + n);\n"
                "    // TODO: Group anagrams\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "#include <string>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int n;\n"
                "    if (!(cin >> n)) return 0;\n"
                "    string dummy;\n"
                "    getline(cin, dummy);\n"
                "    vector<string> strs(n);\n"
                "    for (int i = 0; i < n; i++) getline(cin, strs[i]);\n"
                "    // TODO: Group anagrams\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n"
            "from collections import defaultdict\n\n"
            "def solve():\n"
            "    lines = sys.stdin.read().splitlines()\n"
            "    if not lines:\n"
            "        return\n"
            "    n = int(lines[0].strip())\n"
            "    groups = defaultdict(list)\n"
            "    for i in range(1, 1 + n):\n"
            "        s = lines[i] if i < len(lines) else ''\n"
            "        key = ''.join(sorted(s))\n"
            "        groups[key].append(s)\n"
            "    res = [sorted(group) for group in groups.values()]\n"
            "    res.sort(key=lambda g: g[0] if g else '')\n"
            "    for group in res:\n"
            "        print(' '.join(group))\n\n"
            "solve()\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "6\neat\ntea\ntan\nate\nnat\nbat", "dbInit": None},
            {"stdin": "1\n", "dbInit": None},
            {"stdin": "1\na", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "3\na\nb\nc", "dbInit": None},
            {"stdin": "4\naaa\naaa\naaa\naaa", "dbInit": None},
            # Stress Case: N = 4,000 strings of 10 chars.
            # O(N^2 * K) pairwise comparison checks take > 4s (TLE).
            # O(N * K log K) hash map grouping completes in < 20ms.
            {"stdin": f"4000\n" + "\n".join(["abcdefghij", "jihgfedcba", "bcdefghija", "cdefghijab"] * 1000), "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 5. Top K Frequent Elements (LeetCode 347)
    # --------------------------------------------------------------------------
    {
        "title": "Top K Frequent Elements",
        "slug": "top-k-frequent-elements",
        "difficulty": "MEDIUM",
        "problemType": "GENERIC",
        "topics": ["Array", "Hash Table", "Divide and Conquer", "Sorting", "Heap (Priority Queue)", "Bucket Sort", "Counting"],
        "hints": [
            "Count frequencies using a Hash Map first in O(N) time.",
            "Use Bucket Sort (buckets indexed by frequency 1..N) to find top K elements in O(N) time without sorting."
        ],
        "statement": (
            "Given an integer array `nums` and an integer `k`, return *the* `k` *most frequent elements*.\n\n"
            "You may return the answer in **any order** (print the `k` numbers separated by a space, "
            "sorted in descending order of frequency).\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: nums = [1,1,1,2,2,3], k = 2\n"
            "Output: 1 2\n"
            "Explanation: 1 appears 3 times, 2 appears 2 times, and 3 appears 1 time.\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: nums = [1], k = 1\n"
            "Output: 1\n"
            "```\n\n"
            "### Constraints:\n"
            "- `1 <= nums.length <= 10^5`\n"
            "- `-10^4 <= nums[i] <= 10^4`\n"
            "- `k` is in the range `[1, the number of unique elements in the array]`.\n"
            "- It is **guaranteed** that the answer is **unique**.\n\n"
            "### Follow-up:\n"
            "Your algorithm's time complexity must be better than $O(n \\log n)$, where $n$ is the array's size.\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: Two space-separated integers `n` (array length) and `k`.\n"
            "  - Line 2: `n` space-separated integers representing `nums`.\n"
            "- **Output**:\n"
            "  - Print the `k` most frequent elements separated by a space (sorted by frequency descending)."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def top_k_frequent():\n"
                "    raw = sys.stdin.read().split()\n"
                "    if not raw:\n"
                "        return\n"
                "    n, k = int(raw[0]), int(raw[1])\n"
                "    nums = [int(x) for x in raw[2:2+n]]\n"
                "    # TODO: Print k most frequent elements\n"
                "    pass\n\n"
                "top_k_frequent()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNextInt()) return;\n"
                "        int n = sc.nextInt();\n"
                "        int k = sc.nextInt();\n"
                "        int[] nums = new int[n];\n"
                "        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n"
                "        // TODO: Print k most frequent elements\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (tokens.length < 2) return;\n"
                "    const n = parseInt(tokens[0], 10);\n"
                "    const k = parseInt(tokens[1], 10);\n"
                "    const nums = tokens.slice(2, 2 + n).map(Number);\n"
                "    // TODO: Print k most frequent elements\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int n, k;\n"
                "    if (!(cin >> n >> k)) return 0;\n"
                "    vector<int> nums(n);\n"
                "    for (int i = 0; i < n; i++) cin >> nums[i];\n"
                "    // TODO: Print k most frequent elements\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n"
            "from collections import Counter\n\n"
            "def solve():\n"
            "    raw = sys.stdin.read().split()\n"
            "    if not raw:\n"
            "        return\n"
            "    n = int(raw[0])\n"
            "    k = int(raw[1])\n"
            "    counts = Counter(int(x) for x in raw[2:2+n])\n"
            "    # Bucket sort: bucket[i] stores elements appearing i times\n"
            "    buckets = [[] for _ in range(n + 1)]\n"
            "    for num, freq in counts.items():\n"
            "        buckets[freq].append(num)\n"
            "    res = []\n"
            "    for freq in range(n, 0, -1):\n"
            "        if buckets[freq]:\n"
            "            for num in sorted(buckets[freq]):\n"
            "                res.append(num)\n"
            "                if len(res) == k:\n"
            "                    print(' '.join(str(x) for x in res))\n"
            "                    return\n\n"
            "solve()\n"
        ),
        "requireVerificationScript": True,
        "verificationScriptLanguage": "python-3.12",
        "verificationScript": (
            "import sys\n"
            "from collections import Counter\n\n"
            "# argv[1]: input file (n, k, nums)\n"
            "# argv[2]: candidate output file (k elements)\n"
            "# argv[3]: reference solution output file\n\n"
            "with open(sys.argv[1], 'r') as f:\n"
            "    tokens = [int(x) for x in f.read().split()]\n"
            "    if len(tokens) < 2:\n"
            "        sys.exit(1)\n"
            "    n = tokens[0]\n"
            "    k = tokens[1]\n"
            "    nums = tokens[2:2+n]\n\n"
            "with open(sys.argv[2], 'r') as f:\n"
            "    cand = [int(x) for x in f.read().split()]\n\n"
            "if len(cand) != k:\n"
            "    print(f'Expected {k} numbers, got {len(cand)}', file=sys.stderr)\n"
            "    sys.exit(1)\n\n"
            "if len(set(cand)) != k:\n"
            "    print(f'Duplicate values in output: {cand}', file=sys.stderr)\n"
            "    sys.exit(1)\n\n"
            "counts = Counter(nums)\n"
            "for x in cand:\n"
            "    if x not in counts:\n"
            "        print(f'Value {x} is not in input array', file=sys.stderr)\n"
            "        sys.exit(1)\n\n"
            "min_chosen_freq = min(counts[x] for x in cand)\n"
            "for num, freq in counts.items():\n"
            "    if num not in cand and freq > min_chosen_freq:\n"
            "        print(f'Element {num} with frequency {freq} was omitted while element with frequency {min_chosen_freq} was included', file=sys.stderr)\n"
            "        sys.exit(1)\n\n"
            "sys.exit(0)\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "6 2\n1 1 1 2 2 3", "dbInit": None},
            {"stdin": "1 1\n1", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "4 2\n-1 -1 -2 -2", "dbInit": None},
            {"stdin": "5 1\n4 4 4 4 4", "dbInit": None},
            # Stress Case: N = 10,000, K = 5,000 distinct numbers.
            # Inefficient O(N * K) repeated scan takes > 3.5s (TLE).
            # O(N) bucket sort / heap takes < 10ms.
            {"stdin": f"10000 5000\n{' '.join(str(i) for i in range(10000))}", "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 6. Encode and Decode Strings (LeetCode 271)
    # --------------------------------------------------------------------------
    {
        "title": "Encode and Decode Strings",
        "slug": "encode-and-decode-strings",
        "difficulty": "MEDIUM",
        "problemType": "GENERIC",
        "topics": ["Array", "String", "Design"],
        "hints": [
            "Any custom delimiter character (like ',', '#', or ':') can appear inside the strings themselves.",
            "Prepend each string with its length followed by a delimiter, e.g. '4#lint'."
        ],
        "statement": (
            "Design an algorithm to encode a list of strings to a single string. "
            "The encoded string is then decoded back to the original list of strings.\n\n"
            "Implement `encode` and `decode`:\n"
            "- `encode(strs)`: Serializes a list of strings into a single string.\n"
            "- `decode(s)`: Reconstructs the exact list of strings from the encoded string.\n\n"
            "**Note:** The strings may contain any of the 256 possible ASCII characters, "
            "including delimiters like commas, colons, spaces, and `#`.\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: strs = [\"lint\",\"code\",\"love\",\"you\"]\n"
            "Output: [\"lint\",\"code\",\"love\",\"you\"]\n"
            "Explanation: One valid encoded format is \"4#lint4#code4#love3#you\".\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: strs = [\"we\", \"say\", \":\", \"yes\"]\n"
            "Output: [\"we\", \"say\", \":\", \"yes\"]\n"
            "```\n\n"
            "### Constraints:\n"
            "- `1 <= strs.length <= 10^4`\n"
            "- `0 <= strs[i].length <= 200`\n"
            "- `strs[i]` contains any valid ASCII characters.\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: An integer `n`, the number of strings.\n"
            "  - Next `n` lines: Each contains a single string `strs[i]`.\n"
            "- **Output**:\n"
            "  - Print each reconstructed string on a new line."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def encode(strs):\n"
                "    # TODO: Serialize list of strings\n"
                "    pass\n\n"
                "def decode(s):\n"
                "    # TODO: Deserialize into list of strings\n"
                "    pass\n\n"
                "def main():\n"
                "    lines = sys.stdin.read().splitlines()\n"
                "    if not lines:\n"
                "        return\n"
                "    n = int(lines[0].strip())\n"
                "    strs = [lines[i] if i < len(lines) else '' for i in range(1, 1 + n)]\n"
                "    encoded = encode(strs)\n"
                "    decoded = decode(encoded)\n"
                "    for item in decoded:\n"
                "        print(item)\n\n"
                "main()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static String encode(List<String> strs) {\n"
                "        // TODO: encode\n"
                "        return \"\";\n"
                "    }\n\n"
                "    public static List<String> decode(String s) {\n"
                "        // TODO: decode\n"
                "        return new ArrayList<>();\n"
                "    }\n\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNextInt()) return;\n"
                "        int n = sc.nextInt();\n"
                "        sc.nextLine();\n"
                "        List<String> strs = new ArrayList<>();\n"
                "        for (int i = 0; i < n; i++) {\n"
                "            strs.add(sc.hasNextLine() ? sc.nextLine() : \"\");\n"
                "        }\n"
                "        List<String> decoded = decode(encode(strs));\n"
                "        for (String s : decoded) System.out.println(s);\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function encode(strs) {\n"
                "    // TODO: encode\n"
                "    return '';\n"
                "}\n\n"
                "function decode(s) {\n"
                "    // TODO: decode\n"
                "    return [];\n"
                "}\n\n"
                "function main() {\n"
                "    const lines = fs.readFileSync(0, 'utf-8').split(/\\r?\\n/);\n"
                "    if (!lines[0]) return;\n"
                "    const n = parseInt(lines[0].trim(), 10);\n"
                "    const strs = lines.slice(1, 1 + n);\n"
                "    const decoded = decode(encode(strs));\n"
                "    decoded.forEach(s => console.log(s));\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "#include <string>\n"
                "using namespace std;\n\n"
                "string encode(const vector<string>& strs) {\n"
                "    // TODO: encode\n"
                "    return \"\";\n"
                "}\n\n"
                "vector<string> decode(const string& s) {\n"
                "    // TODO: decode\n"
                "    return {};\n"
                "}\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int n;\n"
                "    if (!(cin >> n)) return 0;\n"
                "    string dummy;\n"
                "    getline(cin, dummy);\n"
                "    vector<string> strs(n);\n"
                "    for (int i = 0; i < n; i++) getline(cin, strs[i]);\n"
                "    vector<string> decoded = decode(encode(strs));\n"
                "    for (const string& s : decoded) cout << s << \"\\n\";\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def encode(strs):\n"
            "    parts = []\n"
            "    for s in strs:\n"
            "        parts.append(f\"{len(s)}#{s}\")\n"
            "    return ''.join(parts)\n\n"
            "def decode(s):\n"
            "    res = []\n"
            "    i = 0\n"
            "    while i < len(s):\n"
            "        j = s.find('#', i)\n"
            "        length = int(s[i:j])\n"
            "        res.append(s[j + 1 : j + 1 + length])\n"
            "        i = j + 1 + length\n"
            "    return res\n\n"
            "def solve():\n"
            "    lines = sys.stdin.read().splitlines()\n"
            "    if not lines:\n"
            "        return\n"
            "    n = int(lines[0].strip())\n"
            "    strs = [lines[i] if i < len(lines) else '' for i in range(1, 1 + n)]\n"
            "    decoded = decode(encode(strs))\n"
            "    for item in decoded:\n"
            "        print(item)\n\n"
            "solve()\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "4\nlint\ncode\nlove\nyou", "dbInit": None},
            {"stdin": "4\nwe\nsay\n:\nyes", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "1\n", "dbInit": None},
            {"stdin": "3\n#\n4#code\n:::", "dbInit": None},
            # Stress Case: N = 5,000 strings.
            # Quadratic string concatenation (e.g. s += ...) triggers TLE > 2000ms.
            {"stdin": f"5000\n" + "\n".join([f"word_{i}#special" for i in range(5000)]), "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 7. Product of Array Except Self (LeetCode 238)
    # --------------------------------------------------------------------------
    {
        "title": "Product of Array Except Self",
        "slug": "product-of-array-except-self",
        "difficulty": "MEDIUM",
        "problemType": "GENERIC",
        "topics": ["Array", "Prefix Sum"],
        "hints": [
            "The product except self at index i is prefix_product[i-1] * suffix_product[i+1].",
            "You can construct the prefix product in the result array, then traverse backwards to multiply suffix products using O(1) extra space."
        ],
        "statement": (
            "Given an integer array `nums`, return *an array* `answer` *such that* `answer[i]` *is equal to the product of all the elements of* `nums` *except* `nums[i]`.\n\n"
            "The product of any prefix or suffix of `nums` is **guaranteed** to fit in a **32-bit** integer.\n\n"
            "You must write an algorithm that runs in **$O(n)$ time** and without using the division operation.\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: nums = [1,2,3,4]\n"
            "Output: 24 12 8 6\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: nums = [-1,1,0,-3,3]\n"
            "Output: 0 0 9 0 0\n"
            "```\n\n"
            "### Constraints:\n"
            "- `2 <= nums.length <= 10^5`\n"
            "- `-30 <= nums[i] <= 30`\n"
            "- The product of any prefix or suffix of `nums` is **guaranteed** to fit in a **32-bit** integer.\n\n"
            "### Follow-up:\n"
            "Can you solve the problem in $O(1)$ extra space complexity? (The output array does not count as extra space for space complexity analysis.)\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: An integer `n`, the size of the array.\n"
            "  - Line 2: `n` space-separated integers representing `nums`.\n"
            "- **Output**:\n"
            "  - Print `n` space-separated integers representing the `answer` array."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def product_except_self():\n"
                "    raw = sys.stdin.read().split()\n"
                "    if not raw:\n"
                "        return\n"
                "    n = int(raw[0])\n"
                "    nums = [int(x) for x in raw[1:1+n]]\n"
                "    # TODO: Compute answer array without division\n"
                "    pass\n\n"
                "product_except_self()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNextInt()) return;\n"
                "        int n = sc.nextInt();\n"
                "        int[] nums = new int[n];\n"
                "        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n"
                "        // TODO: Compute answer array without division\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (!tokens[0]) return;\n"
                "    const n = parseInt(tokens[0], 10);\n"
                "    const nums = tokens.slice(1, 1 + n).map(Number);\n"
                "    // TODO: Compute answer array without division\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int n;\n"
                "    if (!(cin >> n)) return 0;\n"
                "    vector<int> nums(n);\n"
                "    for (int i = 0; i < n; i++) cin >> nums[i];\n"
                "    // TODO: Compute answer array without division\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def solve():\n"
            "    raw = sys.stdin.read().split()\n"
            "    if not raw:\n"
            "        return\n"
            "    n = int(raw[0])\n"
            "    nums = [int(x) for x in raw[1:1+n]]\n"
            "    res = [1] * n\n"
            "    prefix = 1\n"
            "    for i in range(n):\n"
            "        res[i] = prefix\n"
            "        prefix *= nums[i]\n"
            "    postfix = 1\n"
            "    for i in range(n - 1, -1, -1):\n"
            "        res[i] *= postfix\n"
            "        postfix *= nums[i]\n"
            "    print(' '.join(str(x) for x in res))\n\n"
            "solve()\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "4\n1 2 3 4", "dbInit": None},
            {"stdin": "5\n-1 1 0 -3 3", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "2\n0 0", "dbInit": None},
            {"stdin": "3\n1 0 2", "dbInit": None},
            # Stress Case: N = 10,000. Recomputing products via nested loop does 10^8 operations (TLE > 2000ms).
            {"stdin": f"10000\n{' '.join(['1', '-1'] * 5000)}", "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 8. Valid Sudoku (LeetCode 36)
    # --------------------------------------------------------------------------
    {
        "title": "Valid Sudoku",
        "slug": "valid-sudoku",
        "difficulty": "MEDIUM",
        "problemType": "GENERIC",
        "topics": ["Array", "Hash Table", "Matrix"],
        "hints": [
            "Check every row for duplicate digits 1-9.",
            "Check every column for duplicate digits 1-9.",
            "Check each of the nine 3x3 sub-boxes indexed by (r // 3, c // 3)."
        ],
        "statement": (
            "Determine if a `9 x 9` Sudoku board is valid. Only the filled cells need to be validated according to the following rules:\n\n"
            "1. Each row must contain the digits `1-9` without repetition.\n"
            "2. Each column must contain the digits `1-9` without repetition.\n"
            "3. Each of the nine `3 x 3` sub-boxes of the grid must contain the digits `1-9` without repetition.\n\n"
            "**Note:**\n"
            "- A Sudoku board (partially filled) could be valid but is not necessarily solvable.\n"
            "- Only the filled cells need to be validated according to the mentioned rules.\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: board = \n"
            "[[\"5\",\"3\",\".\",\".\",\"7\",\".\",\".\",\".\",\".\"]\n"
            ",[\"6\",\".\",\".\",\"1\",\"9\",\"5\",\".\",\".\",\".\"]\n"
            ",[\".\",\"9\",\"8\",\".\",\".\",\".\",\".\",\"6\",\".\"]\n"
            ",[\"8\",\".\",\".\",\".\",\"6\",\".\",\".\",\".\",\"3\"]\n"
            ",[\"4\",\".\",\".\",\"8\",\".\",\"3\",\".\",\".\",\"1\"]\n"
            ",[\"7\",\".\",\".\",\".\",\"2\",\".\",\".\",\".\",\"6\"]\n"
            ",[\".\",\"6\",\".\",\".\",\".\",\".\",\"2\",\"8\",\".\"]\n"
            ",[\".\",\".\",\".\",\"4\",\"1\",\"9\",\".\",\".\",\"5\"]\n"
            ",[\".\",\".\",\".\",\".\",\"8\",\".\",\".\",\"7\",\"9\"]]\n"
            "Output: true\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: board = \n"
            "[[\"8\",\"3\",\".\",\".\",\"7\",\".\",\".\",\".\",\".\"]\n"
            ",[\"6\",\".\",\".\",\"1\",\"9\",\"5\",\".\",\".\",\".\"]\n"
            ",[\".\",\"9\",\"8\",\".\",\".\",\".\",\".\",\"6\",\".\"]\n"
            ",[\"8\",\".\",\".\",\".\",\"6\",\".\",\".\",\".\",\"3\"]\n"
            ",[\"4\",\".\",\".\",\"8\",\".\",\"3\",\".\",\".\",\"1\"]\n"
            ",[\"7\",\".\",\".\",\".\",\"2\",\".\",\".\",\".\",\"6\"]\n"
            ",[\".\",\"6\",\".\",\".\",\".\",\".\",\"2\",\"8\",\".\"]\n"
            ",[\".\",\".\",\".\",\"4\",\"1\",\"9\",\".\",\".\",\"5\"]\n"
            ",[\".\",\".\",\".\",\".\",\"8\",\".\",\".\",\"7\",\"9\"]]\n"
            "Output: false\n"
            "Explanation: Same as Example 1, except with the 5 in the top left corner modified to 8. "
            "Since there are two 8s in the top-left 3x3 sub-box, it is invalid.\n"
            "```\n\n"
            "### Constraints:\n"
            "- `board.length == 9`\n"
            "- `board[i].length == 9`\n"
            "- `board[i][j]` is a digit `'1'-'9'` or `'.'`. \n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - 9 lines, each containing 9 space-separated characters representing rows of the board.\n"
            "- **Output**:\n"
            "  - Print `true` if the board is valid, otherwise `false` (lowercase)."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def is_valid_sudoku():\n"
                "    lines = sys.stdin.read().splitlines()\n"
                "    board = [line.split() for line in lines if line.strip()]\n"
                "    # TODO: Print true or false\n"
                "    pass\n\n"
                "is_valid_sudoku()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        char[][] board = new char[9][9];\n"
                "        for (int i = 0; i < 9; i++) {\n"
                "            for (int j = 0; j < 9; j++) {\n"
                "                board[i][j] = sc.next().charAt(0);\n"
                "            }\n"
                "        }\n"
                "        // TODO: Print true or false\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (tokens.length < 81) return;\n"
                "    // TODO: Print true or false\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    vector<vector<char>> board(9, vector<char>(9));\n"
                "    for (int i = 0; i < 9; i++) {\n"
                "        for (int j = 0; j < 9; j++) {\n"
                "            cin >> board[i][j];\n"
                "        }\n"
                "    }\n"
                "    // TODO: Print true or false\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def solve():\n"
            "    tokens = sys.stdin.read().split()\n"
            "    if len(tokens) < 81:\n"
            "        return\n"
            "    rows = [set() for _ in range(9)]\n"
            "    cols = [set() for _ in range(9)]\n"
            "    boxes = [set() for _ in range(9)]\n"
            "    for idx in range(81):\n"
            "        r = idx // 9\n"
            "        c = idx % 9\n"
            "        val = tokens[idx]\n"
            "        if val == '.':\n"
            "            continue\n"
            "        box_idx = (r // 3) * 3 + (c // 3)\n"
            "        if val in rows[r] or val in cols[c] or val in boxes[box_idx]:\n"
            "            print('false')\n"
            "            return\n"
            "        rows[r].add(val)\n"
            "        cols[c].add(val)\n"
            "        boxes[box_idx].add(val)\n"
            "    print('true')\n\n"
            "solve()\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {
                "stdin": (
                    "5 3 . . 7 . . . .\n"
                    "6 . . 1 9 5 . . .\n"
                    ". 9 8 . . . . 6 .\n"
                    "8 . . . 6 . . . 3\n"
                    "4 . . 8 . 3 . . 1\n"
                    "7 . . . 2 . . . 6\n"
                    ". 6 . . . . 2 8 .\n"
                    ". . . 4 1 9 . . 5\n"
                    ". . . . 8 . . 7 9"
                ),
                "dbInit": None
            },
            {
                "stdin": (
                    "8 3 . . 7 . . . .\n"
                    "6 . . 1 9 5 . . .\n"
                    ". 9 8 . . . . 6 .\n"
                    "8 . . . 6 . . . 3\n"
                    "4 . . 8 . 3 . . 1\n"
                    "7 . . . 2 . . . 6\n"
                    ". 6 . . . . 2 8 .\n"
                    ". . . 4 1 9 . . 5\n"
                    ". . . . 8 . . 7 9"
                ),
                "dbInit": None
            }
        ],
        "hiddenTestcases": [
            {
                # Completely empty board is valid
                "stdin": "\n".join([". . . . . . . . ."] * 9),
                "dbInit": None
            },
            {
                # Duplicate in column 0
                "stdin": (
                    "2 . . . . . . . .\n"
                    "2 . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . ."
                ),
                "dbInit": None
            },
            {
                # Duplicate in row 0
                "stdin": (
                    "3 3 . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . .\n"
                    ". . . . . . . . ."
                ),
                "dbInit": None
            }
        ]
    },

    # --------------------------------------------------------------------------
    # 9. Longest Consecutive Sequence (LeetCode 128)
    # --------------------------------------------------------------------------
    {
        "title": "Longest Consecutive Sequence",
        "slug": "longest-consecutive-sequence",
        "difficulty": "MEDIUM",
        "problemType": "GENERIC",
        "topics": ["Array", "Hash Table", "Union Find"],
        "hints": [
            "Store all numbers in a Hash Set for O(1) membership lookup.",
            "Only attempt to build a sequence starting at 'num' if 'num - 1' is NOT in the set (i.e. 'num' is the sequence beginning). This ensures each number is visited at most twice for O(N) total time."
        ],
        "statement": (
            "Given an unsorted array of integers `nums`, return *the length of the longest consecutive elements sequence*.\n\n"
            "You must write an algorithm that runs in **$O(n)$ time**.\n\n"
            "### Example 1:\n"
            "```\n"
            "Input: nums = [100,4,200,1,3,2]\n"
            "Output: 4\n"
            "Explanation: The longest consecutive elements sequence is [1, 2, 3, 4]. Therefore its length is 4.\n"
            "```\n\n"
            "### Example 2:\n"
            "```\n"
            "Input: nums = [0,3,7,2,5,8,4,6,0,1]\n"
            "Output: 9\n"
            "Explanation: The consecutive sequence is [0, 1, 2, 3, 4, 5, 6, 7, 8].\n"
            "```\n\n"
            "### Constraints:\n"
            "- `0 <= nums.length <= 10^5`\n"
            "- `-10^9 <= nums[i] <= 10^9`\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: An integer `n`, the length of the array.\n"
            "  - Line 2: `n` space-separated integers representing `nums` (if $n > 0$).\n"
            "- **Output**:\n"
            "  - Print a single integer representing the length of the longest consecutive sequence."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def longest_consecutive():\n"
                "    raw = sys.stdin.read().split()\n"
                "    if not raw:\n"
                "        print(0)\n"
                "        return\n"
                "    n = int(raw[0])\n"
                "    nums = [int(x) for x in raw[1:1+n]]\n"
                "    # TODO: Return length in O(N) time\n"
                "    pass\n\n"
                "longest_consecutive()\n"
            ),
            "java-21": (
                "import java.util.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) {\n"
                "        Scanner sc = new Scanner(System.in);\n"
                "        if (!sc.hasNextInt()) {\n"
                "            System.out.println(0);\n"
                "            return;\n"
                "        }\n"
                "        int n = sc.nextInt();\n"
                "        int[] nums = new int[n];\n"
                "        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n"
                "        // TODO: Return length in O(N) time\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (!tokens[0] || tokens[0] === '0') {\n"
                "        console.log(0);\n"
                "        return;\n"
                "    }\n"
                "    const n = parseInt(tokens[0], 10);\n"
                "    const nums = tokens.slice(1, 1 + n).map(Number);\n"
                "    // TODO: Return length in O(N) time\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int n;\n"
                "    if (!(cin >> n) || n == 0) {\n"
                "        cout << 0 << \"\\n\";\n"
                "        return 0;\n"
                "    }\n"
                "    vector<int> nums(n);\n"
                "    for (int i = 0; i < n; i++) cin >> nums[i];\n"
                "    // TODO: Return length in O(N) time\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def solve():\n"
            "    raw = sys.stdin.read().split()\n"
            "    if not raw:\n"
            "        print(0)\n"
            "        return\n"
            "    n = int(raw[0])\n"
            "    if n == 0:\n"
            "        print(0)\n"
            "        return\n"
            "    nums = set(int(x) for x in raw[1:1+n])\n"
            "    longest = 0\n"
            "    for x in nums:\n"
            "        # Only start sequence if x is the beginning of a streak\n"
            "        if (x - 1) not in nums:\n"
            "            curr = x\n"
            "            streak = 1\n"
            "            while (curr + 1) in nums:\n"
            "                curr += 1\n"
            "                streak += 1\n"
            "            if streak > longest:\n"
            "                longest = streak\n"
            "    print(longest)\n\n"
            "solve()\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "6\n100 4 200 1 3 2", "dbInit": None},
            {"stdin": "10\n0 3 7 2 5 8 4 6 0 1", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "0\n", "dbInit": None},
            {"stdin": "1\n42", "dbInit": None},
            {"stdin": "5\n1 2 0 1 2", "dbInit": None},
            # Adversarial Stress Testcase: N = 10,000 consecutive numbers [1, 2, ..., 10000].
            # An algorithm that does while (x in set) for every number without checking (x - 1 not in set)
            # executes 10000 + 9999 + ... + 1 = 50,000,000 checks -> TLE > 2000ms.
            # The optimal solution checks (x - 1) 10,000 times, only initiates streak at 1, taking ~3ms.
            {"stdin": f"10000\n{' '.join(str(i) for i in range(1, 10001))}", "dbInit": None}
        ]
    },

    # --------------------------------------------------------------------------
    # 10. Sequence Game (Codeforces 1862B)
    # --------------------------------------------------------------------------
    {
        "title": "Sequence Game",
        "slug": "sequence-game",
        "difficulty": "EASY",
        "problemType": "GENERIC",
        "topics": ["Array", "Greedy", "Constructive"],
        "hints": [
            "Notice the rule: a value b_i from the original sequence a is recorded if and only if it is the first element or b_i >= b_{i-1}.",
            "If b_i < b_{i-1}, you can insert an extra intermediate element (such as b_i) right before b_i so that the sequence remains valid and length does not exceed 2*n."
        ],
        "statement": (
            "Tema and Vika are playing the following game.\n\n"
            "First, Vika comes up with a sequence of positive integers $a$ of length $m$ and writes it down on a piece of paper. "
            "Then she takes a new piece of paper and writes down the sequence $b$ according to the following rule:\n\n"
            "- First, she writes down $a_1$.\n"
            "- Then, she writes down only those $a_i$ ($2 \\le i \\le m$) such that $a_{i-1} \\le a_i$. Let the length of this sequence be denoted as $n$.\n\n"
            "For example, from the sequence $a = [4, 3, 2, 6, 3, 3]$, Vika will obtain the sequence $b = [4, 6, 3]$.\n\n"
            "She then gives the piece of paper with the sequence $b$ to Tema. He, in turn, tries to guess the sequence $a$. "
            "Tema considers winning in such a game highly unlikely, but still wants to find at least one sequence $a$ that could have been originally chosen by Vika. "
            "Help him and output any such sequence.\n\n"
            "Note that the length of the sequence you output should not exceed the input sequence length by more than two times ($n \\le m \\le 2n$).\n\n"
            "### Example 1:\n"
            "```\n"
            "Input:\n"
            "6\n"
            "3\n"
            "4 6 3\n"
            "2\n"
            "1 2\n"
            "5\n"
            "1 7 9 5 7\n"
            "1\n"
            "144\n"
            "2\n"
            "1 1\n"
            "3\n"
            "4 3 2\n\n"
            "Output:\n"
            "4\n"
            "4 6 3 3\n"
            "2\n"
            "1 2\n"
            "6\n"
            "1 7 9 5 5 7\n"
            "1\n"
            "144\n"
            "2\n"
            "1 1\n"
            "5\n"
            "4 3 3 2 2\n"
            "```\n\n"
            "### Constraints:\n"
            "- $1 \\le t \\le 10^4$ (number of test cases)\n"
            "- $1 \\le n \\le 2 \\cdot 10^5$ (length of sequence $b$)\n"
            "- $1 \\le b_i \\le 10^9$ (elements of sequence $b$)\n"
            "- The sum of $n$ over all test cases does not exceed $2 \\cdot 10^5$.\n\n"
            "### Input / Output Format:\n"
            "- **Input**:\n"
            "  - Line 1: An integer $t$, the number of test cases.\n"
            "  - For each testcase:\n"
            "    - Line 1: An integer $n$, the length of sequence $b$.\n"
            "    - Line 2: $n$ space-separated integers $b_1, b_2, \\dots, b_n$.\n"
            "- **Output**:\n"
            "  - For each test case, output two lines:\n"
            "    - Line 1: An integer $m$, the length of the assumed sequence ($n \\le m \\le 2n$).\n"
            "    - Line 2: $m$ space-separated integers $a_1, a_2, \\dots, a_m$ ($1 \\le a_i \\le 10^9$).\n"
            "  - If there are multiple suitable sequences, you may output any of them."
        ),
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "starterCode": {
            "python-3.12": (
                "import sys\n\n"
                "def solve():\n"
                "    raw = sys.stdin.read().split()\n"
                "    if not raw:\n"
                "        return\n"
                "    t = int(raw[0])\n"
                "    idx = 1\n"
                "    for _ in range(t):\n"
                "        n = int(raw[idx])\n"
                "        idx += 1\n"
                "        b = [int(x) for x in raw[idx:idx + n]]\n"
                "        idx += n\n"
                "        # TODO: Construct sequence a of length m (n <= m <= 2*n)\n"
                "        # Print m on first line, then m integers on second line\n"
                "        pass\n\n"
                "solve()\n"
            ),
            "java-21": (
                "import java.util.*;\n"
                "import java.io.*;\n\n"
                "public class Solution {\n"
                "    public static void main(String[] args) throws IOException {\n"
                "        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n"
                "        String line = br.readLine();\n"
                "        if (line == null) return;\n"
                "        StringTokenizer st = new StringTokenizer(line);\n"
                "        while (!st.hasMoreTokens()) {\n"
                "            line = br.readLine();\n"
                "            if (line == null) return;\n"
                "            st = new StringTokenizer(line);\n"
                "        }\n"
                "        int t = Integer.parseInt(st.nextToken());\n"
                "        // TODO: Read each testcase (n, then b) and output m and sequence a\n"
                "    }\n"
                "}\n"
            ),
            "javascript-node-20": (
                "const fs = require('fs');\n\n"
                "function main() {\n"
                "    const raw = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);\n"
                "    if (!raw[0]) return;\n"
                "    const t = parseInt(raw[0], 10);\n"
                "    let idx = 1;\n"
                "    // TODO: Process t testcases and output m and sequence a\n"
                "}\n\n"
                "main();\n"
            ),
            "cpp-23": (
                "#include <iostream>\n"
                "#include <vector>\n\n"
                "using namespace std;\n\n"
                "int main() {\n"
                "    ios_base::sync_with_stdio(false);\n"
                "    cin.tie(NULL);\n"
                "    int t;\n"
                "    if (!(cin >> t)) return 0;\n"
                "    while (t--) {\n"
                "        int n;\n"
                "        cin >> n;\n"
                "        vector<int> b(n);\n"
                "        for (int i = 0; i < n; i++) cin >> b[i];\n"
                "        // TODO: Construct and print sequence a\n"
                "    }\n"
                "    return 0;\n"
                "}\n"
            )
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": (
            "import sys\n\n"
            "def solve():\n"
            "    raw = sys.stdin.read().split()\n"
            "    if not raw:\n"
            "        return\n"
            "    t = int(raw[0])\n"
            "    idx = 1\n"
            "    out = []\n"
            "    for _ in range(t):\n"
            "        n = int(raw[idx])\n"
            "        idx += 1\n"
            "        b = [int(x) for x in raw[idx:idx + n]]\n"
            "        idx += n\n"
            "        a = [b[0]]\n"
            "        for x in b[1:]:\n"
            "            if x < a[-1]:\n"
            "                a.append(x)\n"
            "            a.append(x)\n"
            "        out.append(str(len(a)))\n"
            "        out.append(' '.join(str(x) for x in a))\n"
            "    print('\\n'.join(out))\n\n"
            "solve()\n"
        ),
        "requireVerificationScript": True,
        "verificationScriptLanguage": "python-3.12",
        "verificationScript": (
            "import sys\n\n"
            "# argv[1]: input file\n"
            "# argv[2]: candidate output file\n"
            "# argv[3]: reference solution output file\n\n"
            "with open(sys.argv[1], 'r') as f:\n"
            "    in_tokens = [int(x) for x in f.read().split()]\n\n"
            "with open(sys.argv[2], 'r') as f:\n"
            "    out_tokens = [int(x) for x in f.read().split()]\n\n"
            "if not in_tokens:\n"
            "    sys.exit(0)\n\n"
            "t = in_tokens[0]\n"
            "in_ptr = 1\n"
            "out_ptr = 0\n\n"
            "for case in range(1, t + 1):\n"
            "    if in_ptr >= len(in_tokens):\n"
            "        print(f'Testcase {case}: Incomplete input data', file=sys.stderr)\n"
            "        sys.exit(1)\n\n"
            "    n = in_tokens[in_ptr]\n"
            "    in_ptr += 1\n"
            "    b = in_tokens[in_ptr:in_ptr + n]\n"
            "    in_ptr += n\n\n"
            "    if out_ptr >= len(out_tokens):\n"
            "        print(f'Testcase {case}: Output ended prematurely', file=sys.stderr)\n"
            "        sys.exit(1)\n\n"
            "    m = out_tokens[out_ptr]\n"
            "    out_ptr += 1\n\n"
            "    if not (n <= m <= 2 * n):\n"
            "        print(f'Testcase {case}: Sequence length m={m} not in [{n}, {2*n}]', file=sys.stderr)\n"
            "        sys.exit(1)\n\n"
            "    if out_ptr + m > len(out_tokens):\n"
            "        print(f'Testcase {case}: Expected {m} elements, but output ended early', file=sys.stderr)\n"
            "        sys.exit(1)\n\n"
            "    a = out_tokens[out_ptr:out_ptr + m]\n"
            "    out_ptr += m\n\n"
            "    for val in a:\n"
            "        if val < 1 or val > 10**9:\n"
            "            print(f'Testcase {case}: Value {val} out of bounds [1, 10^9]', file=sys.stderr)\n"
            "            sys.exit(1)\n\n"
            "    # Simulate Vika's rule\n"
            "    cand_b = [a[0]]\n"
            "    for i in range(1, m):\n"
            "        if a[i] >= a[i - 1]:\n"
            "            cand_b.append(a[i])\n\n"
            "    if cand_b != b:\n"
            "        print(f\'Testcase {case}: Vika rule produced {cand_b[:5]}, expected {b[:5]}\', file=sys.stderr)\n"
            "        sys.exit(1)\n\n"
            "if out_ptr != len(out_tokens):\n"
            "    print('Extraneous output tokens found after all test cases', file=sys.stderr)\n"
            "    sys.exit(1)\n\n"
            "sys.exit(0)\n"
        ),
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        },
        "sampleTestcases": [
            {"stdin": "6\n3\n4 6 3\n2\n1 2\n5\n1 7 9 5 7\n1\n144\n2\n1 1\n3\n4 3 2", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "3\n1\n1000000000\n3\n1 1 1\n4\n10 9 8 7", "dbInit": None},
            {"stdin": "1\n5\n5 4 3 2 1", "dbInit": None}
        ]
    },
]

