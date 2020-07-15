# NASO (never again stack overflow)

Any function custom stack evaluator with optional memoization.

## Setup

Just copy `core.xxx` file and require it in your project (see `examples.xxx`, `test.xxx`).

## Usage

* [Clojure](#clojure)
* [Python](#python)

## Clojure

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
but how about non-primitive recursions? Ackermann is on the stage!
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

## Python

tail recursion
```
>>> def foo(n, a): return a if (0 == n) else foo(n-1, n+a)
...
>>> foo(10000, 0)
Traceback (most recent call last):
  File "<stdin>", line 1, in <module>
.........
RecursionError: maximum recursion depth exceeded in comparison
```
oh, but we have while!
```
>>> def foo(n):
...   r = 0
...   while (n>0):
...     r += n
...     n -= 1
...   return r
...
>>> foo(10000)
50005000
```
and we also have trampoline!
```
>>> def foo(n, a): return a if (0 == n) else TR(lambda: foo(n-1, n+a))
...
>>> evalTR(foo(10000, 0))
50005000
```

mutual tail call (non-tail recurtion)
```
>>> def isEven(n): return True if (n == 0) else isOdd(n-1)
...
>>> def isOdd(n): return False if (n == 0) else isEven(n-1)
...
>>> isEven(10000)
Traceback (most recent call last):
  File "<stdin>", line 1, in <module>
.........
RecursionError: maximum recursion depth exceeded in comparison
```
oh, but we sill have trampoline!
```
>>> def isEven(n): return True if (n == 0) else TR(lambda: isOdd(n-1))
...
>>> def isOdd(n): return False if (n == 0) else TR(lambda: isEven(n-1))
...
>>> evalTR(isEven(10000))
True
```

non-tail recursion
```
>>> def bar(n): return 0 if (0 == n) else n + bar(n-1)
...
>>> bar(10000)
Traceback (most recent call last):
  File "<stdin>", line 1, in <module>
.........
RecursionError: maximum recursion depth exceeded in comparison
```
but we have custom-stack evaluator!
```
>>> def bar(n): return 0 if (0 == n) else CS(fold = lambda v: n+v, args = lambda: bar(n-1))
...
>>> evalCS(bar(10000))
50005000
```
and we can use it for all previous cases (but can also use recur/trampoline of course, and it will be faster)
```
>>> def foo(n, a): return a if (0 == n) else CS(args = lambda: foo(n-1, n+a))
...
>>> evalCS(foo(10000, 0))
50005000
>>> def isEven(n): return True if (n == 0) else CS(args = lambda: isOdd(n-1))
...
>>> def isOdd(n): return False if (n == 0) else CS(args = lambda: isEven(n-1))
...
>>> evalCS(isEven(10000))
True
```

it has an optional built-in memoization!
```
>>> def fib(n): return n if (n < 2) else CS(fold = lambda x, y: x+y,
...                                         args = [lambda: fib(n-1), lambda: fib(n-2)],
...                                         memo = n)
...
>>> evalCS(fib(50))
12586269025
```
of course, that was not a StackOverflow case, but what about this?
```
>>> def cc(s, coins):
...   def go(s, i):
...     if (s == 0): return 1
...     elif (s < 0 or i >= len(coins)): return 0
...     else: return go(s, i+1) + go(s-coins[i], i)
...   return go(s, 0)
...
>>> cc(10000, [1, 333])
Traceback (most recent call last):
  File "<stdin>", line 1, in <module>
.........
RecursionError: maximum recursion depth exceeded in comparison
>>>
```
oh, lets add a cs!
```
>>> def cc(s, coins):
...   def go(s, i):
...     if (s == 0): return 1
...     elif (s < 0 or i >= len(coins)): return 0
...     else:
...       return CS(fold = lambda x, y: x+y,
...                 args = [lambda: go(s, i+1), lambda: go(s-coins[i], i)])
...   return go(s, 0)
...
>>> evalCS(cc(10000, [1, 333]))
31
```
goood, but
```
evalCS(cc(10000, [1, 5, 10, 25, 50]))
```
never ends!
lets add a memo
```
>>> def cc(s, coins):
...   def go(s, i):
...     if (s == 0): return 1
...     elif (s < 0 or i >= len(coins)): return 0
...     else:
...       return CS(fold = lambda x, y: x+y,
...                 args = [lambda: go(s, i+1), lambda: go(s-coins[i], i)],
...                 memo = (s, i))
...   return go(s, 0)
...
>>> evalCS(cc(10000, [1, 5, 10, 25, 50]))
6794128501
```
cool! lets play a little
```
>>> def height(n, m):
...   return 0 if (m <= 0 or n <= 0) else CS(fold = lambda x, y: x+y+1,
...                                          args = [lambda: height(n, m-1), lambda: height(n-1, m-1)],
...                                          memo = (n, m))
...
>>> evalCS(height(5, 3000))
2021630625377350
```
we avoid as SO as endless time evaluation! not bad, I think.
but how about non-primitive recursions? Ackermann is on the stage!
```
>>> def ack(m, n):
...   if   (0 == m): return n + 1
...   elif (0 == n): return ack(m-1, 1)
...   else:          return ack(m-1, ack(m, n-1))
...
>>> ack(4, 1)
Traceback (most recent call last):
  File "<stdin>", line 1, in <module>
.........
RecursionError: maximum recursion depth exceeded in comparison
```
let wrap it in cs-memo (in a smart way)
```
>>> def ackCS(m, n):
...   if   (0 == m): return n + 1
...   elif (0 == n): return CS(args = lambda: ackCS(m-1, 1), memo = (m, n))
...   else:          return CS(fold = lambda a, b: CS(args = lambda: ackCS(a, b), memo = (a, b)),
...                            args = [m-1, lambda: ackCS(m, n-1)],
...                            memo = (m, n))
...
>>> evalCS(ackCS(4, 1))
65533
>>> evalCS(ackCS(3, 14))
131069
```
not bad again, I think! :)






## Warnings

- you can use custom stack evaluation (with or without optional memoization) on tail call cases - it will work, but slower a bit, and use memory for saving temporary results without rational reason.

- you can add the functions itself to memo-key vector (in a first argument of `cs-memo`) if you have a calls of nested functions, evaluated also via custom stack and have to be memoized, in your top-level function. This will split memoization of differrent functions on same arguments. OR you can wrap all such internal calls in `eval-cs` to evaluate them separately. Simply speaking, avoid memoization (in `cs-memo` macro) of differrent functions on the same arguments by any way :) In a simple cases (like shown above) you can memoize by only arguments - we have only one function to memoize.

## License

Copyright © 2019

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
