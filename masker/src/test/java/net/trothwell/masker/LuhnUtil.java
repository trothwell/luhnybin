package net.trothwell.masker;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;



class LuhnUtil {
  static class LuhnMatch {
    final int len;
    final int offset;

    LuhnMatch(int offset, int len) {
      this.offset = offset;
      this.len = len;
    }

    @Override
    public String toString() {
      return String.format("LuhnMatch [len=%s, offset=%s]", len, offset);
    }
  }

  private static final int[] LUHN_DOUBLE = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};

  private static final Random random = new Random(0x34343434L);

  public static String toString(int[] buf) {
    return toString(buf, 0, buf.length);
  }

  static boolean checkLuhn(int[] buf, int offset, int len) {
    if (len + offset > buf.length) {
      throw new ArrayIndexOutOfBoundsException("Array length=" + buf.length + ", offset=" + offset
          + ", len=" + len);
    }
    int mod10 = 0;
    final int doubleIdx = len % 2 == 0 ? 0 : 1;
    for (int i = 0; i < len; i++) {
      int idx = offset + i;
      if (i % 2 == doubleIdx) {
        mod10 = (mod10 + LUHN_DOUBLE[buf[idx]]) % 10;
      } else {
        mod10 = (mod10 + buf[idx]) % 10;
      }
    }
    return mod10 == 0;
  }

  static int[] createInvalid(int len, int validateMinLen, int validateMaxLen) {
    if (len < 1) {
      throw new IllegalArgumentException("Length must be > 0; len=" + len);
    } else if (validateMaxLen - validateMinLen > 7) {
      throw new IllegalArgumentException("Cannot exceed validation length of 9");
    }
    int[] retval = new int[len];
    Set<Integer> avoid = new HashSet<Integer>();
    for (int end = 0; end < len; end++) {
      debug(String.format("INV: len=%s, min=%s, max=%s, i=%s", len, validateMinLen, validateMaxLen,
          end));
      if (end + 1 < validateMinLen) {
        retval[end] = random.nextInt(9);
        debug(String.format("INV: end=%s, added=%s", end, retval[end]));
      } else {
        avoid.clear();
        int maxLen = end + 1;
        if (maxLen > validateMaxLen) {
          maxLen = validateMaxLen;
        }
        for (int l = validateMinLen; l <= maxLen; l++) {
          int digitsOffset = end - l + 1;
          int digitsLen = l - 1;
          debug(String.format("INV-NEXT: end=%s, l=%s, dOff=%s, dLen=%s", end, l, digitsOffset,
              digitsLen));
          int avoidDigit = nextValidDigit(retval, digitsOffset, digitsLen);
          debug(String.format("INV: Avoiding digit: %s", avoidDigit));
          avoid.add(avoidDigit);
        }
        for (;;) {
          int digit = random.nextInt(9);
          if (avoid.contains(digit)) {
            debug(String.format("INV: Skipping digit: %s", digit));
            continue;
          } else {
            retval[end] = digit;
            break;
          }
        }
      }
    }
    return retval;
  }

  static int nextValidDigit(int[] buf, int offset, int len) {
    if (len + offset > buf.length) {
      throw new ArrayIndexOutOfBoundsException("Array length=" + buf.length + ", offset=" + offset
          + ", len=" + len);
    }
    int mod10 = 0;
    final int doubleIdx = len % 2 == 0 ? 1 : 0;
    for (int i = 0; i < len; i++) {
      int idx = offset + i;
      if (i % 2 == doubleIdx) {
        mod10 = (mod10 + LUHN_DOUBLE[buf[idx]]) % 10;
      } else {
        mod10 = (mod10 + buf[idx]) % 10;
      }
      debug(String.format("i=%s, digit=%s, mod10=%s", idx, buf[idx], mod10));
    }
    return (10 - mod10) % 10;
  }

  static LuhnMatch searchLuhn(int[] buf, int offset, int len, int matchMinLen, int matchMaxLen)
      throws NotFoundException {
    if (len + offset > buf.length) {
      throw new ArrayIndexOutOfBoundsException();
    } else if (len < matchMinLen) {
      throw new IllegalArgumentException("Buffer length is less that min match length.");
    }

    if (len < matchMaxLen) {
      debug(String.format("Short match: len=%s, min=%s, max=%s", len, matchMinLen, matchMaxLen));
      for (int start = 0, end = len - matchMinLen; start <= end; start++) {
        debug(String.format("start=%s, end=%s", start, end));
        for (int searchLen = len - matchMinLen; searchLen >= matchMinLen; searchLen--) {
          int cOffset = offset + start;
          debug(String.format("searchLen=%s, cOffset=%s", searchLen, cOffset));
          if (checkLuhn(buf, cOffset, searchLen)) {
            return new LuhnMatch(cOffset, searchLen);
          }
        }
      }
    } else {
      debug(String.format("Normal match: len=%s, min=%s, max=%s", len, matchMinLen, matchMaxLen));
      for (int start = 0, end = len - matchMaxLen; start <= end; start++) {
        debug(String.format("BEGIN: start=%s, end=%s", start, end));
        for (int searchLen = matchMaxLen; searchLen >= matchMinLen; searchLen--) {
          debug(String.format("searchLen=%s", searchLen));
          if (checkLuhn(buf, offset + start, searchLen)) {
            return new LuhnMatch(offset + start, searchLen);
          }
        }
      }

      for (int start = len - matchMaxLen, end = len - matchMinLen; start <= end; start++) {
        debug(String.format("END: start=%s, end=%s", start, end));
        for (int searchLen = end - start; searchLen >= matchMinLen; searchLen--) {
          debug(String.format("searchLen=%s", searchLen));
          if (checkLuhn(buf, offset + start, searchLen)) {
            return new LuhnMatch(offset + start, searchLen);
          }
        }
      }
    }
    throw new NotFoundException();
  }

  static int[] split(String digits) {
    String clean = digits.replaceAll("[^\\d]+", "");
    int[] retval = new int[clean.length()];
    for (int i = 0; i < clean.length(); i++) {
      retval[i] = clean.charAt(i) - 0x30;
    }
    return retval;
  }

  static String toString(int[] buf, int offset, int len) {
    if (len + offset > buf.length) {
      throw new ArrayIndexOutOfBoundsException();
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      char c = (char) (0x30 + buf[offset + i]);
      if (i > 0 && i % 4 == 0) {
        sb.append(" ");
      }
      sb.append(c);
    }
    return sb.toString();
  }

  private static void debug(String msg) {
    // System.err.println(msg);
  }
}
