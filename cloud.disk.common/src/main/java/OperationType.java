import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum OperationType {
    AUTHORIZATION(1),
    FILE_LIST_REQUEST(2),
    UPLOAD_FILE(3),
    DOWNLOAD_FILE(4),
    FILE_STATUS(5),
    ERROR(6);

    private final int signalByte;
    private static final Map<Integer, OperationType> operationsHash = Collections.unmodifiableMap(initializeOperationsHash());

    OperationType(int signalByte) {
        this.signalByte = signalByte;
    }

    public int getSignalByte() {
        return signalByte;
    }

    public static OperationType getOperation(int signalByte) {
        if (operationsHash == null) initializeOperationsHash();
        return operationsHash.getOrDefault(signalByte, null);
    }

    private static Map<Integer, OperationType> initializeOperationsHash() {
        Map<Integer, OperationType> funcResult = new HashMap<>(2);
        for (OperationType key : OperationType.values()) funcResult.put(key.signalByte, key);
        return funcResult;
    }

}