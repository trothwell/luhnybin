package net.trothwell.masker;

abstract class CCBuffer {
  private enum State {
    CC_DIGIT, CC_GAP, DATA,
  }

  private static final int BUFF_LEN = 1024;
  private static final int CC_BUFF_LEN = 1024;
  private static final int[] LUHN_DOUBLED = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
  private static final int MAX_CC_LEN = 16;
  private static final int MIN_CC_LEN = 14;

  private static void debug(String msg) {
    System.err.println(msg);
  }

  private final char[] buf;
  private int bufNextIdx;
  private final int[] ccDigits;
  private int ccLength;
  private int ccNextIdx;
  private final int[] ccOffsets;
  private boolean ccSoftMatch;
  private int ccSoftMatchLen;
  private int ccSoftMatchStartIdx;
  private State state;

  CCBuffer() {
    this.state = State.DATA;
    this.buf = new char[BUFF_LEN];
    this.bufNextIdx = 0;
    this.ccDigits = new int[CC_BUFF_LEN];
    this.ccOffsets = new int[CC_BUFF_LEN];
    this.ccNextIdx = 0;
    this.ccSoftMatch = false;
  }

  public void append(char c) {
    buf[bufNextIdx] = c;
    bufNextIdx++;
    switch (state) {
      case DATA:
        switch (c) {
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            state = State.CC_DIGIT;
            addDigit(c - 0x30);
            break;
          default:
        }
        break;
      case CC_GAP:
        switch (c) {
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            state = State.CC_DIGIT;
            addDigit(c - 0x30);
            break;
          default:
            reset();
        }
        break;
      case CC_DIGIT:
        switch (c) {
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            addDigit(c - 0x30);
            break;
          // Gaps-separators
          case ' ':
          case '-':
            state = State.CC_GAP;
            break;
          default:
            reset();
        }
        break;
      default:
        throw new IllegalStateException();
    }
  }

  public void append(CharSequence chars) {
    for (int i = 0, len = chars.length(); i < len; i++) {
      append(chars.charAt(i));
    }
  }

  public void close() {
    reset();
  }

  abstract void onCCData(char[] buf, int offset, int len);

  abstract void onData(char[] buf, int offset, int len);

  private void addDigit(int digit) {
    int idx = ccNextIdx++ % CC_BUFF_LEN;
    ccLength++;
    ccDigits[idx] = digit;
    ccOffsets[idx] = bufNextIdx - 1;
    if (ccLength < MIN_CC_LEN) {
      // nothing
    } else {
      int max = ccLength > MAX_CC_LEN ? MAX_CC_LEN : ccLength;
      boolean matchFoundOnDigit = false;
      for (int len = max; len >= MIN_CC_LEN; len--) {
        if (checkLuhn(len)) {
          ccSoftMatchStartIdx = (ccNextIdx - len) % CC_BUFF_LEN;
          ccSoftMatchLen = len;
          ccSoftMatch = true;
          debug("Found soft match: idx=" + ccSoftMatchStartIdx + ", len=" + ccSoftMatchLen
              + ", max=" + max);
          matchFoundOnDigit = true;
          break;
          // FIXME: maximize possible matches
        }
      }
      if (ccSoftMatch && !matchFoundOnDigit) {
        reset();
      }
    }
  }

  private boolean checkLuhn(int len) {
    debug("> LEN=" + len + "(" + (bufNextIdx - 1) + ")");
    int mod10 = 0;
    final int doubleIdx = len % 2 == 0 ? 0 : 1;
    int startIdx = (ccNextIdx - len) % CC_BUFF_LEN;
    for (int i = 0; i < len; i++) {
      int idx = (startIdx + i) % CC_BUFF_LEN;
      int digit = ccDigits[idx];
      if (i % 2 == doubleIdx) {
        mod10 = (mod10 + LUHN_DOUBLED[digit]) % 10;
      } else {
        mod10 = (mod10 + digit) % 10;
      }
    }
    return mod10 == 0;
  }

  private void reset() {
    switch (state) {
      case CC_DIGIT:
      case CC_GAP:
        if (ccSoftMatch) {
          int startIdx = ccSoftMatchStartIdx;
          int endIdx = (ccSoftMatchStartIdx + ccSoftMatchLen - 1) % CC_BUFF_LEN;
          int startOffset = ccOffsets[startIdx];
          int endOffset = ccOffsets[endIdx];
          if (startOffset > 0) {
            onData(buf, 0, startOffset);
          }
          onCCData(buf, startOffset, endOffset - startOffset + 1);
          int trailingDataStartIdx = endOffset + 1;
          if (bufNextIdx > trailingDataStartIdx) {
            onData(buf, trailingDataStartIdx, bufNextIdx - trailingDataStartIdx);
          }
          bufNextIdx = 0;
          ccNextIdx = 0;
          ccLength = 0;
          ccSoftMatch = false;
        }
        break;
      default:
    }

    if (bufNextIdx > 0) {
      onData(buf, 0, bufNextIdx);
      bufNextIdx = 0;
      ccNextIdx = 0;
      ccLength = 0;
    }

    state = State.DATA;
  }
}
