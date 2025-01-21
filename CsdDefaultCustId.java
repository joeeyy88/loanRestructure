package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.CustomerRole;
import com.temenos.t24.api.records.account.AccountRecord;

/**
 * TODO: Document me!
 *
 * @author ykumar
 *
 */
public class CsdDefaultCustId extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        PaymentOrderRecord pORec = new PaymentOrderRecord(currentRecord);
        String arrId = pORec.getCreditAccount().getValue();

        DataAccess dataAccess = new DataAccess(this);
        if (!arrId.isEmpty()) {
            try {
                Contract contract = new Contract(this);
                contract.setContractId(arrId);
                CustomerRole custRole = contract.getCustomerRole();

                pORec.setOrderingCustomer(custRole.getCustomerId());
            } catch (Exception var20) {
                AccountRecord acctRec = new AccountRecord(dataAccess.getRecord("ACCOUNT", arrId));
                pORec.setOrderingCustomer(acctRec.getCustomer().getValue());
                currentRecord.set(pORec.toStructure());
            }
        }
    }
}
