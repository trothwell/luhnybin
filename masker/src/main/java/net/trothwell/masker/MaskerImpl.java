package net.trothwell.masker;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.google.common.io.Closeables;

class MaskerImpl implements Masker {

  private static void debug(String msg) {
    System.err.println(msg);
  }

  private final CCBuffer ccBuffer;
  private final Reader input;
  private final Writer output;

  MaskerImpl(Reader input, Writer writer) {
    this.input = input;
    this.output = writer;
    this.ccBuffer = new CCBuffer() {
      @Override
      void onCCData(char[] buf, int offset, int len) {
        debug(String.format("CC:%s/%s", len, new String(buf, offset, len)));
        try {
          String elided = new String(buf, offset, len).replaceAll("\\d", "X");
          output.write(elided);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      @Override
      void onData(char[] buf, int offset, int len) {
        debug(String.format("data:%s/%s", len, new String(buf, offset, len)));
        try {
          output.write(buf, offset, len);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
  }

  public void close() throws IOException {
    ccBuffer.close();
    Closeables.close(input, false);
    Closeables.close(output, false);
  }

  public void mask() throws IOException {
    for (;;) {
      int charRead = input.read();
      if (charRead == -1) {
        return;
      }
      char c = (char) charRead;
      ccBuffer.append(c);
    }
  }
}
