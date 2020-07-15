"""Custom stack examples."""

from core import TR, evalTR, CS, evalCS

######################################################################################
# trampoline
######################################################################################

print("trampoline ------------------------------------------------------")

# sum from 1 to n

def foo(n, a): return a if (0 == n) else TR(lambda: foo(n-1, n+a))
print("foo(10000, 0)", evalTR(foo(10000, 0)))

# even / odd

def isEven(n): return True if (n == 0) else TR(lambda: isOdd(n-1))
def isOdd(n): return False if (n == 0) else TR(lambda: isEven(n-1))

print("isEven(10000)", evalTR(isEven(10000)))
print("isEven(10001)", evalTR(isEven(10001)))

######################################################################################
# custom stack
######################################################################################

print("custom stack ----------------------------------------------------")

# fibonacci

def fib(n): return n if (n < 2) else fib(n-1) + fib(n-2)

def fib(n): return n if (n < 2) else CS(fold = lambda x, y: x+y,
                                        args = [lambda: fib(n-1), lambda: fib(n-2)],
                                        memo = n)
print("fib(50)", evalCS(fib(50)))

# even / odd

def isEven(n): return True if (n == 0) else CS(args = lambda: isOdd(n-1))
def isOdd(n): return False if (n == 0) else CS(args = lambda: isEven(n-1))

print("isEven(10000)", evalCS(isEven(10000)))
print("isEven(10001)", evalCS(isEven(10001)))

# coin change

def cc(s, coins):
  memo = {}

  def go(s, i):
    k = (s, i)

    if (s == 0): return 1
    elif (s < 0 or i >= len(coins)): return 0
    elif (k in memo): return memo[k]
    else:
      r = go(s, i+1) + go(s-coins[i], i)
      memo[k] = r
      return r
  return go(s, 0)

def ccCS(s, coins):
  def go(s, i):
    if (s == 0): return 1
    elif (s < 0 or i >= len(coins)): return 0
    else:
      return CS(fold = lambda x, y: x+y,
                args = [lambda: go(s, i+1), lambda: go(s-coins[i], i)],
                memo = (s, i))
  return go(s, 0)

print("cc(100, [1, 5, 10, 25, 50]):")
print("system stack:", cc (100, [1, 5, 10, 25, 50]))
print("custom stack:", evalCS(ccCS(100, [1, 5, 10, 25, 50])))
print("cc(10000,  [1, 5, 10, 25, 50])", evalCS(ccCS(10000, [1, 5, 10, 25, 50])))
print("cc(100000, [1, 5, 10, 25, 50])", evalCS(ccCS(100000, [1, 5, 10, 25, 50])))

# Ackermann

def ack(m, n):
  if   (0 == m): return n + 1
  elif (0 == n): return ack(m-1, 1)
  else:          return ack(m-1, ack(m, n-1))

def ackCS(m, n):
  if   (0 == m): return n + 1
  elif (0 == n): return CS(args = lambda: ackCS(m-1, 1), memo = (m, n))
  else:          return CS(fold = lambda a, b: CS(args = lambda: ackCS(a, b), memo = (a, b)),
                           args = [m-1, lambda: ackCS(m, n-1)],
                           memo = (m, n))

print("ack  (3, 5)", ack(3, 5)) # 253
print("ackCS(3, 5)", evalCS(ackCS(3, 5))) # 253

# print("ack(4, 1)", ack(4, 1)) # SO
print("ackCS(4, 1)", evalCS(ackCS(4, 1))) # 65533

# print("ack(3, 14)", ack(3, 14)) # SO
print("ackCS(3, 14)", evalCS(ackCS(3, 14))) # 131069

