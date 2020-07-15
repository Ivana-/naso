(ns examples
  (:require [core :refer :all]))

(comment
  "Liscript"

  "tail recursion"
  (defn foo (n a) cond (= 0 n) a (foo (- n 1) (+ n a)))
  (foo 10000 0)
  (tray foo 10 0)

  "mutual tail call"
  (defn is-even? (n) cond (= 0 n) true  (is-odd?  (- n 1)))
  (defn is-odd?  (n) cond (= 0 n) false (is-even? (- n 1)))
  (is-even? 1000000)
  (tray is-even? 10)

  "non-tail recursion"
  (defn bar (n) cond (= 0 n) 0 (+ n (bar (- n 1))))
  (bar 10000)
  (tray bar 10)
;;
  )

(comment


  "tail recursion - recur or trampoline"

  (defn foo [n a] (if (= 0 n) a (foo (- n 1) (+ n a))))
  (foo 10000 0)

  ;; tail recursion - recur...
  (defn foo [n a] (if (= 0 n) a (recur (- n 1) (+ n a))))
  (foo 10000 0)

  ;; ... or trampoline
  (defn foo [n a] (if (= 0 n) a (tr foo (- n 1) (+ n a))))
  (eval-tr foo 10000 0)


  "mutual tail call - trampoline"

  (declare is-odd?)
  (defn is-even? [n] (if (= 0 n) true  (is-odd?  (- n 1))))
  (defn is-odd?  [n] (if (= 0 n) false (is-even? (- n 1))))
  (is-even? 1000000)

  ;; tail call - trampoline
  (declare is-odd?)
  (defn is-even? [n] (if (= 0 n) true  (tr is-odd?  (- n 1))))
  (defn is-odd?  [n] (if (= 0 n) false (tr is-even? (- n 1))))
  (eval-tr is-even? 1000000)


  "eval-tr/eval-cs transparency"

  (eval-tr inc 1)
  (eval-tr + 1 2 3)
  (eval-cs inc 1)
  (eval-cs + 1 2 3)


  "non-tail recursion - via custom stack!"

  (defn bar [n] (if (= 0 n) 0 (+ n (bar (- n 1)))))
  (bar 10000)

    ;; non-tail call - via custom stack!
  (defn bar [n] (if (= 0 n) 0 (cs + n (bar (- n 1)))))
  (eval-cs bar 10000)


  "tail recursion - also via custom stack!"

  (defn foo [n a] (if (= 0 n) a (cs nil (foo (- n 1) (+ n a)))))
  (eval-cs foo 10000 0)


  "mutual tail call - also via custom stack!"

  (declare is-odd?)
  (defn is-even? [n] (if (= 0 n) true  (cs nil (is-odd?  (- n 1)))))
  (defn is-odd?  [n] (if (= 0 n) false (cs nil (is-even? (- n 1)))))
  (eval-cs is-even? 1000000)


  "non-tail recursion with memoization - via custom stack!"

  ;; fibbonacci
  (defn fib [n] (if (< n 2) n (cs-memo n + (fib (- n 1)) (fib (- n 2)))))
  (eval-cs fib 10)
  (eval-cs fib 30)
  (eval-cs fib 50)

  ;; coin change
  (defn coin-change [s coins]
    (cond (= s 0) 1
          (or (< s 0) (empty? coins)) 0
          :else (cs-memo [s coins]
                         +
                         (coin-change s (rest coins))
                         (coin-change (- s (first coins)) coins))))

  (def coins-list '(1 5 10 25 50))
  (eval-cs coin-change 100    coins-list)
  (eval-cs coin-change 10000  coins-list)

  ;; skyscrappers height
  (defn height [n m]
    (if (or (<= m 0) (<= n 0))
      0
      (cs-memo [n m] + 1 (height n (- m 1)) (height (- n 1) (- m 1)))))

  (eval-cs height 2 14)
  (eval-cs height 5 3000)


  "non-primitive non-tail recursion with memoization - via custom stack!"

    ;; Ackermann
  (defn ack [m n]
    (cond
      (zero? m) (inc n)
      (zero? n) (ack (dec m) 1)
      :else     (ack (dec m) (ack m (dec n)))))

  (ack 4 1)

  (defn ack [m n]
    (cond
      (zero? m) (inc n)
      (zero? n) (cs-memo [m n] nil (ack (dec m) 1))
      :else     (cs-memo [m n]
                         (fn [a b] (cs-memo [a b] nil (ack a b)))
                         (dec m)
                         (ack m (dec n)))))

  (eval-cs ack 4 1)
  (eval-cs ack 3 14)


  "'quick' sort )))) - via custom stack!"

  (defn qs [l]
    (if (empty? l)
      '()
      (let [[p & xs] l]
        (concat
         (qs (filter #(< % p) xs))
         (list p)
         (qs (filter #(>= % p) xs))))))

  (qs (->> 10000 range reverse))

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

  ;;
  )

(comment
  (run-tests)
  )
