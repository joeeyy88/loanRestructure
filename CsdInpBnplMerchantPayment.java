package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author ykumar
 *
 */
public class CsdInpBnplMerchantPayment extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub

        String merchantPayFlag = "";
        String isContractId = "";
        IsContractRecord isContractRec = null;

        DataAccess da = new DataAccess(this);

        PaymentOrderRecord po = new PaymentOrderRecord(currentRecord);
        isContractId = po.getOrderingReference().getValue();

        isContractRec = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
        merchantPayFlag = isContractRec.getLocalRefField("L.MER.PAY.FLAG").getValue();

        if (merchantPayFlag.equals("Y")) {

            po.getOrderingReference().setError("EB-MERCHANT.ALREADY.CREDITED");

        }
        return po.getValidationResponse();
    }

}
