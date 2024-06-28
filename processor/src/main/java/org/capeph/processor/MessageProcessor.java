/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.processor;

import org.capeph.annotations.ReactorMessage;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@SupportedAnnotationTypes("org.capeph.annotations.ReactorMessage")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MessageProcessor extends AbstractProcessor {


    private static class MessageAPI {
        SequencedMap<String, TypeMirror> getters = new LinkedHashMap<>();
        Map<String, TypeMirror> setters = new HashMap<>();
        private final Element element;
        private int id;

        public Element getElement() {
            return element;
        }

        public String getName() {
            return element.getSimpleName().toString();
        }


        public MessageAPI(Element element) {
            this.element = element;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    private final List<MessageAPI> messages = new ArrayList<>();


    private String getBasePackage(String first, String second) {
        String[] firstPath = first.split("\\.");
        String[] secondPath = second.split("\\.");
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < Math.min(firstPath.length, secondPath.length); i++) {
            if (firstPath[i].equals(secondPath[i])) {
                if (i > 0) {
                    path.append('.');
                }
                path.append(firstPath[i]);
            }
            else {
                break;
            }
        }
        return path.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> messageClasses = roundEnv.getElementsAnnotatedWith(ReactorMessage.class);

        String basePackage = null;
        for (Element element : messageClasses) {
            if (element.getKind() == ElementKind.CLASS) {
                String qualified = ((TypeElement)element).getQualifiedName().toString();
                String elementPackage = qualified.substring(0, qualified.lastIndexOf("."));
                basePackage = basePackage == null ? elementPackage : getBasePackage(basePackage, elementPackage);
                MessageAPI api = new MessageAPI(element);
                for (Element enclosed : element.getEnclosedElements()) {
                    recordElement(enclosed, api);
                }
                int id = element.getAnnotation(ReactorMessage.class).id();
                if (id < 0) {
                    throw new IllegalArgumentException("Message id must be a positive integer");
                }
                verifyMessage(api, element);
                messages.add(api);
                api.setId(id == 0 ? -messages.size() : id);
            }
        }
        try {
            if (basePackage != null) {
                processingEnv.getMessager().printNote("Building codec with basePackage " + basePackage);
                BuildCodec(basePackage);
            }
            else {
                processingEnv.getMessager().printNote("Nothing to process in " + roundEnv);
            }
        }
        catch (IOException e) {
            processingEnv.getMessager().printError("Failed to write codec file(s): " +  e.getMessage());
            return false;
        }
        return true;
    }

    private String exclude(String name, String prefix) {
        return name.substring(prefix.length());
    }

    private void recordElement(Element enclosed, MessageAPI api) {
        switch (enclosed.getKind()) {
            case CLASS -> {
                // TODO: handle subclasses
            }
            case METHOD -> {
                String name = enclosed.getSimpleName().toString();
                ExecutableElement method = (ExecutableElement) enclosed;
                if (name.startsWith("get")) {
                    TypeMirror type = method.getReturnType();
                    processingEnv.getMessager().printNote("adding getter " + name);
                    api.getters.put(exclude(name, "get"), type);
                } else if (name.startsWith("is")) {
                        TypeMirror type = method.getReturnType();
                        processingEnv.getMessager().printNote("adding getter " + name);
                        api.getters.put(exclude(name, "is"), type);
                } else if (name.startsWith("set")) {
                    List<? extends VariableElement> parameters = method.getParameters();
                    if (parameters.size() != 1) {
                        throw new IllegalArgumentException("Setter with wrong number of parameters: "
                                + api.element.getSimpleName() + ":" + enclosed.getSimpleName());
                    }
                    processingEnv.getMessager().printNote("adding setter " + name);
                    api.setters.put(exclude(name, "set"), parameters.getFirst().asType());
                }
            }
            default -> {
            }
        }
    }

    private void verifyMessage(MessageAPI api, Element wrapper) {
//        if (!api.getters.keySet().equals(api.setters.keySet())) {
//            throw new IllegalArgumentException("Mismatch of names for getters and setters for "
//                    + wrapper);
//        }
        for (Map.Entry<String, TypeMirror> entry : api.getters.entrySet()) {
            TypeMirror getMirror = entry.getValue();
            // do not check for matching setter for StringBuffer fields
            if (!getMirror.toString().equals(StringBuffer.class.getName())) {
                TypeMirror setterType = api.setters.get(entry.getKey());
                if (!entry.getValue().equals(setterType)) {
                    throw new IllegalArgumentException("Mismatch of types for getters and setters for "
                            + wrapper + ":" + entry.getKey());
                }
            }
        }
    }


    private void BuildCodec(String packageName) throws IOException {
        String codecPackage = packageName + ".codec";
        String className = codecPackage + ".Codec";
        JavaFileObject codecFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter writer = new PrintWriter(codecFile.openWriter())) {

            writer.print("package ");
            writer.print(codecPackage);
            writer.println(";");
            writer.println("");
            writer.println("import org.capeph.reactor.Header;");
            writer.println("import org.capeph.reactor.ICodec;");
            writer.println("import org.capeph.pool.MessagePool;");
            writer.println("import org.capeph.reactor.ReusableMessage;");
            writer.println("import org.agrona.DirectBuffer;");
            writer.println("import org.agrona.MutableDirectBuffer;");
            writer.println("import java.util.HashMap;");
            writer.println("import java.util.Map;");
            writer.println("import java.util.function.Consumer;");
            writer.println("import java.util.function.Function;");
            for(MessageAPI api : messages) {
                writer.print("import ");
                writer.print(((TypeElement)api.getElement()).getQualifiedName().toString());
                writer.println(";");
            }

            writer.println("");
            writer.println("public class Codec implements ICodec {");
            writer.println("");

            localFields(writer);
            functionalInterface(writer);
            constructor(writer);
            lengthMethod(writer);
            lookupMethod(writer);
            encodeMethod(writer);
            decodeMethod(writer);
            clearMethod(writer);

            for(MessageAPI api : messages) {
                lengthFunction(writer, api);
                encodeFunction(writer, api);
                decodeFunction(writer, api);
                clearFunction(writer, api);
            }

            writer.println("}");
        }
    }

    private void constructor(PrintWriter writer) {
        writer.println("   public Codec() {");
        for(MessageAPI api: messages) {
            writer.print("      lengthFuns.put(");
            writer.print(api.getName());
            writer.print(".class, msg -> ");
            writer.print(lengthFunctionName(api));
            writer.print("((");
            writer.print(api.getName());
            writer.println(")msg));");

            writer.print("      encodeFuns.put(");
            writer.print(api.getName());
            writer.print(".class, (msg, buffer, offset) -> ");
            writer.print(encodeFunctionName(api));
            writer.print("((");
            writer.print(api.getName());
            writer.println(")msg, buffer, offset));");

            writer.print("      decodeFuns.put(");
            writer.print(api.getId());
            writer.print(", this::");
            writer.print(decodeFunctionName(api));
            writer.println(");");

            writer.print("      clearFuns.put(");
            writer.print(api.getName());
            writer.print(".class, msg -> ");
            writer.print(clearFunctionName(api));
            writer.print("((");
            writer.print(api.getName());
            writer.println(")msg));");

            writer.print("      messageIdMap.put(");
            writer.print(api.getId());
            writer.print(", ");
            writer.print(api.getName());
            writer.println(".class);");
        }
        writer.println("   }");
        writer.println("");
    }

    private void localFields(PrintWriter writer) {
        writer.println("   private static final int VERSION = 1;");
        writer.println("   private Map<Class<? extends ReusableMessage>, Function<ReusableMessage, Integer>> lengthFuns = new HashMap<>();");
        writer.println("   private Map<Class<? extends ReusableMessage>, TriFunction<ReusableMessage, MutableDirectBuffer, Integer, Integer>> encodeFuns = new HashMap<>();");
        writer.println("   private Map<Integer, TriFunction<DirectBuffer, Integer, MessagePool, ? extends ReusableMessage>> decodeFuns = new HashMap<>();");
        writer.println("   private Map<Integer, Class<? extends ReusableMessage>> messageIdMap = new HashMap<>();");
        writer.println("   private Map<Class<? extends ReusableMessage>, Consumer<ReusableMessage>> clearFuns = new HashMap<>();");
        writer.println("");
    }

    private void functionalInterface(PrintWriter writer) {
        writer.println("   @FunctionalInterface");
        writer.println("   interface TriFunction<T, U, V, R> {");
        writer.println("       R apply(T t, U u, V v);");
        writer.println("   }");
        writer.println("");
    }

    private void lengthMethod(PrintWriter writer) {
        writer.println("   @Override");
        writer.println("   public int encodedLength(ReusableMessage msg) {");
        writer.println("      Function<ReusableMessage, Integer> fun = lengthFuns.get(msg.getClass());");
        writer.println("      if (fun != null) {");
        writer.println("          return fun.apply(msg) + Header.length();");
        writer.println("      }");
        writer.println("      else {");
        writer.println("          throw new IllegalArgumentException(\"No message length function matching \"");
        writer.println("                                              + msg.getClass());");
        writer.println("      }");
        writer.println("   }");
        writer.println("");
    }

    private void lookupMethod(PrintWriter writer) {
        writer.println("   @Override");
        writer.println("   public Class<? extends ReusableMessage> getClassFor(int id) {");
        writer.println("      return messageIdMap.get(id);");
        writer.println("   }");
        writer.println("");
    }

    private void encodeMethod(PrintWriter writer) {
        writer.println("   @Override");
        writer.println("   public int encode(ReusableMessage msg, MutableDirectBuffer buffer, int offset) {");
        writer.println("       TriFunction<ReusableMessage, MutableDirectBuffer, Integer, Integer> fun = encodeFuns.get(msg.getClass());");
        writer.println("       if (fun != null) {");
        writer.println("           return fun.apply(msg, buffer, offset);");
        writer.println("       }");
        writer.println("       else {");
        writer.println("           throw new IllegalArgumentException(\"No encoder function matching \" + msg.getClass());");
        writer.println("       }");
        writer.println("   }");
        writer.println("");
    }


    private void decodeMethod(PrintWriter writer) {
        writer.println("   @Override");
        writer.println("   public ReusableMessage decode(DirectBuffer buffer, int offset, MessagePool messagePool) {");
        writer.println("       int messageType = Header.getMessageType(buffer, offset);");
        writer.println("       return decodeFuns.get(messageType).apply(buffer, offset, messagePool);");
        writer.println("   }");
        writer.println("");
    }

    private void clearMethod(PrintWriter writer) {
        writer.println("   @Override");
        writer.println("   public void clear(ReusableMessage msg) {");
        writer.println("       Consumer<ReusableMessage> fun = clearFuns.get(msg.getClass());");
        writer.println("       if (fun != null) {");
        writer.println("           fun.accept(msg);");
        writer.println("       }");
        writer.println("       else {");
        writer.println("           throw new IllegalArgumentException(\"No clear function matching \" + msg.getClass());");
        writer.println("       }");
        writer.println("   }");
        writer.println("");
    }


    // Code generation for the messages

    // calculate message length

    private String lengthFunctionName(MessageAPI api) {
        return "get" + api.getName() + "EncodedLength";
    }

    private void lengthFunction(PrintWriter writer, MessageAPI api) {
        processingEnv.getMessager().printNote("lengthFunction called for " + api.getName());
        int byteCount = 0;
        List<String> stringFields = new ArrayList<>();
        List<String> stringBufferFields = new ArrayList<>();
        for(Map.Entry<String, TypeMirror> field : api.getters.entrySet()) {
            switch (field.getValue().getKind()) { // values are taken from Agrona.AbstractMutableDirectBuffer
                case BYTE, BOOLEAN -> byteCount += 1;
                case CHAR, SHORT -> byteCount += 2;
                case INT, FLOAT -> byteCount += 4;
                case LONG, DOUBLE -> byteCount += 8;
                case DECLARED -> {
                    TypeElement typeElem = (TypeElement) processingEnv.getTypeUtils().asElement(field.getValue());
                    String typeName = typeElem.getQualifiedName().toString();
                    if (typeName.equals(String.class.getName())) {
                        stringFields.add(field.getKey());
                    }
                    else if (typeName.equals(StringBuffer.class.getName())) {
                        stringBufferFields.add(field.getKey());
                    }
                }
                default ->
                    throw new IllegalStateException("Got unsupported type: " + field.getValue());
            }
        }
        writer.print("   private int ");
        writer.print(lengthFunctionName(api));
        writer.print("(");
        writer.print(api.getName());
        writer.println(" msg) {");
        writer.print("      return ");
        byteCount += 4 * (stringFields.size() + stringBufferFields.size());   // add the encoded lengths
        if (!stringFields.isEmpty()) {
            for(String fieldName: stringFields) {
                writer.print("msg.get");
                writer.print(fieldName);
                writer.print("().length() + ");
            }
        }
        if (!stringBufferFields.isEmpty()) {
            for(String fieldName: stringBufferFields) {
                writer.print("msg.get");
                writer.print(fieldName);
                writer.print("().length() * 2 + ");
            }
        }
        writer.print(byteCount);
        writer.println(";");
        writer.println("   }");
        writer.println("");
    }


    // encoding
    private void writePrimitiveEncoding(PrintWriter writer, String fieldName, String typeName, int length) {
        writer.print("      buffer.put");
        writer.print(typeName);
        writer.print("(dst, msg.get");
        writer.print(fieldName);
        writer.println("());");
        writer.print("      dst += ");
        writer.print(length);
        writer.println(";");
    }

    private void writeFieldEncoding(PrintWriter writer, String fieldName, TypeMirror type) {
        switch (type.getKind()) {
            case BYTE -> writePrimitiveEncoding(writer, fieldName, "Byte", 1);
            case INT -> writePrimitiveEncoding(writer, fieldName, "Int", 4);
            case CHAR -> writePrimitiveEncoding(writer, fieldName, "Char", 2);
            case SHORT -> writePrimitiveEncoding(writer, fieldName, "Short", 2);
            case FLOAT -> writePrimitiveEncoding(writer, fieldName, "Float", 4);
            case LONG -> writePrimitiveEncoding(writer, fieldName, "Long", 8);
            case DOUBLE -> writePrimitiveEncoding(writer, fieldName, "Double", 8);
            case BOOLEAN -> {
                writer.print("      buffer.putByte(dst, msg.is");
                writer.print(fieldName);
                writer.println("() ? (byte)'T' : (byte)'F');");
                writer.println("      dst += 1;");
            }
            case DECLARED -> {
                TypeElement typeElem = (TypeElement) processingEnv.getTypeUtils().asElement(type);

                String typeName = typeElem.getQualifiedName().toString();
                if (typeName.equals(String.class.getName()) ||
                        typeName.equals(StringBuffer.class.getName())) {
                    writer.print("      dst += buffer.putStringAscii(dst, msg.get");
                    writer.print(fieldName);
                    writer.println("());");
                } else {
                    throw new IllegalStateException("Unsupported type");
                }
            }
            default -> throw new IllegalStateException("Unsupported type");
        }
    }

    private String encodeFunctionName(MessageAPI api) {
        return "encode" + api.getName();
    }

    private void encodeFunction(PrintWriter writer, MessageAPI api) {
        processingEnv.getMessager().printNote("encodeFunction called for " + api.getName());
        writer.print("   private int ");
        writer.print(encodeFunctionName(api));
        writer.print("(");
        writer.print(api.getName());
        writer.println(" msg, MutableDirectBuffer buffer, int offset) {");
        writer.print("      int dst = Header.writeHeader(buffer, offset, ");
        writer.print(api.getId());
        writer.println(", VERSION);");

        for(Map.Entry<String, TypeMirror> field: api.getters.entrySet()) {
            String fieldName = field.getKey();
            TypeMirror getterType = field.getValue();
            if (getterType.toString().equals(StringBuffer.class.getName())) {
                writeStringBufferEncoding(writer, fieldName);
            }
            writeFieldEncoding(writer, fieldName, getterType);
        }
        writer.println("      return dst;");
        writer.println("   }");
        writer.println("");
    }


    // decode messages


    private void writePrimitiveDecoding(PrintWriter writer, String fieldName, String typeName, int length) {
        writer.print("      msg.set");
        writer.print(fieldName);
        writer.print("(buffer.get");
        writer.print(typeName);
        writer.println("(src));");
        writer.print("      src += ");
        writer.print(length);
        writer.println(";");
    }

    private void writeStringBufferDecoding(PrintWriter writer, String fieldName) {
        String lengthVar = "lengthOf" + fieldName;
        String fieldVar = "the" + fieldName;
        writer.print("      int ");
        writer.print(lengthVar);
        writer.println(" = buffer.getInt(src);");
        writer.println("      src += 4;");
        writer.print("      StringBuffer ");
        writer.print(fieldVar);
        writer.print(" = msg.get");
        writer.print(fieldName);
        writer.println("();");
        writer.print("      ");
        writer.print(fieldVar);
        writer.println(".setLength(0);");
        writer.print("      for(int i = 0; i < ");
        writer.print(lengthVar);
        writer.println("; i++) {");

        writer.print("         ");
        writer.print(fieldVar);
        writer.println(".append(buffer.getChar(src));");
        writer.println("        src += 2;");
        writer.println("      }");
    }

    private void writeStringBufferEncoding(PrintWriter writer, String fieldName) {
        String lengthVar = "lengthOf" + fieldName;
        writer.print("      int ");
        writer.print(lengthVar);
        writer.print(" = msg.get");
        writer.print(fieldName);
        writer.println("().length();");
        writer.print("      buffer.putInt(dst, ");
        writer.print(lengthVar);
        writer.println(");");
        writer.println("      dst += 4;");
        writer.print("      for(int i = 0; i < ");
        writer.print(lengthVar);
        writer.println("; i++) {");
        writer.print("         buffer.putChar(dst,  msg.get");
        writer.print(fieldName);
        writer.println("().charAt(i));");
        writer.println("         dst += 2;");
        writer.println("      }");
    }



    private void writeFieldDecoding(PrintWriter writer, String fieldName, TypeMirror setterType) {
        switch (setterType.getKind()) {
            case BYTE -> writePrimitiveDecoding(writer, fieldName, "Byte", 1);
            case INT -> writePrimitiveDecoding(writer, fieldName, "Int", 4);
            case CHAR -> writePrimitiveDecoding(writer, fieldName, "Char", 2);
            case SHORT -> writePrimitiveDecoding(writer, fieldName, "Short", 2);
            case FLOAT -> writePrimitiveDecoding(writer, fieldName, "Float", 4);
            case LONG -> writePrimitiveDecoding(writer, fieldName, "Long", 8);
            case DOUBLE -> writePrimitiveDecoding(writer, fieldName, "Double", 8);
            case BOOLEAN -> {
                writer.print("      msg.set");
                writer.print(fieldName);
                writer.println("(buffer.getByte(src) == (byte)'T');");
                writer.println("      src += 1;");
            }
            case DECLARED -> {
                TypeElement typeElem = (TypeElement) processingEnv.getTypeUtils().asElement(setterType);
                if (typeElem.getQualifiedName().toString().equals(String.class.getName())) {
                    writer.print("      msg.set");
                    writer.print(fieldName);
                    writer.println("(buffer.getStringAscii(src));");

                    writer.print("      src += msg.get");
                    writer.print(fieldName);
                    writer.println("().length() + 4;");
                } else {
                    throw new IllegalStateException("Unsupported type");
                }
            }
            default -> throw new IllegalStateException("Unsupported type");
        }
    }

    private void setField(PrintWriter writer, String fieldName, String value) {
        writer.print("      msg.set");
        writer.print(fieldName);
        writer.print("(");
        writer.print(value);
        writer.println(");");
    }

    private void writeClearField(PrintWriter writer, String fieldName, TypeMirror setterType) {
        switch (setterType.getKind()) {
            case BYTE, INT, CHAR, SHORT -> setField(writer, fieldName, "0");
            case FLOAT -> setField(writer, fieldName, "0F");
            case LONG -> setField(writer, fieldName, "0L");
            case DOUBLE -> setField(writer, fieldName, "0D");
            case BOOLEAN -> setField(writer, fieldName, "false");
            case DECLARED -> {
                TypeElement typeElem = (TypeElement) processingEnv.getTypeUtils().asElement(setterType);
                String typeName = typeElem.getQualifiedName().toString();
                if (typeName.equals(String.class.getName())) {
                    setField(writer, fieldName, "\"\"");
                } else if (typeName.equals(StringBuffer.class.getName())) {
                    writer.print("      msg.get");
                    writer.print(fieldName);
                    writer.println("().setLength(0);");
                } else {
                    throw new IllegalStateException("Unsupported type");
                }
            }
            default -> throw new IllegalStateException("Unsupported type");
        }
    }


    private String clearFunctionName(MessageAPI api) {
        return "clear" + api.getName();
    }


    private String decodeFunctionName(MessageAPI api) {
        return "decode" + api.getName();
    }

    private void decodeFunction(PrintWriter writer, MessageAPI api) {
        processingEnv.getMessager().printNote("decodeFunction called for " + api.getName());
        writer.print("   private ");
        writer.print(api.getName());
        writer.print(" ");
        writer.print(decodeFunctionName(api));
        writer.println("(DirectBuffer buffer, int offset, MessagePool pool) {");
        writer.print("      ");
        writer.print(api.getName());
        writer.print(" msg = (");
        writer.print(api.getName());
        writer.print(") pool.getMessageTemplate(");
        writer.print(api.getName());
        writer.println(".class);");
        writer.println("      int src = offset + Header.length();");

        // make sure we have the same iteration order as the getters;
        for(Map.Entry<String, TypeMirror> field: api.getters.entrySet()) {
            String apiName = field.getKey();
            TypeMirror getterType = field.getValue();
            if (getterType.toString().equals(StringBuffer.class.getName())) {
                writeStringBufferDecoding(writer, apiName);
            }
            else {
                TypeMirror setterType = api.setters.get(apiName);
                writeFieldDecoding(writer, apiName, setterType);
            }
        }

        writer.println("      return msg;");
        writer.println("   }");
    }

    private void clearFunction(PrintWriter writer, MessageAPI api) {
        processingEnv.getMessager().printNote("clearFunction called for " + api.getName());
        writer.print("   private void ");
        writer.print(clearFunctionName(api));
        writer.print("(");
        writer.print(api.getName());
        writer.println(" msg) {");

        // make sure we have the same iteration order as the getters;
        for(Map.Entry<String, TypeMirror> field: api.getters.entrySet()) {
            String fieldName = field.getKey();
            TypeMirror getterType = field.getValue();
            TypeMirror fieldType = getterType;   // TODO: use this construction for encoder/decoder
            if (!getterType.toString().equals(StringBuffer.class.getName())) {
                fieldType = api.setters.get(fieldName);
            }
            writeClearField(writer, fieldName, fieldType);
        }
        writer.println("   }");
    }




}
