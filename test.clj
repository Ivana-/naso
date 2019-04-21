(ns test
  (:require [clojure.test :refer :all]
            [core :refer :all]))

(defn is-SO? [f & args]
  (is (= "java.lang.StackOverflowError"
         (try (apply f args)
              (catch Throwable t (.toString t))))))


(deftest common-test


  (testing "tail recursion - recur or trampoline"

    (defn foo [n a] (if (= 0 n) a (foo (- n 1) (+ n a))))
    (is-SO? foo 10000 0)


    ;; tail recursion - recur...
    (defn foo [n a] (if (= 0 n) a (recur (- n 1) (+ n a))))
    (is (= 50005000 (foo 10000 0)))

    ;; ... or trampoline
    (defn foo [n a] (if (= 0 n) a (tr foo (- n 1) (+ n a))))
    (is (= 50005000 (eval-tr foo 10000 0))))


  (testing "mutual tail call - trampoline"

    (declare is-odd?)
    (defn is-even? [n] (if (= 0 n) true  (is-odd?  (- n 1))))
    (defn is-odd?  [n] (if (= 0 n) false (is-even? (- n 1))))
    (is-SO? is-even? 1000000)

    ;; tail call - trampoline
    (declare is-odd?)
    (defn is-even? [n] (if (= 0 n) true  (tr is-odd?  (- n 1))))
    (defn is-odd?  [n] (if (= 0 n) false (tr is-even? (- n 1))))
    (is (eval-tr is-even? 1000000)))


  (testing "eval-tr/eval-cs transparency"

    (is (= 2 (eval-tr inc 1)))
    (is (= 6 (eval-tr + 1 2 3)))
    (is (= 2 (eval-cs inc 1)))
    (is (= 6 (eval-cs + 1 2 3))))


  (testing "non-tail recursion - via custom stack!"

    (defn bar [n] (if (= 0 n) 0 (+ n (bar (- n 1)))))
    (is-SO? bar 10000)

    ;; non-tail call - via custom stack!
    (defn bar [n] (if (= 0 n) 0 (cs + n (bar (- n 1)))))
    (is (= 50005000 (eval-cs bar 10000))))


  (testing "tail recursion - also via custom stack!"

    (defn foo [n a] (if (= 0 n) a (cs nil (foo (- n 1) (+ n a)))))
    (is (= 50005000 (eval-cs foo 10000 0))))


  (testing "mutual tail call - also via custom stack!"

    (declare is-odd?)
    (defn is-even? [n] (if (= 0 n) true  (cs nil (is-odd?  (- n 1)))))
    (defn is-odd?  [n] (if (= 0 n) false (cs nil (is-even? (- n 1)))))
    (is (eval-cs is-even? 1000000)))


  (testing "non-tail recursion with memoization - via custom stack!"

    ;; fibbonacci
    (defn fib [n] (if (< n 2) n (cs-memo n + (fib (- n 1)) (fib (- n 2)))))
    (is (= 55          (eval-cs fib 10)))
    (is (= 832040      (eval-cs fib 30)))
    (is (= 12586269025 (eval-cs fib 50)))

    ;; coin change
    (defn coin-change [s coins]
      (cond (= s 0) 1
            (or (< s 0) (empty? coins)) 0
            :else (cs-memo [s coins]
                           +
                           (coin-change s (rest coins))
                           (coin-change (- s (first coins)) coins))))

    (def coins-list '(1 5 10 25 50))
    (is (= 292            (eval-cs coin-change 100    coins-list)))
    (is (= 6794128501     (eval-cs coin-change 10000  coins-list)))
    ;; (is (= 66793412685001 (eval-cs coin-change 100000 coins-list)))

    ;; skyscrappers height
    (defn height [n m]
      (if (or (<= m 0) (<= n 0))
        0
        (cs-memo [n m] + 1 (height n (- m 1)) (height (- n 1) (- m 1)))))

    (is (= 105              (eval-cs height 2 14)))
    (is (= 2021630625377350 (eval-cs height 5 3000))))


  (testing "non-primitive non-tail recursion with memoization - via custom stack!"

    ;; Ackermann
    (defn ack [m n]
      (cond
        (zero? m) (inc n)
        (zero? n) (ack (dec m) 1)
        :else     (ack (dec m) (ack m (dec n)))))

    (is-SO? ack 4 1)

    (defn ack [m n]
      (cond
        (zero? m) (inc n)
        (zero? n) (cs-memo [m n] nil (ack (dec m) 1))
        :else     (cs-memo [m n]
                           (fn [a b] (cs-memo [a b] nil (ack a b)))
                           (dec m)
                           (ack m (dec n)))))

    (is (= 65533  (eval-cs ack 4 1)))
    (is (= 131069 (eval-cs ack 3 14))))


  (testing "'quick' sort )))) - via custom stack!"

    (defn qs [l]
      (if (empty? l)
        '()
        (let [[p & xs] l]
          (concat
           (qs (filter #(< % p) xs))
           (list p)
           (qs (filter #(>= % p) xs))))))

    (is-SO? qs (->> 10000 range reverse))

    (defn qs [l]
      (if (empty? l)
        []
        (let [[p & xs] l]
          (cs
           (fn [x y z] (vec (concat x y z))) ;; concat
           (qs (filter #(< % p) xs))
           (list p)
           (qs (filter #(>= % p) xs))))))

    ;; true without SO, but runs about 1 minute!
    ;; (is (= (vec (range 10000)) (eval-cs qs (->> 10000 range reverse))))
    )

  ;;
  )

(comment
  (run-tests)
  )
