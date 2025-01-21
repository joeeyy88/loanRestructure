package com.temenos.tdf.loanRestructure;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.iscontract.IsContractRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author anouh, azidan
 *
 */
public class CsdPostBnplAaAutoDeduct extends ActivityLifecycle {
    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {

        DataAccess da = new DataAccess(this);
        String arrangementId = "";
        String losId = "";
        String isContractId = "";
        String isCardFlag = "";

        try {
            arrangementId = arrangementContext.getArrangementId().toString();
            losId = arrangementRecord.getAltIdType(0).getAlternateId().getValue();
        } catch (Exception ex) {
        }
        if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH")) {
            IsContractRecord isContract = null;
            List<String> isContractIdList = da.selectRecords("", "IS.CONTRACT", "", "WITH L.LOS.APPL.ID EQ " + losId);
            if (!isContractIdList.isEmpty()) {
                isContractId = isContractIdList.get(0);
                isContract = new IsContractRecord(da.getRecord("IS.CONTRACT", isContractId));
                try {
                    isCardFlag = isContract.getLocalRefField("IS.CARD.PAYMENT").getValue();
                    if (isCardFlag.equals("1")) {
                        EbBnplLAutoDeductIdTable AutoDeductTable = new EbBnplLAutoDeductIdTable(this);
                        try {
                            EbBnplLAutoDeductIdRecord AutoDeductRecord = new EbBnplLAutoDeductIdRecord(
                                    da.getRecord("EB.BNPL.L.AUTO.DEDUCT.ID", arrangementId));
                        } catch (Exception e) {
                            AutoDeductTable.write(arrangementId, null);
                        }
                    }
                } catch (Exception err) {
                }
            }
        }

    }
}
