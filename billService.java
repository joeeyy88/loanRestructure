package com.temenos.tdf.loanRestructure;
import java.math.BigDecimal;

import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.EbTdfPayoffBillRefRecord;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.EbTdfPayoffBillRefRecord;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.EbTdfPayoffBillRefTable;
import com.temenos.t24.api.tables.ebtdfpayoffbillref.BillRefClass;
import com.temenos.t24.api.system.DataAccess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaactivity.AaActivityRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.tables.ebtdfpayoffbills.EbTdfPayoffBillsRecord;
import com.temenos.t24.api.tables.ebtdfpayoffbills.EbTdfPayoffBillsTable;
import com.temenos.t24.api.tables.ebtdfpayoffbills.LOldLoanArrClass;

// Import statements (unchanged)

public class billService extends ServiceLifecycle {

    DataAccess dataAccess = new DataAccess(this);

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        String condition = " WITH BILL.TYPE EQ 'PAYOFF' AND BILL.STATUS EQ 'ISSUED' AND PAY.METHOD EQ 'INFO'";
        List<String> aaIds = dataAccess.selectRecords("", "AA.ACCOUNT.DETAILS", "", condition);
        return aaIds;
    } // END OF GETIDS

    private String getCustomerId(String aactIds, DataAccess dataAccess) {
        AaArrangementActivityRecord aactRec = new AaArrangementActivityRecord(dataAccess.getRecord("AA.ARRANGEMENT.ACTIVITY", aactIds));
        return aactRec.getCustomer(0).getCustomer().getValue();
    }

    private String getArrangementIds(String aactIds, DataAccess dataAccess) {
        AaArrangementActivityRecord aactRec = new AaArrangementActivityRecord(dataAccess.getRecord("AA.ARRANGEMENT.ACTIVITY", aactIds));
        return aactRec.getArrangement().getValue().toString();
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
                             TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
                             List<TStructure> records) {

        System.out.println("Executed");
        System.out.println("Current ID" +id);
        AaAccountDetailsRecord aaAccDetsRecord = new AaAccountDetailsRecord(dataAccess.getRecord("AA.ACCOUNT.DETAILS", id));
        System.out.println("opened AA ACCOUNT DETAILS"+aaAccDetsRecord);
        List<BillPayDateClass> billPayDateList = aaAccDetsRecord.getBillPayDate();
        int payDateSize = billPayDateList.size();

        List<BillIdClass> billIds = billPayDateList.get(payDateSize - 1).getBillId();
        int billIdSize = billIds.size();
        System.out.println("Bill Ids" + billIds);
        for (int billCount = billIdSize - 1; billCount >= 0; billCount--) {
            String billType = billIds.get(billCount).getBillType().getValue();
            String billStatus = billIds.get(billCount).getBillStatus().getValue();
            String paymentMethod = billIds.get(billCount).getPayMethod().getValue();

            if (billType.equalsIgnoreCase("PAYOFF") && billStatus.equalsIgnoreCase("ISSUED") && paymentMethod.equalsIgnoreCase("INFO")) {
                String originalBillId = billIds.get(billCount).getBillId().getValue();
                String sanitizedBillId = originalBillId.replace("/", ""); // Remove slashes dynamically
                System.out.println("Original Bill ID: " + originalBillId);
                System.out.println("Sanitized Bill ID: " + sanitizedBillId);

                AaBillDetailsRecord billRec = new AaBillDetailsRecord(dataAccess.getRecord("", "AA.BILL.DETAILS", "", sanitizedBillId));
                System.out.println("opened AA BILL DETAILS" + billRec);
                // Processing billRec properties
                if (billRec != null) {
                    System.out.println("After billRec doesnt eqaul null");
                    BigDecimal accountAmount = BigDecimal.ZERO;
                    BigDecimal deferProfitAmount = BigDecimal.ZERO;
                    BigDecimal penaltyAmount = BigDecimal.ZERO;

                    Iterator<PropertyClass> propertyIterator = billRec.getProperty().iterator();
                    System.out.println("Before while loop");
                    while (propertyIterator.hasNext()) {
                        PropertyClass property = propertyIterator.next();

                        // Check for ACCOUNT property
                        System.out.println("Before ACCOUNT check");
                        if ("ACCOUNT".equals(property.getProperty().getValue())) {
                            accountAmount = accountAmount.add(new BigDecimal(property.getOsPropAmount().getValue()));
                            System.out.println("Account Amount: " + accountAmount);
                        }

                        // Check for DEFERPROFIT property
                        System.out.println("Before DEFERPROFIT check");
                        if ("DEFERPROFIT".equals(property.getProperty().getValue())) {
                            deferProfitAmount = deferProfitAmount.add(new BigDecimal(property.getOsPropAmount().getValue()));
                            System.out.println("Defer Profit Amount: " + deferProfitAmount);
                        }

                        // Check for PENALTY property
                        System.out.println("Before PENALTY check");
                        if ("PENALTY".equals(property.getProperty().getValue())) {
                            penaltyAmount = penaltyAmount.add(new BigDecimal(property.getOsPropAmount().getValue()));
                            System.out.println("Penalty Amount: " + penaltyAmount);
                        }
                        
                      EbTdfPayoffBillsTable billTable = null;
                      EbTdfPayoffBillsRecord billRecordx = null;
                     
                    
                      
                      
                      billTable = new EbTdfPayoffBillsTable(this);
                    try {
                          billRecordx = new EbTdfPayoffBillsRecord(dataAccess.getRecord("EB.TDF.PAYOFF.BILLS", customerId));   
                      }
                      catch (Exception e){
              
                          billRecordx = new EbTdfPayoffBillsRecord(this);

                      }
                      
                      billRecord = new EbTdfPayoffBillsRecord(this);
                      String Varx = null;
                      try{
                          LOldLoanArrClass lOldrec = new LOldLoanArrClass();
                          lOldrec.setLOldLoanArr(id.toString());
                          Varx = lOldrec.getLOldLoanArr().getValue().toString();
                          System.out.println("arrangementID" + Varx );
                          lOldrec.setLAccountAmt(accountAmount.toString());
                          lOldrec.setLProfitAmt(deferProfitAmount.toString());
                          lOldrec.setLPenaltyAmt(penaltyAmount.toString());
                          billRecord.setLOldLoanArr(lOldrec, 0);
                          }
                          catch(Exception e){
                              billRecord.setLReserved5("123123");
                              billRecord.setLReserved3(id);
                          }
                      
                      
                      
                      try {
                          billTable.write("123123",billRecord);
                      } catch (T24IOException e) {
                          // TODO Auto-generated catch block
                          // Uncomment and replace with appropriate logger
                          // LOGGER.error(e, e);
                          
                          System.out.println("billRecord after writingggg");
                          
                      }
                        
                        
                        
                        
                        
                        
                        
                    }

                    // Further processing with accountAmount, deferProfitAmount, and penaltyAmount if needed
                }

                break;
            }
        }
    } // END OF POSTUPDATE
}
