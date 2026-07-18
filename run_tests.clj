(require '[clojure.test :as t] 'kizashi.methods.test-charter-gates
         'kizashi.repository-contract-test)
(let [r (t/run-tests 'kizashi.methods.test-charter-gates
                     'kizashi.repository-contract-test)]
  (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))
