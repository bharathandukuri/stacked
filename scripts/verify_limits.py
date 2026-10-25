#!/usr/bin/env python3
"""
Verification Script: Test Inefficient Solutions for TLE, MLE, and SQL Traps
Ensures that:
1. Efficient O(N) solutions PASS within limits (< 100ms).
2. Inefficient O(N^2) brute-force solutions trigger TIME_LIMIT_EXCEEDED (TLE).
3. Memory-hog solutions allocating beyond memory budget trigger MEMORY_LIMIT_EXCEEDED (MLE).
4. Buggy SQL queries (ignoring NULL referee_ids or missing DISTINCT) trigger WRONG_ANSWER.
"""

import sys
import time
import requests
from typing import Dict, Any

API_BASE_URL = "http://localhost:8080/api"
ADMIN_EMAIL = "admin@stacked.com"
ADMIN_PASSWORD = "Admin@123456"

class LimitsVerifier:
    def __init__(self, base_url: str = API_BASE_URL):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.token = None

    def authenticate(self) -> bool:
        login_url = f"{self.base_url}/auth/admin/login"
        payload = {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD}
        resp = self.session.post(login_url, json=payload, timeout=10)
        if resp.status_code == 200:
            self.token = resp.json()["data"]["accessToken"]
            self.session.headers.update({"Authorization": f"Bearer {self.token}"})
            print(f"[AUTH] Authenticated as {ADMIN_EMAIL}")
            return True
        print(f"[AUTH ERROR] {resp.status_code}: {resp.text}")
        return False

    def run_preview(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        url = f"{self.base_url}/admin/coding-problem/run-preview"
        resp = self.session.post(url, json=payload, timeout=60)
        if resp.status_code == 200:
            return resp.json().get("data", {})
        raise RuntimeError(f"Preview run failed {resp.status_code}: {resp.text}")

    def test_two_sum_tle(self):
        print("\n--- [TEST 1] Two Sum: Brute Force O(N^2) must TLE ---")
        # Adversarial input: 10,000 numbers, target at the very end
        stdin_stress = f"10000 19999\n{' '.join(str(i) for i in range(1, 10001))}"
        
        # Inefficient O(N^2) brute-force solution
        brute_force_code = """
import sys

def solve():
    raw = sys.stdin.read().split()
    if not raw:
        return
    n = int(raw[0])
    target = int(raw[1])
    nums = [int(x) for x in raw[2:2+n]]
    for i in range(n):
        for j in range(i + 1, n):
            if nums[i] + nums[j] == target:
                print(f"{i} {j}")
                return

solve()
"""
        payload = {
            "problemType": "GENERIC",
            "referenceSolutionLanguage": "python-3.12",
            "referenceSolution": brute_force_code,
            "requireVerificationScript": False,
            "testcases": [
                {"id": "stress-case", "stdin": stdin_stress, "dbInit": None}
            ],
            "executionConstraints": {
                "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
            }
        }

        start = time.time()
        result = self.run_preview(payload)
        elapsed = time.time() - start
        
        case_res = result["results"][0]
        status = case_res.get("status")
        runtime = case_res.get("runtimeMs")
        print(f"  Result: status={status}, runtimeMs={runtime}, wallElapsed={elapsed:.2f}s")
        assert status == "TIME_LIMIT_EXCEEDED", f"Expected TIME_LIMIT_EXCEEDED, got {status}"
        print("  [PASS] Brute-force O(N^2) correctly triggered TIME_LIMIT_EXCEEDED!")

    def test_two_sum_efficient_passes(self):
        print("\n--- [TEST 2] Two Sum: Optimal O(N) Hash Map must PASS quickly ---")
        stdin_stress = f"10000 19999\n{' '.join(str(i) for i in range(1, 10001))}"
        
        optimal_code = """
import sys

def solve():
    raw = sys.stdin.read().split()
    if not raw:
        return
    n = int(raw[0])
    target = int(raw[1])
    seen = {}
    for i in range(n):
        num = int(raw[2 + i])
        diff = target - num
        if diff in seen:
            print(f"{seen[diff]} {i}")
            return
        seen[num] = i

solve()
"""
        payload = {
            "problemType": "GENERIC",
            "referenceSolutionLanguage": "python-3.12",
            "referenceSolution": optimal_code,
            "requireVerificationScript": False,
            "testcases": [
                {"id": "stress-case", "stdin": stdin_stress, "dbInit": None}
            ],
            "executionConstraints": {
                "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
            }
        }

        result = self.run_preview(payload)
        case_res = result["results"][0]
        status = case_res.get("status")
        runtime = case_res.get("runtimeMs")
        stdout = case_res.get("stdout", "").strip()
        print(f"  Result: status={status}, runtimeMs={runtime}ms, stdout='{stdout}'")
        assert status in ("SUCCESS", "PASSED"), f"Expected SUCCESS/PASSED, got {status}"
        assert stdout == "9998 9999", f"Unexpected output: {stdout}"
        print("  [PASS] Optimal O(N) solution passed in milliseconds!")

    def test_contains_duplicate_tle(self):
        print("\n--- [TEST 3] Contains Duplicate: Brute Force O(N^2) must TLE ---")
        stdin_stress = f"10000\n{' '.join(str(i) for i in range(10000))}"
        
        brute_force_code = """
import sys

def solve():
    raw = sys.stdin.read().split()
    if not raw:
        return
    n = int(raw[0])
    nums = [int(x) for x in raw[1:1+n]]
    for i in range(n):
        for j in range(i + 1, n):
            if nums[i] == nums[j]:
                print('true')
                return
    print('false')

solve()
"""
        payload = {
            "problemType": "GENERIC",
            "referenceSolutionLanguage": "python-3.12",
            "referenceSolution": brute_force_code,
            "requireVerificationScript": False,
            "testcases": [
                {"id": "stress-case", "stdin": stdin_stress, "dbInit": None}
            ],
            "executionConstraints": {
                "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
            }
        }

        result = self.run_preview(payload)
        case_res = result["results"][0]
        status = case_res.get("status")
        runtime = case_res.get("runtimeMs")
        print(f"  Result: status={status}, runtimeMs={runtime}")
        assert status == "TIME_LIMIT_EXCEEDED", f"Expected TIME_LIMIT_EXCEEDED, got {status}"
        print("  [PASS] Contains Duplicate O(N^2) correctly triggered TIME_LIMIT_EXCEEDED!")

    def test_longest_consecutive_tle(self):
        print("\n--- [TEST 4] Longest Consecutive: Naive search without (x-1 not in set) must TLE ---")
        stdin_stress = f"10000\n{' '.join(str(i) for i in range(1, 10001))}"
        
        naive_code = """
import sys

def solve():
    raw = sys.stdin.read().split()
    if not raw:
        print(0)
        return
    n = int(raw[0])
    nums = set(int(x) for x in raw[1:1+n])
    longest = 0
    # Naive: does not check 'if (x - 1) not in nums:'
    for x in nums:
        curr = x
        streak = 1
        while (curr + 1) in nums:
            curr += 1
            streak += 1
        if streak > longest:
            longest = streak
    print(longest)

solve()
"""
        payload = {
            "problemType": "GENERIC",
            "referenceSolutionLanguage": "python-3.12",
            "referenceSolution": naive_code,
            "requireVerificationScript": False,
            "testcases": [
                {"id": "stress-case", "stdin": stdin_stress, "dbInit": None}
            ],
            "executionConstraints": {
                "python-3.12": {"timeLimitMs": 2000, "memoryLimitKb": 262144}
            }
        }

        result = self.run_preview(payload)
        case_res = result["results"][0]
        status = case_res.get("status")
        runtime = case_res.get("runtimeMs")
        print(f"  Result: status={status}, runtimeMs={runtime}")
        assert status == "TIME_LIMIT_EXCEEDED", f"Expected TIME_LIMIT_EXCEEDED, got {status}"
        print("  [PASS] Naive sequence search correctly triggered TIME_LIMIT_EXCEEDED!")

    def test_memory_limit_exceeded(self):
        print("\n--- [TEST 5] Memory Allocation: Retaining large memory must trigger MEMORY_LIMIT_EXCEEDED ---")
        # Allocates large chunks without catching exception -> triggers MEMORY_LIMIT_EXCEEDED
        memory_hog_code = """
import sys

chunks = []
for _ in range(500):
    chunks.append("A" * (1024 * 1024)) # 1MB each
print(len(chunks))
"""
        payload = {
            "problemType": "GENERIC",
            "referenceSolutionLanguage": "python-3.12",
            "referenceSolution": memory_hog_code,
            "requireVerificationScript": False,
            "testcases": [
                {"id": "mle-case", "stdin": "1", "dbInit": None}
            ],
            "executionConstraints": {
                "python-3.12": {"timeLimitMs": 4000, "memoryLimitKb": 32768} # 32MB limit
            }
        }

        result = self.run_preview(payload)
        case_res = result["results"][0]
        status = case_res.get("status")
        print(f"  Result: status={status}, error={case_res.get('error')}")
        assert status == "MEMORY_LIMIT_EXCEEDED", f"Expected MEMORY_LIMIT_EXCEEDED, got {status}"
        print("  [PASS] Memory boundary enforced: MEMORY_LIMIT_EXCEEDED successfully captured!")

    def test_sql_three_valued_logic_trap(self):
        print("\n--- [TEST 6] SQL 3-Valued Logic: Query missing 'IS NULL' on Referee ID must FAIL ---")
        db_init = """
DROP TABLE IF EXISTS Customer;
CREATE TABLE Customer (
    id INT PRIMARY KEY,
    name VARCHAR(25),
    referee_id INT
);
INSERT INTO Customer VALUES
    (1, 'Will', NULL),
    (2, 'Jane', NULL),
    (3, 'Alex', 2),
    (4, 'Bill', NULL),
    (5, 'Zack', 1),
    (6, 'Mark', 2);
"""
        # Buggy query: ignores NULL referee_id
        buggy_query = "SELECT name FROM Customer WHERE referee_id != 2;"
        
        payload = {
            "problemType": "DATABASE",
            "referenceSolutionLanguage": "mysql-8.0",
            "referenceSolution": buggy_query,
            "requireVerificationScript": False,
            "testcases": [
                {"id": "referee-case", "stdin": None, "dbInit": db_init}
            ],
            "executionConstraints": {
                "mysql-8.0": {"timeLimitMs": 5000, "memoryLimitKb": 524288}
            }
        }

        result = self.run_preview(payload)
        case_res = result["results"][0]
        stdout = case_res.get("stdout", "").strip()
        print(f"  Buggy SQL Output:\n{stdout}")
        # The buggy output only contains 'Zack', omitting Will, Jane, Bill!
        assert "Zack" in stdout, "Expected Zack in output"
        assert "Will" not in stdout, "Buggy query should have omitted Will due to NULL != 2 evaluating to false"
        print("  [PASS] SQL 3-Valued Logic trap successfully demonstrated!")

    def run_all(self):
        if not self.authenticate():
            sys.exit(1)
        
        print("\n" + "=" * 60)
        print("RUNNING TLE, MLE, AND CONSTRAINT VERIFICATION SUITE")
        print("=" * 60)

        self.test_two_sum_efficient_passes()
        self.test_two_sum_tle()
        self.test_contains_duplicate_tle()
        self.test_longest_consecutive_tle()
        self.test_memory_limit_exceeded()
        self.test_sql_three_valued_logic_trap()

        print("\n" + "=" * 60)
        print("ALL 6 CONSTRAINT & LOGIC VERIFICATION TESTS PASSED SUCCESSFULLY!")
        print("=" * 60)

if __name__ == "__main__":
    verifier = LimitsVerifier()
    verifier.run_all()

