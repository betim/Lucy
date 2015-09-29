package com.pring.lucy.template;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class Compiler {
  private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
  private Compiler() {}
  private static Compiler rc;

  public static Compiler instance() {
    if (rc == null)
      rc = new Compiler();
    
    return rc;
  }

  public Class<?> compile(String className, String source) throws Exception {
    Class<?> clazz = null;

    List<CompilationUnit> compilationUnits = 
        Arrays.asList(new CompilationUnit(className, source));

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

    List<String> optionList = new ArrayList<String>();
    optionList.addAll(Arrays.asList("-classpath", System.getProperty("java.class.path")));

    SingleFileManager singleFileManager =
        new SingleFileManager(compiler, new ByteCode(className));
    
    JavaCompiler.CompilationTask compile =  compiler.getTask(
        null, singleFileManager, diagnostics, optionList, null, compilationUnits);

    if (!compile.call()) {
      for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
        throw new Exception(diagnostic.toString());
      }
    } else {
      try {
        clazz = singleFileManager.getClassLoader().findClass(className);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }

    return clazz;
  }

  private static class CompilationUnit extends SimpleJavaFileObject {
    public CompilationUnit(String className, String source) {
      super(URI.create("file:///" + className + ".java"), Kind.SOURCE);
      source_ = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return source_;
    }

    @Override
    public OutputStream openOutputStream() {
      throw new IllegalStateException();
    }

    @Override
    public InputStream openInputStream() {
      return new ByteArrayInputStream(source_.getBytes());
    }

    private final String source_;
  }

  private static class ByteCode extends SimpleJavaFileObject {
    public ByteCode(String className) {
      super(URI.create("byte:///" + className + ".class"), Kind.CLASS);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return null;
    }

    @Override
    public OutputStream openOutputStream() {
      byteArrayOutputStream_ = new ByteArrayOutputStream();
      return byteArrayOutputStream_;
    }

    @Override
    public InputStream openInputStream() {
      return null;
    }

    public byte[] getByteCode() {
      return byteArrayOutputStream_.toByteArray();
    }

    private ByteArrayOutputStream byteArrayOutputStream_;
  }

  @SuppressWarnings("rawtypes")
  private static class SingleFileManager extends ForwardingJavaFileManager {
    @SuppressWarnings("unchecked")
    public SingleFileManager(JavaCompiler compiler, ByteCode byteCode) {
      super(compiler.getStandardFileManager(null, null, null));
      singleClassLoader_ = new SingleClassLoader(byteCode);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location notUsed,
        String className, JavaFileObject.Kind kind, FileObject sibling)
        throws IOException {
      return singleClassLoader_.getFileObject();
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
      return singleClassLoader_;
    }

    public SingleClassLoader getClassLoader() {
      return singleClassLoader_;
    }

    private final SingleClassLoader singleClassLoader_;
  }

  private static class SingleClassLoader extends ClassLoader {
    public SingleClassLoader(ByteCode byteCode) {
      byteCode_ = byteCode;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
      return defineClass(className, byteCode_.getByteCode(), 0,
          byteCode_.getByteCode().length);
    }

    ByteCode getFileObject() {
      return byteCode_;
    }

    private final ByteCode byteCode_;
  }
}