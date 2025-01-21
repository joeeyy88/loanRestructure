package com.temenos.tdf.loanRestructure;

import java.util.List;

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
 * @author anouh
 *
 */
public class CsdInpBnplRefundValidation extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        System.out.println("entered validation routine");
        String refundFlag = "";
        String losId = "";
        String isContractId = "";
        IsContractRecord isContract = null;
        DataAccess da = new DataAccess(this);
        PaymentOrderRecord po = new PaymentOrderRecord(currentRecord);
        losId = po.getOrderingReference().getValue();
        List<String> isContractIdList = da.selectRecords("", "IS.CONTRACT", "", "WITH L.LOS.APPL.ID EQ " + losId);

        System.out.println("isContractIdList" + isContractIdList);

        try {
            if (!isContractIdList.isEmpty()) {
                System.out.println("reading refundFlag logic");
                isContractId = isContractIdList.get(0);
                System.out.println("isContractId " + isContractId);
                isContract = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
                System.out.println("isContract is opened");
                refundFlag = isContract.getLocalRefField("L.REFUND.FLAG").getValue();
                System.out.println("refundFlag = " + refundFlag);
            }

            if (!refundFlag.isEmpty() && refundFlag.equalsIgnoreCase("Y")) {
                po.getOrderingReference().setError("AA-BNPL.REFUND.VALIDATION");
            }
        } catch (Exception ex) {
            System.out.println("error1 = " + ex);
        }

        return po.getValidationResponse();
    }

}