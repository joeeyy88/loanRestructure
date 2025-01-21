package com.temenos.tdf.loanRestructure;

import com.temenos.api.TDate;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.OutstandingBalances;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentDueType;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentMethod;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentSchedule;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.CustomerClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PayPropertyClass;
import com.temenos.t24.api.records.aabilldetails.PaymentTypeClass;
import com.temenos.t24.api.records.aaprddessettlement.AaPrdDesSettlementRecord;
import com.temenos.t24.api.records.aaprddessettlement.PayinCurrencyClass;
import com.temenos.t24.api.records.aaproperty.AaPropertyRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.system.DataAccess;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * TODO: Document me!
 *
 * @author mhismail, anouh
 *
 */

public class CsdEmkBnplContractDetails extends Enquiry {

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> outDetails = new ArrayList<String>();
        String contractId = "";
        String arbUniqueReference = "";
        String arrStatus = "";

        String acctName = "ACCOUNT";
        if (!filterCriteria.isEmpty()) {
            Iterator var8 = filterCriteria.iterator();

            while (var8.hasNext()) {
                FilterCriteria selectionCrit = (FilterCriteria) var8.next();
                if (selectionCrit.getFieldname().equals("CallUniqueReference")) {
                    arbUniqueReference = selectionCrit.getValue();
                }

                if (selectionCrit.getFieldname().equals("contractId")) {
                    contractId = selectionCrit.getValue();
                }
            }
        }

        if (!contractId.isEmpty()) {
            String outPut = "";

            Contract contract = new Contract(this);
            contract.setContractId(contractId);
            DataAccess dataAccess = new DataAccess(this);
            String aaId = contractId;
            try {
                AaArrangementRecord aaArrRec = new AaArrangementRecord(
                        dataAccess.getRecord("AA.ARRANGEMENT", contractId));
                arrStatus = aaArrRec.getArrStatus().getValue();

                AccountRecord acctRec = new AccountRecord(
                        dataAccess.getRecord(acctName, aaArrRec.getLinkedAppl(0).getLinkedApplId().getValue()));
                String cic = acctRec.getCustomer().getValue();
                String customerRelationShipWithLoan = "APPLICANT";
                Iterator var15 = aaArrRec.getCustomer().iterator();

                while (var15.hasNext()) {
                    CustomerClass tempcust = (CustomerClass) var15.next();
                    if (tempcust.getCustomer().getValue().equals(cic)) {
                        cic = tempcust.getCustomer().getValue();
                        customerRelationShipWithLoan = tempcust.getCustomerRole().getValue();
                    }
                }

                String contractCurrency = contract.getContract().getCurrency().getValue();
                String loanBookingDate = contract.getContract().getStartDate().getValue();
                String repaymentAccount = "";
                AaPrdDesSettlementRecord settlementRec = new AaPrdDesSettlementRecord(
                        contract.getConditionForProperty("SETTLEMENT"));
                if (!settlementRec.getPayinCurrency().isEmpty()) {
                    repaymentAccount = settlementRec.getPayinCurrency().get(0).getDdMandateRef(0)
                            .getPayinAccount().getValue();
                }

                List<String> outSummary = this.getInstallmentSummary(contract, new TDate(loanBookingDate),
                        contract.getMaturityDate(), acctName, arrStatus);
                String term = contract.getTerm();
                String totalPrincipal = contract.getTermAmount().get();
                String totalProfit = outSummary.get(9);
                BigDecimal totalAmount = BigDecimal.ZERO;
                totalAmount = (new BigDecimal(totalPrincipal)).add(new BigDecimal(totalProfit));
                BigDecimal totalBalance = BigDecimal.ZERO;
                BigDecimal totAcctBal = BigDecimal.ZERO;
                BigDecimal totIntBal = BigDecimal.ZERO;

                try {
                    new OutstandingBalances();
                    OutstandingBalances arrOutstandingBalances = contract.getOutstandingBalance();
                    totAcctBal = (new BigDecimal(arrOutstandingBalances.getAccountBalance().doubleValue())).abs();
                    totIntBal = (new BigDecimal(arrOutstandingBalances.getInterestBalance().doubleValue())).abs();
                    totalBalance = totAcctBal.add(totIntBal);
                } catch (Exception var27) {
                    var27.getMessage();
                }

                String totalOutStandingAmt = totalBalance.setScale(2, RoundingMode.HALF_UP).toString();
                String totalOutStandingPrincipal = totAcctBal.setScale(2, RoundingMode.HALF_UP).toString();

                String altId = aaArrRec.getAltIdType(0).getAlternateId().getValue();

                // ------------------------------------------
                List<String> isRecords = null;
                String isContId = "";
                String downPayment = "";
                String dpPaymentId = "";
                String dpBillChannelPaymentReference = "";
                String dpPaymentType = "";
                String dpPaymentChannel = "";
                String dpProductCode = "";
                String dpPaymentRemarks = "";
                String dpReason = "";

                isRecords = dataAccess.getConcatValues("IS.CONTRACT.L.LOS.APPL.ID", altId);
                String yparts = isRecords.toString();

                if (isRecords != null && !isRecords.isEmpty()) {
                    String trimmedInput = yparts.substring(1, yparts.length() - 1);
                    String[] parts = trimmedInput.split("\\*");
                    isContId = parts[1];
                }
                try {
                    IsContractRecord isconrec = new IsContractRecord(dataAccess.getRecord("IS.CONTRACT", isContId));
                    downPayment = isconrec.getTotalDpAmt().getValue();    
                    
                    try {
                        dpPaymentId = isconrec.getLocalRefField("L.PSH.ID").getValue();
                    }
                    catch(Exception ex) {
                    }
                    
                    try {                        
                        dpBillChannelPaymentReference = isconrec.getLocalRefField("L.BILLCH.REF.NO").getValue();
                    }
                    catch(Exception ex) {
                    }
                    
                    try {
                        dpPaymentType = isconrec.getLocalRefField("L.APL.PYMT.TYPE").getValue();
                    }
                    catch(Exception ex) {
                    }
                    
                    try {
                        dpPaymentChannel = isconrec.getLocalRefField("L.PAY.CHANNEL").getValue();
                    }
                    catch(Exception ex) {
                    }
                    
                    try {
                        dpProductCode = isconrec.getLocalRefField("L.PRODUCT.CODE").getValue();
                    }
                    catch(Exception ex) {
                    }
                    
                    try {
                        dpPaymentRemarks = isconrec.getLocalRefField("L.REMARKS").getValue();
                    }
                    catch(Exception ex) {
                    }
                    
                    try {
                        dpReason = isconrec.getLocalRefField("L.APL.REASON").getValue();
                    }
                    catch(Exception ex) {
                    }
                } catch (Exception e) {
                }
                // ----------------------------------------------

                outPut = cic + "*" + contractId + "*" + customerRelationShipWithLoan + "*" + contractCurrency + "*"
                        + totalAmount + "*" + totalPrincipal + "*" + totalProfit + "*" + totalOutStandingAmt + "*"
                        + totalOutStandingPrincipal + "*" + loanBookingDate + "*" + outSummary.get(0) + "*"
                        + outSummary.get(1) + "*" + outSummary.get(2) + "*" + repaymentAccount + "*"
                        + outSummary.get(3) + "*" + outSummary.get(5) + "*"
                        + outSummary.get(6) + "*" + outSummary.get(7) + "*"
                        + outSummary.get(8) + "*" + arbUniqueReference + "*" + altId + "*" + downPayment + "*"
                        + arrStatus + "*" + term + "*" + isContId + "*"
                        + dpPaymentId + "*" + dpBillChannelPaymentReference + "*" + dpPaymentType + "*" + dpPaymentChannel + "*"
                        + dpProductCode + "*" + dpPaymentRemarks + "*" + dpReason;
                outDetails.add(outPut);
            } catch (Exception var28) {
                String empty = "";
                String downPayment = "";
                String totalAmount = "";
                String term = "";
                String altId = "";
                String isContractId = "";

                List<String> tableDetails = this.getDetailsFromBnplCancelTable(aaId, dataAccess);
                System.out.println("table details = " + tableDetails);
                if (tableDetails.size() > 0) {
                    // cancellation table read successfully
                    arrStatus = tableDetails.get(0);
                    downPayment = tableDetails.get(1);
                    totalAmount = tableDetails.get(2);
                    term = tableDetails.get(3);
                    altId = tableDetails.get(4);
                    isContractId = tableDetails.get(5);

                    // totalPrincipal = termAmount
                    // totalAmount should be "totalPrincipal + totalProfit", so
                    // termAmount = "termAmount + totalProfit"
                    // but since in BNPL no profit, we wrote totalAmount =
                    // termAmount
                } else {
                    // cancellation table wasn't read
                    // do nothing
                }
                
                // need to debug the contractCurrency in the cancellation case because it returns "0.00" instead of empty string

                outPut = empty + "*" + contractId + "*" + empty + "*" + empty + "*" + totalAmount + "*" + empty + "*"
                        + empty + "*" + empty + "*" + empty + "*" + empty + "*" + empty + "*" + empty + "*" + empty
                        + "*" + empty + "*" + empty + "*" + empty + "*" + empty + "*" + empty + "*" + empty + "*"
                        + arbUniqueReference + "*" + altId + "*" + downPayment + "*" + arrStatus + "*" + term + "*"
                        + isContractId + "*"
                        + empty + "*" + empty + "*" + empty + "*" + empty + "*"
                        + empty + "*" + empty + "*" + empty;
                outDetails.add(outPut);
            }
        }

        return outDetails;
    }

    private List<String> getDetailsFromBnplCancelTable(String arrangementId, DataAccess da) {
        List<String> tableDetails = new ArrayList<String>();
        String losId = "";
        String downPayment = "";
        String arrangementStatus = "";
        String termAmount = "";
        String term = "";
        String isContractId = "";
        try {
            System.out.println("Start second saving try");
            EbBnplCancelAaDetsRecord bnplCancelAaDetsRecord = new EbBnplCancelAaDetsRecord(
                    da.getRecord("EB.BNPL.CANCEL.AA.DETS", arrangementId));
            System.out.println("Opening bnpl record");

            arrangementStatus = bnplCancelAaDetsRecord.getStatus().getValue();
            downPayment = bnplCancelAaDetsRecord.getDpAmount().getValue();
            termAmount = bnplCancelAaDetsRecord.getTermAmount().getValue();
            term = bnplCancelAaDetsRecord.getTerm().getValue();
            losId = bnplCancelAaDetsRecord.getLosId().getValue();
            isContractId = bnplCancelAaDetsRecord.getIsContractId().getValue();

            tableDetails.add(arrangementStatus);
            tableDetails.add(downPayment);
            tableDetails.add(termAmount);
            tableDetails.add(term);
            tableDetails.add(losId);
            tableDetails.add(isContractId);
        } catch (Exception ex) {
        }
        return tableDetails;
    }

    private List<String> getInstallmentSummary(Contract contract, TDate startDate, TDate endDate, String acctName,
            String arrStatus) {
        List<String> outSummary = new ArrayList();
        Integer totalInstallments = 0;
        String dueDateFirstInstallment = "";
        String dueDateLastInstallment = "";
        BigDecimal totalPropfitBD = BigDecimal.ZERO;
        String instDueDate = "";
        BigDecimal singleInstallAmount = BigDecimal.ZERO;
        String instAmount = "";
        String instNo = "";
        String instStatus = "";
        int paidBills;
        int installmentNumLeft;
        DataAccess dataAccess = new DataAccess(this);
        List<RepaymentSchedule> repaymentList = contract.getRepaymentSchedule(startDate, endDate);
        Map<String, String> billDetailsMap = this.getBillDetails(dataAccess, contract, acctName);
        Iterator var20 = repaymentList.iterator();

        while (var20.hasNext()) {
            RepaymentSchedule repaymentSchedule = (RepaymentSchedule) var20.next();
            String dueAmount = "";
            Iterator var23 = repaymentSchedule.getRepaymentDueType().iterator();

            while (var23.hasNext()) {
                RepaymentDueType repaymentDueType = (RepaymentDueType) var23.next();
                Iterator var25 = repaymentDueType.getRepaymentMethod().iterator();

                while (var25.hasNext()) {
                    RepaymentMethod repaymentMethod = (RepaymentMethod) var25.next();
                    AaPropertyRecord aaProperty = new AaPropertyRecord(
                            dataAccess.getRecord("AA.PROPERTY", repaymentMethod.getDueProperty()));
                    if (aaProperty.getPropertyClass().getValue().equals("INTEREST")) {
                        totalPropfitBD = totalPropfitBD.add(new BigDecimal(repaymentMethod.getDuePropertyAmount()));
                    }

                    if (repaymentMethod.getDueProperty().equals(acctName)
                            && !repaymentMethod.getDuePropertyAmount().isEmpty()
                            && repaymentMethod.getDueMethod().equals("DUE")) {
                        // pay , due , capitalize
                        if (totalInstallments == 0) {
                            dueDateFirstInstallment = repaymentSchedule.getDueDate().get();
                        } else {
                            dueDateLastInstallment = repaymentSchedule.getDueDate().get();
                        }

                        totalInstallments = totalInstallments + 1;
                        dueAmount = repaymentDueType.getDueTypeAmount().get();
                    }
                }
            }

            String billStatus = "NOT_DUE";
            String dueDate = repaymentSchedule.getDueDate().get();
            if (billDetailsMap.containsKey(dueDate)) {
                billStatus = billDetailsMap.get(dueDate);
            }

            if (!dueAmount.isEmpty()) {
                instDueDate = instDueDate + "|" + dueDate;
                instAmount = instAmount + "|" + dueAmount;
                instNo = instNo + "|" + totalInstallments;
                instStatus = instStatus + "|" + billStatus;
                if (singleInstallAmount.compareTo(new BigDecimal(0)) == 0) {
                    singleInstallAmount = new BigDecimal(dueAmount);
                }
            }
        }

        if (dueDateFirstInstallment.isEmpty()) {
            dueDateFirstInstallment = "NA";
        }

        if (dueDateLastInstallment.isEmpty()) {
            dueDateLastInstallment = "NA";
        }

        if (instDueDate.isEmpty()) {
            instDueDate = "NA";
        }

        if (instAmount.isEmpty()) {
            instAmount = "NA";
        }

        if (instNo.isEmpty()) {
            instNo = "NA";
        }

        if (instStatus.isEmpty()) {
            instStatus = "NA";
        }

        List<String> SettledBills = contract.getBillIds(null, null, "INSTALLMENT", null, "SETTLED", null, null, null,
                null);
        paidBills = SettledBills.size();
        installmentNumLeft = totalInstallments - paidBills;
        if (arrStatus.equalsIgnoreCase("PENDING.CLOSURE") || arrStatus.equalsIgnoreCase("CLOSE")) {

            installmentNumLeft = 0;
        }
        outSummary.add(dueDateFirstInstallment);
        outSummary.add(dueDateLastInstallment);
        outSummary.add(totalInstallments.toString());
        outSummary.add(String.valueOf(installmentNumLeft));
        outSummary.add(singleInstallAmount.toString());
        outSummary.add(instDueDate);
        outSummary.add(instAmount);
        outSummary.add(instNo);
        outSummary.add(instStatus);
        outSummary.add(totalPropfitBD.toString());
        return outSummary;
    }

    public Map<String, String> getBillDetails(DataAccess da, Contract contract, String acctName) {
        Map<String, String> billDetailsMap = new HashMap();
        AaAccountDetailsRecord acctDets = new AaAccountDetailsRecord(
                da.getRecord("", "AA.ACCOUNT.DETAILS", "", contract.getContractId()));
        Iterator var7 = acctDets.getBillPayDate().iterator();

        label46: while (var7.hasNext()) {
            BillPayDateClass tempbillPayCls = (BillPayDateClass) var7.next();
            Iterator var9 = tempbillPayCls.getBillId().iterator();

            label44: while (true) {
                BillIdClass tempBillIdCls;
                do {
                    if (!var9.hasNext()) {
                        continue label46;
                    }

                    tempBillIdCls = (BillIdClass) var9.next();
                } while (!tempBillIdCls.getBillType().getValue().equals("INSTALLMENT"));

                AaBillDetailsRecord aaBillDets = new AaBillDetailsRecord(
                        da.getRecord("", "AA.BILL.DETAILS", "", tempBillIdCls.getBillId().getValue()));
                Iterator var12 = aaBillDets.getPaymentType().iterator();

                while (true) {
                    while (true) {
                        if (!var12.hasNext()) {
                            continue label44;
                        }

                        PaymentTypeClass tempBillPropCls = (PaymentTypeClass) var12.next();
                        Iterator var14 = tempBillPropCls.getPayProperty().iterator();

                        while (var14.hasNext()) {
                            PayPropertyClass tempPayPropCls = (PayPropertyClass) var14.next();
                            if (tempPayPropCls.getPayProperty().getValue().equals(acctName)) {
                                if (tempBillIdCls.getSetStatus().getValue().equals("REPAID")) {
                                    billDetailsMap.put(tempbillPayCls.getBillPayDate().getValue(), "PAID");
                                    break;
                                }

                                if (tempBillIdCls.getSetStatus().getValue().equals("UNPAID")) {
                                    billDetailsMap.put(tempbillPayCls.getBillPayDate().getValue(), "UNPAID");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return billDetailsMap;
    }
}
