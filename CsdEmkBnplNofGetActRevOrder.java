package com.temenos.tdf.loanRestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.records.portransaction.PorTransactionRecord;

/**
 * TODO: Document me!
 *
 * 
 * @author nselsayed , mhisamil
 */
public class CsdEmkBnplNofGetActRevOrder extends Enquiry {
    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        // TODO Auto-generated method stub
        System.out.println("New Print");
        List<String> outDetails = new ArrayList<>();
        String contractId = "";
        String outputLine = "";
        DataAccess da = new DataAccess(this);
        String activity = "";
        String newArrAct = "";
        String tempPO = "";
        String paymentSystemId = "";
        List<String> poList;
        PorTransactionRecord porRec;
        PaymentOrderRecord poRec = null;
        String billChannelRef = "";
        String reasonDetails = "";
        String paymentChannelDetails = "";
        String productCodes = "";
        String paymentTypes = "";
        String poAmount = "";
        String pshId = "";
        String billChannelPaymentReference = "";
        String reason = "";
        String paymentChannel = "";
        String productCode = "";
        String paymentType = "";
        String amt = "";

        // table
        EbEmkHStaticParamRecord reversalOrderRec = new EbEmkHStaticParamRecord(
                da.getRecord("EB.EMK.H.STATIC.PARAM", "BNPL.REVERSE.ACTIVITY.CONFIG"));
        System.out.println("before checking criteria");
        List<TField> tempList = reversalOrderRec.getDataName(0).getDataValue();
        List<String> orderList = new ArrayList<String>();
        System.out.println(
                "TEMP LIST BEFORE DOING ANYTHING: " + tempList + "after convert to str: " + tempList.toString());
        Iterator tempItr = tempList.iterator();
        while (tempItr.hasNext()) {
            TField val = (TField) tempItr.next();
            orderList.add(val.toString());
        }

        System.out.println("order from table: " + orderList.toString());
        if (!filterCriteria.isEmpty()) {
            Iterator var6 = filterCriteria.iterator();

            while (var6.hasNext()) {
                FilterCriteria selectionCrit = (FilterCriteria) var6.next();
                if (selectionCrit.getFieldname().equals("arrangementId")) {
                    System.out.println("selectionCrit.getFieldname() = " + selectionCrit.getFieldname());
                    contractId = selectionCrit.getValue();
                }
            }

            System.out.println("arrId= " + contractId + "?");
        }

        if (!contractId.isEmpty()) {
            Contract contract = new Contract(this);
            contract.setContractId(contractId);
            AaActivityHistoryRecord activityHisRec = new AaActivityHistoryRecord(
                    da.getRecord("AA.ACTIVITY.HISTORY", contractId));

            List<EffectiveDateClass> actEffectiveDtList = activityHisRec.getEffectiveDate();
            Iterator effDateIterator = actEffectiveDtList.iterator();
            // System.out.println("acitivty list" + actEffectiveDtList.size());
            while (effDateIterator.hasNext()) {
                EffectiveDateClass effDateCls = (EffectiveDateClass) effDateIterator.next();
                Iterator actRefIterator = effDateCls.getActivityRef().iterator();

                while (actRefIterator.hasNext()) {
                    ActivityRefClass actRefCls = (ActivityRefClass) actRefIterator.next();
                    System.out.println("currentActRef obj = " + actRefCls.toString());
                    activity = actRefCls.getActivity().getValue();
                    System.out.println("activity ==== " + activity);
                    if (orderList.contains(activity)) {
                        if (activity.equals("LENDING-NEW-ARRANGEMENT")) {
                            newArrAct = actRefCls.getActivityRef().getValue();
                        } else {
                            System.out.println("actStatus= " + actRefCls.getActStatus().getValue());
                            if (actRefCls.getActStatus().getValue().equals("AUTH")) {
                                // GET ACITIVITY PSH ID AFTER USING THE PO
                                tempPO = actRefCls.getContractId().getValue();
                                System.out.println("tempPO now =" + tempPO);
                                poList = Arrays.asList(tempPO.split("\\\\"));
                                System.out.println("poList now =" + poList);
                                tempPO = poList.get(0);
                                System.out.println("tempPO now after split=" + tempPO);
                                try {
                                    // THIS CHANGE IS STILL NOT DEPLOYED
                                    if (tempPO.contains("BNK")) {
                                        // outputLine = outputLine +
                                        // actRefCls.getActivityRef().getValue()
                                        // + ",";
                                        // Change to include the BNK-<BNK of PO>
                                        // instead of AACT ID
                                        outputLine = outputLine + poList.get(2) + "-" + tempPO + ",";
                                        porRec = new PorTransactionRecord(da.getRecord("POR.TRANSACTION", tempPO));
                                        System.out.println("PO ID =" + porRec.getSendersreferenceincoming().getValue()
                                                + "////END");
                                        String poID = porRec.getSendersreferenceincoming().getValue();
                                        try {
                                            poRec = new PaymentOrderRecord(da.getRecord("PAYMENT.ORDER", poID));
                                            System.out.println("after getting PO from LIVE");
                                        } catch (Exception e) {

                                            poRec = new PaymentOrderRecord(da.getHistoryRecord("PAYMENT.ORDER", poID));
                                            System.out.println("poRec L.PSH.ID = "
                                                    + poRec.getLocalRefField("L.PSH.ID").getValue() + "//END");
                                            System.out.println(
                                                    "poRec amount = " + poRec.getPaymentAmount().getValue() + "//END");
                                        }

                                        try {
                                            pshId = poRec.getLocalRefField("L.PSH.ID").getValue();
                                            paymentSystemId = paymentSystemId + pshId + ",";
                                        } catch (Exception ex) {
                                            // Handle exception if needed
                                        }

                                        try {
                                            billChannelPaymentReference = poRec.getLocalRefField("L.BILLCH.REF.NO")
                                                    .getValue();
                                            billChannelRef = billChannelRef + billChannelPaymentReference + ",";
                                        } catch (Exception ex) {
                                            // Handle exception if needed
                                        }

                                        try {
                                            reason = poRec.getLocalRefField("L.APL.REASON").getValue();
                                            reasonDetails = reasonDetails + reason + ",";
                                        } catch (Exception ex) {
                                            // Handle exception if needed
                                        }

                                        try {
                                            paymentChannel = poRec.getLocalRefField("L.PAY.CHANNEL").getValue();
                                            paymentChannelDetails = paymentChannelDetails + paymentChannel + ",";
                                        } catch (Exception ex) {
                                            // Handle exception if needed
                                        }

                                        try {
                                            productCode = poRec.getLocalRefField("L.PRODUCT.CODE").getValue();
                                            productCodes = productCodes + productCode + ",";
                                        } catch (Exception ex) {
                                            // Handle exception if needed
                                        }

                                        try {
                                            paymentType = poRec.getLocalRefField("L.APL.PYMT.TYPE").getValue();
                                            paymentTypes = paymentTypes + paymentType + ",";
                                        } catch (Exception ex) {
                                            // Handle exception if needed
                                        }

                                        try {
                                            amt = poRec.getPaymentAmount().getValue();
                                            poAmount = poAmount + amt + ",";
                                        } catch (Exception ex) {
                                            // Handle exception if needed
                                        }

                                    }

                                } catch (Exception e) {
                                    System.out.println("in opening por or po");
                                    System.out.println(e);
                                }
                            }

                        }

                        System.out.println("added to the list. =" + activity);
                    }
                }
            }
        }

        System.out.println("output line =" + outputLine + "////END");
        System.out.println("paymentSystemId line =" + paymentSystemId + "////END");
        System.out.println("poAmount =" + poAmount + "////END");

        if (!outputLine.isEmpty()) {
            if (outputLine.charAt(outputLine.length() - 1) == ',') {
                outputLine = outputLine.substring(0, outputLine.length() - 1);
            }

            if (!paymentSystemId.isEmpty() && paymentSystemId.charAt(paymentSystemId.length() - 1) == ',') {
                paymentSystemId = paymentSystemId.substring(0, paymentSystemId.length() - 1);
            }

            if (!poAmount.isEmpty() && poAmount.charAt(poAmount.length() - 1) == ',') {
                poAmount = poAmount.substring(0, poAmount.length() - 1);
            }

            if (!billChannelRef.isEmpty() && billChannelRef.charAt(billChannelRef.length() - 1) == ',') {
                billChannelRef = billChannelRef.substring(0, billChannelRef.length() - 1);
            }

            if (!reasonDetails.isEmpty() && reasonDetails.charAt(reasonDetails.length() - 1) == ',') {
                reasonDetails = reasonDetails.substring(0, reasonDetails.length() - 1);
            }

            if (!paymentChannelDetails.isEmpty()
                    && paymentChannelDetails.charAt(paymentChannelDetails.length() - 1) == ',') {
                paymentChannelDetails = paymentChannelDetails.substring(0, paymentChannelDetails.length() - 1);
            }

            if (!productCodes.isEmpty() && productCodes.charAt(productCodes.length() - 1) == ',') {
                productCodes = productCodes.substring(0, productCodes.length() - 1);
            }

            if (!paymentTypes.isEmpty() && paymentTypes.charAt(paymentTypes.length() - 1) == ',') {
                paymentTypes = paymentTypes.substring(0, paymentTypes.length() - 1);
            }

            outputLine = newArrAct + "*" + outputLine + "*" + poAmount + "*" + paymentSystemId + "*" + billChannelRef
                    + "*" + paymentChannelDetails + "*" + reasonDetails + "*" + productCodes + "*" + paymentTypes;
        } else {
            outputLine = newArrAct;
        }

        outDetails.add(outputLine);
        return outDetails;
    }

}
