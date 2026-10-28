#!/usr/bin/env python3
"""
Seed Script: Ingest 15 Foundational Coding Problems
- 10 Algorithmic DSA Problems (with industry-standard 3-file verification scripts)
- 5 LeetCode SQL 50 Select Essentials (MySQL 8.0)

Idempotent, robust error handling, sandboxed execution verification.
"""

import os
import sys
import time
import requests
from typing import Dict, Any, List

# Ensure scripts directory is in sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from seed_data.coding_problems_dsa import CODING_PROBLEMS_DSA
from seed_data.coding_problems_sql import CODING_PROBLEMS_SQL

API_BASE_URL = "http://localhost:8080/api"
ADMIN_EMAIL = "admin@stacked.com"
ADMIN_PASSWORD = "Admin@123456"

DEFAULT_PAYLOAD_FIELDS = {
    "requireVerificationScript": False,
    "verificationScriptLanguage": None,
    "verificationScript": None,
    "generatorLanguage": None,
    "generatorScript": None,
    "generatorStdin": None,
    "generatorDelimiter": None,
    "imageFileIds": [],
    "hints": [],
}

class CodingProblemSeeder:
    def __init__(self, base_url: str = API_BASE_URL):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.token = None

    def authenticate(self) -> bool:
        login_url = f"{self.base_url}/auth/admin/login"
        payload = {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD}
        try:
            resp = self.session.post(login_url, json=payload, timeout=10)
            if resp.status_code == 200:
                data = resp.json().get("data", {})
                self.token = data.get("accessToken")
                self.session.headers.update({"Authorization": f"Bearer {self.token}"})
                print(f"[AUTH] Successfully authenticated as {ADMIN_EMAIL}")
                return True
            else:
                print(f"[AUTH ERROR] Failed login: {resp.status_code} - {resp.text}")
                return False
        except Exception as e:
            print(f"[AUTH EXCEPTION] {e}")
            return False

    def get_existing_problem(self, slug: str) -> Dict[str, Any] | None:
        url = f"{self.base_url}/coding-problem/{slug}"
        resp = self.session.get(url)
        if resp.status_code == 200:
            return resp.json().get("data")
        return None

    def create_problem(self, problem_data: Dict[str, Any]) -> Dict[str, Any] | None:
        url = f"{self.base_url}/coding-problem"
        slug = problem_data.get("slug")
        title = problem_data.get("title")

        payload = {**DEFAULT_PAYLOAD_FIELDS, **problem_data}

        print(f"\n[SEEDING] Creating: '{title}' ({slug})...")
        start_time = time.time()
        resp = self.session.post(url, json=payload, timeout=120)
        elapsed = time.time() - start_time

        if resp.status_code in (200, 201):
            created = resp.json().get("data")
            prob_id = created.get("id")
            sample_count = len(created.get("sampleTestCases", []))
            hidden_count = len(created.get("hiddenTestCases", []))
            print(f"  [CREATED] ID: {prob_id} ({sample_count} samples + {hidden_count} hidden verified in {elapsed:.2f}s)")
            return created
        else:
            print(f"  [ERROR] {resp.status_code}: {resp.text}")
            return None

    def publish_problem(self, prob_id: str) -> bool:
        url = f"{self.base_url}/coding-problem/{prob_id}/publish"
        resp = self.session.patch(url, timeout=10)
        if resp.status_code == 200:
            print(f"  [PUBLISHED] Problem {prob_id} is now LIVE")
            return True
        else:
            print(f"  [PUBLISH ERROR] Failed to publish {prob_id}: {resp.text}")
            return False

    def run(self):
        if not self.authenticate():
            sys.exit(1)

        all_problems = CODING_PROBLEMS_DSA + CODING_PROBLEMS_SQL
        total = len(all_problems)
        print(f"\n=== Starting Ingestion of {total} Foundational Coding Problems ===")

        created_count = 0
        published_count = 0

        for idx, prob in enumerate(all_problems, 1):
            slug = prob["slug"]
            existing = self.get_existing_problem(slug)
            prob_id = None

            if existing:
                prob_id = existing.get("id")
                print(f"\n[{idx}/{total}] Problem '{slug}' already exists (ID: {prob_id}).")
            else:
                created = self.create_problem(prob)
                if created:
                    prob_id = created.get("id")
                    created_count += 1
                else:
                    print(f"[{idx}/{total}] ABORTING further seeding due to error on '{slug}'.")
                    sys.exit(1)

            if prob_id:
                if self.publish_problem(prob_id):
                    published_count += 1

        print("\n" + "=" * 60)
        print(f"[SUMMARY] Total Problems: {total} | Created: {created_count} | Published: {published_count}")
        print("=" * 60)

if __name__ == "__main__":
    seeder = CodingProblemSeeder()
    seeder.run()

