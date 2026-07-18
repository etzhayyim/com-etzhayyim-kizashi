# com.etzhayyim.kizashi.* — kizashi (兆) Lexicons

**Owner actor**: `did:web:kizashi.etzhayyim.com` (``)
**ADR**: ADR-2605312700 (R0 scaffold)
**Status**: R0 schema skeletons. Full schemas (additionalProperties=false + required fields + encryptedPayloadCid mandatory on L1/L2/L4) at R1+.

## 6 Lexicons

| # | Lexicon | Consumer cell | Structural invariant |
|---|---|---|---|
| L1 | `scanSessionAttestation` | scan_session | G2: **encryptedPayloadCid REQUIRED**; biometric scan = 要配慮 PII; 30-day rotating pseudonym DID |
| L2 | `modalityObservation` | signal_fusion | G2: **encryptedPayloadCid REQUIRED**; `modalityId` MUST exist in capability ledger (G10); regulated modality needs clearance CID (G4) |
| L3 | `attributionReport` | attribution | G3: schema **FORBIDS** diagnosis/prescription/treatmentPlan; G7: **confidence + disclaimer (const) + consultRecommendation REQUIRED** |
| L4 | `wellbecomingTrajectory` | wellbecoming_track | G2 + G8: **encryptedPayloadCid REQUIRED**; baseline = member's OWN prior scan only; no population field exists |
| L5 | `triageReferral` | triage_referral | G5: `emergencyFlag` REQUIRED; `targetActor ∈ {mitate, iyashi, kokoro}` (kizashi cannot self-treat) |
| L6 | `modalityCapability` | modality_registry | PUBLIC (not encrypted); G10: **evidenceGrade + regulatoryClass + canDetect + cannotDetect REQUIRED**; bio-resonance/aura/波動 EXCLUDED (grade X) |

## Schema Discipline (R1+)

- `additionalProperties: false` at top-level record schema;
- L1/L2/L4 MUST carry `encryptedPayloadCid` (G2 structural — rejects
  plaintext biometric content);
- **L3 `attributionReport` is the load-bearing non-diagnostic gate**:
  `diagnosis` / `prescription` / `treatmentPlan` fields are
  schema-forbidden (G3, 医師法 §17); `confidence` + `disclaimer`
  (const non-diagnostic text) + `consultRecommendation` are required
  (G7 — no false precision, no silent claim);
- L5 `triageReferral.targetActor` constrained to the clinical-
  adjudication actors — kizashi can refer but never close a clinical
  loop;
- L6 `modalityCapability.evidenceGrade` gates emission (G10): a
  modality not ledgered with a defensible grade cannot produce an
  observation; `regulatoryClass` + `ionizing` gate regulated/ionizing
  modalities to R3 + licensed-facility (G4 + G9).

## R0 Status

Schemas at R0 are skeleton-level: the field set is enumerated but
strict validation + encryptedPayloadCid enforcement + the
diagnosis-field prohibition become active at R1 (schemas evolve
through R1 Council attestation review).

## Related Files

- `/manifest.jsonld` — owner actor manifest
- `/CLAUDE.md` — actor operational doc
- `/90-docs/adr/2605312700-kizashi-noninvasive-multimodal-bodyscan-tier-b-actor-r0.md` — Master ADR
- `/00-contracts/lexicons/com/etzhayyim/iyashi/` — sibling encrypted-envelope discipline (G2 pattern)
