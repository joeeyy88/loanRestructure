package com.temenos.tdf.loanRestructure;

import com.temenos.t24.api.hook.system.RecordLifecycle;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.records.ebbnplhstaticparam.DataNameClass;
import com.temenos.t24.api.records.ebbnplhstaticparam.EbBnplHStaticParamRecord;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author wielsheikh
 *
 */
public class CsdAuthBnplMerchantPayment extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String merchantPayFlag = "";
        String isVersion = "";
        String ofsSource = "";
        String isContractId = "";
        IsContractRecord isContractRec = null;

        DataAccess da = new DataAccess(this);

        EbBnplHStaticParamRecord bnplHStaticRec = new EbBnplHStaticParamRecord(
                da.getRecord("EB.BNPL.H.STATIC.PARAM", "BNPL"));

        for (DataNameClass dxn : bnplHStaticRec.getDataName()) {

            switch (dxn.getDataName().getValue()) {

            case "OFS.SOURCE":
                ofsSource = dxn.getDataValue(0).toString();
                break;

            case "IS.VERSION":
                isVersion = dxn.getDataValue(0).toString();
                break;

            default:
            }
        }

        PaymentOrderRecord po = new PaymentOrderRecord(currentRecord);
        isContractId = po.getOrderingReference().getValue();

        isContractRec = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
        merchantPayFlag = isContractRec.getLocalRefField("L.MER.PAY.FLAG").getValue();

        if (merchantPayFlag.isEmpty()) {

            isContractRec.getLocalRefField("L.MER.PAY.FLAG").setValue("Y");
            TransactionData td = new TransactionData();
            td.setFunction("INPUT");
            td.setSourceId(ofsSource);
            td.setVersionId(isVersion);
            td.setTransactionId(isContractId);
            currentRecords.add(isContractRec.toStructure());
            transactionData.add(td);

        }

    }

}
