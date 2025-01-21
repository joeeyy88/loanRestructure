package com.temenos.tdf.loanRestructure;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebbnpllpoconcatid.EbBnplLPoConcatIdRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebbnpllpoconcatid.EbBnplLPoConcatIdTable;
import java.util.List;

/**
 * TODO: Document me!
 *
 * @author mhismail
 *
 */
public class CsdAuthBnplPoConcatId extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String orderingRef;
        DataAccess da = new DataAccess();
        int pos;
        PaymentOrderRecord poRec = new PaymentOrderRecord(currentRecord);
        orderingRef = poRec.getOrderingReference().getValue();

        try {

            EbBnplLPoConcatIdRecord poContractRecord = new EbBnplLPoConcatIdRecord(
                    da.getRecord("EB.BNPL.L.PO.CONCAT.ID", orderingRef));
            EbBnplLPoConcatIdTable poContractTable = new EbBnplLPoConcatIdTable(this);
            pos = poContractRecord.getPoRef().size();

            poContractRecord.setPoRef(currentRecordId, pos);

            poContractTable.write(orderingRef, poContractRecord);
        } catch (Exception e) {
            EbBnplLPoConcatIdRecord poContractRecord = new EbBnplLPoConcatIdRecord(this);
            EbBnplLPoConcatIdTable poContractTable = new EbBnplLPoConcatIdTable(this);
            poContractRecord.setPoRef(currentRecordId, 0);
            try {
                poContractTable.write(orderingRef, poContractRecord);
            } catch (T24IOException e1) {

            }
        }

    }
}
