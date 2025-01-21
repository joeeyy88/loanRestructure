/* Decompiler 427ms, total 646ms, lines 394 */
package com.temenos.csd.emkan.enq;

import com.temenos.api.TDate;
import com.temenos.api.TField;
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
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PayPropertyClass;
import com.temenos.t24.api.records.aabilldetails.PaymentTypeClass;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.aaproperty.AaPropertyRecord;
import com.temenos.t24.api.records.ebemkaavibandets.EbEmkAaVibanDetsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CsdEmkNofGetLoanDets extends Enquiry {
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> outDetails = new ArrayList();
        DataAccess dataAccess = new DataAccess(this);
        String arrangementId = "";
        String arbUniqueReference = "";
        String crNumber = "";
        String nationalId = "";
        if (!filterCriteria.isEmpty()) {
            Iterator var10 = filterCriteria.iterator();

            while (var10.hasNext()) {
                FilterCriteria selectionCrit = (FilterCriteria) var10.next();
                if (selectionCrit.getFieldname().equals("contractId")) {
                    arrangementId = selectionCrit.getValue();
                }

                if (selectionCrit.getFieldname().equals("crNumber")) {
                    crNumber = selectionCrit.getValue();
                }

                if (selectionCrit.getFieldname().equals("callUniqueReference")) {
                    arbUniqueReference = selectionCrit.getValue();
                }

                if (selectionCrit.getFieldname().equals("nationalId")) {
                    nationalId = selectionCrit.getValue();
                }
            }
        }

        List<String> arrList = new ArrayList();
        if (crNumber.isEmpty() && nationalId.isEmpty()) {
            if (!arrangementId.isEmpty()) {
                arrList.add(arrangementId);
            }
        } else {
            try {
                List<String> custList = this.getCustomerList(dataAccess, crNumber, nationalId);
                if (!custList.isEmpty()) {
                    AaCustomerArrangementRecord custArrRec = new AaCustomerArrangementRecord(
                            dataAccess.getRecord("AA.CUSTOMER.ARRANGEMENT", (String) custList.get(0)));
                    Iterator var13 = custArrRec.getProductLine().iterator();

                    label81: while (true) {
                        ProductLineClass tempPrdLncls;
                        do {
                            if (!var13.hasNext()) {
                                break label81;
                            }

                            tempPrdLncls = (ProductLineClass) var13.next();
                        } while (!tempPrdLncls.getProductLine().getValue().equals("LENDING"));

                        Iterator var15 = tempPrdLncls.getArrangement().iterator();

                        while (var15.hasNext()) {
                            ArrangementClass tempArrCls = (ArrangementClass) var15.next();
                            arrList.add(tempArrCls.getArrangement().getValue());
                        }
                    }
                }
            } catch (Exception var32) {
                return outDetails;
            }
        }

        if (!arrList.isEmpty()) {
            Iterator var36 = arrList.iterator();

            while (var36.hasNext()) {
                String contractId = (String) var36.next();

                try {
                    String outPut = "";
                    String aaId = contractId;
                    System.out.println("this is the aaId---" +aaId);
                    Contract contract = new Contract(this);
                    System.out.println("this is the aaId(contract) before setting the contract---" +contract);
                    contract.setContractId(contractId);
                    System.out.println("this is the aaId(contract) after setting the contract---" +contract);
                    AaArrangementRecord aaArrRec = new AaArrangementRecord(
                            dataAccess.getRecord("AA.ARRANGEMENT", contractId));
                    String arrSts = aaArrRec.getArrStatus().getValue();
                    String loanBookingDate = contract.getContract().getStartDate().getValue();
                    List<String> outSummary = this.getInstallmentSummary(contract, new TDate(loanBookingDate),
                            contract.getMaturityDate());
                    String totalPrincipal = contract.getTermAmount().get();
                    String totalProfit = (String) outSummary.get(9);
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    totalAmount = (new BigDecimal(totalPrincipal)).add(new BigDecimal(totalProfit));
                    BigDecimal totalBalance = BigDecimal.ZERO;
                    BigDecimal totAcctBal = BigDecimal.ZERO;
                    BigDecimal totIntBal = BigDecimal.ZERO;
                    new OutstandingBalances();
                    OutstandingBalances arrOutstandingBalances = contract.getOutstandingBalance();
                    totAcctBal = (new BigDecimal(arrOutstandingBalances.getAccountBalance().doubleValue())).abs();
                    totIntBal = (new BigDecimal(arrOutstandingBalances.getInterestBalance().doubleValue())).abs();
                    totalBalance = totAcctBal.add(totIntBal);
                    String totalOutStandingAmt = totalBalance.setScale(2, RoundingMode.HALF_UP).toString();
                    String totalOutStandingPrincipal = totAcctBal.setScale(2, RoundingMode.HALF_UP).toString();
                    String productType = "";
                    AaProductRecord productRec = new AaProductRecord(
                            dataAccess.getRecord("AA.PRODUCT", contract.getProductId()));
                    Iterator var30 = productRec.getDescription().iterator();

                    while (var30.hasNext()) {
                        TField temp = (TField) var30.next();
                        if (productType.isEmpty()) {
                            productType = temp.getValue();
                        } else {
                            productType = productType + " " + temp.getValue();
                        }
                    }

                    String vIban=null;
                    try{ 
                       EbEmkAaVibanDetsRecord vIbanRec = new EbEmkAaVibanDetsRecord(
                            dataAccess.getRecord("EB.EMK.AA.VIBAN.DETS", aaId));
                  vIban = vIbanRec.getLVibanId().toString();
                  System.out.println("this is the VIBAN ---" +vIban);
                   } catch (Exception var27){
                       
                   }
                    
                    
                    outPut = contractId + "*" + arrSts + "*" + totalAmount + "*" + totalPrincipal + "*" + totalProfit
                            + "*" + totalOutStandingAmt + "*" + totalOutStandingPrincipal + "*" + loanBookingDate + "*"
                            + (String) outSummary.get(0) + "*" + (String) outSummary.get(1) + "*"
                            + (String) outSummary.get(2) + "*" + (String) outSummary.get(3) + "*"
                            + (String) outSummary.get(4) + "*" + (String) outSummary.get(5) + "*"
                            + (String) outSummary.get(6) + "*" + (String) outSummary.get(7) + "*"
                            + (String) outSummary.get(8) + "*" + (String) outSummary.get(9) + "*"
                            + (String) outSummary.get(10) + "*" + (String) outSummary.get(11) + "*"
                            + (String) outSummary.get(12) + "*" + arbUniqueReference + "*" + productType + "*"
                            + contract.getTerm() + "*" + (String) outSummary.get(13) + "*" + (String) outSummary.get(14)
                            + "*" + (String) outSummary.get(15) + "*" + (String) outSummary.get(16) + "*"
                            + (String) outSummary.get(17) + "*" + (String) outSummary.get(18)+"*"+vIban;

                    outDetails.add(outPut);
                } catch (Exception var31) {
                    return outDetails;
                }
            }
        }

        return outDetails;
    }

    private List<String> getCustomerList(DataAccess dataAccess, String crNumber, String nationalId) {
        List<String> custList = new ArrayList();
        String custName = "CUSTOMER";
        if (!nationalId.isEmpty()) {
            custList = dataAccess.selectRecords("", custName, "", "WITH CSD.INDV.ID.NUM EQ " + nationalId);
        } else if (!crNumber.isEmpty()) {
            custList = dataAccess.selectRecords("", custName, "", "WITH CR.NUMBER EQ " + crNumber);
        }

        return (List) custList;
    }

    private List<String> getInstallmentSummary(Contract contract, TDate startDate, TDate endDate) {
        List<String> outSummary = new ArrayList();
        Integer totalInstallments = 0;
        String dueDateFirstInstallment = "";
        String dueDateLastInstallment = "";
        BigDecimal totalPropfitBD = BigDecimal.ZERO;
        String oDinstDueDate = "";
        String oDinstAmount = "";
        String oDinstNo = "";
        String oDinstStatus = "";
        String uPinstDueDate = "";
        String uPinstAmount = "";
        String uPinstNo = "";
        BigDecimal singleInstallAmount = BigDecimal.ZERO;
        int installmentNumLeft = 0;
        String nextinstallmentDate = "";
        String nextinstallmentAmount = "";
        String nextinstallmentNumber = "";
        String[] yBillStatus;
        String actualPayDate = "";
        String instStatus = "";
        String instDate = "";
        String paymentDate = "";
        String repayBillSatus = "";

        Session session = new Session(this);
        Integer today = Integer.valueOf(session.getCurrentVariable("!TODAY"));
        DataAccess dataAccess = new DataAccess(this);
        List<RepaymentSchedule> repaymentList = contract.getRepaymentSchedule(startDate, endDate);
        Map<String, String> billDetailsMap = this.getBillDetails(dataAccess, contract);
        boolean nextInstallmentFnd = false;
        Iterator var28 = repaymentList.iterator();

        while (true) {
            while (var28.hasNext()) {
                RepaymentSchedule repaymentSchedule = (RepaymentSchedule) var28.next();
                String dueAmount = "0";
                String dueDate = "";
                Iterator var32 = repaymentSchedule.getRepaymentDueType().iterator();

                while (var32.hasNext()) {
                    RepaymentDueType repaymentDueType = (RepaymentDueType) var32.next();
                    Iterator var34 = repaymentDueType.getRepaymentMethod().iterator();

                    while (var34.hasNext()) {
                        RepaymentMethod repaymentMethod = (RepaymentMethod) var34.next();
                        AaPropertyRecord aaProperty = new AaPropertyRecord(
                                dataAccess.getRecord("AA.PROPERTY", repaymentMethod.getDueProperty()));
                        if (aaProperty.getPropertyClass().getValue().equals("INTEREST")) {
                            totalPropfitBD = totalPropfitBD.add(new BigDecimal(repaymentMethod.getDuePropertyAmount()));
                        }

                        if (repaymentMethod.getDueProperty().equals("ACCOUNT")
                                && !repaymentMethod.getDuePropertyAmount().isEmpty()
                                && repaymentMethod.getDueMethod().equals("DUE")) {
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
                boolean oDOrUpFlg = false;
                dueDate = repaymentSchedule.getDueDate().get();
                if (billDetailsMap.containsKey(dueDate)) {

                    yBillStatus = ((String) billDetailsMap.get(dueDate)).split("-");
                    billStatus = yBillStatus[0];
                    actualPayDate = yBillStatus[1];
                    if (billStatus.equals("UNPAID")) {
                        oDOrUpFlg = true;
                    }

                    instDate = instDate + "|" + dueDate;
                    paymentDate = paymentDate + "|" + actualPayDate;
                    if (actualPayDate.equals("NA")) {
                        repayBillSatus = "UNPAID";
                    } else {
                        repayBillSatus = "PAID";
                    }
                    instStatus = instStatus + "|" + repayBillSatus;
                }

                dueDate = repaymentSchedule.getDueDate().get();
                if (today <= Integer.valueOf(repaymentSchedule.getDueDate().get()) && !nextInstallmentFnd) {
                    nextInstallmentFnd = true;
                    nextinstallmentDate = repaymentSchedule.getDueDate().get();
                    nextinstallmentAmount = dueAmount;
                    nextinstallmentNumber = String.valueOf(totalInstallments);
                }

                if (singleInstallAmount.compareTo(new BigDecimal(0)) == 0) {
                    singleInstallAmount = new BigDecimal(dueAmount);
                }

                if (!dueAmount.isEmpty() && oDinstAmount.isEmpty() && oDOrUpFlg) {
                    oDinstAmount = dueAmount;
                    oDinstNo = String.valueOf(totalInstallments);
                    oDinstDueDate = dueDate;
                    oDinstStatus = billStatus;
                } else if (!dueAmount.isEmpty() && oDOrUpFlg) {
                    oDinstAmount = oDinstAmount + "|" + dueAmount;
                    oDinstNo = oDinstNo + "|" + totalInstallments;
                    oDinstDueDate = oDinstDueDate + "|" + dueDate;
                    oDinstStatus = oDinstStatus + "|" + billStatus;
                }

                if (!dueAmount.isEmpty() && uPinstAmount.isEmpty() && !oDOrUpFlg
                        && today <= Integer.valueOf(repaymentSchedule.getDueDate().get())) {
                    uPinstAmount = dueAmount;
                    uPinstNo = String.valueOf(totalInstallments);
                    uPinstDueDate = dueDate;
                    ++installmentNumLeft;
                } else if (!dueAmount.isEmpty() && !oDOrUpFlg
                        && today <= Integer.valueOf(repaymentSchedule.getDueDate().get())) {
                    uPinstAmount = uPinstAmount + "|" + dueAmount;
                    uPinstNo = uPinstNo + "|" + totalInstallments;
                    uPinstDueDate = uPinstDueDate + "|" + dueDate;
                    ++installmentNumLeft;
                }
            }

            if (dueDateFirstInstallment.isEmpty()) {
                dueDateFirstInstallment = "NA";
            }

            if (dueDateLastInstallment.isEmpty()) {
                dueDateLastInstallment = "NA";
            }

            if (oDinstDueDate.isEmpty()) {
                oDinstDueDate = "NA";
            }

            if (oDinstAmount.isEmpty()) {
                oDinstAmount = "NA";
            }

            if (oDinstNo.isEmpty()) {
                oDinstNo = "NA";
            }

            if (oDinstStatus.isEmpty()) {
                oDinstStatus = "NA";
            }

            if (nextinstallmentDate.isEmpty()) {
                nextinstallmentDate = "NA";
            }

            if (nextinstallmentAmount.isEmpty()) {
                nextinstallmentAmount = "NA";
            }

            if (nextinstallmentNumber.isEmpty()) {
                nextinstallmentNumber = "NA";
            }

            if (uPinstDueDate.isEmpty()) {
                uPinstDueDate = "NA";
            }

            if (uPinstAmount.isEmpty()) {
                uPinstAmount = "NA";
            }

            if (uPinstNo.isEmpty()) {
                uPinstNo = "NA";
            }
            if (instDate.isEmpty()) {
                instDate = "NA";
            }

            if (paymentDate.isEmpty()) {
                paymentDate = "NA";
            }
            if (instStatus.isEmpty()) {
                instStatus = "NA";

            }
            outSummary.add(dueDateFirstInstallment);
            outSummary.add(dueDateLastInstallment);
            outSummary.add(totalInstallments.toString());
            outSummary.add(String.valueOf(installmentNumLeft));
            outSummary.add(singleInstallAmount.toString());
            outSummary.add(oDinstDueDate);
            outSummary.add(oDinstAmount);
            outSummary.add(oDinstNo);
            outSummary.add(oDinstStatus);
            outSummary.add(totalPropfitBD.toString());
            outSummary.add(nextinstallmentDate);
            outSummary.add(nextinstallmentAmount);
            outSummary.add(nextinstallmentNumber);
            outSummary.add(uPinstDueDate);
            outSummary.add(uPinstAmount);
            outSummary.add(uPinstNo);
            outSummary.add(instDate);
            outSummary.add(paymentDate);
            outSummary.add(instStatus);
            return outSummary;
        }

    }

    public Map<String, String> getBillDetails(DataAccess da, Contract contract) {
        Map<String, String> billDetailsMap = new HashMap();
        String setStatusName = "UNPAID";
        String actualPayDate = "";
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
                            if (tempPayPropCls.getPayProperty().getValue().equals("ACCOUNT")) {
                                if (tempBillIdCls.getSetStatus().getValue().equals("REPAID")) {
                                    actualPayDate = aaBillDets.getSettleStatus(0).getSetStChgDt().getValue();
                                    billDetailsMap.put(tempbillPayCls.getBillPayDate().getValue(),
                                            "PAID" + "-" + actualPayDate);
                                    break;
                                }

                                if (tempBillIdCls.getSetStatus().getValue().equals(setStatusName)) {
                                    actualPayDate = "NA";
                                    billDetailsMap.put(tempbillPayCls.getBillPayDate().getValue(),
                                            setStatusName + "-" + actualPayDate);
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
