package clouddiskclient.model;

import clouddiskclient.util.FXMLContainer;
import common.OperationResult;
import common.OperationType;

public interface FormController {
	void setContainer(FXMLContainer container);
	default void processOperation(OperationType ot, OperationResult or) {
	}
}
