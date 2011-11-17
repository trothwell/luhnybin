package net.trothwell.masker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import net.trothwell.masker.LuhnUtil.LuhnMatch;

import org.junit.Test;

public class UTLuhnUtil {
  @Test
  public void testCheck01() {
    List<String> validNums = Arrays.asList("4111 1111 1111 1111", "18", "4242 4242 4242 4242");
    for (String validNum : validNums) {
      int[] digits = LuhnUtil.split(validNum);
      assertTrue(validNum, LuhnUtil.checkLuhn(digits, 0, digits.length));
    }
  }

  @Test
  public void testInValid01() {
    for (int i = 2; i < 100; i++) {
      int[] digits = LuhnUtil.createInvalid(i, 2, 2);
      try {
        LuhnMatch m = LuhnUtil.searchLuhn(digits, 0, digits.length, 2, 2);
        fail(LuhnUtil.toString(digits, 0, digits.length) + " " + m);
      } catch (NotFoundException e) {
        // expected
      }
    }
  }

  @Test
  public void testInValid02() {
    for (int i = 14; i < 100; i++) {
      int[] digits = LuhnUtil.createInvalid(i, 14, 16);
      try {
        LuhnMatch m = LuhnUtil.searchLuhn(digits, 0, digits.length, 14, 16);
        fail(LuhnUtil.toString(digits, 0, digits.length) + " " + m);
      } catch (NotFoundException e) {
        // expected
      }
    }
  }

  @Test
  public void testNextValid01() {
    int[] digits = LuhnUtil.split("4111 1111 1111 111");
    int next16 = LuhnUtil.nextValidDigit(digits, 0, 15);
    assertEquals(1, next16);

    int next15 = LuhnUtil.nextValidDigit(digits, 0, 14);
    assertEquals(6, next15);

    int next14 = LuhnUtil.nextValidDigit(digits, 0, 13);
    assertEquals(4, next14);
  }

  @Test
  public void testNextValid02() {
    int[] digits = LuhnUtil.split("567");
    int next4 = LuhnUtil.nextValidDigit(digits, 0, 3);
    assertEquals(8, next4);
  }

  @Test
  public void testNextValid03() {
    int[] digits = LuhnUtil.split("3");
    int next2 = LuhnUtil.nextValidDigit(digits, 0, 1);
    assertEquals(4, next2);
  }

  @Test
  public void testSearch01() throws NotFoundException {
    int[] digits = LuhnUtil.split("18");
    assertTrue(LuhnUtil.checkLuhn(digits, 0, digits.length));
    LuhnMatch m = LuhnUtil.searchLuhn(digits, 0, digits.length, 2, 2);
    assertNotNull(m);
    assertEquals(2, m.len);
    assertEquals(0, m.offset);
  }

  @Test
  public void testSearch02() throws NotFoundException {
    int[] digits = LuhnUtil.split("11" + "18" + "11");
    assertFalse(LuhnUtil.checkLuhn(digits, 0, digits.length));
    assertTrue(LuhnUtil.checkLuhn(digits, 2, 2));
    LuhnMatch m = LuhnUtil.searchLuhn(digits, 0, digits.length, 2, 2);
    assertNotNull(m);
    assertEquals(2, m.len);
    assertEquals(2, m.offset);
  }

  @Test
  public void testSplit() {
    int[] digits = LuhnUtil.split("4111 1111 1111 111");
    int[] expected = {4, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    assertTrue(Arrays.equals(expected, digits));
  }

  @Test
  public void testToString() {
    int[] digits = LuhnUtil.split("4111-1111-1111-111");
    String actual = LuhnUtil.toString(digits, 0, digits.length);
    String expected = "4111 1111 1111 111";
    assertEquals(expected, actual);
  }
}
