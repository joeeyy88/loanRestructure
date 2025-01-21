package com.temenos.tdf.loanRestructure;

import com.temenos.api.TDate;
import com.temenos.t24.api.arrangement.accounting.Contract;
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
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaproperty.AaPropertyRecord;
import com.temenos.t24.api.system.DataAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;

/**
 * TODO: Document me!
 *
 * @author ykumar
 *
 */
public class CsdEmkBnplOsContractDets extends Enquiry {
    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> outDetails = new ArrayList();
        String contractId = "";
        String nationalId = "";
        String acctName = "ACCOUNT";
        if (!filterCriteria.isEmpty()) {
            Iterator var8 = filterCriteria.iterator();

            while (var8.hasNext()) {
                FilterCriteria selectionCrit = (FilterCriteria) var8.next();
                if (selectionCrit.getFieldname().equals("nationalId")) {
                    nationalId = selectionCrit.getValue();
                }

                if (selectionCrit.getFieldname().equals("contractId")) {
                    contractId = selectionCrit.getValue();
                }
            }
        }

        DataAccess da = new DataAccess(this);
        if (!contractId.isEmpty()) {

            AaArrangementRecord arr = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", contractId));
            if (arr.getProductLine().getValue().equals("LENDING")) {
                outDetails.addAll(getOutPut(contractId, acctName));
            }
        } else if (!nationalId.isEmpty()) {

            String customerID = da.selectRecords("", "CUSTOMER", "", "WITH CSD.INDV.ID.NUM EQ " + nationalId).get(0);

            AaCustomerArrangementRecord custArrRec = new AaCustomerArrangementRecord(
                    da.getRecord("AA.CUSTOMER.ARRANGEMENT", customerID));
            List<ProductLineClass> proudctLineLIst = custArrRec.getProductLine();
            Iterator var13 = proudctLineLIst.iterator();

            while (var13.hasNext()) {
                ProductLineClass Plclass = (ProductLineClass) var13.next();
                if (Plclass.getProductLine().getValue().equals("LENDING")) {
                    Iterator var14 = Plclass.getArrangement().iterator();
                    while (var14.hasNext()) {
                        ArrangementClass contract = (ArrangementClass) var14.next();
                        contractId = contract.getArrangement().getValue();
                        if (!contractId.isEmpty()) {
                            outDetails.addAll(getOutPut(contractId, acctName));
                        }
                    }
                }

            }
        }

        return outDetails;
    }

    public List<String> getOutPut(String contractId, String acctName) {
        List<String> outDetails = new ArrayList();

        String outPut = "";
        Contract contract = new Contract(this);
        contract.setContractId(contractId);
        DataAccess dataAccess = new DataAccess(this);

        try {
            AaArrangementRecord aaArrRec = new AaArrangementRecord(dataAccess.getRecord("AA.ARRANGEMENT", contractId));

            String loanBookingDate = contract.getContract().getStartDate().getValue();
            List<String> outSummary = this.getInstallmentSummary(contract, new TDate(loanBookingDate),
                    contract.getMaturityDate(), acctName);
            outPut = contractId + "*" + outSummary.get(0) + "*" + outSummary.get(1) + "*"
                    + outSummary.get(2);
            outDetails.add(outPut);

        } catch (Exception var28) {
            return outDetails;
        }

        return outDetails;
    }

    private List<String> getInstallmentSummary(Contract contract, TDate startDate, TDate endDate, String acctName) {
        List<String> outSummary = new ArrayList();
        Integer totalInstallments = 0;
        String instDueDate = "";
        String instAmount = "";
        String instStatus = "";
        boolean installmentNumLeft = false;
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
                    }

                    if (repaymentMethod.getDueProperty().equals(acctName)
                            && !repaymentMethod.getDuePropertyAmount().isEmpty()
                            && repaymentMethod.getDueMethod().equals("DUE")) {

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

            if (!dueAmount.isEmpty() && (billStatus != "PAID")) {
                instDueDate = instDueDate + "|" + dueDate;
                instAmount = instAmount + "|" + dueAmount;
                instStatus = instStatus + "|" + billStatus;
            }
        }
        if (instDueDate.isEmpty()) {
            instDueDate = "NA";
        }

        if (instAmount.isEmpty()) {
            instAmount = "NA";
        }
        if (instStatus.isEmpty()) {
            instStatus = "NA";
        }
        outSummary.add(instDueDate);
        outSummary.add(instAmount);
        outSummary.add(instStatus);
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
