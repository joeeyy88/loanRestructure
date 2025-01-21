package com.temenos.tdf.loanRestructure;

import java.math.BigDecimal;
import java.util.List;
import com.temenos.t24.api.records.ebbnplhstaticparam.*;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author nselsayed
 *
 */
public class CsdAuthBnplIsContractPurchase extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String payInFlag = "";
        String vendorAccount = "";
        String upfTrdDisAccount = "";
        String purchaseAccount = "";
        String poVersion = "";
        String isVersion = "";
        String ofsSource = "";
        String pendingDeliveryAccount = "";
        String downPayAccount = "";
        String loanAccount = "";

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

            case "PENDING.DLVRY.ACCOUNT":
                pendingDeliveryAccount = dxn.getDataValue(0).toString();
                break;

            case "DP.ACCOUNT":
                downPayAccount = dxn.getDataValue(0).toString();
                break;

            case "PURCHASE.ACCOUNT":
                purchaseAccount = dxn.getDataValue(0).toString();
                break;

            case "LOAN.ACCOUNT":
                loanAccount = dxn.getDataValue(0).toString();
                break;

            case "PO.VERSION":
                poVersion = dxn.getDataValue(0).toString();
                break;

            case "IS.VERSION":
                isVersion = dxn.getDataValue(0).toString();
                break;

            case "OFS.SOURCE":
                ofsSource = dxn.getDataValue(0).toString();
                break;

            default:
            }
        }

        IsContractRecord isContract = new IsContractRecord(currentRecord);

        payInFlag = isContract.getLocalRefField("L.PAY.IN.FULL").getValue();

        BigDecimal trdDisAmt = new BigDecimal(isContract.getLocalRefField("L.TRD.DIS.AMT").getValue());
        BigDecimal ypurchaseAmount = new BigDecimal(isContract.getTotPurchasePrice().getValue());
        BigDecimal yPaymentAmount = ypurchaseAmount.subtract(trdDisAmt);

        String merchantAmount = yPaymentAmount.toString();
        String profitAmount = trdDisAmt.toString();
        String purchaseAmount = ypurchaseAmount.toString();

        if (payInFlag.equals("Y")) {
            if (isContract.getStatus().getValue().equals("APPROVAL")) {
                isContract.setStatus("PURCHASE");

                TransactionData isTd = new TransactionData();

                isTd.setFunction("INPUT");
                isTd.setSourceId(ofsSource);
                isTd.setVersionId(isVersion);
                isTd.setTransactionId(currentRecordId);

                transactionData.add(isTd);
                currentRecords.add(isContract.toStructure());

            }
            TransactionData poTd = new TransactionData();

            poTd.setFunction("INPUT");
            poTd.setSourceId(ofsSource);
            poTd.setVersionId(poVersion);

            PaymentOrderRecord poRec1 = formPORecord(vendorAccount, upfTrdDisAccount, profitAmount, "SAR", today,
                    currentRecordId);

            if (poRec1 != null) {
                transactionData.add(poTd);
                currentRecords.add(poRec1.toStructure());
            }

            PaymentOrderRecord poRec2 = formPORecord(loanAccount, purchaseAccount, purchaseAmount, "SAR", today,
                    currentRecordId);

            if (poRec2 != null) {
                transactionData.add(poTd);
                currentRecords.add(poRec2.toStructure());
            }

            PaymentOrderRecord poRec3 = formPORecord(vendorAccount, pendingDeliveryAccount, merchantAmount, "SAR",
                    today, currentRecordId);

            if (poRec3 != null) {
                transactionData.add(poTd);
                currentRecords.add(poRec3.toStructure());
            }

            PaymentOrderRecord poRec4 = formPORecord(pendingDeliveryAccount, downPayAccount, merchantAmount, "SAR",
                    today, currentRecordId);

            if (poRec4 != null) {
                transactionData.add(poTd);
                currentRecords.add(poRec4.toStructure());
            }
        }
    }

    PaymentOrderRecord formPORecord(String debitAcct, String creditAcct, String paymentAmt, String paymentCcy,
            String today, String orderingRef) {

        if (Double.parseDouble(paymentAmt) > 0) {
            PaymentOrderRecord poRec = new PaymentOrderRecord(this);

            poRec.setDebitAccount(debitAcct);
            poRec.setCreditAccount(creditAcct);
            poRec.setPaymentAmount(paymentAmt);
            poRec.setPaymentCurrency(paymentCcy);
            poRec.setPaymentExecutionDate(today);
            poRec.setOrderingReference(orderingRef);

            return poRec;

        } else {
            return null;
        }
    }
}