package net.trothwell.masker;

abstract class CCBuffer1 {
  private enum State {
    CC_DIGIT, CC_GAP, DATA,
  }

  private static final int BUFF_LEN = 1024;
  private static final int[] LUHN_DOUBLED = {0, 2, 6, 8, 1, 3, 5, 7, 9};
  private static final int MAX_CC_LEN = 16;
  private static final int MIN_CC_LEN = 14;

  private final char[] buf;
  private final int[] ccDigits;
  private final int[] ccEvenSums;
  private int ccFirstIdx;
  private int ccLength;
  private boolean ccLuhnMatch;
  private int ccLuhnMatchIdx;
  private int ccLuhnMatchLength;
  private int ccNextIdx;
  private final int[] ccOddSums;
  private final int[] ccOffsets;
  private int nextIdx;
  private State state;

  CCBuffer1() {
    this.state = State.DATA;

    this.buf = new char[BUFF_LEN];
    this.nextIdx = 0;

    this.ccNextIdx = 0;
    this.ccLength = 0;
    this.ccOffsets = new int[MAX_CC_LEN];
    this.ccEvenSums = new int[MAX_CC_LEN];
    this.ccOddSums = new int[MAX_CC_LEN];
    this.ccDigits = new int[MAX_CC_LEN];
    this.ccLuhnMatch = false;
    this.ccLuhnMatchIdx = 0;
    this.ccLuhnMatchLength = 0;
  }

  public void append(char c) {
    buf[nextIdx] = c;
    nextIdx++;
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
            if (ccLuhnMatch) {
              dumpLuhnMatch();
            }
            state = State.DATA;
            ccFirstIdx = ccNextIdx;
            ccLength = 0;
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
            state = State.CC_DIGIT;
            addDigit(c - 0x30);
            break;
          case ' ':
          case '-':
            state = State.CC_GAP;
            break;
          default:
            if (ccLuhnMatch) {
              dumpLuhnMatch();
            }
            state = State.DATA;
            ccFirstIdx = ccNextIdx;
            ccLength = 0;
        }
        break;
      default:
        throw new IllegalStateException();
    }
    if (nextIdx >= buf.length) {
      dumpData();
    }
  }

  public void append(CharSequence chars) {
    for (int i = 0, len = chars.length(); i < len; i++) {
      append(chars.charAt(i));
    }
  }

  public void close() {
    state = State.DATA;
    dumpData();
  }

  abstract void onCCData(char[] buf, int offset, int len);

  abstract void onData(char[] buf, int offset, int len);

  private void addDigit(int digit) {
    ccLength += 1;
    final int ccIdx = ccNextIdx;
    ccNextIdx = wrapCCIdx(ccNextIdx + 1);
    if (ccLength >= MAX_CC_LEN) {
      ccLength = MAX_CC_LEN;
      final int evenSumDiff;
      final int oddSumDiff;
      if (ccLength % 2 == 0) { // even
        evenSumDiff = LUHN_DOUBLED[digit] - LUHN_DOUBLED[ccDigits[ccNextIdx]];
        oddSumDiff = digit - ccDigits[ccNextIdx];
      } else { // odd
        evenSumDiff = digit - ccDigits[ccNextIdx];
        oddSumDiff = LUHN_DOUBLED[digit] - LUHN_DOUBLED[ccDigits[ccNextIdx]];
      }
      this.ccEvenSums[ccIdx] += evenSumDiff;
      this.ccOddSums[ccIdx] += oddSumDiff;
    } else if (ccLength % 2 == 0) { // even
      this.ccEvenSums[ccIdx] = LUHN_DOUBLED[digit];
      this.ccOddSums[ccIdx] = digit;
    } else { // odd
      this.ccEvenSums[ccIdx] = digit;
      this.ccOddSums[ccIdx] = LUHN_DOUBLED[digit];
    }
    if (ccLength >= MIN_CC_LEN) {
      int maxLenOffset = ccLength - MIN_CC_LEN;
      // test for luhn matches
      for (int len = maxLenOffset; len >= 0; len--) {
        int sumIdx = wrapCCIdx(nextIdx - len);
        final int sum;
        if ((ccLength - len) % 2 == 0) { // event
          sum = ccEvenSums[sumIdx];
        } else { // odd
          sum = ccOddSums[sumIdx];
        }
        if (sum % 10 == 0) {
          if (ccLength >= MAX_CC_LEN) {
            // max length found--go with it
            int buffOffsetStart = ccOffsets[nextIdx];
            int buffOffsetEnd = ccOffsets[wrapCCIdx(nextIdx - 1)];
            onData(buf, 0, buffOffsetStart);
            onCCData(buf, buffOffsetStart, buffOffsetEnd - buffOffsetStart);
            nextIdx = 0;
            ccNextIdx = 0;
            ccLength = 0;
          } else {
            ccLuhnMatch = true;
            this.ccLuhnMatchIdx = sumIdx;
            this.ccLuhnMatchLength = ccLength - len;
          }
          break;
        } else if (ccLuhnMatch) {
          dumpLuhnMatch();
          break;
        }
      }
      if (ccLuhnMatch) {
        if (ccLength - ccLuhnMatchLength > 1) {
          dumpLuhnMatch();
        }
      }

    }

    this.ccDigits[ccIdx] = digit;
    this.ccOffsets[ccIdx] = nextIdx - 1;
  }

  private void dumpData() {
    switch (state) {
      case DATA:
        onData(buf, 0, nextIdx);
        nextIdx = 0;
        break;
      default:
        final int firstDigitIdx;
        if (ccLength >= MAX_CC_LEN) {
          firstDigitIdx = ccNextIdx;
        } else {
          firstDigitIdx = ccFirstIdx;
        }
        int firstDigitOffset = ccOffsets[firstDigitIdx];
        onData(buf, 0, firstDigitOffset);
        for (int i = 0; i < MAX_CC_LEN; i++) {
          ccOffsets[i] -= firstDigitOffset; // reducing all--even if unnecessary
        }
        System.arraycopy(buf, firstDigitOffset, buf, 0, nextIdx - firstDigitOffset);
        nextIdx -= firstDigitOffset;
    }
  }

  private void dumpLuhnMatch() {
    int buffOffsetStart = ccOffsets[wrapCCIdx(ccLuhnMatchIdx - ccLuhnMatchLength)];
    int buffOffsetEnd = ccOffsets[ccLuhnMatchIdx];
    onData(buf, 0, buffOffsetStart);
    onCCData(buf, buffOffsetStart, buffOffsetEnd - buffOffsetStart);
    System.arraycopy(buf, buffOffsetEnd, buf, 0, nextIdx - buffOffsetEnd);
    nextIdx -= buffOffsetEnd;
    ccNextIdx = 0;
    ccLength = 0;
    ccLuhnMatch = false;
  }

  private int wrapCCIdx(int idx) {
    if (idx < 0) {
      throw new IllegalArgumentException("Idx=" + idx);
    }
    return idx % MAX_CC_LEN;
  }
}
