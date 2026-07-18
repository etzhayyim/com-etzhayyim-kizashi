(ns kizashi.murakumo-test
  (:require [clojure.test :refer [deftest is testing]]
            [kizashi.murakumo :as kizashi]))

(def full-attestations
  (into {}
        (map (fn [gate] [gate (str "attested-" (name gate))]))
        (distinct (mapcat :required-gates (vals kizashi/cell-specs)))))

(deftest maps-all-legacy-kizashi-cells
  (is (= #{"kizashi_attribution"
           "kizashi_modality_registry"
           "kizashi_scan_session"
           "kizashi_signal_fusion"
           "kizashi_triage_referral"
           "kizashi_wellbecoming_track"}
         (set (map :legacy-cell (vals kizashi/cell-specs))))))

(deftest r0-gates-block-effects
  (let [plan (kizashi/cell-plan :attribution
                                {:member-did "did:example:member"
                                 :scan-session-id "scan-001"
                                 :computed-at "2026-06-29T00:00:00Z"})]
    (is (= :blocked (:status plan)))
    (is (= [:council-charter-attestation
            :silen-kizashi-baseline-review
            :encrypted-biometric-envelope-baseline
            :per-scan-consent-baseline
            :non-diagnostic-sign-sensing-baseline
            :medical-device-boundary-baseline
            :verified-modality-only-baseline
            :uncertainty-honest-baseline
            :murakumo-only-inference-baseline
            :no-commercial-imaging-cloud-baseline
            :no-population-ranking-baseline
            :no-diagnosis-prescription-treatment-field-baseline
            :probabilistic-attribution-baseline
            :consult-recommendation-baseline
            :licensed-clinician-diagnosis-handoff-baseline]
           (:missing-gates plan)))
    (is (empty? (:effects plan)))))

(deftest attested-attribution-is-non-diagnostic
  (let [plan (kizashi/cell-plan :attribution
                                {:attestations full-attestations
                                 :member-did "did:example:member"
                                 :scan-session-id "scan-001"
                                 :attribution-id "attr-001"
                                 :computed-at "2026-06-29T00:00:00Z"
                                 :record {:tid "attr-001"
                                          :confidence 0.62
                                          :consultRecommendation "mitate"}})
        effect (first (:effects plan))]
    (is (= :ready (:status plan)))
    (is (= :mst/put-record (:op effect)))
    (is (= kizashi/actor-did (:actor effect)))
    (is (= "com.etzhayyim.kizashi.attributionReport" (:collection effect)))
    (is (= "attr-001" (:rkey effect)))
    (is (= true (get-in effect [:record :encryptedPayloadRequired])))
    (is (= true (get-in effect [:record :nonDiagnostic])))
    (is (re-find #"確定原因ではありません" (get-in effect [:record :disclaimer])))))

(deftest signal-fusion-is-transient-only
  (let [plan (kizashi/cell-plan :signal-fusion
                                {:attestations full-attestations
                                 :scan-session-id "scan-001"})]
    (is (= :ready (:status plan)))
    (is (empty? (:records plan)))
    (is (empty? (:effects plan)))))

(deftest modality-registry-keeps-pseudoscience-excluded
  (let [attestations (dissoc full-attestations :pseudoscience-grade-x-excluded-baseline)
        plan (kizashi/cell-plan :modality-registry {:attestations attestations})]
    (is (= :blocked (:status plan)))
    (is (= [:pseudoscience-grade-x-excluded-baseline] (:missing-gates plan)))))

(deftest triage-referral-routes-only-to-clinical-actors
  (let [attestations (dissoc full-attestations :mitate-iyashi-kokoro-target-baseline)
        plan (kizashi/cell-plan :triage-referral {:attestations attestations})]
    (is (= [:mitate-iyashi-kokoro-target-baseline] (:missing-gates plan)))))

(deftest all-cell-plans-ready-when-attested
  (let [plans (kizashi/all-cell-plans {:attestations full-attestations
                                       :member-did "did:example:member"
                                       :scan-session-id "scan-001"
                                       :modality-id "infrared-thermography"
                                       :observation-id "obs-001"
                                       :attribution-id "attr-001"
                                       :trajectory-id "traj-001"
                                       :referral-id "ref-001"
                                       :consent-cid "bafkreiconsent"
                                       :computed-at "2026-06-29T00:00:00Z"})]
    (is (= (set (keys kizashi/cell-specs)) (set (keys plans))))
    (is (every? #(= :ready (:status %)) (vals plans)))
    (is (= 5 (count (mapcat :effects (vals plans)))))))
