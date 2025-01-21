package com.temenos.tdf.loanRestructure;

import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.records.iscontract.DpAccountClass;
import com.temenos.t24.api.records.iscontract.DpCommodityClass;

/**
 * TODO: Document me!
 *
 * @author mhismail
 *
 */
public class CsdChkBnplSetDpAmt extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub

        IsContractRecord isContract = new IsContractRecord(currentRecord);
        List<DpCommodityClass> yDpCommodity = isContract.getDpCommodity();

        for (DpCommodityClass dpCommodity : yDpCommodity) {
            String dpValue = dpCommodity.getDpValue().getValue();
            List<DpAccountClass> yDpAccount = dpCommodity.getDpAccount();

            dpCommodity.getDpContribType(0).setDpContribAmt(dpValue);

            for (DpAccountClass dpacc : yDpAccount) {
                try {
                    dpacc.setDpRecAmount(dpValue);

                } catch (Exception var20) {
                }
            }

        }
        currentRecord.set(isContract.toStructure());
    }
}