package dev.flutter.netbeans.plugin.project;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Durable auxiliary metadata for Flutter projects.
 *
 * <p>Private state is kept in one slash-free filesystem attribute attached to
 * the project directory. Shared state is written to the project only when a
 * caller explicitly requests shared storage. Providing both project SPIs
 * prevents NetBeans' generic fallback from encoding each XML fragment as a
 * slash-containing attribute which its folder-ordering code then mistakes for
 * a relative-ordering rule.</p>
 */
final class FlutterProjectMetadata implements AuxiliaryConfiguration, AuxiliaryProperties {
    static final String LEGACY_ATTRIBUTE_PREFIX = AuxiliaryConfiguration.class.getName() + ".";
    static final String PRIVATE_METADATA_ATTRIBUTE
            = "dev.flutter.netbeans.projectMetadata";
    static final String TRANSIENT_ATTRIBUTE_PREFIX = "transient:";
    static final String LEGACY_VALUE_QUARANTINE_PREFIX
            = "dev.flutter.netbeans.legacyQuarantine.";
    static final String EXTERNAL_PAYLOAD_QUARANTINE_PREFIX
            = "dev.flutter.netbeans.payloadQuarantine.";
    static final String SHARED_METADATA_PATH = ".netbeans/flutter-metadata.xml";

    private static final Logger LOGGER = Logger.getLogger(FlutterProjectMetadata.class.getName());
    private static final String METADATA_NAMESPACE
            = "urn:dev.flutter.netbeans:project-metadata:1";
    private static final String ROOT_ELEMENT = "metadata";
    private static final String FRAGMENTS_ELEMENT = "fragments";
    private static final String PROPERTIES_ELEMENT = "properties";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String MIGRATIONS_ELEMENT = "legacy-migrations";
    private static final String MIGRATION_ELEMENT = "fragment";
    private static final String QUARANTINE_ELEMENT = "quarantine";
    private static final String QUARANTINE_ENTRY_ELEMENT = "entry";
    private static final String KEY_ATTRIBUTE = "key";
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String IDENTITY_ATTRIBUTE = "identity";
    private static final String KIND_ATTRIBUTE = "kind";
    private static final String HASH_ATTRIBUTE = "hash";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String LEGACY_XML_KIND = "legacy-xml";
    private static final String CORRUPT_PRIVATE_KIND = "corrupt-private";
    private static final int MAX_METADATA_BYTES = 4 * 1024 * 1024;
    private static final int MAX_INLINE_QUARANTINE_VALUE_BYTES
            = MAX_METADATA_BYTES / 2;
    private static final int MAX_XML_DEPTH = 256;
    private static final int MAX_XML_NODES = 100_000;

    private final FileObject projectDirectory;
    private final MetadataWriter metadataWriter;
    private final Object storageLock = new Object();
    private final Set<String> reportedReadFailures = new HashSet<>();

    FlutterProjectMetadata(FileObject projectDirectory) {
        this(projectDirectory, FlutterProjectMetadata::writeMetadata);
    }

    FlutterProjectMetadata(
            FileObject projectDirectory,
            MetadataWriter metadataWriter) {
        this.projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory");
        this.metadataWriter = Objects.requireNonNull(metadataWriter, "metadataWriter");
    }

    @Override
    public Element getConfigurationFragment(
            String elementName,
            String namespace,
            boolean shared) {
        requireFragmentIdentity(elementName, namespace);
        synchronized (storageLock) {
            try {
                Document document = readDocument(shared);
                if (document == null) {
                    return null;
                }
                Element fragment = findFragment(document, elementName, namespace);
                return fragment == null ? null : detachedCopy(fragment);
            } catch (IOException ex) {
                logReadFailure("configuration fragment", shared, ex);
                return null;
            }
        }
    }

    @Override
    public void putConfigurationFragment(Element fragment, boolean shared)
            throws IllegalArgumentException {
        Objects.requireNonNull(fragment, "fragment");
        String elementName = fragment.getLocalName();
        String namespace = fragment.getNamespaceURI();
        requireFragmentIdentity(elementName, namespace);

        synchronized (storageLock) {
            try {
                Document document = readOrCreateDocument(shared);
                Element fragments = findContainer(document, FRAGMENTS_ELEMENT, true);
                Element previous = findDirectChild(fragments, elementName, namespace);
                Node imported = document.importNode(fragment, true);
                if (previous == null) {
                    fragments.appendChild(imported);
                } else {
                    fragments.replaceChild(imported, previous);
                }
                writeDocument(document, shared);
            } catch (IOException ex) {
                throw storageFailure("write", shared, ex);
            }
        }
    }

    @Override
    public boolean removeConfigurationFragment(
            String elementName,
            String namespace,
            boolean shared) throws IllegalArgumentException {
        requireFragmentIdentity(elementName, namespace);
        synchronized (storageLock) {
            try {
                Document document = readDocument(shared);
                if (document == null) {
                    return false;
                }
                Element fragment = findFragment(document, elementName, namespace);
                if (fragment == null) {
                    return false;
                }
                fragment.getParentNode().removeChild(fragment);
                writeDocument(document, shared);
                return true;
            } catch (IOException ex) {
                throw storageFailure("remove a fragment from", shared, ex);
            }
        }
    }

    @Override
    public String get(String key, boolean shared) {
        Objects.requireNonNull(key, "key");
        synchronized (storageLock) {
            try {
                Document document = readDocument(shared);
                if (document == null) {
                    return null;
                }
                Element property = findProperty(document, key);
                return property == null
                        ? null
                        : decode(property.getAttribute(VALUE_ATTRIBUTE));
            } catch (IOException ex) {
                logReadFailure("property", shared, ex);
                return null;
            }
        }
    }

    @Override
    public void put(String key, String value, boolean shared) {
        Objects.requireNonNull(key, "key");
        synchronized (storageLock) {
            try {
                Document document = value == null
                        ? readDocument(shared)
                        : readOrCreateDocument(shared);
                if (document == null) {
                    return;
                }
                Element properties = findContainer(document, PROPERTIES_ELEMENT, true);
                Element previous = findProperty(properties, key);
                if (value == null) {
                    if (previous != null) {
                        properties.removeChild(previous);
                        writeDocument(document, shared);
                    }
                    return;
                }
                Element property = previous;
                if (property == null) {
                    property = document.createElementNS(METADATA_NAMESPACE, PROPERTY_ELEMENT);
                    property.setAttribute(KEY_ATTRIBUTE, encode(key));
                    properties.appendChild(property);
                }
                property.setAttribute(VALUE_ATTRIBUTE, encode(value));
                writeDocument(document, shared);
            } catch (IOException ex) {
                throw storageFailure("write properties to", shared, ex);
            }
        }
    }

    @Override
    public Iterable<String> listKeys(boolean shared) {
        synchronized (storageLock) {
            try {
                Document document = readDocument(shared);
                if (document == null) {
                    return List.of();
                }
                Element properties = findContainer(document, PROPERTIES_ELEMENT, false);
                if (properties == null) {
                    return List.of();
                }
                List<String> keys = new ArrayList<>();
                for (Node child = properties.getFirstChild(); child != null;
                        child = child.getNextSibling()) {
                    if (child instanceof Element element
                            && METADATA_NAMESPACE.equals(element.getNamespaceURI())
                            && PROPERTY_ELEMENT.equals(element.getLocalName())) {
                        keys.add(decode(element.getAttribute(KEY_ATTRIBUTE)));
                    }
                }
                Collections.sort(keys);
                return List.copyOf(keys);
            } catch (IOException ex) {
                logReadFailure("property keys", shared, ex);
                return List.of();
            }
        }
    }

    /**
     * Imports metadata written by NetBeans' generic private fallback. Invalid
     * legacy values are quarantined under slash-free attributes. A legacy
     * attribute is removed only after its contents are durably recoverable.
     */
    void migrateLegacyPrivateConfiguration() {
        List<String> attributeNames = new ArrayList<>();
        Enumeration<String> attributes = projectDirectory.getAttributes();
        while (attributes.hasMoreElements()) {
            String attributeName = attributes.nextElement();
            if (attributeName.startsWith(LEGACY_ATTRIBUTE_PREFIX)) {
                attributeNames.add(attributeName);
            }
        }

        for (String attributeName : attributeNames) {
            Object value = projectDirectory.getAttribute(attributeName);
            if (!(value instanceof String xml)) {
                quarantineUnsupportedLegacyValue(attributeName, value);
                continue;
            }

            Element fragment;
            try {
                fragment = parseFragment(xml);
                String elementName = fragment.getLocalName();
                String namespace = fragment.getNamespaceURI();
                requireFragmentIdentity(elementName, namespace);
                requireMatchingLegacyAttribute(attributeName, elementName, namespace);
            } catch (IOException | RuntimeException ex) {
                quarantineMalformedLegacyXml(attributeName, xml, ex);
                continue;
            }

            try {
                String fallbackHash = sha256(xml);
                migrateLegacyFragment(fragment, fallbackHash);
                projectDirectory.setAttribute(attributeName, null);
            } catch (IOException | RuntimeException ex) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not migrate private NetBeans project metadata attribute "
                                + attributeName + " for " + projectDirectory.getPath()
                                + "; the original value was preserved: " + ex.getMessage(),
                        ex);
            }
        }
    }

    private void quarantineMalformedLegacyXml(
            String attributeName,
            String xml,
            Exception parseFailure) {
        try {
            synchronized (storageLock) {
                Document document = readOrCreateDocument(false);
                if (putInlineQuarantineEntryIfItFits(
                        document,
                        LEGACY_XML_KIND,
                        attributeName,
                        xml)) {
                    writeDocument(document, false);
                } else {
                    quarantineExternally(LEGACY_XML_KIND, attributeName, xml);
                }
            }
            projectDirectory.setAttribute(attributeName, null);
            LOGGER.log(
                    Level.INFO,
                    "Quarantined malformed private NetBeans project metadata attribute {0} "
                            + "for {1}; the legacy slash-containing attribute was removed.",
                    new Object[]{attributeName, projectDirectory.getPath()});
            LOGGER.log(Level.FINE, "Malformed legacy metadata", parseFailure);
        } catch (IOException | RuntimeException quarantineFailure) {
            quarantineFailure.addSuppressed(parseFailure);
            LOGGER.log(
                    Level.WARNING,
                    "Could not quarantine private NetBeans project metadata attribute "
                            + attributeName + " for " + projectDirectory.getPath()
                            + "; the original value was preserved: "
                            + quarantineFailure.getMessage(),
                    quarantineFailure);
        }
    }

    private void quarantineUnsupportedLegacyValue(String attributeName, Object value) {
        try {
            if (value != null) {
                String safeAttribute = LEGACY_VALUE_QUARANTINE_PREFIX + encode(attributeName);
                quarantineAtSafeAttribute(safeAttribute, value);
            }
            projectDirectory.setAttribute(attributeName, null);
            LOGGER.log(
                    Level.INFO,
                    "Moved unsupported private NetBeans project metadata attribute {0} "
                            + "to slash-free quarantine for {1}.",
                    new Object[]{attributeName, projectDirectory.getPath()});
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not quarantine unsupported private NetBeans project metadata "
                            + "attribute " + attributeName + " for "
                            + projectDirectory.getPath()
                            + "; the original value was preserved: " + ex.getMessage(),
                    ex);
        }
    }

    private Document readOrCreateDocument(boolean shared) throws IOException {
        try {
            Document document = readDocument(shared);
            return document == null ? newDocument() : document;
        } catch (IOException ex) {
            if (shared) {
                throw ex;
            }
            return recoverCorruptPrivateDocument(ex);
        }
    }

    private Document recoverCorruptPrivateDocument(IOException readFailure)
            throws IOException {
        Object stored = projectDirectory.getAttribute(PRIVATE_METADATA_ATTRIBUTE);
        Document recovered = newDocument();
        if (stored instanceof String xml) {
            if (!putInlineQuarantineEntryIfItFits(
                    recovered,
                    CORRUPT_PRIVATE_KIND,
                    PRIVATE_METADATA_ATTRIBUTE,
                    xml)) {
                quarantineExternally(
                        CORRUPT_PRIVATE_KIND,
                        PRIVATE_METADATA_ATTRIBUTE,
                        xml);
            }
        } else if (stored != null) {
            quarantineExternally(
                    CORRUPT_PRIVATE_KIND,
                    PRIVATE_METADATA_ATTRIBUTE,
                    stored);
        } else {
            throw readFailure;
        }
        logReadFailure("private metadata container", false, readFailure);
        return recovered;
    }

    private boolean putInlineQuarantineEntryIfItFits(
            Document document,
            String kind,
            String identity,
            String value) throws IOException {
        if (value.getBytes(StandardCharsets.UTF_8).length
                > MAX_INLINE_QUARANTINE_VALUE_BYTES) {
            return false;
        }
        putQuarantineEntry(document, kind, identity, value);
        return serializedDocument(document).length <= MAX_METADATA_BYTES;
    }

    private void quarantineExternally(
            String kind,
            String identity,
            Object value) throws IOException {
        String safeAttribute = EXTERNAL_PAYLOAD_QUARANTINE_PREFIX
                + quarantineFingerprint(kind, identity, value);
        quarantineAtSafeAttribute(safeAttribute, value);
    }

    private void quarantineAtSafeAttribute(String baseAttribute, Object value)
            throws IOException {
        String safeAttribute = baseAttribute;
        for (int suffix = 1;
                projectDirectory.getAttribute(safeAttribute) != null
                        && !Objects.equals(
                                value,
                                projectDirectory.getAttribute(safeAttribute));
                suffix++) {
            safeAttribute = baseAttribute + "." + suffix;
        }
        projectDirectory.setAttribute(
                TRANSIENT_ATTRIBUTE_PREFIX + safeAttribute,
                value);
        requireQuarantineValue(safeAttribute, value);
    }

    private void requireQuarantineValue(String attributeName, Object expected)
            throws IOException {
        Object quarantined = projectDirectory.getAttribute(attributeName);
        if (!Objects.equals(expected, quarantined)) {
            throw new IOException(
                    "Flutter project metadata quarantine was not durably stored in attribute "
                            + attributeName);
        }
    }

    private static String quarantineFingerprint(
            String kind,
            String identity,
            Object value) {
        String valueType = value == null ? "null" : value.getClass().getName();
        return sha256(kind + "\u0000" + identity + "\u0000" + valueType
                + "\u0000" + String.valueOf(value));
    }

    private Document readDocument(boolean shared) throws IOException {
        if (!shared) {
            Object stored = projectDirectory.getAttribute(PRIVATE_METADATA_ATTRIBUTE);
            if (stored == null) {
                return null;
            }
            if (!(stored instanceof String xml)) {
                throw new IOException("Unsupported private Flutter project metadata value");
            }
            ensureSizeWithinLimit(xml.getBytes(StandardCharsets.UTF_8).length, "private metadata");
            return validateDocument(parse(new InputSource(new StringReader(xml))),
                    "private project attribute");
        }

        FileObject file = sharedMetadataFile();
        if (file == null) {
            return null;
        }
        ensureSizeWithinLimit(file.getSize(), file.getPath());
        try (InputStream input = file.getInputStream()) {
            InputSource source = new InputSource(input);
            source.setSystemId(file.toURI().toASCIIString());
            return validateDocument(parse(source), file.getPath());
        }
    }

    private Document newDocument() {
        Document document = XMLUtil.createDocument(
                ROOT_ELEMENT,
                METADATA_NAMESPACE,
                null,
                null);
        Element root = document.getDocumentElement();
        root.setAttribute(VERSION_ATTRIBUTE, "1");
        return document;
    }

    private void writeDocument(Document document, boolean shared) throws IOException {
        if (!hasPayload(document)) {
            if (shared) {
                FileObject existing = sharedMetadataFile();
                if (existing != null) {
                    existing.delete();
                }
            } else {
                projectDirectory.setAttribute(PRIVATE_METADATA_ATTRIBUTE, null);
            }
            return;
        }

        byte[] bytes = serializedDocument(document);
        ensureSizeWithinLimit(bytes.length, shared ? "shared metadata" : "private metadata");
        metadataWriter.write(projectDirectory, shared, bytes);
    }

    private static byte[] serializedDocument(Document document) throws IOException {
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        XMLUtil.write(document, serialized, StandardCharsets.UTF_8.name());
        return serialized.toByteArray();
    }

    private FileObject sharedMetadataFile() throws IOException {
        FileObject file = projectDirectory.getFileObject(SHARED_METADATA_PATH);
        if (file == null) {
            return null;
        }

        File localRoot = FileUtil.toFile(projectDirectory);
        File localFile = FileUtil.toFile(file);
        if (localRoot != null && localFile != null) {
            Path realRoot = localRoot.toPath().toRealPath();
            Path realFile = localFile.toPath().toRealPath();
            if (!realFile.startsWith(realRoot)) {
                throw new IOException(
                        "Flutter shared metadata resolves outside project " + realRoot);
            }
        }
        return file;
    }

    private static Element parseFragment(String xml) throws IOException {
        ensureSizeWithinLimit(
                xml.getBytes(StandardCharsets.UTF_8).length,
                "legacy private metadata");
        Document document = parse(new InputSource(new StringReader(xml)));
        validateXmlComplexity(document, "legacy private project attribute");
        return document.getDocumentElement();
    }

    private static Document parse(InputSource source) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                throw new SAXException("External XML entities are not allowed");
            });
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return builder.parse(source);
        } catch (ParserConfigurationException | SAXException | IllegalArgumentException ex) {
            throw new IOException("Invalid Flutter project metadata XML", ex);
        }
    }

    private static Document validateDocument(Document document, String source)
            throws IOException {
        Element root = document.getDocumentElement();
        if (!METADATA_NAMESPACE.equals(root.getNamespaceURI())
                || !ROOT_ELEMENT.equals(root.getLocalName())) {
            throw new IOException("Unsupported Flutter project metadata root in " + source);
        }
        validateXmlComplexity(document, source);
        return document;
    }

    private static void validateXmlComplexity(Document document, String source)
            throws IOException {
        ArrayDeque<NodeDepth> pending = new ArrayDeque<>();
        pending.push(new NodeDepth(document.getDocumentElement(), 1));
        int nodeCount = 0;
        while (!pending.isEmpty()) {
            NodeDepth current = pending.pop();
            if (current.depth() > MAX_XML_DEPTH) {
                throw new IOException("Flutter project metadata exceeds maximum XML depth in "
                        + source);
            }
            nodeCount++;
            if (nodeCount > MAX_XML_NODES) {
                throw new IOException("Flutter project metadata contains too many XML nodes in "
                        + source);
            }
            for (Node child = current.node().getFirstChild(); child != null;
                    child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    pending.push(new NodeDepth(child, current.depth() + 1));
                }
            }
        }
    }

    private static void ensureSizeWithinLimit(long byteCount, String source)
            throws IOException {
        if (byteCount > MAX_METADATA_BYTES) {
            throw new IOException("Flutter project metadata exceeds "
                    + MAX_METADATA_BYTES + " bytes in " + source);
        }
    }

    private static Element detachedCopy(Element fragment) {
        return (Element) fragment.cloneNode(true);
    }

    private static Element findFragment(
            Document document,
            String elementName,
            String namespace) {
        Element fragments = findContainer(document, FRAGMENTS_ELEMENT, false);
        return fragments == null
                ? null
                : findDirectChild(fragments, elementName, namespace);
    }

    private static Element findContainer(
            Document document,
            String localName,
            boolean create) {
        Element root = document.getDocumentElement();
        Element result = findDirectChild(root, localName, METADATA_NAMESPACE);
        if (result == null && create) {
            result = document.createElementNS(METADATA_NAMESPACE, localName);
            root.appendChild(result);
        }
        return result;
    }

    private static Element findDirectChild(
            Element parent,
            String localName,
            String namespace) {
        for (Node child = parent.getFirstChild(); child != null;
                child = child.getNextSibling()) {
            if (child instanceof Element element
                    && localName.equals(element.getLocalName())
                    && namespace.equals(element.getNamespaceURI())) {
                return element;
            }
        }
        return null;
    }

    private static Element findProperty(Document document, String key) throws IOException {
        Element properties = findContainer(document, PROPERTIES_ELEMENT, false);
        return properties == null ? null : findProperty(properties, key);
    }

    private static Element findProperty(Element properties, String key) throws IOException {
        for (Node child = properties.getFirstChild(); child != null;
                child = child.getNextSibling()) {
            if (child instanceof Element element
                    && METADATA_NAMESPACE.equals(element.getNamespaceURI())
                    && PROPERTY_ELEMENT.equals(element.getLocalName())
                    && key.equals(decode(element.getAttribute(KEY_ATTRIBUTE)))) {
                return element;
            }
        }
        return null;
    }

    private void migrateLegacyFragment(Element fragment, String hash) throws IOException {
        synchronized (storageLock) {
            Document document = readOrCreateDocument(false);
            String elementName = fragment.getLocalName();
            String namespace = fragment.getNamespaceURI();
            Element migrations = findContainer(document, MIGRATIONS_ELEMENT, true);
            String identity = migrationIdentity(elementName, namespace);
            Element migration = findMigration(migrations, identity);
            if (migration != null && hash.equals(migration.getAttribute(HASH_ATTRIBUTE))) {
                return;
            }

            // A changed fallback may have been written by an older plugin after
            // a downgrade, so update the fragment and provenance in one commit.
            Element fragments = findContainer(document, FRAGMENTS_ELEMENT, true);
            Element previous = findDirectChild(fragments, elementName, namespace);
            Node imported = document.importNode(fragment, true);
            if (previous == null) {
                fragments.appendChild(imported);
            } else {
                fragments.replaceChild(imported, previous);
            }
            if (migration == null) {
                migration = document.createElementNS(METADATA_NAMESPACE, MIGRATION_ELEMENT);
                migration.setAttribute(IDENTITY_ATTRIBUTE, identity);
                migrations.appendChild(migration);
            }
            migration.setAttribute(HASH_ATTRIBUTE, hash);
            writeDocument(document, false);
        }
    }

    private static Element findMigration(Element migrations, String identity) {
        for (Node child = migrations.getFirstChild(); child != null;
                child = child.getNextSibling()) {
            if (child instanceof Element element
                    && METADATA_NAMESPACE.equals(element.getNamespaceURI())
                    && MIGRATION_ELEMENT.equals(element.getLocalName())
                    && identity.equals(element.getAttribute(IDENTITY_ATTRIBUTE))) {
                return element;
            }
        }
        return null;
    }

    private static void putQuarantineEntry(
            Document document,
            String kind,
            String identity,
            String value) {
        Element quarantine = findContainer(document, QUARANTINE_ELEMENT, true);
        Element entry = findQuarantineEntry(quarantine, kind, identity);
        if (entry == null) {
            entry = document.createElementNS(METADATA_NAMESPACE, QUARANTINE_ENTRY_ELEMENT);
            entry.setAttribute(KIND_ATTRIBUTE, kind);
            entry.setAttribute(IDENTITY_ATTRIBUTE, encode(identity));
            quarantine.appendChild(entry);
        }
        entry.setAttribute(VALUE_ATTRIBUTE, encode(value));
    }

    private static Element findQuarantineEntry(
            Element quarantine,
            String kind,
            String identity) {
        String encodedIdentity = encode(identity);
        for (Node child = quarantine.getFirstChild(); child != null;
                child = child.getNextSibling()) {
            if (child instanceof Element element
                    && METADATA_NAMESPACE.equals(element.getNamespaceURI())
                    && QUARANTINE_ENTRY_ELEMENT.equals(element.getLocalName())
                    && kind.equals(element.getAttribute(KIND_ATTRIBUTE))
                    && encodedIdentity.equals(element.getAttribute(IDENTITY_ATTRIBUTE))) {
                return element;
            }
        }
        return null;
    }

    private static String migrationIdentity(String elementName, String namespace) {
        return encode(namespace + "\u0000" + elementName);
    }

    private static boolean hasPayload(Document document) {
        return hasElementChildren(findContainer(document, FRAGMENTS_ELEMENT, false))
                || hasElementChildren(findContainer(document, PROPERTIES_ELEMENT, false))
                || hasElementChildren(findContainer(document, MIGRATIONS_ELEMENT, false))
                || hasElementChildren(findContainer(document, QUARANTINE_ELEMENT, false));
    }

    private static boolean hasElementChildren(Element parent) {
        if (parent == null) {
            return false;
        }
        for (Node child = parent.getFirstChild(); child != null;
                child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    private static void requireFragmentIdentity(String elementName, String namespace) {
        if (elementName == null || elementName.isBlank()
                || namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException(
                    "Auxiliary configuration fragments require a local name and namespace");
        }
    }

    private static void requireMatchingLegacyAttribute(
            String attributeName,
            String elementName,
            String namespace) throws IOException {
        String identity = attributeName.substring(LEGACY_ATTRIBUTE_PREFIX.length());
        int separator = identity.lastIndexOf('#');
        if (separator <= 0
                || separator == identity.length() - 1
                || !namespace.equals(identity.substring(0, separator))
                || !elementName.equals(identity.substring(separator + 1))) {
            throw new IOException(
                    "Legacy auxiliary metadata attribute does not match its XML fragment");
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) throws IOException {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid encoded Flutter project metadata value", ex);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static void writeMetadata(
            FileObject projectDirectory,
            boolean shared,
            byte[] bytes) throws IOException {
        if (!shared) {
            projectDirectory.setAttribute(
                    TRANSIENT_ATTRIBUTE_PREFIX + PRIVATE_METADATA_ATTRIBUTE,
                    new String(bytes, StandardCharsets.UTF_8));
            return;
        }

        File localRoot = FileUtil.toFile(projectDirectory);
        if (localRoot != null) {
            writeLocalMetadata(localRoot.toPath(), SHARED_METADATA_PATH, bytes);
            FileUtil.refreshFor(localRoot.toPath()
                    .resolve(SHARED_METADATA_PATH.replace('/', File.separatorChar))
                    .toFile());
            refreshRelativePath(projectDirectory, SHARED_METADATA_PATH);
            return;
        }

        FileObject file = FileUtil.createData(projectDirectory, SHARED_METADATA_PATH);
        try (FileLock lock = file.lock(); var output = file.getOutputStream(lock)) {
            output.write(bytes);
        }
    }

    private static void writeLocalMetadata(
            Path root,
            String relativePath,
            byte[] bytes) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path target = normalizedRoot
                .resolve(relativePath.replace('/', File.separatorChar))
                .normalize();
        if (!target.startsWith(normalizedRoot)) {
            throw new IOException("Flutter metadata path escapes its storage root");
        }
        Files.createDirectories(target.getParent());
        Path realRoot = normalizedRoot.toRealPath();
        Path realParent = target.getParent().toRealPath();
        if (!realParent.startsWith(realRoot)) {
            throw new IOException("Flutter shared metadata directory resolves outside project "
                    + realRoot);
        }
        target = realParent.resolve(target.getFileName());
        Path temporary = Files.createTempFile(
                target.getParent(),
                ".flutter-metadata-",
                ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                replaceWithBackup(temporary, target);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void refreshRelativePath(FileObject root, String relativePath) {
        String[] segments = relativePath.split("/");
        FileObject current = root;
        for (int index = 0; index < segments.length && current != null; index++) {
            current.refresh(true);
            if (index < segments.length - 1) {
                current = current.getFileObject(segments[index]);
            }
        }
        if (current != null) {
            current.refresh(true);
        }
    }

    private static void replaceWithBackup(Path temporary, Path target) throws IOException {
        Path backup = null;
        if (Files.exists(target)) {
            try {
                backup = Files.createTempFile(
                        target.getParent(),
                        ".flutter-metadata-backup-",
                        ".tmp");
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException backupFailure) {
                if (backup != null) {
                    try {
                        Files.deleteIfExists(backup);
                    } catch (IOException cleanupFailure) {
                        backupFailure.addSuppressed(cleanupFailure);
                    }
                }
                throw backupFailure;
            }
        }
        boolean removeBackup = false;
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            removeBackup = true;
        } catch (IOException replacementFailure) {
            if (backup != null) {
                try {
                    Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                    removeBackup = true;
                } catch (IOException restoreFailure) {
                    replacementFailure.addSuppressed(restoreFailure);
                    throw new IOException(
                            "Could not replace Flutter metadata or restore its backup at "
                                    + backup,
                            replacementFailure);
                }
            }
            throw replacementFailure;
        } finally {
            if (removeBackup && backup != null) {
                try {
                    Files.deleteIfExists(backup);
                } catch (IOException cleanupFailure) {
                    LOGGER.log(
                            Level.WARNING,
                            "Flutter metadata was saved, but its temporary backup {0} "
                                    + "could not be removed.",
                            backup);
                    LOGGER.log(Level.FINE, "Metadata backup cleanup failure", cleanupFailure);
                }
            }
        }
    }

    private UncheckedIOException storageFailure(
            String operation,
            boolean shared,
            IOException cause) {
        String scope = shared ? "shared" : "private";
        return new UncheckedIOException(
                "Could not " + operation + " " + scope
                        + " metadata for Flutter project " + projectDirectory.getPath(),
                cause);
    }

    private void logReadFailure(String item, boolean shared, IOException cause) {
        String scope = shared ? "shared" : "private";
        String failureKey = scope + "\u0000" + cause.getClass().getName()
                + "\u0000" + cause.getMessage();
        if (!reportedReadFailures.add(failureKey)) {
            return;
        }
        LOGGER.log(
                Level.WARNING,
                "Could not read " + item + " from " + scope
                        + " metadata for Flutter project " + projectDirectory.getPath()
                        + "; the requested value is unavailable and the stored data was "
                        + "preserved: " + cause.getMessage(),
                cause);
    }

    @FunctionalInterface
    interface MetadataWriter {
        void write(FileObject projectDirectory, boolean shared, byte[] bytes) throws IOException;
    }

    private record NodeDepth(Node node, int depth) {
    }
}
