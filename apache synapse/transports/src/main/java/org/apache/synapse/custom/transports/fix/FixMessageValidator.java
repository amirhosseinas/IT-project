package org.apache.synapse.custom.transports.fix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator for FIX messages to ensure they meet protocol requirements.
 */
public class FixMessageValidator {
    private static final Logger logger = LoggerFactory.getLogger(FixMessageValidator.class);
    
    // Set of required header fields for all FIX messages
    private static final Set<Integer> REQUIRED_HEADER_FIELDS = new HashSet<>();
    
    static {
        REQUIRED_HEADER_FIELDS.add(BeginString.FIELD);
        REQUIRED_HEADER_FIELDS.add(BodyLength.FIELD);
        REQUIRED_HEADER_FIELDS.add(MsgType.FIELD);
        REQUIRED_HEADER_FIELDS.add(SenderCompID.FIELD);
        REQUIRED_HEADER_FIELDS.add(TargetCompID.FIELD);
        REQUIRED_HEADER_FIELDS.add(MsgSeqNum.FIELD);
        REQUIRED_HEADER_FIELDS.add(SendingTime.FIELD);
    }
    
    /**
     * Validate a FIX message
     * 
     * @param message The message to validate
     * @return ValidationResult containing validation status and any error messages
     */
    public static ValidationResult validate(quickfix.Message message) {
        if (message == null) {
            return new ValidationResult(false, "Message is null");
        }
        
        try {
            // Validate required header fields
            for (int field : REQUIRED_HEADER_FIELDS) {
                if (!message.getHeader().isSetField(field)) {
                    return new ValidationResult(false, "Missing required header field: " + field);
                }
            }
            
            // Validate message type specific fields
            String msgType = message.getHeader().getString(MsgType.FIELD);
            ValidationResult msgTypeResult = validateMessageType(message, msgType);
            if (!msgTypeResult.isValid()) {
                return msgTypeResult;
            }
            
            // Validate checksum
            try {
                message.getTrailer().getString(CheckSum.FIELD);
            } catch (FieldNotFound e) {
                return new ValidationResult(false, "Missing checksum field");
            }
            
            return new ValidationResult(true, "Message is valid");
        } catch (Exception e) {
            logger.warn("Error validating FIX message", e);
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Validate message type specific fields
     * 
     * @param message The message to validate
     * @param msgType The message type
     * @return ValidationResult containing validation status and any error messages
     */
    private static ValidationResult validateMessageType(quickfix.Message message, String msgType) {
        try {
            // Validate based on message type
            switch (msgType) {
                case MsgType.LOGON:
                    return validateLogon(message);
                case MsgType.HEARTBEAT:
                    return validateHeartbeat(message);
                case MsgType.TEST_REQUEST:
                    return validateTestRequest(message);
                case MsgType.RESEND_REQUEST:
                    return validateResendRequest(message);
                case MsgType.REJECT:
                    return validateReject(message);
                case MsgType.SEQUENCE_RESET:
                    return validateSequenceReset(message);
                case MsgType.LOGOUT:
                    return validateLogout(message);
                case MsgType.NEW_ORDER_SINGLE:
                    return validateNewOrderSingle(message);
                case MsgType.EXECUTION_REPORT:
                    return validateExecutionReport(message);
                case MsgType.ORDER_CANCEL_REQUEST:
                    return validateOrderCancelRequest(message);
                case MsgType.ORDER_CANCEL_REJECT:
                    return validateOrderCancelReject(message);
                default:
                    // For other message types, just return valid
                    return new ValidationResult(true, "Message type " + msgType + " validation not implemented");
            }
        } catch (Exception e) {
            logger.warn("Error validating message type: " + msgType, e);
            return new ValidationResult(false, "Message type validation error: " + e.getMessage());
        }
    }
    
    private static ValidationResult validateLogon(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(EncryptMethod.FIELD)) {
            return new ValidationResult(false, "Logon message missing EncryptMethod field");
        }
        if (!message.isSetField(HeartBtInt.FIELD)) {
            return new ValidationResult(false, "Logon message missing HeartBtInt field");
        }
        return new ValidationResult(true, "Logon message is valid");
    }
    
    private static ValidationResult validateHeartbeat(quickfix.Message message) {
        // No required fields for heartbeat beyond standard header
        return new ValidationResult(true, "Heartbeat message is valid");
    }
    
    private static ValidationResult validateTestRequest(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(TestReqID.FIELD)) {
            return new ValidationResult(false, "TestRequest message missing TestReqID field");
        }
        return new ValidationResult(true, "TestRequest message is valid");
    }
    
    private static ValidationResult validateResendRequest(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(BeginSeqNo.FIELD)) {
            return new ValidationResult(false, "ResendRequest message missing BeginSeqNo field");
        }
        if (!message.isSetField(EndSeqNo.FIELD)) {
            return new ValidationResult(false, "ResendRequest message missing EndSeqNo field");
        }
        return new ValidationResult(true, "ResendRequest message is valid");
    }
    
    private static ValidationResult validateReject(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(RefSeqNum.FIELD)) {
            return new ValidationResult(false, "Reject message missing RefSeqNum field");
        }
        return new ValidationResult(true, "Reject message is valid");
    }
    
    private static ValidationResult validateSequenceReset(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(NewSeqNo.FIELD)) {
            return new ValidationResult(false, "SequenceReset message missing NewSeqNo field");
        }
        return new ValidationResult(true, "SequenceReset message is valid");
    }
    
    private static ValidationResult validateLogout(quickfix.Message message) {
        // No required fields for logout beyond standard header
        return new ValidationResult(true, "Logout message is valid");
    }
    
    private static ValidationResult validateNewOrderSingle(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(ClOrdID.FIELD)) {
            return new ValidationResult(false, "NewOrderSingle message missing ClOrdID field");
        }
        if (!message.isSetField(Symbol.FIELD)) {
            return new ValidationResult(false, "NewOrderSingle message missing Symbol field");
        }
        if (!message.isSetField(Side.FIELD)) {
            return new ValidationResult(false, "NewOrderSingle message missing Side field");
        }
        if (!message.isSetField(OrdType.FIELD)) {
            return new ValidationResult(false, "NewOrderSingle message missing OrdType field");
        }
        if (!message.isSetField(TransactTime.FIELD)) {
            return new ValidationResult(false, "NewOrderSingle message missing TransactTime field");
        }
        return new ValidationResult(true, "NewOrderSingle message is valid");
    }
    
    private static ValidationResult validateExecutionReport(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(OrderID.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing OrderID field");
        }
        if (!message.isSetField(ExecID.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing ExecID field");
        }
        if (!message.isSetField(ExecType.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing ExecType field");
        }
        if (!message.isSetField(OrdStatus.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing OrdStatus field");
        }
        if (!message.isSetField(Symbol.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing Symbol field");
        }
        if (!message.isSetField(Side.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing Side field");
        }
        if (!message.isSetField(LeavesQty.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing LeavesQty field");
        }
        if (!message.isSetField(CumQty.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing CumQty field");
        }
        if (!message.isSetField(AvgPx.FIELD)) {
            return new ValidationResult(false, "ExecutionReport message missing AvgPx field");
        }
        return new ValidationResult(true, "ExecutionReport message is valid");
    }
    
    private static ValidationResult validateOrderCancelRequest(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(OrigClOrdID.FIELD)) {
            return new ValidationResult(false, "OrderCancelRequest message missing OrigClOrdID field");
        }
        if (!message.isSetField(ClOrdID.FIELD)) {
            return new ValidationResult(false, "OrderCancelRequest message missing ClOrdID field");
        }
        if (!message.isSetField(Symbol.FIELD)) {
            return new ValidationResult(false, "OrderCancelRequest message missing Symbol field");
        }
        if (!message.isSetField(Side.FIELD)) {
            return new ValidationResult(false, "OrderCancelRequest message missing Side field");
        }
        if (!message.isSetField(TransactTime.FIELD)) {
            return new ValidationResult(false, "OrderCancelRequest message missing TransactTime field");
        }
        return new ValidationResult(true, "OrderCancelRequest message is valid");
    }
    
    private static ValidationResult validateOrderCancelReject(quickfix.Message message) throws FieldNotFound {
        if (!message.isSetField(OrderID.FIELD)) {
            return new ValidationResult(false, "OrderCancelReject message missing OrderID field");
        }
        if (!message.isSetField(ClOrdID.FIELD)) {
            return new ValidationResult(false, "OrderCancelReject message missing ClOrdID field");
        }
        if (!message.isSetField(OrigClOrdID.FIELD)) {
            return new ValidationResult(false, "OrderCancelReject message missing OrigClOrdID field");
        }
        if (!message.isSetField(OrdStatus.FIELD)) {
            return new ValidationResult(false, "OrderCancelReject message missing OrdStatus field");
        }
        if (!message.isSetField(CxlRejResponseTo.FIELD)) {
            return new ValidationResult(false, "OrderCancelReject message missing CxlRejResponseTo field");
        }
        return new ValidationResult(true, "OrderCancelReject message is valid");
    }
    
    /**
     * Result of a validation operation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
} 