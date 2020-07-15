"""Custom stack core."""

######################################################################################
# bare-bones trampoline
######################################################################################

class TR:
  def __init__(self, fn): self.fn = fn

def evalTR(tr):
  while isinstance(tr, TR): tr = tr.fn()
  return tr

######################################################################################
# non-tail recursion: custom-stack evaluation with optional memoization
######################################################################################

class CS:
  def __init__(self, fold = None, args = None, memo = None):
    self.fold = fold
    self.args = args if isinstance(args, list) else [args]
    self.memo = memo
    self.i = 0

def evalCS(cs):
  if (not isinstance(cs, CS)): return cs

  m = {}
  s = [cs]
  while (True):
    h = s[-1]

    if (h.i < len(h.args)):
      v = h.args[h.i]
      if callable(v): v = v()
      if (isinstance(v, CS) and not v.memo is None and v.memo in m): v = m[v.memo]
      if isinstance(v, CS):
        s.append(v)
      else:
        h.args[h.i] = v
        h.i += 1
    else:
      h = s.pop()
      r = h.args[0] if h.fold is None else h.fold(*h.args)
      if isinstance(r, CS):
        s.append(r)
      else:
        if (not h.memo is None): m[h.memo] = r
        if (not s):
          return r
        else:
          h = s[-1]
          h.args[h.i] = r
          h.i += 1
