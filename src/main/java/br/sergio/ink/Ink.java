package br.sergio.ink;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public abstract class Ink implements Serializable {

    public static final String SOURCE_ATTRIBUTE_NAME = "src";
    public static final String DEFAULT_TAG_NAME = "ink";
    private static DocumentBuilder docBuilder;
    private static Transformer transformer;

    private String source = "";

    public Ink() {}
    
    public abstract void read(Element element) throws InkException;
    
    public abstract Element write(Document document);

    public final String getSource() {
        return source;
    }

    public final void setSource(String source) {
        this.source = source == null ? "" : source.trim();
    }

    public final boolean hasSource() {
        return !source.isEmpty();
    }

    protected <T> void missing(Predicate<T> predicate, T obj, String tagName) throws InkException {
        if(predicate.test(obj)) {
            throw new InkException("missing \"" + tagName + "\" in " + getTagName(this));
        }
    }

    public static <T> T readInk(Class<T> clazz, File file) throws IOException, SAXException, InkException {
        return readInk(clazz, new FileInputStream(file));
    }

    public static <T> T readInk(Class<T> inkClass, InputStream inputStream) throws IOException, SAXException, InkException {
        return readDirectInkElement(inkClass, docBuilder.parse(inputStream).getDocumentElement());
    }

    public static <T> T readInkElement(Class<T> inkClass, Element element) throws InkException {
        validateInk(inkClass);
        String source = element.getAttribute(SOURCE_ATTRIBUTE_NAME);
        if(!(source == null || source.isBlank())) {
            T ink;
            try {
                ink = readInk(inkClass, new File(source));
            } catch(IOException | SAXException e) {
                throw new InkException(e);
            }
            ((Ink) ink).setSource(source);
            return ink;
        }
        return readDirectInkElement(inkClass, element);
    }

    private static <T> T readDirectInkElement(Class<T> inkClass, Element element) throws InkException {
        validateInk(inkClass);
        T ink;
        try {
            Constructor<T> constructor = inkClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            ink = constructor.newInstance();
        } catch(NoSuchMethodException e) {
            throw new InkException("ink class must have an empty constructor", e);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        ((Ink) ink).read(element);
        return ink;
    }

    public static void writeInk(File file, Ink ink) throws TransformerException, IOException  {
        writeInk(new FileOutputStream(file), ink);
    }

    public static void writeInk(OutputStream outputStream, Ink ink) throws TransformerException, IOException {
        if(outputStream instanceof FileOutputStream fileOS) {
            
        }
        Document document = docBuilder.newDocument();
        document.appendChild(ink.write(document));
        save(document, outputStream);
    }

    public static Element writeInkElement(Document document, Ink ink) {
        Element element;
        if(ink.hasSource()) {
            element = document.createElement(getTagName(ink.getClass()));
            element.setAttribute(SOURCE_ATTRIBUTE_NAME, ink.getSource());
        } else {
            element = ink.write(document);
        }
        return element;
    }

    public static String getTagName(Ink ink) {
        return getTagName(ink.getClass());
    }

    public static String getTagName(Class<? extends Ink> clazz) {
        if(clazz.isAnnotationPresent(TagName.class)) {
            return clazz.getAnnotation(TagName.class).value();
        } else {
            return clazz.isAnonymousClass() ? DEFAULT_TAG_NAME : clazz.getSimpleName();
        }
    }

    public static void validateInk(Object obj) {
        validateInk(obj.getClass());
    }

    public static void validateInk(Class<?> inkClass) {
        if(!Ink.class.isAssignableFrom(inkClass)) {
            throw new IllegalArgumentException("class must be a subtype of ink");
        }
        if(Modifier.isAbstract(inkClass.getModifiers())) {
            throw new IllegalArgumentException("class must not be abstract");
        }
    }

    private static void save(Document document, OutputStream outputStream) throws TransformerException, IOException {
        DOMSource domSource = new DOMSource(document);
        StreamResult result = new StreamResult(outputStream);
        transformer.transform(domSource, result);
        outputStream.close();
    }

    static {
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch(Exception e) {
            throw new Error("error while creating document builder and transformer", e);
        }
    }

}
