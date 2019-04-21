(ns core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; bare-bones trampoline
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro tr [f & args] `(with-meta (fn [] (~f ~@args)) {:trampoline? true}))

(defn eval-tr [f & args]
  (loop [t (apply f args)] (if (:trampoline? (meta t)) (recur (t)) t)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; non-tail recursion: custom-stack evaluation with optional memoization
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro cs [f & args]
  ^:custom-stack? {:function f
                   :args (mapv #(list 'fn '[] %) args)})

(defmacro cs-memo [args-memo f & args]
  ^:custom-stack? {:function f
                   :args (mapv #(list 'fn '[] %) args)
                   :args-memo args-memo})

(defn eval-cs [f & args]
  (let [r (apply f args)]
    (if-not (:custom-stack? (meta r))
      r
      (let [make-si (fn [x] (merge (dissoc x :args)
                                   {:eval-args []
                                    :not-eval-args (map (fn [f] (f)) (:args x))}))
            m (atom {})]
        (loop [[h & t] (list (make-si r))]
          (let [hne (-> h :not-eval-args first)]
            (cond

              (empty? (:not-eval-args h))
              (let [{:keys [function eval-args]} h
                    r (if function (apply function eval-args) (first eval-args))
                    [ht & tt] t]
                (cond
                  (:custom-stack? (meta r)) (recur (cons (make-si r) t))
                  (empty? t) r
                  :else (do
                          (when (contains? h :args-memo) (swap! m assoc (:args-memo h) r))
                          (recur (cons (update ht :eval-args conj r) tt)))))

              (:custom-stack? (meta hne))
              (if (and (contains? hne :args-memo) (contains? @m (:args-memo hne)))
                (recur (cons (-> h
                                 (update :eval-args conj (get @m (:args-memo hne)))
                                 (update :not-eval-args rest)) t))
                (recur (->> t
                            (cons (update h :not-eval-args rest))
                            (cons (make-si hne)))))

              :else
              (recur (cons (-> h
                               (update :eval-args conj hne)
                               (update :not-eval-args rest)) t)))))))))
