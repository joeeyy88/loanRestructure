package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author mhismail
 *
 */
public class CsdDefBnplOrderingCustomer extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);
        PaymentOrderRecord poRec = new PaymentOrderRecord(currentRecord);
        String orderReference = poRec.getOrderingReference().getValue();

        String arrRec = "";
        String aaId = "";
        arrRec = da.getConcatValues("AA.ARRANGEMENT.ALTERNATE.ID", orderReference).toString();
        if (arrRec != null && !arrRec.isEmpty()) {

            String trimmedInput = arrRec.substring(1, arrRec.length() - 1);

            String[] parts = trimmedInput.split("\\*");
            aaId = parts[1];

        }
        if (!aaId.isEmpty()) {
            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", aaId));
            String accNo = aaRec.getLinkedAppl(0).getLinkedApplId().getValue();
            String Customer = aaRec.getCustomer(0).getCustomer().toString();
            poRec.setCreditAccount(accNo);
            poRec.setOrderingCustomer(Customer);
            currentRecord.set(poRec.toStructure());
        }

    }

}
