# NASO (never again stack overflow)

Any function custom stack evaluator with optional memoization.

## Setup

Just copy core.clj file and require it in your project :).

## Usage

tail recursion
```
test=> (defn foo [n a] (if (= 0 n) a (foo (- n 1) (+ n a))))
#'test/foo
test=> (foo 10000 0)
Execution error (StackOverflowError) at test/foo (REPL:1).
null
```
oh, but we have recur!
```
test=> (defn foo [n a] (if (= 0 n) a (recur (- n 1) (+ n a))))
#'test/foo
test=> (foo 10000 0)
50005000
```
and we also have trampoline!
```
test=> (defn foo [n a] (if (= 0 n) a (tr foo (- n 1) (+ n a))))
#'test/foo
test=> (eval-tr foo 10000 0)
50005000
```

mutual tail call (non-tail recurtion)
```
test=> (declare is-odd?)
#'test/is-odd?
test=> (defn is-even? [n] (if (= 0 n) true  (is-odd?  (- n 1))))
#'test/is-even?
test=> (defn is-odd?  [n] (if (= 0 n) false (is-even? (- n 1))))
#'test/is-odd?
test=> (is-even? 1000000)
Execution error (StackOverflowError) at test/is-even? (REPL:1).
null
```
oh, but we sill have trampoline!
```
test=> (defn is-even? [n] (if (= 0 n) true  (tr is-odd?  (- n 1))))
#'test/is-even?
test=> (defn is-odd?  [n] (if (= 0 n) false (tr is-even? (- n 1))))
#'test/is-odd?
test=> (eval-tr is-even? 1000000)
true
```

non-tail recursion
```
test=> (defn bar [n] (if (= 0 n) 0 (+ n (bar (- n 1)))))
#'test/bar
test=> (bar 10000)
Execution error (StackOverflowError) at test/bar (REPL:1).
null
```
but we have custom-stack evaluator!
```
test=> (defn bar [n] (if (= 0 n) 0 (cs + n (bar (- n 1)))))
#'test/bar
test=> (eval-cs bar 10000)
50005000
```
and we can use it for all previous cases (but can also use recur/trampoline of course, and it will be faster)
```
test=> (defn foo [n a] (if (= 0 n) a (cs nil (foo (- n 1) (+ n a)))))
#'test/foo
test=> (eval-cs foo 10000 0)
50005000
test=> (defn is-even? [n] (if (= 0 n) true  (cs nil (is-odd?  (- n 1)))))
#'test/is-even?
test=> (defn is-odd?  [n] (if (= 0 n) false (cs nil (is-even? (- n 1)))))
#'test/is-odd?
test=> (eval-cs is-even? 1000000)
true
```

it has an optional built-in memoization!
```
test=> (defn fib [n] (if (< n 2) n (cs-memo n + (fib (- n 1)) (fib (- n 2)))))
#'test/fib
test=> (eval-cs fib 50)
12586269025
```
of course, that was not a StackOverflow case, but what about this?
```
test=> (defn coin-change [s coins]
  #_=>       (cond (= s 0) 1
  #_=>             (or (< s 0) (empty? coins)) 0
  #_=>             :else (+
  #_=>                    (coin-change s (rest coins))
  #_=>                    (coin-change (- s (first coins)) coins))))
#'test/coin-change
test=> (coin-change 10000 '(1 333))
Execution error (StackOverflowError) at test/coin-change (REPL:3).
null
```
oh, lets add a cs!
```
test=> (defn coin-change [s coins]
  #_=>       (cond (= s 0) 1
  #_=>             (or (< s 0) (empty? coins)) 0
  #_=>             :else (cs +
  #_=>                    (coin-change s (rest coins))
  #_=>                    (coin-change (- s (first coins)) coins))))
#'test/coin-change
test=> (eval-cs coin-change 10000 '(1 333))
31
```
goood, but
```
(eval-cs coin-change 10000 '(1 5 10 25 50))
```
never ends!
lets add a memo
```
test=> (defn coin-change [s coins]
  #_=>       (cond (= s 0) 1
  #_=>             (or (< s 0) (empty? coins)) 0
  #_=>             :else (cs-memo [s coins]
  #_=>                            +
  #_=>                            (coin-change s (rest coins))
  #_=>                            (coin-change (- s (first coins)) coins))))
#'test/coin-change
test=> (eval-cs coin-change 10000 '(1 5 10 25 50))
6794128501
```
cool! lets play a little
```
test=> (defn height [n m]
  #_=>       (if (or (<= m 0) (<= n 0))
  #_=>         0
  #_=>         (cs-memo [n m] + 1 (height n (- m 1)) (height (- n 1) (- m 1)))))
#'test/height
test=> (eval-cs height 5 3000)
2021630625377350
```
we avoid as SO as endless time evaluation! not bad, I think.
but how abou non-primitive recursions? Ackermann is on the stage!
```
test=> (defn ack [m n]
  #_=>       (cond
  #_=>         (zero? m) (inc n)
  #_=>         (zero? n) (ack (dec m) 1)
  #_=>         :else     (ack (dec m) (ack m (dec n)))))
#'test/ack
test=> (ack 4 1)
Execution error (StackOverflowError) at test/ack (REPL:3).
null
```
let wrap it in cs-memo (in a smart way)
```
test=> (defn ack [m n]
  #_=>       (cond
  #_=>         (zero? m) (inc n)
  #_=>         (zero? n) (cs-memo [m n] nil (ack (dec m) 1))
  #_=>         :else     (cs-memo [m n]
  #_=>                            (fn [a b] (cs-memo [a b] nil (ack a b)))
  #_=>                            (dec m)
  #_=>                            (ack m (dec n)))))
#'test/ack
test=> (eval-cs ack 4 1)
65533
test=> (eval-cs ack 3 14)
131069
```
not bad again, I think! :)

See `test.clj`

## Warnings

- you can use custom stack evaluation (with or without optional memoization) on tail call cases - it will work, but slower a bit, and use memory for saving temporary results without rational reason.

- you can add the functions itself to memo-key vector (in a first argument of `cs-memo`) if you have a calls of nested functions, evaluated also via custom stack and have to be memoized, in your top-level function. This will split memoization of differrent functions on same arguments. OR you can wrap all such internal calls in `eval-cs` to evaluate them separately. Simply speaking, avoid memoization (in `cs-memo` macro) of differrent functions on the same arguments by any way :) In a simple cases (like shown above) you can memoize by only arguments - we have only one function to memoize.

## License

Copyright © 2019

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
