package dev.flutter.netbeans.plugin.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Secure, bounded, typed handoff of private state across project moves. */
final class FlutterMoveHandoff {
    static final String HANDOFF_PATH = ".netbeans/.flutter-move-handoff-v1.xml";
    static final String TARGET_HANDOFF_PATH =
            ".netbeans/.flutter-move-handoff-v1.target.xml";
    static final String COMMITTED_HANDOFF_PATH =
            ".netbeans/.flutter-move-handoff-v1.committed.xml";

    private static final String NAMESPACE = "urn:dev.flutter.netbeans:move-handoff:1";
    private static final String ROOT_ELEMENT = "move-handoff";
    private static final String ENTRY_ELEMENT = "entry";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String PHASE_ATTRIBUTE = "phase";
    private static final String TRANSACTION_ID_ATTRIBUTE = "transaction-id";
    private static final String SOURCE_URI_ATTRIBUTE = "source-uri";
    private static final String TARGET_DISPLAY_NAME_ATTRIBUTE =
            "target-display-name";
    private static final String VERSION = "1";
    private static final int MAX_HANDOFF_BYTES = 16 * 1024 * 1024;
    private static final int MAX_ENTRIES = 1024;
    private static final int MAX_XML_DEPTH = 8;
    private static final int MAX_XML_EVENTS = 8192;
    private static final int MAX_ENTRY_PAYLOAD_CHARS = MAX_HANDOFF_BYTES;
    private static final int MAX_SOURCE_URI_LENGTH = 8192;
    private static final int MAX_DISPLAY_NAME_LENGTH = 2048;
    private static final Pattern OWNED_TEMP_NAME = Pattern.compile(
            "\\.flutter-move-handoff-v1-(?:prepared|target|committed)-"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                    + "[0-9a-f]{4}-[0-9a-f]{12}\\.tmp");

    private final FileObject projectDirectory;

    FlutterMoveHandoff(FileObject projectDirectory) {
        this.projectDirectory = Objects.requireNonNull(
                projectDirectory,
                "projectDirectory");
    }

    boolean exists() throws IOException {
        cleanupOwnedTemps();
        return file(COMMITTED_HANDOFF_PATH) != null
                || file(TARGET_HANDOFF_PATH) != null
                || file(HANDOFF_PATH) != null;
    }

    Transaction writePrepared(Map<String, Object> snapshot, String sourceUri)
            throws IOException {
        if (exists()) {
            throw new IOException(
                    "A pending Flutter move handoff already exists at "
                            + projectDirectory.getPath() + "/" + HANDOFF_PATH);
        }
        Transaction transaction = checkedTransaction(
                snapshot,
                Phase.PREPARED,
                UUID.randomUUID().toString(),
                sourceUri,
                null);
        writeNew(HANDOFF_PATH, transaction);
        return transaction;
    }

    Transaction writeTargetIntent(
            Transaction previous,
            String targetDisplayName) throws IOException {
        requireCurrent(previous);
        if (previous.phase() != Phase.PREPARED) {
            throw new IOException(
                    "A Flutter move target intent can only follow PREPARED");
        }
        String normalizedName = requireTargetDisplayName(targetDisplayName);
        Transaction target = checkedTransaction(
                previous.snapshot(),
                Phase.TARGET_READY,
                previous.transactionId(),
                previous.sourceUri(),
                normalizedName);
        writePhase(TARGET_HANDOFF_PATH, target);
        return target;
    }

    Transaction writeCommitted(Transaction previous) throws IOException {
        requireCurrent(previous);
        if (previous.phase() != Phase.TARGET_READY) {
            throw new IOException(
                    "A Flutter move commit can only follow TARGET_READY");
        }
        Transaction committed = checkedTransaction(
                previous.snapshot(),
                Phase.COMMITTED,
                previous.transactionId(),
                previous.sourceUri(),
                previous.targetDisplayName());
        writePhase(COMMITTED_HANDOFF_PATH, committed);
        return committed;
    }

    Transaction read() throws IOException {
        cleanupOwnedTemps();
        FileObject file = effectiveFile();
        if (file == null) {
            throw new IOException(
                    "Flutter move handoff is missing from "
                            + projectDirectory.getPath());
        }
        Transaction effective = readFile(file);
        FileObject preparedFile = file(HANDOFF_PATH);
        FileObject targetFile = file(TARGET_HANDOFF_PATH);
        if (preparedFile != null && preparedFile != file) {
            requirePredecessor(
                    readFile(preparedFile),
                    Phase.PREPARED,
                    effective,
                    false);
        }
        if (targetFile != null && targetFile != file) {
            requirePredecessor(
                    readFile(targetFile),
                    Phase.TARGET_READY,
                    effective,
                    true);
        }
        return effective;
    }

    List<FileObject> existingOwnedFiles() throws IOException {
        List<FileObject> files = new ArrayList<>();
        addExisting(files, HANDOFF_PATH);
        addExisting(files, TARGET_HANDOFF_PATH);
        addExisting(files, COMMITTED_HANDOFF_PATH);

        requireStorageFolderContained();
        FileObject folder = projectDirectory.getFileObject(".netbeans");
        if (folder != null) {
            requireContained(folder);
            for (FileObject child : folder.getChildren()) {
                if (child.isData()
                        && OWNED_TEMP_NAME.matcher(child.getNameExt()).matches()) {
                    requireContained(child);
                    files.add(child);
                }
            }
        }
        return List.copyOf(files);
    }

    private void addExisting(List<FileObject> files, String path)
            throws IOException {
        FileObject existing = file(path);
        if (existing != null) {
            files.add(existing);
        }
    }

    void delete() throws IOException {
        cleanupOwnedTemps();
        // Delete predecessor phases first. At every crash boundary the newest,
        // complete transaction remains available until it is deleted last.
        deleteFile(HANDOFF_PATH);
        deleteFile(TARGET_HANDOFF_PATH);
        deleteFile(COMMITTED_HANDOFF_PATH);
        cleanupOwnedTemps();
        if (exists()) {
            throw new IOException(
                    "Flutter move handoff could not be removed from "
                            + projectDirectory.getPath());
        }
    }

    private void writePhase(String path, Transaction transaction)
            throws IOException {
        FileObject existing = file(path);
        if (existing != null) {
            requireSameTransaction(transaction, readFile(existing));
            return;
        }
        writeNew(path, transaction);
    }

    private void writeNew(String path, Transaction transaction)
            throws IOException {
        requireStorageFolderContained();
        cleanupOwnedTemps();
        byte[] bytes = serialize(transaction);
        if (bytes.length > MAX_HANDOFF_BYTES) {
            throw new IOException(
                    "Flutter move handoff exceeds " + MAX_HANDOFF_BYTES + " bytes");
        }

        FileObject folder = FileUtil.createFolder(projectDirectory, ".netbeans");
        requireContained(folder);
        String finalName = finalName(path);
        String temporaryName = ".flutter-move-handoff-v1-"
                + phaseName(path)
                + "-"
                + UUID.randomUUID();
        FileObject temporary = folder.createData(temporaryName, "tmp");
        FileObject completed = null;
        try {
            requireContained(temporary);
            writeBytes(temporary, bytes);
            requireSameTransaction(transaction, readFile(temporary));
            try (FileLock lock = temporary.lock()) {
                temporary.rename(lock, finalName, "xml");
            }
            completed = folder.getFileObject(finalName, "xml");
            if (completed == null) {
                throw new IOException("Flutter move handoff disappeared after its rename");
            }
            requireContained(completed);
            requireSameTransaction(transaction, readFile(completed));
        } catch (IOException | RuntimeException ex) {
            FileObject cleanup = completed != null ? completed : temporary;
            if (cleanup.isValid()) {
                try {
                    cleanup.delete();
                } catch (IOException cleanupFailure) {
                    ex.addSuppressed(cleanupFailure);
                }
            }
            throw ex;
        }
    }

    static boolean snapshotsEqual(
            Map<String, Object> first,
            Map<String, Object> second) {
        if (!first.keySet().equals(second.keySet())) {
            return false;
        }
        for (String key : first.keySet()) {
            if (!valuesEqual(first.get(key), second.get(key))) {
                return false;
            }
        }
        return true;
    }

    static boolean valuesEqual(Object first, Object second) {
        if (first instanceof byte[] firstBytes
                && second instanceof byte[] secondBytes) {
            return Arrays.equals(firstBytes, secondBytes);
        }
        if (first instanceof URL firstUrl && second instanceof URL secondUrl) {
            return firstUrl.toExternalForm().equals(secondUrl.toExternalForm());
        }
        return Objects.equals(first, second);
    }

    private static Transaction checkedTransaction(
            Map<String, Object> snapshot,
            Phase phase,
            String transactionId,
            String sourceUri,
            String targetDisplayName) throws IOException {
        Map<String, Object> safeSnapshot = checkedCopy(snapshot);
        Objects.requireNonNull(phase, "phase");
        String safeTransactionId = requireTransactionId(transactionId);
        String safeSourceUri = requireSourceUri(sourceUri);
        String safeTargetName = targetDisplayName == null
                ? null
                : requireTargetDisplayName(targetDisplayName);
        if (phase == Phase.PREPARED && safeTargetName != null) {
            throw new IOException(
                    "A prepared Flutter move handoff must not have a target display name");
        }
        if (phase != Phase.PREPARED && safeTargetName == null) {
            throw new IOException(
                    "A target-ready or committed Flutter move handoff requires a display name");
        }
        return new Transaction(
                safeSnapshot,
                phase,
                safeTransactionId,
                safeSourceUri,
                safeTargetName);
    }

    private static Map<String, Object> checkedCopy(Map<String, Object> snapshot)
            throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.size() > MAX_ENTRIES) {
            throw new IOException(
                    "Flutter move handoff has too many entries: " + snapshot.size());
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            String name = Objects.requireNonNull(entry.getKey(), "handoff entry name");
            requireSafeName(name);
            Object value = Objects.requireNonNull(
                    entry.getValue(),
                    "handoff entry value");
            encodeValue(value);
            copy.put(name, copyValue(value));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        return value instanceof byte[] bytes ? bytes.clone() : value;
    }

    private static byte[] serialize(Transaction transaction)
            throws IOException {
        Document document = XMLUtil.createDocument(
                ROOT_ELEMENT,
                NAMESPACE,
                null,
                null);
        Element root = document.getDocumentElement();
        root.setAttribute(VERSION_ATTRIBUTE, VERSION);
        root.setAttribute(PHASE_ATTRIBUTE, transaction.phase().xmlValue());
        root.setAttribute(
                TRANSACTION_ID_ATTRIBUTE,
                transaction.transactionId());
        root.setAttribute(
                SOURCE_URI_ATTRIBUTE,
                encodeUtf8(transaction.sourceUri()));
        if (transaction.targetDisplayName() != null) {
            root.setAttribute(
                    TARGET_DISPLAY_NAME_ATTRIBUTE,
                    encodeUtf8(transaction.targetDisplayName()));
        }
        for (Map.Entry<String, Object> entry : transaction.snapshot().entrySet()) {
            EncodedValue encoded = encodeValue(entry.getValue());
            Element element = document.createElementNS(NAMESPACE, ENTRY_ELEMENT);
            element.setAttribute(NAME_ATTRIBUTE, encodeUtf8(entry.getKey()));
            element.setAttribute(TYPE_ATTRIBUTE, encoded.type());
            element.appendChild(document.createTextNode(encoded.payload()));
            root.appendChild(element);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLUtil.write(document, output, StandardCharsets.UTF_8.name());
        return output.toByteArray();
    }

    private Transaction readFile(FileObject file) throws IOException {
        requireContained(file);
        byte[] bytes = readBounded(file);
        return parseTransaction(bytes);
    }

    private byte[] readBounded(FileObject file) throws IOException {
        requireContained(file);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        try (InputStream input = file.getInputStream()) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_HANDOFF_BYTES) {
                    throw new IOException(
                            "Flutter move handoff exceeds "
                                    + MAX_HANDOFF_BYTES + " bytes");
                }
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private static Transaction parseTransaction(byte[] bytes) throws IOException {
        XMLStreamReader reader = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
            factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
                throw new XMLStreamException(
                        "External XML resources are forbidden in a Flutter move handoff");
            });
            reader = factory.createXMLStreamReader(
                    new ByteArrayInputStream(bytes),
                    StandardCharsets.UTF_8.name());
            return readTransaction(reader);
        } catch (IllegalArgumentException | XMLStreamException ex) {
            throw new IOException("Invalid Flutter move handoff XML", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                    // The complete document was already consumed or rejected.
                }
            }
        }
    }

    private static Transaction readTransaction(XMLStreamReader reader)
            throws IOException, XMLStreamException {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        RootFields root = null;
        EntryFields entry = null;
        int depth = 0;
        int events = 1;
        int entries = 0;
        boolean rootClosed = false;
        while (reader.hasNext()) {
            int event = reader.next();
            if (++events > MAX_XML_EVENTS) {
                throw new IOException("Flutter move handoff has too many XML events");
            }
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    if (++depth > MAX_XML_DEPTH) {
                        throw new IOException(
                                "Flutter move handoff XML is too deeply nested");
                    }
                    if (depth == 1) {
                        if (root != null || rootClosed) {
                            throw new IOException(
                                    "Flutter move handoff has multiple roots");
                        }
                        root = readRootFields(reader);
                    } else if (depth == 2 && entry == null) {
                        requireElement(reader, ENTRY_ELEMENT);
                        if (++entries > MAX_ENTRIES) {
                            throw new IOException(
                                    "Flutter move handoff has too many entries");
                        }
                        entry = readEntryFields(reader);
                    } else {
                        throw new IOException(
                                "Unexpected nested element in Flutter move handoff");
                    }
                }
                case XMLStreamConstants.CHARACTERS,
                        XMLStreamConstants.CDATA,
                        XMLStreamConstants.SPACE -> {
                    String text = reader.getText();
                    if (entry == null) {
                        if (!text.isBlank()) {
                            throw new IOException(
                                    "Unexpected text in Flutter move handoff");
                        }
                    } else {
                        if (entry.payload().length() + text.length()
                                > MAX_ENTRY_PAYLOAD_CHARS) {
                            throw new IOException(
                                    "Flutter move handoff entry payload is too large");
                        }
                        entry.payload().append(text);
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if (depth == 2) {
                        requireElement(reader, ENTRY_ELEMENT);
                        if (entry == null) {
                            throw new IOException(
                                    "Missing Flutter move handoff entry state");
                        }
                        String name = decodeUtf8(entry.encodedName());
                        requireSafeName(name);
                        Object value = decodeValue(
                                entry.type(),
                                entry.payload().toString());
                        if (snapshot.putIfAbsent(name, value) != null) {
                            throw new IOException(
                                    "Duplicate Flutter move handoff entry " + name);
                        }
                        entry = null;
                    } else if (depth == 1) {
                        requireElement(reader, ROOT_ELEMENT);
                        rootClosed = true;
                    } else {
                        throw new IOException(
                                "Unexpected end element in Flutter move handoff");
                    }
                    depth--;
                }
                case XMLStreamConstants.START_DOCUMENT,
                        XMLStreamConstants.END_DOCUMENT,
                        XMLStreamConstants.COMMENT -> {
                    // Counted but semantically inert.
                }
                case XMLStreamConstants.DTD,
                        XMLStreamConstants.ENTITY_REFERENCE -> throw new IOException(
                                "DTD and entity references are forbidden in a Flutter move handoff");
                default -> throw new IOException(
                        "Unexpected XML event in Flutter move handoff: " + event);
            }
        }
        if (root == null || !rootClosed || depth != 0 || entry != null) {
            throw new IOException("Incomplete Flutter move handoff XML");
        }
        return checkedTransaction(
                snapshot,
                root.phase(),
                root.transactionId(),
                root.sourceUri(),
                root.targetDisplayName());
    }

    private static RootFields readRootFields(XMLStreamReader reader)
            throws IOException {
        requireElement(reader, ROOT_ELEMENT);
        Map<String, String> attributes = readAttributes(
                reader,
                VERSION_ATTRIBUTE,
                PHASE_ATTRIBUTE,
                TRANSACTION_ID_ATTRIBUTE,
                SOURCE_URI_ATTRIBUTE,
                TARGET_DISPLAY_NAME_ATTRIBUTE);
        String version = requiredAttribute(attributes, VERSION_ATTRIBUTE);
        if (!VERSION.equals(version)) {
            throw new IOException("Unsupported Flutter move handoff version " + version);
        }
        Phase phase = Phase.fromXmlValue(
                requiredAttribute(attributes, PHASE_ATTRIBUTE));
        String transactionId = requiredAttribute(
                attributes,
                TRANSACTION_ID_ATTRIBUTE);
        String sourceUri = decodeUtf8(requiredAttribute(
                attributes,
                SOURCE_URI_ATTRIBUTE));
        String encodedTargetName = attributes.get(TARGET_DISPLAY_NAME_ATTRIBUTE);
        String targetDisplayName = encodedTargetName == null
                ? null
                : decodeUtf8(encodedTargetName);
        return new RootFields(
                phase,
                transactionId,
                sourceUri,
                targetDisplayName);
    }

    private static EntryFields readEntryFields(XMLStreamReader reader)
            throws IOException {
        Map<String, String> attributes = readAttributes(
                reader,
                NAME_ATTRIBUTE,
                TYPE_ATTRIBUTE);
        return new EntryFields(
                requiredAttribute(attributes, NAME_ATTRIBUTE),
                requiredAttribute(attributes, TYPE_ATTRIBUTE),
                new StringBuilder());
    }

    private static Map<String, String> readAttributes(
            XMLStreamReader reader,
            String... allowedNames) throws IOException {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (int index = 0; index < reader.getAttributeCount(); index++) {
            String namespace = reader.getAttributeNamespace(index);
            String name = reader.getAttributeLocalName(index);
            boolean allowed = namespace == null || namespace.isEmpty();
            if (allowed) {
                allowed = false;
                for (String allowedName : allowedNames) {
                    if (allowedName.equals(name)) {
                        allowed = true;
                        break;
                    }
                }
            }
            if (!allowed) {
                throw new IOException(
                        "Unexpected attribute in Flutter move handoff: " + name);
            }
            if (attributes.putIfAbsent(name, reader.getAttributeValue(index)) != null) {
                throw new IOException(
                        "Duplicate attribute in Flutter move handoff: " + name);
            }
        }
        return attributes;
    }

    private static String requiredAttribute(
            Map<String, String> attributes,
            String name) throws IOException {
        String value = attributes.get(name);
        if (value == null) {
            throw new IOException(
                    "Missing attribute in Flutter move handoff: " + name);
        }
        return value;
    }

    private static void requireElement(
            XMLStreamReader reader,
            String expectedLocalName) throws IOException {
        if (!NAMESPACE.equals(reader.getNamespaceURI())
                || !expectedLocalName.equals(reader.getLocalName())) {
            throw new IOException(
                    "Unexpected element in Flutter move handoff: "
                            + reader.getName());
        }
    }

    private static String requireSourceUri(String sourceUri) throws IOException {
        if (sourceUri == null
                || sourceUri.isBlank()
                || sourceUri.length() > MAX_SOURCE_URI_LENGTH) {
            throw new IOException("Invalid Flutter move handoff source URI");
        }
        try {
            URI parsed = new URI(sourceUri);
            if (!parsed.isAbsolute()) {
                throw new IOException("Flutter move handoff source URI is not absolute");
            }
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid Flutter move handoff source URI", ex);
        }
        return sourceUri;
    }

    private static String requireTransactionId(String transactionId)
            throws IOException {
        if (transactionId == null || transactionId.length() != 36) {
            throw new IOException("Invalid Flutter move handoff transaction ID");
        }
        try {
            String canonical = UUID.fromString(transactionId).toString();
            if (!canonical.equals(transactionId)) {
                throw new IOException("Non-canonical Flutter move handoff transaction ID");
            }
            return canonical;
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid Flutter move handoff transaction ID", ex);
        }
    }

    private static String requireTargetDisplayName(String targetDisplayName)
            throws IOException {
        if (targetDisplayName == null) {
            throw new IOException("Missing Flutter move target display name");
        }
        String normalized = targetDisplayName.strip();
        if (normalized.isEmpty()
                || normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IOException("Invalid Flutter move target display name");
        }
        return normalized;
    }

    private static void requireSafeName(String name) throws IOException {
        boolean allowed = FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE.equals(name)
                || hasNonEmptySuffix(
                        name,
                        FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX)
                || hasNonEmptySuffix(
                        name,
                        FlutterProjectMetadata.EXTERNAL_PAYLOAD_QUARANTINE_PREFIX);
        if (!allowed || name.length() > 2048 || name.indexOf('/') >= 0) {
            throw new IOException(
                    "Untrusted Flutter move handoff entry name " + name);
        }
    }

    private static boolean hasNonEmptySuffix(String value, String prefix) {
        if (!value.startsWith(prefix) || value.length() == prefix.length()) {
            return false;
        }
        String suffix = value.substring(prefix.length());
        return suffix.matches("[A-Za-z0-9_-]+(?:\\.[1-9][0-9]*)?");
    }

    private static EncodedValue encodeValue(Object value) throws IOException {
        if (value instanceof String string) {
            return encoded("string", string);
        }
        if (value instanceof Boolean bool) {
            return encoded("boolean", bool.toString());
        }
        if (value instanceof Byte number) {
            return encoded("byte", number.toString());
        }
        if (value instanceof Short number) {
            return encoded("short", number.toString());
        }
        if (value instanceof Integer number) {
            return encoded("integer", number.toString());
        }
        if (value instanceof Long number) {
            return encoded("long", number.toString());
        }
        if (value instanceof Float number) {
            return encoded("float", number.toString());
        }
        if (value instanceof Double number) {
            return encoded("double", number.toString());
        }
        if (value instanceof Character character) {
            return encoded("character", Integer.toString(character));
        }
        if (value instanceof URL url) {
            return encoded("url", url.toExternalForm());
        }
        if (value instanceof byte[] bytes) {
            return new EncodedValue("bytes", encodeBytes(bytes));
        }
        throw new IOException(
                "Unsupported Flutter move handoff value type "
                        + value.getClass().getName());
    }

    private static EncodedValue encoded(String type, String value) {
        return new EncodedValue(type, encodeUtf8(value));
    }

    private static Object decodeValue(String type, String payload)
            throws IOException {
        try {
            return switch (type) {
                case "string" -> decodeUtf8(payload);
                case "boolean" -> decodeBoolean(payload);
                case "byte" -> Byte.valueOf(decodeUtf8(payload));
                case "short" -> Short.valueOf(decodeUtf8(payload));
                case "integer" -> Integer.valueOf(decodeUtf8(payload));
                case "long" -> Long.valueOf(decodeUtf8(payload));
                case "float" -> Float.valueOf(decodeUtf8(payload));
                case "double" -> Double.valueOf(decodeUtf8(payload));
                case "character" -> decodeCharacter(payload);
                case "url" -> decodeUrl(payload);
                case "bytes" -> decodeBytes(payload);
                default -> throw new IOException(
                        "Unsupported Flutter move handoff value type " + type);
            };
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid numeric Flutter move handoff value", ex);
        }
    }

    private static Boolean decodeBoolean(String payload) throws IOException {
        String value = decodeUtf8(payload);
        if ("true".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equals(value)) {
            return Boolean.FALSE;
        }
        throw new IOException("Invalid boolean Flutter move handoff value");
    }

    private static Character decodeCharacter(String payload) throws IOException {
        int value;
        try {
            value = Integer.parseInt(decodeUtf8(payload));
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid character Flutter move handoff value", ex);
        }
        if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) {
            throw new IOException("Character Flutter move handoff value is out of range");
        }
        return (char) value;
    }

    private static URL decodeUrl(String payload) throws IOException {
        String externalForm = decodeUtf8(payload);
        try {
            return new URI(externalForm).toURL();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw new IOException("Invalid URL Flutter move handoff value", ex);
        }
    }

    private static String encodeUtf8(String value) {
        return encodeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeUtf8(String value) throws IOException {
        byte[] bytes = decodeBytes(value);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ex) {
            throw new IOException("Invalid UTF-8 Flutter move handoff value", ex);
        }
    }

    private static String encodeBytes(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] decodeBytes(String value) throws IOException {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid Base64 Flutter move handoff value", ex);
        }
    }

    private static void writeBytes(FileObject file, byte[] bytes) throws IOException {
        try (FileLock lock = file.lock(); var output = file.getOutputStream(lock)) {
            output.write(bytes);
        }
    }

    private void requireCurrent(Transaction expected) throws IOException {
        requireSameTransaction(expected, read());
    }

    private FileObject effectiveFile() throws IOException {
        FileObject committed = file(COMMITTED_HANDOFF_PATH);
        if (committed != null) {
            return committed;
        }
        FileObject target = file(TARGET_HANDOFF_PATH);
        return target != null ? target : file(HANDOFF_PATH);
    }

    private FileObject file(String path) throws IOException {
        requireStorageFolderContained();
        FileObject file = projectDirectory.getFileObject(path);
        if (file != null) {
            requireContained(file);
        }
        return file;
    }

    private void deleteFile(String path) throws IOException {
        FileObject file = file(path);
        if (file != null) {
            requireContained(file);
            file.delete();
        }
        if (file(path) != null) {
            throw new IOException(
                    "Flutter move handoff phase could not be removed: " + path);
        }
    }

    private static String finalName(String path) {
        int slash = path.lastIndexOf('/');
        String fileName = slash < 0 ? path : path.substring(slash + 1);
        if (!fileName.endsWith(".xml")) {
            throw new IllegalArgumentException("Handoff phase path must end in .xml");
        }
        return fileName.substring(0, fileName.length() - ".xml".length());
    }

    private static String phaseName(String path) {
        return switch (path) {
            case HANDOFF_PATH -> "prepared";
            case TARGET_HANDOFF_PATH -> "target";
            case COMMITTED_HANDOFF_PATH -> "committed";
            default -> throw new IllegalArgumentException(
                    "Unknown Flutter move handoff phase path " + path);
        };
    }

    private void cleanupOwnedTemps() throws IOException {
        requireStorageFolderContained();
        FileObject folder = projectDirectory.getFileObject(".netbeans");
        if (folder == null) {
            return;
        }
        requireContained(folder);
        for (FileObject child : folder.getChildren()) {
            if (child.isData()
                    && OWNED_TEMP_NAME.matcher(child.getNameExt()).matches()) {
                requireContained(child);
                child.delete();
                if (child.isValid()) {
                    throw new IOException(
                            "Orphan Flutter move handoff temporary file could not be removed: "
                                    + child.getPath());
                }
            }
        }
    }

    private void requireStorageFolderContained() throws IOException {
        File localRoot = FileUtil.toFile(projectDirectory);
        if (localRoot == null) {
            return;
        }
        Path normalizedRoot = localRoot.toPath().toAbsolutePath().normalize();
        Path storageFolder = normalizedRoot.resolve(".netbeans").normalize();
        if (!storageFolder.startsWith(normalizedRoot)) {
            throw new IOException("Flutter move handoff path escapes its project root");
        }
        Path realRoot = normalizedRoot.toRealPath();
        if (Files.exists(storageFolder, LinkOption.NOFOLLOW_LINKS)) {
            Path realStorageFolder = storageFolder.toRealPath();
            if (!realStorageFolder.startsWith(realRoot)) {
                throw new IOException(
                        "Flutter move handoff directory resolves outside project "
                                + realRoot);
            }
        }
    }

    private void requireContained(FileObject candidate) throws IOException {
        File localRoot = FileUtil.toFile(projectDirectory);
        File localCandidate = FileUtil.toFile(candidate);
        if (localRoot == null || localCandidate == null) {
            return;
        }
        Path realRoot = localRoot.toPath().toRealPath();
        Path realCandidate = localCandidate.toPath().toRealPath();
        if (!realCandidate.startsWith(realRoot)) {
            throw new IOException(
                    "Flutter move handoff resolves outside project " + realRoot);
        }
    }

    private static void requireSameTransaction(
            Transaction expected,
            Transaction actual) throws IOException {
        if (expected.phase() != actual.phase()
                || !expected.transactionId().equals(actual.transactionId())
                || !expected.sourceUri().equals(actual.sourceUri())
                || !Objects.equals(
                        expected.targetDisplayName(),
                        actual.targetDisplayName())
                || !snapshotsEqual(expected.snapshot(), actual.snapshot())) {
            throw new IOException("Flutter move handoff failed its read-back verification");
        }
    }

    private static void requirePredecessor(
            Transaction predecessor,
            Phase expectedPhase,
            Transaction effective,
            boolean requireSameTargetName) throws IOException {
        if (predecessor.phase() != expectedPhase
                || !predecessor.transactionId().equals(effective.transactionId())
                || !predecessor.sourceUri().equals(effective.sourceUri())
                || (requireSameTargetName
                        && !Objects.equals(
                                predecessor.targetDisplayName(),
                                effective.targetDisplayName()))
                || !snapshotsEqual(
                        predecessor.snapshot(),
                        effective.snapshot())) {
            throw new IOException("Inconsistent Flutter move handoff phase chain");
        }
    }

    private record EncodedValue(String type, String payload) {
    }

    private record RootFields(
            Phase phase,
            String transactionId,
            String sourceUri,
            String targetDisplayName) {
    }

    private record EntryFields(
            String encodedName,
            String type,
            StringBuilder payload) {
    }

    record Transaction(
            Map<String, Object> snapshot,
            Phase phase,
            String transactionId,
            String sourceUri,
            String targetDisplayName) {
    }

    enum Phase {
        PREPARED("prepared"),
        TARGET_READY("target-ready"),
        COMMITTED("committed");

        private final String xmlValue;

        Phase(String xmlValue) {
            this.xmlValue = xmlValue;
        }

        String xmlValue() {
            return xmlValue;
        }

        static Phase fromXmlValue(String value) throws IOException {
            for (Phase phase : values()) {
                if (phase.xmlValue.equals(value)) {
                    return phase;
                }
            }
            throw new IOException("Unsupported Flutter move handoff phase " + value);
        }
    }

}
