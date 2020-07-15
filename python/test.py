"""Test."""

import unittest

from core import TR, evalTR, CS, evalCS

def isSO(f, *args):
  try:
    r = f(*args)
    return False
  except Exception:
    return True




class TestTR(unittest.TestCase):


  def test_evalTR_transparency(self):

    self.assertEqual(None, evalTR(None))
    self.assertEqual(1, evalTR(1))
    self.assertEqual([1, 2, 3], evalTR([1, 2, 3]))
    self.assertEqual(max, evalTR(max))
    self.assertEqual(map, evalTR(map))


  def test_tail_recursion(self):

    def foo(n, a): return a if (0 == n) else foo(n-1, n+a)
    self.assertEqual(5050, foo(100, 0))
    self.assertFalse(isSO(foo, 100, 0))
    self.assertTrue (isSO(foo, 10000, 0))

    def foo(n, a): return a if (0 == n) else TR(lambda: foo(n-1, n+a))
    self.assertEqual(5050, evalTR(foo(100, 0)))
    self.assertEqual(50005000, evalTR(foo(10000, 0)))


  def test_mutual_tail_call(self):

    def isEven(n): return True if (n == 0) else isOdd(n-1)
    def isOdd(n): return False if (n == 0) else isEven(n-1)
    self.assertTrue (isEven(100))
    self.assertFalse(isSO(isEven, 100))
    self.assertTrue (isSO(isEven, 10000))

    def isEven(n): return True if (n == 0) else TR(lambda: isOdd(n-1))
    def isOdd(n): return False if (n == 0) else TR(lambda: isEven(n-1))
    self.assertTrue (evalTR(isEven(10000)))
    self.assertFalse(evalTR(isEven(10001)))
    self.assertFalse(evalTR(isOdd(10000)))
    self.assertTrue (evalTR(isOdd(10001)))




class TestCS(unittest.TestCase):


  def test_evalCS_transparency(self):

    self.assertEqual(None, evalCS(None))
    self.assertEqual(1, evalCS(1))
    self.assertEqual([1, 2, 3], evalCS([1, 2, 3]))
    self.assertEqual(max, evalCS(max))
    self.assertEqual(map, evalCS(map))


  def test_tail_recursion(self):

    def foo(n, a): return a if (0 == n) else foo(n-1, n+a)
    self.assertEqual(5050, foo(100, 0))
    self.assertFalse(isSO(foo, 100, 0))
    self.assertTrue (isSO(foo, 10000, 0))

    def foo(n, a): return a if (0 == n) else CS(args = lambda: foo(n-1, n+a))
    self.assertEqual(5050, evalCS(foo(100, 0)))
    self.assertEqual(50005000, evalCS(foo(10000, 0)))


  def test_mutual_tail_call(self):

    def isEven(n): return True if (n == 0) else isOdd(n-1)
    def isOdd(n): return False if (n == 0) else isEven(n-1)
    self.assertTrue (isEven(100))
    self.assertFalse(isSO(isEven, 100))
    self.assertTrue (isSO(isEven, 10000))

    def isEven(n): return True if (n == 0) else CS(args = lambda: isOdd(n-1))
    def isOdd(n): return False if (n == 0) else CS(args = lambda: isEven(n-1))
    self.assertTrue (evalCS(isEven(10000)))
    self.assertFalse(evalCS(isEven(10001)))
    self.assertFalse(evalCS(isOdd(10000)))
    self.assertTrue (evalCS(isOdd(10001)))


  def test_non_tail_recursion(self):

    def bar(n): return 0 if (0 == n) else n + bar(n-1)
    self.assertEqual(5050, bar(100))
    self.assertFalse(isSO(bar, 100))
    self.assertTrue (isSO(bar, 10000))

    def bar(n): return 0 if (0 == n) else CS(fold = lambda v: n+v, args = lambda: bar(n-1))
    self.assertEqual(5050, evalCS(bar(100)))
    self.assertEqual(50005000, evalCS(bar(10000)))


  def test_non_tail_recursion_with_memo(self):

    def fib(n): return n if (n < 2) else CS(fold = lambda x, y: x+y,
                                            args = [lambda: fib(n-1), lambda: fib(n-2)],
                                            memo = n)
    self.assertEqual(55, evalCS(fib(10)))
    self.assertEqual(832040, evalCS(fib(30)))
    self.assertEqual(12586269025, evalCS(fib(50)))

    def cc(s, coins):
      def go(s, i):
        if (s == 0): return 1
        elif (s < 0 or i >= len(coins)): return 0
        else:
          return CS(fold = lambda x, y: x+y,
                    args = [lambda: go(s, i+1), lambda: go(s-coins[i], i)],
                    memo = (s, i))
      return go(s, 0)

    coins = [1, 5, 10, 25, 50]
    self.assertEqual(292, evalCS(cc(100, coins)))
    self.assertEqual(6794128501, evalCS(cc(10000, coins)))
    # self.assertEqual(66793412685001, evalCS(cc(100000, coins)))

    def height(n, m):
      return 0 if (m <= 0 or n <= 0) else CS(fold = lambda x, y: x+y+1,
                                             args = [lambda: height(n, m-1), lambda: height(n-1, m-1)],
                                             memo = (n, m))
    self.assertEqual(105, evalCS(height(2, 14)))
    self.assertEqual(2021630625377350, evalCS(height(5, 3000)))


  def test_non_primitive_non_tail_recursion_with_memo(self):

    def ack(m, n):
      if   (0 == m): return n + 1
      elif (0 == n): return ack(m-1, 1)
      else:          return ack(m-1, ack(m, n-1))
    self.assertEqual(253, ack(3, 5))
    self.assertFalse(isSO(ack, 3, 5))
    self.assertTrue (isSO(ack, 4, 1))

    def ack(m, n):
      if   (0 == m): return n + 1
      elif (0 == n): return CS(args = lambda: ack(m-1, 1), memo = (m, n))
      else:          return CS(fold = lambda a, b: CS(args = lambda: ack(a, b), memo = (a, b)),
                               args = [m-1, lambda: ack(m, n-1)],
                               memo = (m, n))
    self.assertEqual(253, evalCS(ack(3, 5)))
    self.assertEqual(65533, evalCS(ack(4, 1)))
    # self.assertEqual(131069, evalCS(ack(3, 14)))




if __name__ == '__main__':
    unittest.main()
