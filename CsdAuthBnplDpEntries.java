package com.temenos.tdf.loanRestructure;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebbnplhstaticparam.DataNameClass;
import com.temenos.t24.api.records.ebbnplhstaticparam.EbBnplHStaticParamRecord;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author ykumar
 *
 */
public class CsdAuthBnplDpEntries extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        BigDecimal trdDisAmt;
        String vendorAccount = "";
        String upfTrdDisAccount = "";
        String poVersion = "";
        String ofsSource = "";

        Session session = new Session(this);
        String today = session.getCurrentVariable("!TODAY");

        DataAccess da = new DataAccess();
        EbBnplHStaticParamRecord bnplHStaticRec = new EbBnplHStaticParamRecord(
                da.getRecord("EB.BNPL.H.STATIC.PARAM", "BNPL"));

        for (DataNameClass dxn : bnplHStaticRec.getDataName()) {

            switch (dxn.getDataName().getValue()) {
            case "VENDOR.ACCOUNT":
                vendorAccount = dxn.getDataValue(0).toString();
                break;

            case "UPF.TRD.DIS.ACCOUNT":
                upfTrdDisAccount = dxn.getDataValue(0).toString();
                break;

            case "PO.VERSION":
                poVersion = dxn.getDataValue(0).toString();
                break;

            case "OFS.SOURCE":
                ofsSource = dxn.getDataValue(0).toString();
                break;

            default:
            }
        }

        IsContractRecord isContract = new IsContractRecord(currentRecord);

        trdDisAmt = new BigDecimal(isContract.getLocalRefField("L.TRD.DIS.AMT").getValue());
        String profitAmount = trdDisAmt.toString();

        if ((profitAmount != null) && (Double.parseDouble(profitAmount) > 0)) {

            PaymentOrderRecord poRec = new PaymentOrderRecord();
            TransactionData td = new TransactionData();

            td.setFunction("INPUT");
            td.setSourceId(ofsSource);
            td.setVersionId(poVersion);

            poRec.setDebitAccount(vendorAccount);
            poRec.setCreditAccount(upfTrdDisAccount);
            poRec.setPaymentAmount(profitAmount);
            poRec.setPaymentCurrency("SAR");
            poRec.setPaymentExecutionDate(today);
            poRec.setOrderingReference(currentRecordId);

            transactionData.add(td);
            currentRecords.add(poRec.toStructure());

        }
    }

}
