package net.trothwell.masker;

import java.io.Closeable;
import java.io.IOException;

public interface Masker extends Closeable {
  /**
   * Blocks current thread until all input has been masked.
   * 
   * @throws IOException on any failure to read/write
   */
  void mask() throws IOException;
}
