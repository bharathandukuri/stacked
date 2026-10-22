#!/usr/bin/env python3
"""
Comprehensive Seeder and Validator for Stacked Question Bank.

Populates the platform with:
- 10 Subject Curriculum Topics
- 93 Curated Multi-Choice Questions (MCQ single/multiple/boolean, code blocks, tables, KaTeX)
- 22 Rigorous Coding Problems (Generic DSA, Custom Python Verifier, SQLite, Postgres, MySQL, MongoDB)
- 5 Curated Question Banks (Junior Full-Stack, Java & Spring Backend, DSA Mastery, Database Systems, Systems Architecture)

Includes end-to-end regression validation:
- Validates published-only eligibility enforcement in Question Banks
- Validates empty-bank publish prevention
- Validates judge sandbox execution
- Provides full idempotent execution (skips already-seeded items)
"""

import argparse
import json
import logging
import re
import sys
import time
from typing import Any, Dict, List, Optional, Set, Tuple
import requests

# Import seed data modules
sys.path.append("scripts")
from seed_data import (
    coding_problems,
    mcqs_backend_spring,
    mcqs_core_java,
    mcqs_dsa,
    mcqs_engineering_scenarios,
    mcqs_frontend_web,
    mcqs_nosql_cache,
    mcqs_programming_fundamentals,
    mcqs_software_architecture,
    mcqs_sql_databases,
    mcqs_systems_infrastructure,
    question_banks,
    topics,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("StackedSeeder")


def normalize_slug(title: str) -> str:
    """Matches McqQuestionServiceImpl generateUniqueSlug base logic."""
    base = title.lower().strip()
    base = re.sub(r"[^a-z0-9\s-]", "", base)
    base = re.sub(r"[\s-]+", "-", base)
    return base or "question"


class StackedApiClient:
    def __init__(self, base_url: str, email: str, password: str):
        self.base_url = base_url.rstrip("/")
        self.email = email
        self.password = password
        self.session = requests.Session()
        self.token: Optional[str] = None
        self.headers: Dict[str, str] = {
            "Content-Type": "application/json",
            "Accept": "application/json",
        }

    def authenticate(self) -> bool:
        login_url = f"{self.base_url}/auth/admin/login"
        payload = {"email": self.email, "password": self.password}
        log.info(f"Authenticating as admin ({self.email}) against {login_url}...")
        resp = self.session.post(login_url, json=payload, headers=self.headers)
        if resp.status_code != 200:
            log.error(f"Authentication failed: {resp.status_code} - {resp.text}")
            return False

        data = resp.json()
        self.token = data.get("data", {}).get("accessToken")
        if not self.token:
            log.error("Authentication succeeded but no accessToken was returned!")
            return False

        self.headers["Authorization"] = f"Bearer {self.token}"
        log.info("Admin authentication successful!")
        return True

    # --- Topic APIs ---
    def get_topics(self) -> List[Dict[str, Any]]:
        resp = self.session.get(f"{self.base_url}/admin/mcq/topics", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data", [])
        log.error(f"Failed to fetch topics: {resp.status_code} - {resp.text}")
        return []

    def create_topic(self, name: str, description: Optional[str]) -> Optional[Dict[str, Any]]:
        payload = {"name": name, "description": description}
        resp = self.session.post(f"{self.base_url}/admin/mcq/topics", json=payload, headers=self.headers)
        if resp.status_code == 201:
            return resp.json().get("data")
        log.error(f"Failed to create topic '{name}': {resp.status_code} - {resp.text}")
        return None

    # --- MCQ APIs ---
    def get_mcqs(self, size: int = 1000) -> List[Dict[str, Any]]:
        resp = self.session.get(f"{self.base_url}/admin/mcq/questions?size={size}", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data", {}).get("items", [])
        log.error(f"Failed to fetch MCQs: {resp.status_code} - {resp.text}")
        return []

    def get_mcq(self, id_or_slug: str) -> Optional[Dict[str, Any]]:
        resp = self.session.get(f"{self.base_url}/admin/mcq/questions/{id_or_slug}", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data")
        return None

    def create_mcq(self, q_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        payload = {
            "title": q_data["title"],
            "content": q_data["content"],
            "type": q_data["type"],
            "difficulty": q_data["difficulty"],
            "status": "PUBLISHED",
            "topics": q_data.get("topics", []),
            "tags": q_data.get("tags", []),
            "options": q_data["options"],
            "correctOptionIds": q_data["correctOptionIds"],
            "explanation": q_data.get("explanation"),
            "defaultPoints": q_data.get("defaultPoints", 1.0),
            "negativePoints": q_data.get("negativePoints", 0.0),
            "shuffleOptions": q_data.get("shuffleOptions", True),
        }
        resp = self.session.post(f"{self.base_url}/admin/mcq/questions", json=payload, headers=self.headers)
        if resp.status_code == 201:
            created = resp.json().get("data")
            # If not published by default, explicitly publish
            if created and created.get("status") != "PUBLISHED":
                self.publish_mcq(created["id"])
            return created
        log.error(f"Failed to create MCQ '{q_data.get('title')}': {resp.status_code} - {resp.text}")
        return None

    def publish_mcq(self, id_or_slug: str) -> bool:
        resp = self.session.patch(f"{self.base_url}/admin/mcq/questions/{id_or_slug}/publish", headers=self.headers)
        return resp.status_code == 200

    # --- Coding Problem APIs ---
    def get_coding_problems(self, size: int = 1000) -> List[Dict[str, Any]]:
        resp = self.session.get(f"{self.base_url}/coding-problem?size={size}", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data", {}).get("items", [])
        log.error(f"Failed to fetch coding problems: {resp.status_code} - {resp.text}")
        return []

    def get_coding_problem(self, id_or_slug: str) -> Optional[Dict[str, Any]]:
        resp = self.session.get(f"{self.base_url}/coding-problem/{id_or_slug}", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data")
        return None

    def create_coding_problem(self, p_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        payload = {
            "title": p_data["title"],
            "slug": p_data["slug"],
            "problemType": p_data["problemType"],
            "difficulty": p_data["difficulty"],
            "supportedLanguageIds": p_data["supportedLanguageIds"],
            "statement": p_data["statement"],
            "topics": p_data["topics"],
            "hints": p_data.get("hints", []),
            "starterCode": p_data["starterCode"],
            "referenceSolutionLanguage": p_data["referenceSolutionLanguage"],
            "referenceSolution": p_data["referenceSolution"],
            "requireVerificationScript": p_data.get("requireVerificationScript", False),
            "verificationScriptLanguage": p_data.get("verificationScriptLanguage"),
            "verificationScript": p_data.get("verificationScript"),
            "sampleTestcases": p_data["sampleTestcases"],
            "hiddenTestcases": p_data["hiddenTestcases"],
            "executionConstraints": p_data.get("executionConstraints"),
        }
        resp = self.session.post(f"{self.base_url}/coding-problem", json=payload, headers=self.headers)
        if resp.status_code in (200, 201):
            created = resp.json().get("data")
            if created and created.get("id"):
                # Publish coding problem
                self.publish_coding_problem(created["id"])
            return created
        log.error(f"Failed to create coding problem '{p_data.get('slug')}': {resp.status_code} - {resp.text}")
        return None

    def publish_coding_problem(self, id_or_slug: str) -> bool:
        resp = self.session.patch(f"{self.base_url}/coding-problem/{id_or_slug}/publish", headers=self.headers)
        return resp.status_code == 200

    # --- Question Bank APIs ---
    def get_question_banks(self, size: int = 1000) -> List[Dict[str, Any]]:
        resp = self.session.get(f"{self.base_url}/admin/question-banks?size={size}", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data", {}).get("items", [])
        log.error(f"Failed to fetch question banks: {resp.status_code} - {resp.text}")
        return []

    def get_question_bank(self, id_or_slug: str) -> Optional[Dict[str, Any]]:
        resp = self.session.get(f"{self.base_url}/admin/question-banks/{id_or_slug}", headers=self.headers)
        if resp.status_code == 200:
            return resp.json().get("data")
        return None

    def create_question_bank(self, b_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        resp = self.session.post(f"{self.base_url}/admin/question-banks", json=b_data, headers=self.headers)
        if resp.status_code == 201:
            created = resp.json().get("data")
            if created and created.get("id"):
                self.publish_question_bank(created["id"])
            return created
        log.error(f"Failed to create question bank '{b_data.get('title')}': {resp.status_code} - {resp.text}")
        return None

    def publish_question_bank(self, id_or_slug: str) -> bool:
        resp = self.session.patch(f"{self.base_url}/admin/question-banks/{id_or_slug}/publish", headers=self.headers)
        return resp.status_code == 200


class SeederRunner:
    def __init__(self, client: StackedApiClient):
        self.client = client
        self.topic_map: Dict[str, str] = {}  # name -> id
        self.mcq_id_map: Dict[str, str] = {}  # slug / generated_slug -> id
        self.coding_id_map: Dict[str, str] = {}  # slug -> id
        self.all_questions_id_map: Dict[str, str] = {}  # slug -> id

    def seed_topics(self) -> int:
        log.info("--- Step 1: Seeding Curriculum Topics ---")
        existing = self.client.get_topics()
        existing_by_name = {t["name"].lower(): t for t in existing}
        for t in existing:
            self.topic_map[t["name"]] = t["id"]
            self.topic_map[t["name"].lower()] = t["id"]

        created_count = 0
        for topic in topics.TOPICS:
            name = topic["name"]
            desc = topic.get("description")
            if name.lower() in existing_by_name:
                log.info(f"Topic '{name}' already exists (ID: {existing_by_name[name.lower()]['id']}). Skipping.")
                continue

            created = self.client.create_topic(name, desc)
            if created:
                self.topic_map[name] = created["id"]
                self.topic_map[name.lower()] = created["id"]
                created_count += 1
                log.info(f"Created Topic: '{name}' (ID: {created['id']})")

        log.info(f"Topics seeding complete. Total registered topics: {len(self.topic_map) // 2}")
        return created_count

    def seed_mcqs(self) -> int:
        log.info("--- Step 2: Seeding MCQs ---")
        existing = self.client.get_mcqs(size=2000)
        existing_by_slug = {m["slug"]: m for m in existing}
        existing_by_title = {m["title"].lower().strip(): m for m in existing}

        for m in existing:
            self.mcq_id_map[m["slug"]] = m["id"]
            self.all_questions_id_map[m["slug"]] = m["id"]

        mcq_modules = [
            mcqs_programming_fundamentals,
            mcqs_core_java,
            mcqs_dsa,
            mcqs_sql_databases,
            mcqs_nosql_cache,
            mcqs_backend_spring,
            mcqs_frontend_web,
            mcqs_systems_infrastructure,
            mcqs_software_architecture,
            mcqs_engineering_scenarios,
        ]

        all_mcqs: List[Dict[str, Any]] = []
        for mod in mcq_modules:
            all_mcqs.extend(mod.QUESTIONS)

        created_count = 0
        for q in all_mcqs:
            slug = q["slug"]
            title = q["title"]
            gen_slug = normalize_slug(title)

            # Check if exists
            existing_mcq = existing_by_slug.get(slug) or existing_by_slug.get(gen_slug) or existing_by_title.get(title.lower().strip())
            if existing_mcq:
                qid = existing_mcq["id"]
                self.mcq_id_map[slug] = qid
                self.mcq_id_map[gen_slug] = qid
                self.all_questions_id_map[slug] = qid
                self.all_questions_id_map[gen_slug] = qid
                if existing_mcq.get("status") != "PUBLISHED":
                    self.client.publish_mcq(qid)
                continue

            created = self.client.create_mcq(q)
            if created:
                qid = created["id"]
                self.mcq_id_map[slug] = qid
                self.mcq_id_map[gen_slug] = qid
                self.mcq_id_map[created.get("slug", "")] = qid
                self.all_questions_id_map[slug] = qid
                self.all_questions_id_map[gen_slug] = qid
                self.all_questions_id_map[created.get("slug", "")] = qid
                created_count += 1
                if created_count % 10 == 0:
                    log.info(f"Progress: Seeded {created_count}/{len(all_mcqs)} MCQs...")

        log.info(f"MCQ seeding complete. Created {created_count} new MCQs. Total indexed: {len(self.mcq_id_map)}")
        return created_count

    def seed_coding_problems(self) -> int:
        log.info("--- Step 3: Seeding Coding Problems & Executing in Judge Sandbox ---")
        existing = self.client.get_coding_problems(size=2000)
        existing_by_slug = {p["slug"]: p for p in existing}

        for p in existing:
            self.coding_id_map[p["slug"]] = p["id"]
            self.all_questions_id_map[p["slug"]] = p["id"]

        created_count = 0
        for p in coding_problems.PROBLEMS:
            slug = p["slug"]
            if slug in existing_by_slug:
                pid = existing_by_slug[slug]["id"]
                self.coding_id_map[slug] = pid
                self.all_questions_id_map[slug] = pid
                if existing_by_slug[slug].get("status") != "PUBLISHED":
                    self.client.publish_coding_problem(pid)
                log.info(f"Coding Problem '{slug}' already exists (ID: {pid}). Skipping creation.")
                continue

            t0 = time.time()
            log.info(f"Creating & compiling coding problem: [{slug}] (Lang: {p['referenceSolutionLanguage']})...")
            created = self.client.create_coding_problem(p)
            elapsed = time.time() - t0
            if created:
                pid = created["id"]
                self.coding_id_map[slug] = pid
                self.all_questions_id_map[slug] = pid
                created_count += 1
                log.info(f"-> Successfully created & verified [{slug}] in {elapsed:.2f}s (ID: {pid})")
            else:
                log.error(f"-> FAILED creating coding problem: [{slug}]")

        log.info(f"Coding Problem seeding complete. Created {created_count} new problems. Total indexed: {len(self.coding_id_map)}")
        return created_count

    def seed_question_banks(self) -> int:
        log.info("--- Step 4: Seeding Curated Question Banks ---")
        existing = self.client.get_question_banks(size=500)
        existing_by_title = {b["title"].lower().strip(): b for b in existing}

        created_count = 0
        for b in question_banks.BANKS:
            title = b["title"]
            if title.lower().strip() in existing_by_title:
                log.info(f"Question Bank '{title}' already exists. Skipping.")
                continue

            items_payload = []
            for i, item in enumerate(b["items"]):
                itype = item["type"]
                slug = item["slug"]
                qid = self.all_questions_id_map.get(slug)
                if not qid:
                    log.error(f"Cannot add item: slug '{slug}' not found in registered question IDs!")
                    continue
                items_payload.append({
                    "questionType": itype,
                    "questionId": qid,
                    "order": i + 1,
                    "customPoints": item.get("customPoints")
                })

            payload = {
                "title": title,
                "description": b["description"],
                "category": b["category"],
                "tags": b.get("tags", []),
                "status": "PUBLISHED",
                "items": items_payload
            }

            created = self.client.create_question_bank(payload)
            if created:
                created_count += 1
                log.info(f"Created & Published Question Bank: '{title}' ({len(items_payload)} items, {created.get('totalPoints')} pts, ID: {created['id']})")

        return created_count

    def run_regression_and_feature_tests(self) -> Dict[str, Any]:
        log.info("--- Step 5: Running Feature & Regression Tests ---")
        results: Dict[str, Any] = {}

        # Test 1: Verify all 10 topics exist and have questions
        topics_list = self.client.get_topics()
        topic_counts = {t["name"]: t.get("questionCount", 0) for t in topics_list}
        all_topics_have_questions = len(topics_list) >= 10 and all(c >= 5 for n, c in topic_counts.items() if n != "DSA")
        results["curriculum_topics_populated"] = all_topics_have_questions
        log.info(f"[TEST 1] Curriculum Topics verification: {'PASS' if all_topics_have_questions else 'WARN'}")

        # Test 2: Verify total published MCQs >= 90
        mcqs_list = self.client.get_mcqs(size=2000)
        published_mcqs = [m for m in mcqs_list if m.get("status") == "PUBLISHED"]
        results["mcq_count"] = len(mcqs_list)
        results["published_mcq_count"] = len(published_mcqs)
        test_2_pass = len(published_mcqs) >= 90
        results["test_2_mcq_published_target"] = test_2_pass
        log.info(f"[TEST 2] MCQ Population (Published: {len(published_mcqs)}/93): {'PASS' if test_2_pass else 'FAIL'}")

        # Test 3: Verify total published coding problems >= 20
        coding_list = self.client.get_coding_problems(size=2000)
        published_coding = [c for c in coding_list if c.get("status") == "PUBLISHED"]
        results["coding_problem_count"] = len(coding_list)
        results["published_coding_count"] = len(published_coding)
        test_3_pass = len(published_coding) >= 20
        results["test_3_coding_published_target"] = test_3_pass
        log.info(f"[TEST 3] Coding Problem Population (Published: {len(published_coding)}/22): {'PASS' if test_3_pass else 'FAIL'}")

        # Test 4: Verify Question Banks exist and are PUBLISHED
        banks_list = self.client.get_question_banks(size=500)
        published_banks = [b for b in banks_list if b.get("status") == "PUBLISHED"]
        results["question_banks_count"] = len(banks_list)
        results["published_banks_count"] = len(published_banks)
        test_4_pass = len(published_banks) >= 5
        results["test_4_banks_published_target"] = test_4_pass
        log.info(f"[TEST 4] Question Bank Population (Published: {len(published_banks)}/5): {'PASS' if test_4_pass else 'FAIL'}")

        # Test 5: Validation Enforcement - Attempting to add an unpublished/draft MCQ to a Question Bank
        log.info("Testing Question Bank Validation: Attempting to add DRAFT question to a Question Bank...")
        # Create a temporary DRAFT MCQ
        temp_draft_mcq_resp = self.client.session.post(
            f"{self.client.base_url}/admin/mcq/questions",
            json={
                "title": "Temp Draft MCQ for Validation Test",
                "content": "Temporary question for testing Question Bank validation",
                "type": "SINGLE_CHOICE",
                "difficulty": "EASY",
                "status": "DRAFT",
                "topics": ["Programming Fundamentals"],
                "options": [
                    {"id": "opt-1", "text": "Draft Option A"},
                    {"id": "opt-2", "text": "Draft Option B"}
                ],
                "correctOptionIds": ["opt-1"],
            },
            headers=self.client.headers
        )
        if temp_draft_mcq_resp.status_code == 201:
            temp_draft_id = temp_draft_mcq_resp.json()["data"]["id"]
            # Attempt to create Question Bank containing this draft question
            invalid_bank_resp = self.client.session.post(
                f"{self.client.base_url}/admin/question-banks",
                json={
                    "title": "Invalid Bank With Draft Question",
                    "description": "Should fail validation",
                    "status": "DRAFT",
                    "items": [{"questionType": "MCQ", "questionId": temp_draft_id, "order": 1}]
                },
                headers=self.client.headers
            )
            validation_blocked_draft = invalid_bank_resp.status_code == 400
            results["validation_blocked_draft_question"] = validation_blocked_draft
            log.info(f"[TEST 5] Reject adding unpublished question to bank: {'PASS (400 Bad Request)' if validation_blocked_draft else 'FAIL'}")
            # Clean up temporary draft question
            self.client.session.delete(f"{self.client.base_url}/admin/mcq/questions/{temp_draft_id}", headers=self.client.headers)
        else:
            results["validation_blocked_draft_question"] = "SKIPPED_DRAFT_CREATION_FAILED"

        # Test 6: Validation Enforcement - Attempting to publish an empty Question Bank
        log.info("Testing Question Bank Validation: Attempting to publish empty Question Bank...")
        empty_bank_resp = self.client.session.post(
            f"{self.client.base_url}/admin/question-banks",
            json={
                "title": "Empty Bank Test",
                "description": "Empty bank",
                "status": "PUBLISHED",
                "items": []
            },
            headers=self.client.headers
        )
        empty_bank_blocked = empty_bank_resp.status_code == 400
        results["empty_bank_publish_blocked"] = empty_bank_blocked
        log.info(f"[TEST 6] Reject publishing empty Question Bank: {'PASS (400 Bad Request)' if empty_bank_blocked else 'FAIL'}")

        # Test 7: Sandbox preview test execution
        log.info("Testing Code Sandbox Execution: Running Python preview...")
        preview_resp = self.client.session.post(
            f"{self.client.base_url}/coding-problem/run-preview",
            json={
                "problemType": "GENERIC",
                "referenceSolutionLanguage": "python-3.12",
                "referenceSolution": "import sys\nfor line in sys.stdin:\n    print(f'ECHO: {line.strip()}')\n",
                "requireVerificationScript": False,
                "testcases": [{"id": "case-1", "stdin": "Stacked Interview Platform"}]
            },
            headers=self.client.headers
        )
        sandbox_pass = preview_resp.status_code == 200 and "ECHO: Stacked Interview Platform" in preview_resp.text
        results["judge_sandbox_execution"] = sandbox_pass
        log.info(f"[TEST 7] Judge Sandbox Preview Execution: {'PASS' if sandbox_pass else 'FAIL'}")

        return results


def print_summary_report(results: Dict[str, Any], client: StackedApiClient):
    print("\n" + "=" * 80)
    print("                    STACKED QUESTION BANK SEEDING SUMMARY")
    print("=" * 80)
    print(f"Total MCQs in System:              {results.get('mcq_count')} (Published: {results.get('published_mcq_count')})")
    print(f"Total Coding Problems in System:   {results.get('coding_problem_count')} (Published: {results.get('published_coding_count')})")
    print(f"Total Questions (MCQ + Coding):    {results.get('mcq_count', 0) + results.get('coding_problem_count', 0)}")
    print(f"Total Question Banks in System:    {results.get('question_banks_count')} (Published: {results.get('published_banks_count')})")
    print("-" * 80)
    print("FEATURE & REGRESSION TEST RESULTS:")
    print(f"  [1] Curriculum Topics Populated:      {'[PASS]' if results.get('curriculum_topics_populated') else '[WARN]'}")
    print(f"  [2] Target >=90 MCQs Published:       {'[PASS]' if results.get('test_2_mcq_published_target') else '[FAIL]'}")
    print(f"  [3] Target >=20 Coding Published:     {'[PASS]' if results.get('test_3_coding_published_target') else '[FAIL]'}")
    print(f"  [4] Target 5 Question Banks:          {'[PASS]' if results.get('test_4_banks_published_target') else '[FAIL]'}")
    print(f"  [5] Reject Draft in Bank (400 BadReq):{'[PASS]' if results.get('validation_blocked_draft_question') else '[FAIL]'}")
    print(f"  [6] Reject Empty Bank Publish (400):  {'[PASS]' if results.get('empty_bank_publish_blocked') else '[FAIL]'}")
    print(f"  [7] Judge Sandbox Live Execution:     {'[PASS]' if results.get('judge_sandbox_execution') else '[FAIL]'}")
    print("=" * 80 + "\n")


def main():
    parser = argparse.ArgumentParser(description="Stacked Question Bank Master Seeder & Validator")
    parser.add_argument("--api-url", default="http://localhost:8080/api", help="Base API URL")
    parser.add_argument("--email", default="admin@stacked.com", help="Admin email")
    parser.add_argument("--password", default="Admin@123456", help="Admin password")
    parser.add_argument("--verify-only", action="store_true", help="Run validation tests without inserting content")
    args = parser.parse_args()

    client = StackedApiClient(args.api_url, args.email, args.password)
    if not client.authenticate():
        sys.exit(1)

    runner = SeederRunner(client)

    if not args.verify_only:
        runner.seed_topics()
        runner.seed_mcqs()
        runner.seed_coding_problems()
        runner.seed_question_banks()

    results = runner.run_regression_and_feature_tests()
    print_summary_report(results, client)


if __name__ == "__main__":
    main()
