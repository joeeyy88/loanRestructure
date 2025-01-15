package com.temenos.tdf.loanRestructure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author azidan , mhismail
 *
 */
public class CsdBnplGetLoanEligibility extends Enquiry {

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        List<String> outDetails = new ArrayList();
        String losId = "";
        String nationalId = "";
        String arrangmentID = "";
        String Output = "";
        String loanEligibilityStatus = "";
        String installmentAmount = "";
        String installmentDate = "";
        String arrStatus = "";

        if (!filterCriteria.isEmpty()) {
            Iterator var8 = filterCriteria.iterator();
            while (var8.hasNext()) {
                FilterCriteria selectionCrit = (FilterCriteria) var8.next();
                if (selectionCrit.getFieldname().equals("losId")) {
                    losId = selectionCrit.getValue();
                }
                if (selectionCrit.getFieldname().equals("nationalId")) {
                    nationalId = selectionCrit.getValue();
                }
            }
        }

        if (!losId.isEmpty()) {
            DataAccess dataAccess = new DataAccess(this);
            String recValue = dataAccess.getConcatValues("AA.ARRANGEMENT.ALTERNATE.ID", losId).toString();
            if (recValue != null && !recValue.isEmpty()) {
                String trimmedInput = recValue.substring(1, recValue.length() - 1);
                String[] parts = trimmedInput.split("\\*");
                arrangmentID = parts[1];
            }
            try {
                EbBnplLAutoDeductIdRecord ebRec = new EbBnplLAutoDeductIdRecord(
                        dataAccess.getRecord("EB.BNPL.L.AUTO.DEDUCT.ID", arrangmentID));
                Contract contract = new Contract(this);
                contract.setContractId(arrangmentID);

                List<String> unPaidBills = contract.getBillIds(null, null, "INSTALLMENT", null, null, "UNPAID", null,
                        null, null);

                for (int i = 0; i < unPaidBills.size(); i++) {

                    try {
                        AaBillDetailsRecord aaBillDets = new AaBillDetailsRecord(
                                dataAccess.getRecord("", "AA.BILL.DETAILS", "", unPaidBills.get(i)));
                        installmentAmount = installmentAmount + "|" + aaBillDets.getOsTotalAmount().getValue();
                        installmentDate = installmentDate + "|" + aaBillDets.getPaymentDate().getValue();

                    } catch (Exception E) {
                    }

                }

                arrStatus = contract.getContract().getArrStatus().getValue();
                if (unPaidBills.isEmpty() && arrStatus.equals("CURRENT")) {
                    loanEligibilityStatus = "NOT-ELIGIBLE-PARTIALLY";
                } else if (!unPaidBills.isEmpty() && arrStatus.equals("CURRENT")) {
                    loanEligibilityStatus = "ELIGIBLE";
                } else {
                    loanEligibilityStatus = "NOT-ELIGIBLE";
                }
            } catch (Exception e) {
                loanEligibilityStatus = "NOT-ELIGIBLE";
                // ------------------------------
//                IsContractRecord isContract = null;
//                List<String> isContractIdList = dataAccess.selectRecords("", "IS.CONTRACT", "",
//                        "WITH L.LOS.APPL.ID EQ " + losId);
//                if (!isContractIdList.isEmpty()) {
//                    String isContractId = isContractIdList.get(0);
//                    isContract = new IsContractRecord(dataAccess.getRecord("IS.CONTRACT", isContractId));
//                    try {
//                        String isCardFlag = isContract.getLocalRefField("IS.CARD.PAYMENT").getValue();
//                        String isAutodeduct = isContract.getLocalRefField("IS.AUTO.DEDUCT").getValue();
//                        if (isCardFlag.equals("1") && isAutodeduct.equals("1")) {
//                            loanEligibilityStatus = "NOT-ELIGIBLE-PARTIALLY";
//                        }
//                    } catch (Exception err) {
//                    }
//                }

            }

            Output = losId + "*" + loanEligibilityStatus + "*" + installmentAmount + "*" + installmentDate;
            outDetails.add(Output);
        }

        return outDetails;
    }
}
