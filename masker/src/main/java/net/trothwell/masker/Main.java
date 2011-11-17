package net.trothwell.masker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

public class Main {
  public static void main(String[] args) {
    Charset cs = Charset.forName("UTF-8");
    if (args.length == 1) {
      String charsetName = args[0];
      try {
        cs = Charset.forName(charsetName);
      } catch (IllegalCharsetNameException e) {
        System.err.println("Charset not available: " + charsetName);
        usage();
        return;
      } catch (UnsupportedCharsetException e) {
        System.err.println("Charset not available: " + charsetName);
        usage();
        return;
      } catch (IllegalArgumentException e) {
        System.err.println("Charset not available: " + charsetName);
        usage();
        return;
      }
    }

    Masker masker = null;
    try {
      masker = MaskerFactory.create(System.in, cs, System.out, cs);
      masker.mask(); // do the work
    } catch (IOException e) {
      System.err.println("Failed to mask data.");
    } finally {
      if (masker != null) {
        try {
          masker.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

  private static void usage() {
    System.err.println("java -jar <jar> [charset]");
  }
}
