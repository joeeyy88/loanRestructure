package com.temenos.csd.emk.sme.po.posting.dets;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.paymentorder.PaymentOrderRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebemknddmfiledets.EbEmknDdmFileDetsTable;
import com.temenos.t24.api.records.ebemknddmfiledets.EbEmknDdmFileDetsRecord;
import com.temenos.t24.api.tables.ebbnplhstaticparam.EbBnplHStaticParamTable;
import com.temenos.t24.api.records.ebbnplhstaticparam.EbBnplHStaticParamRecord;
import com.temenos.t24.api.records.ebbnplhstaticparam.DataNameClass;

public class CsdBPostPoDets extends ServiceLifecycle {
    DataAccess da = new DataAccess(this);
    
    Session session = new Session(this);
    String today = session.getCurrentVariable("!TODAY");
    private DataAccess dataAccess = new DataAccess(this);
    private String filePath = "";
    private String fileName = "";
    private boolean areRequiredFieldsFilled(String... fields) {
        for (String field : fields) {
            if (field == null || field.isEmpty()) {
                return false;
            }
        }
        return true;
    }
 
    
 
   
   

    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> accountIds = new ArrayList<>();
        try {
            EbBnplHStaticParamRecord staticParamRecord = new EbBnplHStaticParamRecord(da.getRecord("EB.EMK.H.STATIC.PARAM", "EMKAN"));
            Iterator var12 =staticParamRecord.getDataName().iterator();
            
            while(var12.hasNext()){            
                DataNameClass dataName  = (DataNameClass) var12.next();
                if (dataName.getDataName().equals("DDM.FILE.PATH")) {
                    filePath = dataName.getDataValue(0).getValue();
                }
               
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        filePath = filePath + "/" + "DDMPostingInputFile-" + today+ ".csv";

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = bufferedReader.readLine()) != null) {
                // Skip the first line (header)
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                accountIds.add(line);
            }
        } catch (IOException e) {
          
        }
        System.out.println(accountIds.toString());
        return accountIds; // Return the list of account IDs
    }

    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {

        String aaId = "";
        String custId = "";
        EbEmknDdmFileDetsRecord ddmRecord = new EbEmknDdmFileDetsRecord();
        EbEmknDdmFileDetsTable ddmTable = new EbEmknDdmFileDetsTable(this);

        List<String> csvLines = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                csvLines.add(line);
            }
        } catch (IOException e) {
           
        }
        String losId = "";
        String receiptNo = "";
        String customerName="";
        String receiptDate = "";
        String paymentMode = "";
        String totalReceiptAmt="";


        String[] split = id.split("[|]");
        try {
            receiptNo = split[0];
            System.out.println("/" + receiptNo + "/");
            customerName = split[1];
            System.out.println(customerName);
            receiptDate = split[2];
            System.out.println(receiptDate);
            paymentMode = split[3];
            System.out.println(paymentMode);
            totalReceiptAmt = split[4];
            System.out.println(totalReceiptAmt);
            losId = split[5];
            System.out.println("/" + losId + "/");
        }
        catch (Exception e) {
            //            System.out.println(e);
        }
        List<String> arrRec = null;
        AaArrangementRecord arrRec2 = null;
        try {
            arrRec = da.getConcatValues("AA.ARRANGEMENT.ALTERNATE.ID", losId);
            String yparts = arrRec.toString();

            if (arrRec != null && !arrRec.isEmpty()) {
                System.out.println("string before spliting"+ yparts);
                String trimmedInput = yparts.substring(1, yparts.length() - 1);
                System.out.println("string before spliting but trimmed"+trimmedInput);
                String[] parts = trimmedInput.split("\\*");                
                aaId = parts[1];
                System.out.println("string after splitting and trimming "+aaId);

            } else {
                System.out.println("Error: arrRec is null or empty.");
            }

            arrRec2 = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", aaId));
            custId = arrRec2.getCustomer().get(0).getCustomer().toString();

        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " );
           
        }

        List<String> lendingArrangements = new ArrayList<>();
        try {
            AaCustomerArrangementRecord custArrRec = new AaCustomerArrangementRecord(da.getRecord("AA.CUSTOMER.ARRANGEMENT", custId));
            for (ProductLineClass productLine : custArrRec.getProductLine()) {
                if (productLine.getProductLine().getValue().equals("LENDING")) {
                    for (ArrangementClass arrangement : productLine.getArrangement()) {
                        lendingArrangements.add(arrangement.getArrangement().getValue());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred while retrieving lending arrangements: " );
            
        }

        // Proceed only if there is exactly one lending arrangement
        if (lendingArrangements.size() == 1) {
            if (areRequiredFieldsFilled(losId, receiptNo, customerName, receiptDate, paymentMode, totalReceiptAmt)) {
                PaymentOrderRecord paymentOrder = new PaymentOrderRecord(this);
                paymentOrder.getLocalRefField("L.RECEIPT.NO").setValue(receiptNo);
                paymentOrder.setCreditAccount(lendingArrangements.get(0)); // Use the single lending arrangement
                paymentOrder.setOrderingCustName(customerName);
                paymentOrder.getLocalRefField("L.PAYMENT.METH").setValue(paymentMode);
                paymentOrder.getLocalRefField("L.RECEIPT.DATE").setValue(receiptDate);     
                paymentOrder.getLocalRefField("L.TOT.RCPT.AMT").setValue(totalReceiptAmt);
                paymentOrder.setPaymentAmount(totalReceiptAmt);
                paymentOrder.getLocalRefField("L.AA.ID").setValue(lendingArrangements.get(0));
               paymentOrder.getLocalRefField("L.LOS.ID").setValue(losId);
               paymentOrder.setOrderingCustomer(custId);
          
                //add aaid locally
                System.out.println("po details " + paymentOrder.toString());
                TransactionData txnData = new TransactionData();
                txnData.setFunction("INPUT");
                txnData.setSourceId("CSD.EMK.OFS");
                txnData.setVersionId("PAYMENT.ORDER,CSD.POST.OFS"); //CHANGE NAME OF VERSION
                transactionData.add(txnData);
                records.add(paymentOrder.toStructure());
            } else {
                System.out.println("Error: Required fields are not filled.");
            }
        } else {
            System.out.println("Error: There must be exactly one lending arrangement for the customer.");
        }

        // Handle the case where there is no valid arrangement for posting
        if (lendingArrangements.size() != 1 || !areRequiredFieldsFilled(losId, receiptNo, customerName, receiptDate, paymentMode, totalReceiptAmt)) {
            TField status = new TField ();
            status.setValue("Rejected");
            Session ySession = new Session(this);
            String today = ySession.getCurrentVariable("!TODAY");
            ddmRecord.setReceiptNo(receiptNo);
            ddmRecord.setPaymentMode(paymentMode);
            ddmRecord.setReceiptDate(receiptDate);
            ddmRecord.setTotalRcpAmt(totalReceiptAmt);
            ddmRecord.setCustomerName(customerName);
            ddmRecord.setLosId(losId);
            ddmRecord.setInstStatus(status);
            String recId = receiptNo + "*" + aaId + "*" + today; //session date 
            System.out.println("this is the id of the record" + recId);
            try {
                ddmTable.write(recId, ddmRecord);
            } catch (T24IOException e1) {
                // TODO Auto-generated catch block
                // Uncomment and replace with appropriate logger
                // LOGGER.error(e1, e1);
            }
        }
    }
}
