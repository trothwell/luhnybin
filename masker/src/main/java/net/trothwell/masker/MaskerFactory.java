package net.trothwell.masker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

public class MaskerFactory {
  public static Masker create(InputStream input, Charset inputCharset, OutputStream output,
      Charset outputCharset) {
    Reader reader = new InputStreamReader(new BufferedInputStream(input), inputCharset);
    Writer writer = new OutputStreamWriter(new BufferedOutputStream(output), outputCharset);
    Masker retval = new MaskerImpl(reader, writer);
    return retval;
  }
}
