package net.trothwell.masker;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UTMasker {
  private static class TestValue {
    private final String expectedOutput;
    private final String input;

    private TestValue(String input, String expectedOutput) {
      this.input = input;
      this.expectedOutput = expectedOutput;
    }

    private void assertOutput(String actualOutput) {
      assertEquals(input, expectedOutput, actualOutput);
    }
  }

  private TestValue create(String input, String expectedOutput) {
    return new TestValue(input, expectedOutput);
  }

  @Test
  public void test() throws IOException {
    List<TestValue> values = new ArrayList<TestValue>();

    values.add(create("56613959932537\n", "XXXXXXXXXXXXXX\n"));
    values.add(create("LF only ->\n<- LF only\n", "LF only ->\n<- LF only\n"));
    values.add(create("56613959932535089 has too many digits.\n",
        "56613959932535089 has too many digits.\n"));

    for (int i = 0; i < values.size(); i++) {
      TestValue tv = values.get(i);
      System.out.println("Value [" + i + "]");
      Reader r = new StringReader(tv.input);
      StringWriter w = new StringWriter();
      Masker m = new MaskerImpl(r, w);
      m.mask();
      String out = w.getBuffer().toString();
      tv.assertOutput(out);
    }
  }
}
