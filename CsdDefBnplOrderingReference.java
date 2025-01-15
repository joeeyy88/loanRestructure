package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author anouh
 *
 */

public class CsdDefBnplOrderingReference extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);

        String losId = "";
        String creditAccount = "";
        String aaArrangementId = "";

        PaymentOrderRecord rec = new PaymentOrderRecord(currentRecord);

        creditAccount = rec.getCreditAccount().toString();
        if (creditAccount.startsWith("AA")) {

            aaArrangementId = creditAccount;
        } else {
            try {
                AccountRecord accnt = new AccountRecord(da.getRecord("ACCOUNT", creditAccount));
                aaArrangementId = accnt.getArrangementId().toString();
            } catch (Exception ex) {
            }
        }
        try {
            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", aaArrangementId));           

            losId = aaRec.getAltIdType().get(0).getAlternateId().toString();
            rec.setOrderingReference(losId);
            currentRecord.set(rec.toStructure());
        } catch (Exception ex) {
        }
    }
}
