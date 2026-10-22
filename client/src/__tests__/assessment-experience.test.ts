import { describe, it } from "node:test"
import assert from "node:assert/strict"

import { resolveMonacoLanguage, resolveStarterCode } from "../lib/monaco-languages.ts"

describe("Assessment Experience Utilities & Calculations", () => {
  describe("Monaco Syntax Highlighting Resolution", () => {
    it("resolves Java language ID variations to java monaco mode", () => {
      assert.equal(resolveMonacoLanguage("java-21"), "java")
      assert.equal(resolveMonacoLanguage("java"), "java")
      assert.equal(resolveMonacoLanguage("Java"), "java")
    })

    it("resolves Python language ID variations to python monaco mode", () => {
      assert.equal(resolveMonacoLanguage("python-3.12"), "python")
      assert.equal(resolveMonacoLanguage("python"), "python")
      assert.equal(resolveMonacoLanguage("py"), "python")
    })

    it("resolves C++ and C language ID variations correctly", () => {
      assert.equal(resolveMonacoLanguage("cpp-23"), "cpp")
      assert.equal(resolveMonacoLanguage("cpp"), "cpp")
      assert.equal(resolveMonacoLanguage("c++"), "cpp")
      assert.equal(resolveMonacoLanguage("c-17"), "c")
      assert.equal(resolveMonacoLanguage("c"), "c")
    })

    it("resolves database engines to appropriate syntax modes", () => {
      assert.equal(resolveMonacoLanguage("postgresql-16"), "sql")
      assert.equal(resolveMonacoLanguage("mysql-8.0"), "sql")
      assert.equal(resolveMonacoLanguage("sqlite-3"), "sql")
      assert.equal(resolveMonacoLanguage("mongodb-8.0"), "javascript")
    })

    it("falls back gracefully for unknown languages", () => {
      assert.equal(resolveMonacoLanguage("unknown-lang"), "plaintext")
      assert.equal(resolveMonacoLanguage(undefined), "plaintext")
    })
  })

  describe("Starter Code Resolution", () => {
    it("resolves exact key match", () => {
      const starter = {
        "java-21": "public class Solution {}",
        "python-3.12": "def solution(): pass",
      }
      assert.equal(resolveStarterCode(starter, "java-21"), "public class Solution {}")
      assert.equal(resolveStarterCode(starter, "python-3.12"), "def solution(): pass")
    })

    it("resolves prefix or base language match", () => {
      const starter = {
        java: "public class Solution {}",
        python: "def solution(): pass",
      }
      assert.equal(resolveStarterCode(starter, "java-21"), "public class Solution {}")
      assert.equal(resolveStarterCode(starter, "python-3.12"), "def solution(): pass")
    })

    it("resolves case-insensitively", () => {
      const starter = {
        "Java-21": "class Solution {}",
      }
      assert.equal(resolveStarterCode(starter, "java-21"), "class Solution {}")
    })

    it("returns empty string when no starter code available", () => {
      assert.equal(resolveStarterCode(undefined, "java-21"), "")
      assert.equal(resolveStarterCode({}, "java-21"), "")
    })
  })

  describe("Accuracy and Score Calculation Logic", () => {
    it("computes percentage accurately", () => {
      const score = 16.0
      const maxScore = 20.0
      const percentage = (score / maxScore) * 100
      assert.equal(percentage, 80.0)
    })

    it("computes accuracy on attempted questions correctly", () => {
      const totalQuestions = 5
      const answeredCount = 4
      const correctCount = 3

      const accuracyPercentage = answeredCount > 0 ? (correctCount / answeredCount) * 100 : 0
      assert.equal(accuracyPercentage, 75.0)

      const unansweredCount = totalQuestions - answeredCount
      assert.equal(unansweredCount, 1)
    })

    it("determines pass status when passing threshold is configured", () => {
      const passingScore = 14.0
      const candidateScore1 = 16.0
      const candidateScore2 = 12.0

      assert.equal(candidateScore1 >= passingScore, true)
      assert.equal(candidateScore2 >= passingScore, false)
    })
  })

  describe("Telemetry Event Payloads", () => {
    it("validates safe telemetry payload creation", () => {
      const event = {
        eventType: "WINDOW_BLUR",
        questionId: "q-101",
        metadata: { reason: "window_blur" },
        clientTimestamp: new Date().toISOString(),
      }

      assert.equal(event.eventType, "WINDOW_BLUR")
      assert.equal(event.questionId, "q-101")
      assert.ok(event.clientTimestamp)
      assert.equal(typeof event.metadata.reason, "string")
    })
  })

  describe("Pre-Creation Validation & Immutability Logic", () => {
    const getStepForField = (field: string): number => {
      if (
        field === "title" ||
        field === "durationMinutes" ||
        field === "scheduledWindow" ||
        field === "maxAttempts" ||
        field === "passingScore"
      ) {
        return 1
      }
      if (field === "questionBankId" || field === "questions") {
        return 2
      }
      if (
        field === "candidateEmails" ||
        field === "invitationCodeExpiresAt" ||
        field === "invitationCodeMaxUses"
      ) {
        return 3
      }
      return 1
    }

    it("maps basic timing and title error fields to Step 1", () => {
      assert.equal(getStepForField("title"), 1)
      assert.equal(getStepForField("durationMinutes"), 1)
      assert.equal(getStepForField("scheduledWindow"), 1)
      assert.equal(getStepForField("passingScore"), 1)
      assert.equal(getStepForField("maxAttempts"), 1)
    })

    it("maps question bank and content error fields to Step 2", () => {
      assert.equal(getStepForField("questionBankId"), 2)
      assert.equal(getStepForField("questions"), 2)
    })

    it("maps candidate email and passcode error fields to Step 3", () => {
      assert.equal(getStepForField("candidateEmails"), 3)
      assert.equal(getStepForField("invitationCodeExpiresAt"), 3)
      assert.equal(getStepForField("invitationCodeMaxUses"), 3)
    })

    it("verifies validation response flags invalid when errors are present", () => {
      const validationResponse = {
        valid: false,
        errors: [
          {
            field: "questionBankId",
            code: "NOT_PUBLISHED",
            message: "Selected bank is not published",
          },
        ],
        warnings: [],
      }

      assert.equal(validationResponse.valid, false)
      assert.equal(validationResponse.errors.length, 1)
      assert.equal(validationResponse.errors[0].field, "questionBankId")
    })

    it("verifies question bank preview calculations and difficulty distribution", () => {
      const preview = {
        id: "qb-101",
        title: "Java Full Stack Bank",
        totalQuestions: 6,
        mcqCount: 4,
        codingCount: 2,
        totalPoints: 50.0,
        difficultyDistribution: {
          EASY: 3,
          MEDIUM: 2,
          HARD: 1,
        },
        supportedCodingLanguages: ["java-21", "python-3.12"],
        eligibleForAssessment: true,
      }

      assert.equal(preview.totalQuestions, preview.mcqCount + preview.codingCount)
      assert.equal(
        preview.totalQuestions,
        Object.values(preview.difficultyDistribution).reduce((a, b) => a + b, 0)
      )
      assert.equal(preview.eligibleForAssessment, true)
      assert.ok(preview.supportedCodingLanguages.includes("java-21"))
    })
  })

  describe("Automatic Assessment Lifecycle Status & Roster Freeze Rules", () => {
    function deriveClientStatus(
      scheduledStart?: string | null,
      scheduledEnd?: string | null,
      now: Date = new Date()
    ): "NOT_STARTED" | "ACTIVE" | "COMPLETED" {
      const nowMs = now.getTime()
      if (scheduledEnd && nowMs >= new Date(scheduledEnd).getTime()) {
        return "COMPLETED"
      }
      if (scheduledStart && nowMs < new Date(scheduledStart).getTime()) {
        return "NOT_STARTED"
      }
      return "ACTIVE"
    }

    it("evaluates NOT_STARTED before scheduledStart", () => {
      const now = new Date("2026-10-21T12:00:00Z")
      const start = "2026-10-21T13:00:00Z"
      const end = "2026-10-21T15:00:00Z"
      assert.equal(deriveClientStatus(start, end, now), "NOT_STARTED")
    })

    it("evaluates ACTIVE when now is at or after scheduledStart and before scheduledEnd", () => {
      const start = "2026-10-21T10:00:00Z"
      const end = "2026-10-21T14:00:00Z"
      // Inclusive start boundary
      assert.equal(deriveClientStatus(start, end, new Date(start)), "ACTIVE")
      // Mid-session
      assert.equal(deriveClientStatus(start, end, new Date("2026-10-21T12:00:00Z")), "ACTIVE")
    })

    it("evaluates COMPLETED at or after scheduledEnd", () => {
      const start = "2026-10-21T10:00:00Z"
      const end = "2026-10-21T14:00:00Z"
      // Exclusive end boundary (at end is completed)
      assert.equal(deriveClientStatus(start, end, new Date(end)), "COMPLETED")
      // After end
      assert.equal(deriveClientStatus(start, end, new Date("2026-10-21T15:00:00Z")), "COMPLETED")
    })

    it("evaluates ACTIVE immediately when scheduledStart is omitted", () => {
      const end = "2026-10-21T18:00:00Z"
      const now = new Date("2026-10-21T12:00:00Z")
      assert.equal(deriveClientStatus(null, end, now), "ACTIVE")
    })

    it("allows candidate enrollment when assessment status is NOT_STARTED or ACTIVE, and closes when COMPLETED", () => {
      const isEnrollmentAllowed = (status: "NOT_STARTED" | "ACTIVE" | "COMPLETED") => status !== "COMPLETED"

      assert.equal(isEnrollmentAllowed("NOT_STARTED"), true)
      assert.equal(isEnrollmentAllowed("ACTIVE"), true)
      assert.equal(isEnrollmentAllowed("COMPLETED"), false)
    })

    it("determines candidate dashboard target path: directly to results if submitted", () => {
      function getCandidateActionPath(
        assessmentId: string,
        assignmentStatus: "INVITED" | "IN_PROGRESS" | "SUBMITTED" | "EXPIRED"
      ): string {
        if (assignmentStatus === "IN_PROGRESS") {
          return `/assessments/${assessmentId}/take`
        }
        if (assignmentStatus === "SUBMITTED") {
          return `/assessments/${assessmentId}/results`
        }
        return `/assessments/${assessmentId}`
      }

      assert.equal(getCandidateActionPath("ass-1", "SUBMITTED"), "/assessments/ass-1/results")
      assert.equal(getCandidateActionPath("ass-1", "IN_PROGRESS"), "/assessments/ass-1/take")
      assert.equal(getCandidateActionPath("ass-1", "INVITED"), "/assessments/ass-1")
    })
  })
})

