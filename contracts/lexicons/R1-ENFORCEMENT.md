# kizashi Lexicons — R1 schema-enforcement spec

Per ADR-2605312700. R0 ships skeleton schemas (field set enumerated, lenient).
**R1** hardens each schema under Council review. This file is the *exact,
mechanical contract* for that hardening — what `required[]` to finalize, where
`additionalProperties: false` goes, which fields are schema-FORBIDDEN (the
structural gates), and which become closed enums / `const`. §D is a drift-check
runnable NOW against the R0 skeletons.

> This is a SPEC, not the enforcement itself. R0 ceiling holds: schemas stay
> lenient until R1 Council attestation (`additionalProperties:false` flips the
> records to strict). Do not apply §A–C before R1.

## A. Universal R1 rules (all 6 records)

1. `additionalProperties: false` on every `defs.main.record` (and on the
   `contribution` def). This is what makes the *forbidden-field* gates
   structural — an unknown field is rejected, not ignored.
2. Every `required[]` field must be a declared property (no orphans).
3. `charterRiderScanPass` becomes `required` (G1 — every emitted doc is scanned).

## B. Per-lexicon R1 contract

| Lexicon | Gate | `required[]` to finalize (R1) | Forbidden fields (rejected by `additionalProperties:false`) | Closed enum / const |
|---|---|---|---|---|
| `scanSessionAttestation` | G2 | + (keep) `encryptedPayloadCid`, `scanConsentCid`, `memberPseudonymDid`, `modalitySet` | any plaintext biometric content (`rawScan`, `features`, `image`, `measurements`) — must live in the encrypted payload | — |
| `modalityObservation` | G2 + G10 + G4 | (keep current 4) | plaintext feature/raw fields | **conditional**: if `modalityId`'s ledger `regulatoryClass ∈ {regulated-medical-device, samd-software, ionizing-licensed-facility-only}` ⇒ `regulatedModalityClearanceCid` REQUIRED (cross-field; enforced at cell entry + kqe, not pure JSON Schema) |
| `attributionReport` | **G3 + G7** | (keep current 7) | **`diagnosis`, `diagnoses`, `prescription`, `prescriptions`, `treatmentPlan`, `treatment`, `medication`, `dosage`, `icdCode`, `icd10`, `dxCode`** — the load-bearing non-diagnostic gate (医師法 §17) | `disclaimer` = `const` (✓ already); `confidence`/`consultRecommendation`/`symptomDomain` closed enums (✓) |
| `wellbecomingTrajectory` | **G2 + G8** | (keep current 5) | **`populationBaseline`, `percentile`, `rank`, `ranking`, `healthScore`, `cohortComparison`, `norm`** — G8 self-referenced (no population field may exist) | `trajectoryDirection` closed enum (✓) |
| `triageReferral` | G5 + G3 | (keep current 4) | any self-treatment/order field (`order`, `prescription`, `treatment`) | `targetActor` closed `enum` = `{mitate, iyashi, kokoro}` (kizashi can only refer) |
| `modalityCapability` | G10 + G4 + G9 | + promote `ionizing` + `phaseGate` to REQUIRED (G4/G9 depend on them) | — (public registry) | `evidenceGrade`, `regulatoryClass`, `phaseGate` closed enums (✓) |

## C. Cross-field rules (cell-entry / kqe validator, not pure JSON Schema)

- **G4/G9 phase gate**: a `modalityObservation` may emit only if its
  `modalityId` ledger entry has `phaseGate ≤ current phase` AND
  (`regulatoryClass = non-regulated-wellness` OR `regulatedModalityClearanceCid`
  is set) AND (`ionizing = false` for any in-pod capture).
- **G10 emission gate**: `modalityId` must resolve to a `modalityCapability`
  entry whose `verificationStatus = council-verified` and `evidenceGrade ≠
  X-excluded-pseudoscience` (see `registry/VERIFICATION.md`).
- **G5 emergency**: `attributionReport.consultRecommendation = emergency` ⇒ a
  `triageReferral` with `emergencyFlag = true` MUST be produced, referencing the
  mitate red-flag set (`mitate.emergencyEscalation.redFlagCategory`).

## D. R0 drift-check (runnable now)

Asserts the R0 skeletons are already consistent with the R1 contract above —
catches drift between skeleton and planned enforcement before R1.

```bash
python3 - <<'PY'
import json, glob, os
D='wire/contracts/lexicons/'
def rec(n): return json.load(open(f'{D}/{n}.json'))['defs']['main']['record']
ok=True
def chk(c,m):
    global ok
    print(('ok  ' if c else 'FAIL')+m); ok = ok and c
# 1. no orphan required fields, anywhere
for f in glob.glob(f'{D}/*.json'):
    r=json.load(open(f))['defs']['main']['record']; props=set(r['properties'])
    chk(set(r.get('required',[]))<=props, f'{os.path.basename(f)}: required ⊆ properties')
# 2. G2 — encrypted lexicons require encryptedPayloadCid
for n in ['scanSessionAttestation','modalityObservation','wellbecomingTrajectory']:
    chk('encryptedPayloadCid' in rec(n)['required'], f'G2 {n}: encryptedPayloadCid required')
# 3. G3 — attributionReport forbids any diagnosis-class field
FORB={'diagnosis','diagnoses','prescription','prescriptions','treatmentPlan','treatment','medication','dosage','icdCode','icd10','dxCode'}
chk(not (FORB & set(rec('attributionReport')['properties'])), 'G3 attributionReport: no diagnosis-class field')
chk('disclaimer' in rec('attributionReport')['required'], 'G7 attributionReport: disclaimer required')
# 4. G8 — wellbecomingTrajectory has no population field
POP={'populationBaseline','percentile','rank','ranking','healthScore','cohortComparison','norm'}
chk(not (POP & set(rec('wellbecomingTrajectory')['properties'])), 'G8 wellbecomingTrajectory: no population field')
# 5. G5/G3 — triageReferral target is the clinical-adjudication set only
tv=rec('triageReferral')['properties']['targetActor'].get('knownValues',[])
chk(set(tv)=={'mitate','iyashi','kokoro'}, 'G3/G5 triageReferral: targetActor = {mitate,iyashi,kokoro}')
print('\nDRIFT-CHECK', 'PASS' if ok else 'FAIL'); raise SystemExit(0 if ok else 1)
PY
```

**Last verified**: 2026-06-01 (R0) — drift-check PASS.
