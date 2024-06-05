package org.capeph.processor;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class Utils {
  private Utils() {
    throw new UnsupportedOperationException("utility class");
  }

  public static void createFile(Filer filer, String packageName, String className, String content) {
    try {
      FileObject object = filer.createSourceFile(packageName + className);
      try (PrintWriter pw = new PrintWriter(object.openOutputStream())) {
        pw.println(String.format("""
            package %s;
            class %s { %s }
            """, packageName, className, content));
        pw.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
