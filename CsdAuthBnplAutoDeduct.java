package com.temenos.tdf.loanRestructure;

import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author mhismail
 *
 */
public class CsdAuthBnplAutoDeduct extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String isCardFlag = "";
        String accountId = "";
        String arrId = "";

        DataAccess da = new DataAccess(this);
        PaymentOrderRecord poRec = new PaymentOrderRecord(currentRecord);
        try {
            isCardFlag = poRec.getLocalRefField("IS.CARD.PAYMENT").getValue();
        } catch (Exception e1) {
        }

        if (isCardFlag.equals("1")) {
            EbBnplLAutoDeductIdTable AutoDeductTable = new EbBnplLAutoDeductIdTable(this);
            accountId = poRec.getCreditAccount().getValue();
            AccountRecord account = new AccountRecord(da.getRecord("ACCOUNT", accountId));
            arrId = account.getArrangementId().getValue();
            try {
                EbBnplLAutoDeductIdRecord AutoDeductRecord = new EbBnplLAutoDeductIdRecord(
                        da.getRecord("EB.BNPL.L.AUTO.DEDUCT.ID", arrId));
            } catch (Exception e) {
                try {
                    AutoDeductTable.write(arrId, null);
                } catch (T24IOException e1) {
                }
            }
        }
    }
}
