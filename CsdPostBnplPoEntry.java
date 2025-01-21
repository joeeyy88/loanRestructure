package com.temenos.tdf.loanRestructure;

import java.math.BigDecimal;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.ebbnplhstaticparam.DataNameClass;
import com.temenos.t24.api.records.ebbnplhstaticparam.EbBnplHStaticParamRecord;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mhismail
 *
 */

public class CsdPostBnplPoEntry extends ActivityLifecycle {

    @Override
    public void postCoreTableUpdate(
            com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord accountDetailRecord,
            com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord arrangementActivityRecord,
            com.temenos.t24.api.complex.aa.activityhook.ArrangementContext arrangementContext,
            com.temenos.t24.api.records.aaarrangement.AaArrangementRecord arrangementRecord,
            com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord,
            com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord productRecord, TStructure record,
            List<com.temenos.t24.api.complex.aa.activityhook.TransactionData> transactionData,
            List<TStructure> transactionRecord) {

        if (arrangementContext.getActivityStatus().equals("AUTH")) {
            IsContractRecord isContract = null;
            String pendingDeliveryAccount = "";
            String downPayAccount = "";
            String poVersion = "";
            String ofsSource = "";
            String vendorAccount = "";
            PaymentOrderRecord paymentOrder1 = null;
            PaymentOrderRecord paymentOrder2 = null;

            DataAccess da = new DataAccess(this);
            Session session = new Session(this);
            String today = session.getCurrentVariable("!TODAY");

            EbBnplHStaticParamRecord bnplParamRecord = new EbBnplHStaticParamRecord(
                    da.getRecord("EB.BNPL.H.STATIC.PARAM", "BNPL"));

            for (DataNameClass dxn : bnplParamRecord.getDataName()) {

                switch (dxn.getDataName().getValue()) {

                case "VENDOR.ACCOUNT":
                    vendorAccount = dxn.getDataValue(0).toString();
                    break;

                case "PENDING.DLVRY.ACCOUNT":
                    pendingDeliveryAccount = dxn.getDataValue(0).toString();
                    break;

                case "DP.ACCOUNT":
                    downPayAccount = dxn.getDataValue(0).toString();
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

            String isContractId = masterActivityRecord.getLocalRefField("IS.CONTRACT.REF").getValue();

            isContract = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
            String eChannelFlag = isContract.getLocalRefField("L.E.CHANNEL").getValue();

            BigDecimal purchaseAmount = new BigDecimal(isContract.getTotPurchasePrice().getValue());
            BigDecimal profitAmount = new BigDecimal(isContract.getLocalRefField("L.TRD.DIS.AMT").getValue());
            BigDecimal yPaymentAmount = purchaseAmount.subtract(profitAmount);

            String paymentAmount = yPaymentAmount.toString();

            TransactionData poTd = new TransactionData();

            poTd.setFunction("INPUT");
            poTd.setSourceId(ofsSource);
            poTd.setVersionId(poVersion);

            paymentOrder1 = formPORecord(vendorAccount, pendingDeliveryAccount, paymentAmount, "SAR", today,
                    isContractId);

            if (paymentOrder1 != null) {
                transactionData.add(poTd);
                transactionRecord.add(paymentOrder1.toStructure());
            }

            if (eChannelFlag.equals("Y")) {

                paymentOrder2 = formPORecord(pendingDeliveryAccount, downPayAccount, paymentAmount, "SAR", today,
                        isContractId);

                if (paymentOrder2 != null) {
                    transactionData.add(poTd);
                    transactionRecord.add(paymentOrder2.toStructure());
                }
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
