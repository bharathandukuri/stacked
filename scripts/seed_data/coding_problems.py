"""Coding Problems Definitions for Stacked."""

PROBLEMS = [
    {
        "title": "Custom FizzBuzz Simulator",
        "slug": "fizz-buzz-custom",
        "problemType": "GENERIC",
        "difficulty": "EASY",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given an integer $N$, print numbers from $1$ to $N$ on separate lines with the following substitutions:
- For multiples of $3$, print `Fizz` instead of the number.
- For multiples of $5$, print `Buzz` instead of the number.
- For numbers which are multiples of both $3$ and $5$, print `FizzBuzz`.
- For all other numbers, print the number itself.

#### Input Format
A single integer $N$ ($1 \\le N \\le 1000$).

#### Output Format
$N$ lines containing the string or integer representation for each number from $1$ to $N$.

#### Example
**Input:**
```text
5
```
**Output:**
```text
1
2
Fizz
4
Buzz
```
""",
        "topics": ["Programming Fundamentals"],
        "hints": [
            "Check for divisibility by 15 (both 3 and 5) first before checking 3 or 5 individually."
        ],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    raw = sys.stdin.read().split()
    if not raw:
        return
    n = int(raw[0])
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.Scanner;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const input = fs.readFileSync(0, 'utf-8').trim();
    if (!input) return;
    const n = parseInt(input, 10);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
using namespace std;

int main() {
    int n;
    if (!(cin >> n)) return 0;
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    raw = sys.stdin.read().split()
    if not raw:
        return
    n = int(raw[0])
    for i in range(1, n + 1):
        if i % 15 == 0:
            print("FizzBuzz")
        elif i % 3 == 0:
            print("Fizz")
        elif i % 5 == 0:
            print("Buzz")
        else:
            print(i)

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "5", "dbInit": None},
            {"stdin": "15", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "1", "dbInit": None},
            {"stdin": "3", "dbInit": None},
            {"stdin": "30", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Two Sum",
        "slug": "two-sum",
        "problemType": "GENERIC",
        "difficulty": "EASY",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given an array of integers `nums` and an integer `target`, return the 0-based indices of the two numbers such that they add up to `target`.

Assume that each input has exactly one valid solution, and you may not use the same element twice.

#### Input Format
- First line: integer $N$ (number of elements) and integer $target$.
- Next line: $N$ space-separated integers.

#### Output Format
Print the two indices separated by a space in ascending order: `i j`.

#### Example
**Input:**
```text
4 9
2 7 11 15
```
**Output:**
```text
0 1
```
""",
        "topics": ["Data Structures & Algorithms", "Programming Fundamentals"],
        "hints": [
            "A hash table allows you to check for the complement target - num in O(1) time."
        ],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    target = int(data[1])
    nums = [int(x) for x in data[2:2+n]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int target = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length < 2) return;
    const n = parseInt(tokens[0], 10);
    const target = parseInt(tokens[1], 10);
    const nums = tokens.slice(2, 2 + n).map(Number);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
using namespace std;

int main() {
    int n, target;
    if (!(cin >> n >> target)) return 0;
    vector<int> nums(n);
    for (int i = 0; i < n; i++) cin >> nums[i];
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    target = int(data[1])
    nums = [int(x) for x in data[2:2+n]]
    seen = {}
    for i, num in enumerate(nums):
        diff = target - num
        if diff in seen:
            print(f"{seen[diff]} {i}")
            return
        seen[num] = i

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "4 9\n2 7 11 15", "dbInit": None},
            {"stdin": "3 6\n3 2 4", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "2 6\n3 3", "dbInit": None},
            {"stdin": "4 0\n-3 4 3 90", "dbInit": None},
            {"stdin": "5 100\n10 20 30 40 60", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Valid Parentheses",
        "slug": "valid-parentheses",
        "problemType": "GENERIC",
        "difficulty": "EASY",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given a string `s` containing just the characters `'('`, `')'`, `'{'`, `'}'`, `'['` and `']'`, determine if the input string is valid.

An input string is valid if:
1. Open brackets must be closed by the same type of brackets.
2. Open brackets must be closed in the correct order.
3. Every close bracket has a corresponding open bracket of the same type.

#### Input Format
A single string `s` ($1 \\le |s| \\le 10^4$).

#### Output Format
Print `true` if valid, otherwise print `false`.

#### Example
**Input:**
```text
()[]{}
```
**Output:**
```text
true
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": [
            "Use a LIFO stack to push opening brackets and pop/match when encountering closing brackets."
        ],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    s = sys.stdin.read().strip()
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNext() ? sc.next() : "";
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const s = fs.readFileSync(0, 'utf-8').trim();
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <string>
using namespace std;

int main() {
    string s;
    if (!(cin >> s)) return 0;
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    s = sys.stdin.read().strip()
    stack = []
    mapping = {')': '(', '}': '{', ']': '['}
    for char in s:
        if char in mapping:
            top = stack.pop() if stack else '#'
            if mapping[char] != top:
                print("false")
                return
        else:
            stack.append(char)
    print("true" if not stack else "false")

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "()[]{}", "dbInit": None},
            {"stdin": "(]", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "([)]", "dbInit": None},
            {"stdin": "{[]}", "dbInit": None},
            {"stdin": "((((((((", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Valid Palindrome",
        "slug": "valid-palindrome",
        "problemType": "GENERIC",
        "difficulty": "EASY",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
A phrase is a **palindrome** if, after converting all uppercase letters into lowercase letters and removing all non-alphanumeric characters, it reads the same forward and backward.

Given a string `s`, return `true` if it is a palindrome, or `false` otherwise.

#### Input Format
A single line of text representing string `s`.

#### Output Format
Print `true` if the filtered string is a palindrome, otherwise print `false`.

#### Example
**Input:**
```text
A man, a plan, a canal: Panama
```
**Output:**
```text
true
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Use two pointers moving inward while skipping non-alphanumeric characters."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    s = sys.stdin.read().strip()
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const s = fs.readFileSync(0, 'utf-8').trim();
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <string>
using namespace std;

int main() {
    string s;
    getline(cin, s);
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    raw = sys.stdin.read().strip()
    filtered = [ch.lower() for ch in raw if ch.isalnum()]
    is_pal = (filtered == filtered[::-1])
    print("true" if is_pal else "false")

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "A man, a plan, a canal: Panama", "dbInit": None},
            {"stdin": "race a car", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "   ", "dbInit": None},
            {"stdin": "0P", "dbInit": None},
            {"stdin": "ab_a", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Classic Binary Search",
        "slug": "classic-binary-search",
        "problemType": "GENERIC",
        "difficulty": "EASY",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given an array of integers `nums` which is sorted in ascending order, and an integer `target`, write a function to search `target` in `nums`.

If `target` exists, return its 0-based index. Otherwise, return `-1`.

You must write an algorithm with $\\mathcal{O}(\\log N)$ runtime complexity.

#### Input Format
- First line: integer $N$ (array size) and integer $target$.
- Next line: $N$ space-separated integers in sorted order.

#### Output Format
Print the index of `target`, or `-1` if not found.

#### Example
**Input:**
```text
6 9
-1 0 3 5 9 12
```
**Output:**
```text
4
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Calculate mid using low + (high - low) // 2 to avoid integer overflow."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    target = int(data[1])
    nums = [int(x) for x in data[2:2+n]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int target = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length < 2) return;
    const n = parseInt(tokens[0], 10);
    const target = parseInt(tokens[1], 10);
    const nums = tokens.slice(2, 2 + n).map(Number);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
using namespace std;

int main() {
    int n, target;
    if (!(cin >> n >> target)) return 0;
    vector<int> nums(n);
    for (int i = 0; i < n; i++) cin >> nums[i];
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    target = int(data[1])
    nums = [int(x) for x in data[2:2+n]]
    low, high = 0, n - 1
    while low <= high:
        mid = (low + high) // 2
        if nums[mid] == target:
            print(mid)
            return
        elif nums[mid] < target:
            low = mid + 1
        else:
            high = mid - 1
    print(-1)

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "6 9\n-1 0 3 5 9 12", "dbInit": None},
            {"stdin": "6 2\n-1 0 3 5 9 12", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "1 5\n5", "dbInit": None},
            {"stdin": "1 0\n5", "dbInit": None},
            {"stdin": "5 1\n1 2 3 4 5", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Longest Substring Without Repeating Characters",
        "slug": "longest-substring-without-repeating-characters",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given a string `s`, find the length of the **longest substring** without repeating characters.

#### Input Format
A single line string `s` ($0 \\le |s| \\le 5 \\times 10^4$).

#### Output Format
Print a single integer: the length of the longest non-repeating substring.

#### Example
**Input:**
```text
abcabcbb
```
**Output:**
```text
3
```
*Explanation: The answer is `abc`, with the length of 3.*
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Use a sliding window with a hash map storing the most recent index of each character."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    s = sys.stdin.read().rstrip('\\n')
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const s = fs.readFileSync(0, 'utf-8').replace(/\\r?\\n$/, '');
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <string>
using namespace std;

int main() {
    string s;
    getline(cin, s);
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    s = sys.stdin.read().rstrip('\\n')
    char_index = {}
    max_len = 0
    start = 0
    for i, ch in enumerate(s):
        if ch in char_index and char_index[ch] >= start:
            start = char_index[ch] + 1
        char_index[ch] = i
        max_len = max(max_len, i - start + 1)
    print(max_len)

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "abcabcbb", "dbInit": None},
            {"stdin": "bbbbb", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "pwwkew", "dbInit": None},
            {"stdin": "", "dbInit": None},
            {"stdin": "dvdf", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Merge Intervals",
        "slug": "merge-intervals",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given an array of intervals where `intervals[i] = [start_i, end_i]`, merge all overlapping intervals, and return an array of the non-overlapping intervals that cover all the intervals in the input.

#### Input Format
- First line: integer $N$ ($1 \\le N \\le 10^4$)
- Next $N$ lines: two integers `start` and `end` per line.

#### Output Format
Print merged intervals one per line as `start end` sorted by start time.

#### Example
**Input:**
```text
4
1 3
2 6
8 10
15 18
```
**Output:**
```text
1 6
8 10
15 18
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Sort intervals by their starting value first."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    lines = sys.stdin.read().split()
    if not lines:
        return
    n = int(lines[0])
    intervals = []
    idx = 1
    for _ in range(n):
        intervals.append([int(lines[idx]), int(lines[idx+1])])
        idx += 2
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int[][] intervals = new int[n][2];
        for (int i = 0; i < n; i++) {
            intervals[i][0] = sc.nextInt();
            intervals[i][1] = sc.nextInt();
        }
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length === 0) return;
    const n = parseInt(tokens[0], 10);
    const intervals = [];
    let idx = 1;
    for (let i = 0; i < n; i++) {
        intervals.push([parseInt(tokens[idx], 10), parseInt(tokens[idx+1], 10)]);
        idx += 2;
    }
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

int main() {
    int n;
    if (!(cin >> n)) return 0;
    vector<pair<int, int>> intervals(n);
    for (int i = 0; i < n; i++) {
        cin >> intervals[i].first >> intervals[i].second;
    }
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    intervals = []
    idx = 1
    for _ in range(n):
        intervals.append([int(data[idx]), int(data[idx+1])])
        idx += 2
    intervals.sort(key=lambda x: x[0])
    merged = []
    for interval in intervals:
        if not merged or merged[-1][1] < interval[0]:
            merged.append(interval)
        else:
            merged[-1][1] = max(merged[-1][1], interval[1])
    for s, e in merged:
        print(f"{s} {e}")

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "4\n1 3\n2 6\n8 10\n15 18", "dbInit": None},
            {"stdin": "2\n1 4\n4 5", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "1\n1 10", "dbInit": None},
            {"stdin": "3\n1 4\n0 4\n3 5", "dbInit": None},
            {"stdin": "3\n2 3\n4 5\n6 7", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Group Anagrams",
        "slug": "group-anagrams",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20"],
        "statement": """### Problem Statement
Given an array of strings, group the anagrams together.

To produce deterministic output:
1. Sort words within each anagram group lexicographically.
2. Sort the groups by group size descending; if sizes are equal, sort groups lexicographically by their first word.
3. Print each group on a separate line with words separated by a space.

#### Input Format
- First line: integer $N$ ($1 \\le N \\le 10^4$).
- Next line: $N$ space-separated words.

#### Output Format
Each grouped anagram set on its own line.

#### Example
**Input:**
```text
6
eat tea tan ate nat bat
```
**Output:**
```text
ate eat tea
nat tan
bat
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Use the sorted characters of each word as a hash map key."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    tokens = sys.stdin.read().split()
    if not tokens:
        return
    n = int(tokens[0])
    words = tokens[1:1+n]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        String[] words = new String[n];
        for (int i = 0; i < n; i++) words[i] = sc.next();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length === 0) return;
    const n = parseInt(tokens[0], 10);
    const words = tokens.slice(1, 1 + n);
    // Write solution here
}

solve();
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys
from collections import defaultdict

def solve():
    tokens = sys.stdin.read().split()
    if not tokens:
        return
    n = int(tokens[0])
    words = tokens[1:1+n]
    groups = defaultdict(list)
    for word in words:
        key = "".join(sorted(word))
        groups[key].append(word)
    sorted_groups = []
    for g in groups.values():
        sorted_groups.append(sorted(g))
    sorted_groups.sort(key=lambda g: (-len(g), g[0]))
    for g in sorted_groups:
        print(" ".join(g))

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "6\neat tea tan ate nat bat", "dbInit": None},
            {"stdin": "1\na", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "3\nabc bca cab", "dbInit": None},
            {"stdin": "4\nstop pots tops step", "dbInit": None},
            {"stdin": "2\nab ba", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "Maximum Subarray (Kadane's Algorithm)",
        "slug": "maximum-subarray-kadane",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given an integer array `nums`, find the subarray with the largest sum, and return its sum.

A subarray is a contiguous non-empty sequence of elements within an array.

#### Input Format
- First line: integer $N$ ($1 \\le N \\le 10^5$)
- Next line: $N$ space-separated integers.

#### Output Format
Print a single integer: the maximum subarray sum.

#### Example
**Input:**
```text
9
-2 1 -3 4 -1 2 1 -5 4
```
**Output:**
```text
6
```
*Explanation: The subarray `[4, -1, 2, 1]` has the largest sum 6.*
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Keep track of the maximum sum ending at the current position: current_max = max(num, current_max + num)."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    nums = [int(x) for x in data[1:1+n]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length === 0) return;
    const n = parseInt(tokens[0], 10);
    const nums = tokens.slice(1, 1 + n).map(Number);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
using namespace std;

int main() {
    int n;
    if (!(cin >> n)) return 0;
    vector<int> nums(n);
    for (int i = 0; i < n; i++) cin >> nums[i];
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    nums = [int(x) for x in data[1:1+n]]
    max_so_far = nums[0]
    curr_max = nums[0]
    for x in nums[1:]:
        curr_max = max(x, curr_max + x)
        max_so_far = max(max_so_far, curr_max)
    print(max_so_far)

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "9\n-2 1 -3 4 -1 2 1 -5 4", "dbInit": None},
            {"stdin": "1\n1", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "5\n5 4 -1 7 8", "dbInit": None},
            {"stdin": "4\n-5 -2 -8 -1", "dbInit": None},
            {"stdin": "3\n-1 -2 -3", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Coin Change (Minimum Coins)",
        "slug": "coin-change-minimum-coins",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
You are given an integer array `coins` representing coins of different denominations and an integer `amount` representing a total amount of money.

Return the fewest number of coins that you need to make up that amount. If that amount of money cannot be made up by any combination of the coins, return `-1`.

You may assume that you have an infinite number of each kind of coin.

#### Input Format
- First line: integer $N$ (number of coin types) and integer `amount` ($0 \\le \\text{amount} \\le 10^4$).
- Next line: $N$ space-separated coin denominations.

#### Output Format
Print the minimum number of coins, or `-1`.

#### Example
**Input:**
```text
3 11
1 2 5
```
**Output:**
```text
3
```
*Explanation: 11 = 5 + 5 + 1.*
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Bottom-up DP: dp[i] represents minimum coins to make amount i."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    amount = int(data[1])
    coins = [int(x) for x in data[2:2+n]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int amount = sc.nextInt();
        int[] coins = new int[n];
        for (int i = 0; i < n; i++) coins[i] = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length < 2) return;
    const n = parseInt(tokens[0], 10);
    const amount = parseInt(tokens[1], 10);
    const coins = tokens.slice(2, 2 + n).map(Number);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
using namespace std;

int main() {
    int n, amount;
    if (!(cin >> n >> amount)) return 0;
    vector<int> coins(n);
    for (int i = 0; i < n; i++) cin >> coins[i];
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    amount = int(data[1])
    coins = [int(x) for x in data[2:2+n]]
    dp = [float('inf')] * (amount + 1)
    dp[0] = 0
    for coin in coins:
        for i in range(coin, amount + 1):
            dp[i] = min(dp[i], dp[i - coin] + 1)
    ans = dp[amount] if dp[amount] != float('inf') else -1
    print(ans)

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "3 11\n1 2 5", "dbInit": None},
            {"stdin": "1 3\n2", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "1 0\n1", "dbInit": None},
            {"stdin": "4 6249\n186 419 83 408", "dbInit": None},
            {"stdin": "3 7\n2 4 6", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Min Stack Operations Simulator",
        "slug": "min-stack-design",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Design a stack that supports `push`, `pop`, `top`, and retrieving the minimum element in $\\mathcal{O}(1)$ time.

Simulate the following operations:
- `push X`: Pushes integer $X$ onto the stack.
- `pop`: Removes the element on the top of the stack.
- `top`: Prints the top element.
- `getMin`: Prints the minimum element in the stack.

#### Input Format
- First line: integer $Q$ (number of operations).
- Next $Q$ lines: each line contains an operation command.

#### Output Format
For each `top` and `getMin` operation, print the resulting integer on its own line.

#### Example
**Input:**
```text
6
push -2
push 0
push -3
getMin
pop
getMin
```
**Output:**
```text
-3
-2
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Maintain a companion stack of minimum values or store (value, current_min) pairs in the stack."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    lines = sys.stdin.read().splitlines()
    if not lines:
        return
    q = int(lines[0])
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int q = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const lines = fs.readFileSync(0, 'utf-8').trim().split('\\n');
    if (!lines || lines.length === 0) return;
    const q = parseInt(lines[0], 10);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <string>
#include <stack>
using namespace std;

int main() {
    int q;
    if (!(cin >> q)) return 0;
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    lines = sys.stdin.read().splitlines()
    if not lines:
        return
    q = int(lines[0])
    stack = []
    min_stack = []
    for line in lines[1:1+q]:
        parts = line.strip().split()
        if not parts:
            continue
        cmd = parts[0]
        if cmd == "push":
            val = int(parts[1])
            stack.append(val)
            if not min_stack or val <= min_stack[-1]:
                min_stack.append(val)
            else:
                min_stack.append(min_stack[-1])
        elif cmd == "pop":
            if stack:
                stack.pop()
                min_stack.pop()
        elif cmd == "top":
            if stack:
                print(stack[-1])
        elif cmd == "getMin":
            if min_stack:
                print(min_stack[-1])

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "6\npush -2\npush 0\npush -3\ngetMin\npop\ngetMin", "dbInit": None},
            {"stdin": "4\npush 10\ntop\ngetMin\npop", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "5\npush 5\npush 5\ngetMin\npop\ngetMin", "dbInit": None},
            {"stdin": "6\npush 2\npush 1\ntop\ngetMin\npop\ntop", "dbInit": None},
            {"stdin": "4\npush -100\npush 100\ngetMin\ntop", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "LRU Cache Operations Simulator",
        "slug": "lru-cache-simulation",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20"],
        "statement": """### Problem Statement
Design a data structure that follows the constraints of a **Least Recently Used (LRU) Cache**.

Implement operations:
- `put K V`: Set key $K$ to value $V$. If capacity is exceeded, evict the least recently used key.
- `get K`: Output the value of key $K$ if it exists, otherwise print `-1`. Accessing a key marks it as recently used.

#### Input Format
- First line: integer $C$ (capacity) and integer $Q$ (number of operations).
- Next $Q$ lines: operation commands `put K V` or `get K`.

#### Output Format
Print the return value for each `get` operation on its own line.

#### Example
**Input:**
```text
2 6
put 1 1
put 2 2
get 1
put 3 3
get 2
get 3
```
**Output:**
```text
1
-1
3
```
""",
        "topics": ["Data Structures & Algorithms", "Core Java"],
        "hints": ["Combine a doubly linked list with a hash map to achieve O(1) get and put."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    lines = sys.stdin.read().splitlines()
    if not lines:
        return
    c, q = map(int, lines[0].split())
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int c = sc.nextInt();
        int q = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const lines = fs.readFileSync(0, 'utf-8').trim().split('\\n');
    if (!lines || lines.length === 0) return;
    const [c, q] = lines[0].split(/\\s+/).map(Number);
    // Write solution here
}

solve();
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys
from collections import OrderedDict

def solve():
    lines = sys.stdin.read().splitlines()
    if not lines:
        return
    c, q = map(int, lines[0].split())
    cache = OrderedDict()
    for line in lines[1:1+q]:
        parts = line.strip().split()
        if not parts:
            continue
        cmd = parts[0]
        if cmd == "put":
            k, v = int(parts[1]), int(parts[2])
            if k in cache:
                cache.move_to_end(k)
            cache[k] = v
            if len(cache) > c:
                cache.popitem(last=False)
        elif cmd == "get":
            k = int(parts[1])
            if k in cache:
                cache.move_to_end(k)
                print(cache[k])
            else:
                print(-1)

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "2 6\nput 1 1\nput 2 2\nget 1\nput 3 3\nget 2\nget 3", "dbInit": None},
            {"stdin": "1 4\nput 1 10\nget 1\nput 2 20\nget 1", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "2 4\nput 1 1\nput 1 10\nget 1\nget 2", "dbInit": None},
            {"stdin": "3 6\nput 1 1\nput 2 2\nput 3 3\nput 4 4\nget 1\nget 4", "dbInit": None},
            {"stdin": "2 3\nget 5\nput 5 50\nget 5", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "Token Bucket Rate Limiter",
        "slug": "rate-limiter-token-bucket",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20"],
        "statement": """### Problem Statement
Simulate a **Token Bucket Rate Limiter**:
- The bucket has maximum capacity $C$ tokens.
- Tokens refill at a rate of $R$ tokens per millisecond.
- The bucket starts completely full ($C$ tokens at time $0$).
- When a request arrives at timestamp $T$ (milliseconds), tokens are first refilled based on elapsed time:
  $$\\text{tokens} = \\min(C, \\text{current\\_tokens} + (T - \\text{last\\_timestamp}) \\times R)$$
- If $\\text{tokens} \\ge 1.0$, the request consumes $1.0$ token and is `ALLOWED`.
- Otherwise, no tokens are consumed and the request is `REJECTED`.

#### Input Format
- First line: integer $C$ (capacity), float $R$ (tokens per millisecond), integer $Q$ (requests count).
- Next line: $Q$ space-separated integers representing request timestamps in strictly non-decreasing order.

#### Output Format
Print $Q$ lines, each containing either `ALLOWED` or `REJECTED`.

#### Example
**Input:**
```text
3 0.001 5
0 100 200 300 400
```
**Output:**
```text
ALLOWED
ALLOWED
ALLOWED
REJECTED
REJECTED
```
""",
        "topics": ["Backend Engineering & Spring", "Programming Fundamentals"],
        "hints": ["Calculate fractional tokens refilled accurately using floating-point math."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    c = float(data[0])
    r = float(data[1])
    q = int(data[2])
    timestamps = [int(x) for x in data[3:3+q]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNext()) return;
        double c = sc.nextDouble();
        double r = sc.nextDouble();
        int q = sc.nextInt();
        long[] timestamps = new long[q];
        for (int i = 0; i < q; i++) timestamps[i] = sc.nextLong();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length < 3) return;
    const c = parseFloat(tokens[0]);
    const r = parseFloat(tokens[1]);
    const q = parseInt(tokens[2], 10);
    const timestamps = tokens.slice(3, 3 + q).map(Number);
    // Write solution here
}

solve();
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    c = float(data[0])
    r = float(data[1])
    q = int(data[2])
    timestamps = [int(x) for x in data[3:3+q]]
    tokens = c
    last_time = 0.0
    for t in timestamps:
        elapsed = t - last_time
        tokens = min(c, tokens + elapsed * r)
        last_time = t
        if tokens >= 1.0:
            tokens -= 1.0
            print("ALLOWED")
        else:
            print("REJECTED")

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "3 0.001 5\n0 100 200 300 400", "dbInit": None},
            {"stdin": "1 0.002 3\n0 400 600", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "2 0.005 4\n0 10 200 210", "dbInit": None},
            {"stdin": "1 0.0 2\n0 1000", "dbInit": None},
            {"stdin": "5 0.01 6\n0 10 20 30 40 50", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "First and Last Position in Sorted Array",
        "slug": "find-first-and-last-position",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given an array of integers `nums` sorted in non-decreasing order, find the starting and ending position of a given `target` value.

If `target` is not found in the array, return `-1 -1`.

You must write an algorithm with $\\mathcal{O}(\\log N)$ runtime complexity.

#### Input Format
- First line: integer $N$ ($0 \\le N \\le 10^5$) and integer $target$.
- Next line: $N$ space-separated integers in sorted order.

#### Output Format
Print two space-separated integers: `start_index end_index`.

#### Example
**Input:**
```text
6 8
5 7 7 8 8 10
```
**Output:**
```text
3 4
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Use binary search twice: once to find the leftmost boundary, and once for the rightmost boundary."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    target = int(data[1])
    nums = [int(x) for x in data[2:2+n]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int target = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length < 2) return;
    const n = parseInt(tokens[0], 10);
    const target = parseInt(tokens[1], 10);
    const nums = tokens.slice(2, 2 + n).map(Number);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
using namespace std;

int main() {
    int n, target;
    if (!(cin >> n >> target)) return 0;
    vector<int> nums(n);
    for (int i = 0; i < n; i++) cin >> nums[i];
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def find_bound(nums, target, is_first):
    low, high = 0, len(nums) - 1
    bound = -1
    while low <= high:
        mid = (low + high) // 2
        if nums[mid] == target:
            bound = mid
            if is_first:
                high = mid - 1
            else:
                low = mid + 1
        elif nums[mid] < target:
            low = mid + 1
        else:
            high = mid - 1
    return bound

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    target = int(data[1])
    nums = [int(x) for x in data[2:2+n]]
    start = find_bound(nums, target, True)
    end = find_bound(nums, target, False)
    print(f"{start} {end}")

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "6 8\n5 7 7 8 8 10", "dbInit": None},
            {"stdin": "6 6\n5 7 7 8 8 10", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "0 0\n", "dbInit": None},
            {"stdin": "1 1\n1", "dbInit": None},
            {"stdin": "4 2\n2 2 2 2", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Trapping Rain Water",
        "slug": "trapping-rain-water",
        "problemType": "GENERIC",
        "difficulty": "HARD",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given $N$ non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.

#### Input Format
- First line: integer $N$ ($1 \\le N \\le 2 \\times 10^4$).
- Next line: $N$ space-separated non-negative integers representing heights.

#### Output Format
Print a single integer: total units of trapped rain water.

#### Example
**Input:**
```text
12
0 1 0 2 1 0 1 3 2 1 2 1
```
**Output:**
```text
6
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Use two pointers moving inward, keeping track of left_max and right_max."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    heights = [int(x) for x in data[1:1+n]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int[] heights = new int[n];
        for (int i = 0; i < n; i++) heights[i] = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length === 0) return;
    const n = parseInt(tokens[0], 10);
    const heights = tokens.slice(1, 1 + n).map(Number);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
using namespace std;

int main() {
    int n;
    if (!(cin >> n)) return 0;
    vector<int> heights(n);
    for (int i = 0; i < n; i++) cin >> heights[i];
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    height = [int(x) for x in data[1:1+n]]
    if not height:
        print(0)
        return
    left, right = 0, len(height) - 1
    left_max, right_max = 0, 0
    trapped = 0
    while left < right:
        if height[left] < height[right]:
            if height[left] >= left_max:
                left_max = height[left]
            else:
                trapped += left_max - height[left]
            left += 1
        else:
            if height[right] >= right_max:
                right_max = height[right]
            else:
                trapped += right_max - height[right]
            right -= 1
    print(trapped)

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "12\n0 1 0 2 1 0 1 3 2 1 2 1", "dbInit": None},
            {"stdin": "6\n4 2 0 3 2 5", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "3\n3 0 3", "dbInit": None},
            {"stdin": "3\n1 2 3", "dbInit": None},
            {"stdin": "4\n5 4 1 2", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Median of Two Sorted Arrays",
        "slug": "median-of-two-sorted-arrays",
        "problemType": "GENERIC",
        "difficulty": "HARD",
        "supportedLanguageIds": ["python-3.12", "java-21", "javascript-node-20", "cpp-23"],
        "statement": """### Problem Statement
Given two sorted arrays `nums1` and `nums2` of size $M$ and $N$ respectively, return the **median** of the two sorted arrays.

The overall run time complexity should be $\\mathcal{O}(\\log(M + N))$.

#### Input Format
- First line: integer $M$ and integer $N$.
- Next line: $M$ sorted integers.
- Next line: $N$ sorted integers.

#### Output Format
Print the median formatted to 1 decimal place (e.g., `2.0` or `2.5`).

#### Example
**Input:**
```text
2 1
1 3
2
```
**Output:**
```text
2.0
```
""",
        "topics": ["Data Structures & Algorithms"],
        "hints": ["Binary search on the partition position of the smaller array."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    m, n = int(data[0]), int(data[1])
    idx = 2
    nums1 = [int(x) for x in data[idx:idx+m]]
    idx += m
    nums2 = [int(x) for x in data[idx:idx+n]]
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int m = sc.nextInt();
        int n = sc.nextInt();
        int[] nums1 = new int[m];
        for (int i = 0; i < m; i++) nums1[i] = sc.nextInt();
        int[] nums2 = new int[n];
        for (int i = 0; i < n; i++) nums2[i] = sc.nextInt();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
    if (!tokens || tokens.length < 2) return;
    const m = parseInt(tokens[0], 10);
    const n = parseInt(tokens[1], 10);
    let idx = 2;
    const nums1 = tokens.slice(idx, idx + m).map(Number);
    idx += m;
    const nums2 = tokens.slice(idx, idx + n).map(Number);
    // Write solution here
}

solve();
""",
            "cpp-23": """#include <iostream>
#include <vector>
using namespace std;

int main() {
    int m, n;
    if (!(cin >> m >> n)) return 0;
    vector<int> nums1(m), nums2(n);
    for (int i = 0; i < m; i++) cin >> nums1[i];
    for (int i = 0; i < n; i++) cin >> nums2[i];
    // Write solution here
    return 0;
}
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    data = sys.stdin.read().split()
    if not data:
        return
    m, n = int(data[0]), int(data[1])
    idx = 2
    a = [int(x) for x in data[idx:idx+m]]
    idx += m
    b = [int(x) for x in data[idx:idx+n]]
    if len(a) > len(b):
        a, b = b, a
    x, y = len(a), len(b)
    low, high = 0, x
    while low <= high:
        partition_x = (low + high) // 2
        partition_y = (x + y + 1) // 2 - partition_x
        max_left_x = float('-inf') if partition_x == 0 else a[partition_x - 1]
        min_right_x = float('inf') if partition_x == x else a[partition_x]
        max_left_y = float('-inf') if partition_y == 0 else b[partition_y - 1]
        min_right_y = float('inf') if partition_y == y else b[partition_y]
        if max_left_x <= min_right_y and max_left_y <= min_right_x:
            if (x + y) % 2 == 0:
                med = (max(max_left_x, max_left_y) + min(min_right_x, min_right_y)) / 2.0
            else:
                med = float(max(max_left_x, max_left_y))
            print(f"{med:.1f}")
            return
        elif max_left_x > min_right_y:
            high = partition_x - 1
        else:
            low = partition_x + 1

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {"stdin": "2 1\n1 3\n2", "dbInit": None},
            {"stdin": "2 2\n1 2\n3 4", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "0 1\n\n1", "dbInit": None},
            {"stdin": "2 0\n2 3\n", "dbInit": None},
            {"stdin": "1 4\n1\n2 3 4 5", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "cpp-23": {"timeLimitMs": 1000, "memoryLimitKb": 131072}
        }
    },
    {
        "title": "Distinct Prime Factorization",
        "slug": "distinct-prime-factorization-verifier",
        "problemType": "GENERIC",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["python-3.12", "javascript-node-20", "java-21"],
        "statement": """### Problem Statement
Given an integer $N$ ($2 \\le N \\le 10^9$), print all **distinct prime factors** of $N$ separated by spaces.

Because any ordering of the distinct prime factors is mathematically valid, a verification script evaluates your answer.

#### Input Format
A single integer $N$.

#### Output Format
Space-separated distinct prime factors of $N$ in any order.

#### Example
**Input:**
```text
60
```
**Output:**
```text
2 3 5
```
""",
        "topics": ["Programming Fundamentals", "Data Structures & Algorithms"],
        "hints": ["Factor out 2 first, then test odd numbers up to sqrt(N)."],
        "starterCode": {
            "python-3.12": """import sys

def solve():
    raw = sys.stdin.read().strip()
    if not raw:
        return
    n = int(raw)
    # Write solution here
    pass

if __name__ == '__main__':
    solve()
""",
            "java-21": """import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!sc.hasNextLong()) return;
        long n = sc.nextLong();
        // Write solution here
    }
}
""",
            "javascript-node-20": """const fs = require('fs');

function solve() {
    const raw = fs.readFileSync(0, 'utf-8').trim();
    if (!raw) return;
    const n = parseInt(raw, 10);
    // Write solution here
}

solve();
"""
        },
        "referenceSolutionLanguage": "python-3.12",
        "referenceSolution": """import sys

def solve():
    raw = sys.stdin.read().strip()
    if not raw:
        return
    n = int(raw)
    d = 2
    factors = []
    while d * d <= n:
        if n % d == 0:
            factors.append(d)
            while n % d == 0:
                n //= d
        d += 1
    if n > 1:
        factors.append(n)
    print(" ".join(str(x) for x in factors))

if __name__ == '__main__':
    solve()
""",
        "requireVerificationScript": True,
        "verificationScriptLanguage": "python-3.12",
        "verificationScript": """import sys

raw = sys.stdin.read()
parts = raw.split("---STACKED_INPUT_END---")
if len(parts) != 2:
    sys.exit(1)

n = int(parts[0].strip())
cand_primes = set(int(x) for x in parts[1].strip().split())

def get_factors(num):
    d = 2
    factors = set()
    while d * d <= num:
        if num % d == 0:
            factors.add(d)
            while num % d == 0:
                num //= d
        d += 1
    if num > 1:
        factors.add(num)
    return factors

expected_primes = get_factors(n)
assert cand_primes == expected_primes, f"Expected factors {expected_primes}, got {cand_primes}"
""",
        "sampleTestcases": [
            {"stdin": "60", "dbInit": None},
            {"stdin": "84", "dbInit": None}
        ],
        "hiddenTestcases": [
            {"stdin": "97", "dbInit": None},
            {"stdin": "1024", "dbInit": None},
            {"stdin": "1000000000", "dbInit": None}
        ],
        "executionConstraints": {
            "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144},
            "java-21": {"timeLimitMs": 3000, "memoryLimitKb": 262144},
            "javascript-node-20": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "Second Highest Salary",
        "slug": "sql-second-highest-salary",
        "problemType": "DATABASE",
        "difficulty": "EASY",
        "supportedLanguageIds": ["sqlite-3"],
        "statement": """### Problem Statement
Write a SQL query to find the second highest distinct salary from the `Employee` table. If there is no second highest salary, return `NULL`.

#### Schema
```sql
CREATE TABLE Employee (
    id INT PRIMARY KEY,
    salary INT
);
```

#### Example
| id | salary |
| :--- | :--- |
| 1 | 100 |
| 2 | 200 |
| 3 | 300 |

**Output:**
```text
SecondHighestSalary
-------------------
200
```
""",
        "topics": ["SQL & Relational Databases"],
        "hints": ["Use a subquery with MAX() or DISTINCT salary with LIMIT 1 OFFSET 1."],
        "starterCode": {
            "sqlite-3": "-- Write your SQL query below\n"
        },
        "referenceSolutionLanguage": "sqlite-3",
        "referenceSolution": """SELECT MAX(salary) AS SecondHighestSalary
FROM Employee
WHERE salary < (SELECT MAX(salary) FROM Employee);
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Employee (id INT PRIMARY KEY, salary INT);
INSERT INTO Employee VALUES (1, 100), (2, 200), (3, 300);
"""
            },
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Employee (id INT PRIMARY KEY, salary INT);
INSERT INTO Employee VALUES (1, 100);
"""
            }
        ],
        "hiddenTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Employee (id INT PRIMARY KEY, salary INT);
INSERT INTO Employee VALUES (1, 500), (2, 500), (3, 200);
"""
            },
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Employee (id INT PRIMARY KEY, salary INT);
"""
            }
        ],
        "executionConstraints": {
            "sqlite-3": {"timeLimitMs": 5000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "Department Highest Salary",
        "slug": "sql-department-highest-salary",
        "problemType": "DATABASE",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["sqlite-3"],
        "statement": """### Problem Statement
Find employees who have the highest salary in each of the departments.

Return the result table with columns: `Department`, `Employee`, and `Salary`, ordered by `Department` ascending, then `Employee` ascending.

#### Schema
```sql
CREATE TABLE Department (
    id INT PRIMARY KEY,
    name TEXT
);

CREATE TABLE Employee (
    id INT PRIMARY KEY,
    name TEXT,
    salary INT,
    department_id INT
);
```
""",
        "topics": ["SQL & Relational Databases"],
        "hints": ["Use an inner join with a correlated subquery finding MAX(salary) grouped by department."],
        "starterCode": {
            "sqlite-3": "-- Write your SQL query below\n"
        },
        "referenceSolutionLanguage": "sqlite-3",
        "referenceSolution": """SELECT d.name AS Department, e.name AS Employee, e.salary AS Salary
FROM Employee e
JOIN Department d ON e.department_id = d.id
WHERE e.salary = (
    SELECT MAX(e2.salary)
    FROM Employee e2
    WHERE e2.department_id = e.department_id
)
ORDER BY Department ASC, Employee ASC;
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Department (id INT PRIMARY KEY, name TEXT);
CREATE TABLE Employee (id INT PRIMARY KEY, name TEXT, salary INT, department_id INT);

INSERT INTO Department VALUES (1, 'IT'), (2, 'Sales');
INSERT INTO Employee VALUES (1, 'Joe', 85000, 1), (2, 'Henry', 80000, 2), (3, 'Sam', 60000, 2), (4, 'Max', 90000, 1);
"""
            },
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Department (id INT PRIMARY KEY, name TEXT);
CREATE TABLE Employee (id INT PRIMARY KEY, name TEXT, salary INT, department_id INT);

INSERT INTO Department VALUES (1, 'IT');
INSERT INTO Employee VALUES (1, 'Alice', 90000, 1), (2, 'Bob', 90000, 1), (3, 'Charlie', 70000, 1);
"""
            }
        ],
        "hiddenTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Department (id INT PRIMARY KEY, name TEXT);
CREATE TABLE Employee (id INT PRIMARY KEY, name TEXT, salary INT, department_id INT);
"""
            },
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Department (id INT PRIMARY KEY, name TEXT);
CREATE TABLE Employee (id INT PRIMARY KEY, name TEXT, salary INT, department_id INT);

INSERT INTO Department VALUES (1, 'Finance');
INSERT INTO Employee VALUES (1, 'Ghost', 100000, NULL), (2, 'Dan', 75000, 1);
"""
            }
        ],
        "executionConstraints": {
            "sqlite-3": {"timeLimitMs": 5000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "Customer High-Value Orders Analysis",
        "slug": "sql-customer-high-value-orders",
        "problemType": "DATABASE",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["postgresql-16"],
        "statement": """### Problem Statement
In PostgreSQL 16:
Calculate the total spending on `COMPLETED` orders for every customer.

If a customer has no completed orders, output `0.00` for total spent.

Return columns: `name`, `total_spent`. Sort by `total_spent` descending, then `name` ascending.

#### Schema
```sql
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL
);
```
""",
        "topics": ["SQL & Relational Databases"],
        "hints": ["Use a LEFT JOIN with a condition on o.status = 'COMPLETED' in the ON clause."],
        "starterCode": {
            "postgresql-16": "-- Write PostgreSQL query\n"
        },
        "referenceSolutionLanguage": "postgresql-16",
        "referenceSolution": """SELECT c.name, COALESCE(SUM(o.amount), 0) AS total_spent
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id AND o.status = 'COMPLETED'
GROUP BY c.id, c.name
ORDER BY total_spent DESC, c.name ASC;
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE customers (id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL);
CREATE TABLE orders (id SERIAL PRIMARY KEY, customer_id INT NOT NULL, amount NUMERIC(10,2) NOT NULL, status VARCHAR(20) NOT NULL);

INSERT INTO customers (id, name) VALUES (1, 'Alice'), (2, 'Bob'), (3, 'Charlie');
INSERT INTO orders (customer_id, amount, status) VALUES (1, 100.50, 'COMPLETED'), (1, 200.00, 'COMPLETED'), (2, 50.00, 'CANCELLED');
"""
            }
        ],
        "hiddenTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE customers (id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL);
CREATE TABLE orders (id SERIAL PRIMARY KEY, customer_id INT NOT NULL, amount NUMERIC(10,2) NOT NULL, status VARCHAR(20) NOT NULL);

INSERT INTO customers (id, name) VALUES (1, 'EmptyCust');
"""
            },
            {
                "stdin": None,
                "dbInit": """CREATE TABLE customers (id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL);
CREATE TABLE orders (id SERIAL PRIMARY KEY, customer_id INT NOT NULL, amount NUMERIC(10,2) NOT NULL, status VARCHAR(20) NOT NULL);

INSERT INTO customers (id, name) VALUES (1, 'Zoe'), (2, 'Adam');
INSERT INTO orders (customer_id, amount, status) VALUES (1, 500.00, 'COMPLETED'), (2, 500.00, 'COMPLETED');
"""
            }
        ],
        "executionConstraints": {
            "postgresql-16": {"timeLimitMs": 6000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "Department Top Three Salaries",
        "slug": "sql-department-top-three-salaries",
        "problemType": "DATABASE",
        "difficulty": "HARD",
        "supportedLanguageIds": ["mysql-8.0"],
        "statement": """### Problem Statement
In MySQL 8.0:
A company's executives are interested in seeing who earns the most money in each of the company's departments. A high earner in a department is an employee who has a salary in the **top three unique salaries** for that department.

Write a query to find the employees who are high earners in each of the departments.

Return the result table with columns: `Department`, `Employee`, `Salary`.

Order by `Department` ascending, `Salary` descending, `Employee` ascending.

#### Schema
```sql
CREATE TABLE Department (
    id INT PRIMARY KEY,
    name VARCHAR(50)
);

CREATE TABLE Employee (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    salary INT,
    departmentId INT
);
```
""",
        "topics": ["SQL & Relational Databases"],
        "hints": ["Use DENSE_RANK() OVER (PARTITION BY departmentId ORDER BY salary DESC) in a Common Table Expression (CTE)."],
        "starterCode": {
            "mysql-8.0": "-- Write MySQL query\n"
        },
        "referenceSolutionLanguage": "mysql-8.0",
        "referenceSolution": """WITH RankedEmployees AS (
    SELECT d.name AS Department,
           e.name AS Employee,
           e.salary AS Salary,
           DENSE_RANK() OVER (PARTITION BY e.departmentId ORDER BY e.salary DESC) AS rnk
    FROM Employee e
    JOIN Department d ON e.departmentId = d.id
)
SELECT Department, Employee, Salary
FROM RankedEmployees
WHERE rnk <= 3
ORDER BY Department ASC, Salary DESC, Employee ASC;
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Department (id INT PRIMARY KEY, name VARCHAR(50));
CREATE TABLE Employee (id INT PRIMARY KEY, name VARCHAR(50), salary INT, departmentId INT);

INSERT INTO Department VALUES (1, 'IT'), (2, 'Sales');
INSERT INTO Employee VALUES (1, 'Joe', 85000, 1), (2, 'Henry', 80000, 2), (3, 'Sam', 60000, 2),
                           (4, 'Max', 90000, 1), (5, 'Janet', 69000, 1), (6, 'Randy', 85000, 1), (7, 'Will', 70000, 1);
"""
            }
        ],
        "hiddenTestcases": [
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Department (id INT PRIMARY KEY, name VARCHAR(50));
CREATE TABLE Employee (id INT PRIMARY KEY, name VARCHAR(50), salary INT, departmentId INT);

INSERT INTO Department VALUES (1, 'HR');
INSERT INTO Employee VALUES (1, 'Amy', 50000, 1), (2, 'Bob', 50000, 1);
"""
            },
            {
                "stdin": None,
                "dbInit": """CREATE TABLE Department (id INT PRIMARY KEY, name VARCHAR(50));
CREATE TABLE Employee (id INT PRIMARY KEY, name VARCHAR(50), salary INT, departmentId INT);
"""
            }
        ],
        "executionConstraints": {
            "mysql-8.0": {"timeLimitMs": 6000, "memoryLimitKb": 262144}
        }
    },
    {
        "title": "E-Commerce Category Revenue Summary",
        "slug": "mongo-ecommerce-category-revenue",
        "problemType": "DATABASE",
        "difficulty": "MEDIUM",
        "supportedLanguageIds": ["mongodb-8.0"],
        "statement": """### Problem Statement
In MongoDB 8.0:
Write an aggregation pipeline query on the `orders` collection to calculate the total revenue and total number of items sold per product `category`.

Only consider orders where `status` is `"DELIVERED"`.

Return documents with:
- `_id`: category name
- `totalRevenue`: total sum of `price * quantity`
- `itemsSold`: total sum of `quantity`

Sort the output by `totalRevenue` descending, then `_id` ascending.
Print each result document using `printjson`.

#### Schema
Documents in `orders` collection:
```json
{
  "_id": "order_1",
  "category": "Electronics",
  "price": 100,
  "quantity": 2,
  "status": "DELIVERED"
}
```
""",
        "topics": ["NoSQL & Distributed Caching"],
        "hints": ["Use $match on status, then $group by category with $sum multiplying price and quantity."],
        "starterCode": {
            "mongodb-8.0": "// Write MongoDB aggregation pipeline and printjson\n"
        },
        "referenceSolutionLanguage": "mongodb-8.0",
        "referenceSolution": """db.orders.aggregate([
  { $match: { status: "DELIVERED" } },
  {
    $group: {
      _id: "$category",
      totalRevenue: { $sum: { $multiply: ["$price", "$quantity"] } },
      itemsSold: { $sum: "$quantity" }
    }
  },
  { $sort: { totalRevenue: -1, _id: 1 } }
]).forEach(printjson);
""",
        "requireVerificationScript": False,
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": """db.orders.insertMany([
  { _id: 1, category: "Electronics", price: 100, quantity: 2, status: "DELIVERED" },
  { _id: 2, category: "Electronics", price: 50, quantity: 1, status: "DELIVERED" },
  { _id: 3, category: "Books", price: 20, quantity: 3, status: "DELIVERED" },
  { _id: 4, category: "Books", price: 30, quantity: 1, status: "CANCELLED" }
]);
"""
            }
        ],
        "hiddenTestcases": [
            {
                "stdin": None,
                "dbInit": """db.orders.insertMany([
  { _id: 1, category: "Clothing", price: 40, quantity: 5, status: "DELIVERED" },
  { _id: 2, category: "Home", price: 200, quantity: 1, status: "DELIVERED" }
]);
"""
            },
            {
                "stdin": None,
                "dbInit": """db.createCollection("orders");
"""
            }
        ],
        "executionConstraints": {
            "mongodb-8.0": {"timeLimitMs": 8000, "memoryLimitKb": 524288}
        }
    }
]
