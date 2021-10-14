package com.abc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.plexus.util.FileUtils;
import utilities.APIUtil;
import utilities.Constants;
import utilities.PropertiesUtility;
import utilities.QueryHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

public class ToolForModification {
    static String enviromentName = "";
    static String analysisMode = "";
    static String propertyName = "";
    static String propertyValue = "";
    static String entityId = "";
    static String attributeShortName ="";
    static JsonArray entityTypeArray;
    static String deleteValue;
    static String delteAttribute;
    static JsonArray contextTypeArray;
    static boolean isUpdateEntity = false;
    static String tenantID = "";
    static List<String> selfReportHeader;
    static int batchsize;
    static String updateURL = "";
    static int fileCount = 0;
    String excelFilePath = null;
    static String contextUpdateFlag = "";
    static String attributeNamesForQuery = "";
    public static String testReportDate = new SimpleDateFormat("dd-MMM-yyyy_hh-mm_a").format(new Date());
    XSSFWorkbook workbook = null;
    File createExcelFile = null;
    static int count = 0;
    Map<String, String> selfReportMap = new HashMap<>();
    Map<String, String> contextReportMap = new HashMap<>();
    Map<String, Map<String, String>> selfReportExcelMap = new HashMap<>();
    Map<String, Map<String, String>> contextReportExcelMap = new HashMap<>();
    String testReport = System.getProperty("user.dir") + "/testReport/";
    static String selfUpdateFlag = "";
    FileOutputStream outputStream;
    static String systemDataLocale = "";
    private static final Logger logger = Logger.getLogger(ToolForModification.class);
    APIUtil apiUtil = null;

    static JsonArray excludeAttributeArray;
    static HashMap<String,List<String>> excludeAttributeMap= new HashMap<>();

    static {
        PropertiesUtility property = new PropertiesUtility();
        property.loadPropertiesJSON(Constants.FILE_PATH);


        batchsize = PropertiesUtility.getBatchSize();
        analysisMode = PropertiesUtility.getAnalysisMode();
        tenantID = PropertiesUtility.getTenantID();
        deleteValue = PropertiesUtility.DeleteValue();
        delteAttribute = PropertiesUtility.DeleteAttribute();
        propertyName = PropertiesUtility.getPropertyName();



        enviromentName = PropertiesUtility.getEnvironmentName();
        entityTypeArray = PropertiesUtility.getEntityTypeArray();

        propertyValue = PropertiesUtility.getPropertyValue();
        selfUpdateFlag = PropertiesUtility.getSelfUpdateFlag();
        contextUpdateFlag = PropertiesUtility.getContextUpdateFlag();
        systemDataLocale = PropertiesUtility.getSystemDataLocale();
        selfReportHeader = getSelfReportHeader();
        updateURL = QueryHelper.getMangeURL(enviromentName, tenantID, Constants.ENTITY_SERVICE, Constants.OPERATION_UPDATE);

        excludeAttributeArray = PropertiesUtility.getExcludeAttributeArray();
        excludeAttributeMap = getExcludeAttributeHashMap();
    }

    public static void main(String[] args) {
        ToolForModification obj=new ToolForModification();
        obj.startProcess();
    }

    public void startProcess() {
        FileUtils.mkdir(testReport + testReportDate);

        CreateFile();
        try {
            performSelfUpdateOperation();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }



    private String getEntityTypeForQuery() {

        String result = "";
        if (entityTypeArray != null && entityTypeArray.size() > 0) {
            for (JsonElement entityTypeElement : entityTypeArray) {
                String entityType = entityTypeElement.getAsJsonObject().get(Constants.ENTITY_TYPE).getAsString();
                result += "\"" + entityType + "\"" + ",";
            }

        }

        return result.substring(0, result.length() - 1);
    }


    private static HashMap<String,List<String>> getExcludeAttributeHashMap()
    {


        HashMap<String,List<String>> attributeMap = new HashMap<>();

    if(excludeAttributeArray!=null && excludeAttributeArray.size() > 0)
    {
        for(JsonElement attributeElement : excludeAttributeArray)
        {
           JsonObject specificEntityTypeAttributeObject =  attributeElement.getAsJsonObject();

           String entityType = specificEntityTypeAttributeObject.get(Constants.ENTITY_TYPE).getAsString();

            JsonArray jsonArrayAttributeArray = specificEntityTypeAttributeObject.getAsJsonArray(Constants.Attribute_List);
              if(jsonArrayAttributeArray!=null && jsonArrayAttributeArray.size()>0){

                  List<String> attributeList = new ArrayList<>();
                      for(JsonElement excludeAttributeElememt : jsonArrayAttributeArray){

                          JsonObject specificExcludeAttributeObject = excludeAttributeElememt.getAsJsonObject();

                          String attributName = specificExcludeAttributeObject.get(Constants.Attribute_Name).getAsString();
                          attributeList.add(attributName);

                      }

                  attributeMap.put(entityType,attributeList);

                  }


              }


        }

    return attributeMap;
    }





    private void CreateFile() {
        try {

            // FileUtils.mkdir(testReport + testReportDate);

            excelFilePath = testReport + testReportDate + "/Data Cleanup Analysis Report" + ++fileCount + ".xlsx";
            createExcelFile = new File(excelFilePath);
            if (!createExcelFile.exists()) {
                createExcelFile.createNewFile();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void writeToExcel() {
        try {
            outputStream = new FileOutputStream(excelFilePath);
            workbook.write(outputStream);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void prepareExcelReport(Map<String, Map<String, String>> selfReportMap, Map<String, Map<String, String>> contextReportMap) throws Exception {
        String individualPropertyName = "", scenarioID = "";
        if (createExcelFile.length() > 0) {
            File f = new File(createExcelFile.toString());
            FileInputStream ios = new FileInputStream(f);
            workbook = new XSSFWorkbook(ios);
            workbook.getSheet(Constants.SHEET1);
            if (selfReportMap != null)
                printSelfAttributesReportInExcel(selfReportMap, workbook, Constants.SHEET1);

            if (contextReportMap != null) {
                if (workbook.getSheet(Constants.SHEET2) == null) {
                    workbook.createSheet(Constants.SHEET2);
                    printContextSpecificAttributesReportInExcel(contextReportMap, workbook, Constants.SHEET2);
                } else {
                    printContextSpecificAttributesReportInExcel(contextReportMap, workbook, Constants.SHEET2);
                }

            }

        } else {

            if (workbook == null) {
                FileInputStream ios = new FileInputStream(createExcelFile);
                workbook = new XSSFWorkbook();
                workbook.createSheet(Constants.SHEET1);


                if (selfReportMap != null) {
                    printSelfAttributesReportInExcel(selfReportMap, workbook, Constants.SHEET1);
                }
                if (contextReportMap != null) {
                    if (workbook.getSheet(Constants.SHEET2) == null) {
                        workbook.createSheet(Constants.SHEET2);
                        printContextSpecificAttributesReportInExcel(contextReportMap, workbook, Constants.SHEET2);
                    } else {
                        printContextSpecificAttributesReportInExcel(contextReportMap, workbook, Constants.SHEET2);
                    }

                }


            } else {
                if (selfReportMap != null) {
                    printSelfAttributesReportInExcel(selfReportMap, workbook, Constants.SHEET1);
                }
//
//                printReportInExcel(selfReportMap, workbook, "Relationship Attribute Difference");
                if (contextReportMap != null) {
                    if (workbook.getSheet(Constants.SHEET2) == null) {
                        workbook.createSheet(Constants.SHEET2);
                        printContextSpecificAttributesReportInExcel(contextReportMap, workbook, Constants.SHEET2);
                    } else {
                        printContextSpecificAttributesReportInExcel(contextReportMap, workbook, Constants.SHEET2);
                    }

                }

            }
        }
    }

    private static List<String> getSelfReportHeader() {
        List<String> selfHeader = new ArrayList<>();
        String selfHeaderNames = "Entity Type,Entity ID,Attribute Name,Property Name,Property Value,Action,Status";
        selfHeader = Arrays.asList(selfHeaderNames.split(","));
        return selfHeader;
    }

    private void printContextSpecificAttributesReportInExcel(Map<String, Map<String, String>> reportMap, XSSFWorkbook workbook, String sheetName) {
        XSSFCell cell;
        //"Client Attribute Difference"
        XSSFSheet sheet = workbook.getSheet(sheetName);
        XSSFRow row;
        if (sheet == null) {
            row = null;
        } else {
            row = sheet.getRow(0);
        }
        Map<String, Integer> headerRow = new HashMap<>();
        if (row == null) {
            row = sheet.createRow(0);
            cell = row.createCell(0);
            cell.setCellValue("Context Type");
        }
        for (int i = 0; i < row.getLastCellNum(); i++) {
            headerRow.put(row.getCell(i).toString(), i);
        }
        int rowCount = workbook.getSheet(sheetName).getLastRowNum();
        for (Map.Entry<String, Map<String, String>> loopSelfAttribute : reportMap.entrySet()) {
            Row newRow = sheet.createRow(++rowCount);
//            loopSelfAttribute.getValue().put("Attribute Name", loopSelfAttribute.getKey());
            for (Map.Entry<String, String> value :
                    loopSelfAttribute.getValue().entrySet()) {
                if (headerRow.containsKey(value.getKey())) {
                    Cell newRowCell = newRow.createCell(headerRow.get(value.getKey()));
                    newRowCell.setCellValue(value.getValue());
                } else {
                    Integer lastnum = (int) row.getLastCellNum();
                    headerRow.put(value.getKey(), lastnum);
                    row.createCell(lastnum).setCellValue(value.getKey());
                    Cell newRowCell = newRow.createCell(lastnum);
                    newRowCell.setCellValue(value.getValue());
                }
            }
        }
    }

    private void createHeader(XSSFRow row, XSSFCell cell, List<String> headerNames) {
        CellStyle style;
        style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int i = 0;
        for (String name : headerNames) {
            cell = row.createCell(i++);
            cell.setCellValue(name);
            cell.setCellStyle(style);
        }

    }

    private void printSelfAttributesReportInExcel(Map<String, Map<String, String>> reportMap, XSSFWorkbook workbook, String sheetName) {
        XSSFCell cell = null;
        XSSFSheet sheet;
        //"Client Attribute Difference"
        if (sheetName != Constants.SHEET1) {
            sheet = workbook.getSheetAt(1);
        } else {
            sheet = workbook.getSheet(sheetName);
        }
        XSSFRow row = sheet.getRow(0);
        Map<String, Integer> headerRow = new HashMap<>();
        if (row == null) {
            row = sheet.createRow(0);
            createHeader(row, cell, selfReportHeader);
        }
        for (int i = 0; i < row.getLastCellNum(); i++) {
            headerRow.put(row.getCell(i).toString(), i);
        }
        int rowCount;
        if (sheetName != Constants.SHEET1) {
            rowCount = workbook.getSheetAt(1).getLastRowNum();
        } else {
            rowCount = workbook.getSheet(sheetName).getLastRowNum();
        }
        for (Map.Entry<String, Map<String, String>> loopSelfAttribute : reportMap.entrySet()) {
            Row newRow = sheet.createRow(++rowCount);
            //loopSelfAttribute.getValue().put("Context Name", loopSelfAttribute.getKey());
            for (Map.Entry<String, String> value :
                    loopSelfAttribute.getValue().entrySet()) {
                if (headerRow.containsKey(value.getKey())) {
                    Cell newRowCell = newRow.createCell(headerRow.get(value.getKey()));
                    newRowCell.setCellValue(value.getValue());
                } else {
                    Integer lastnum = (int) row.getLastCellNum();
                    headerRow.put(value.getKey(), lastnum);
                    row.createCell(lastnum).setCellValue(value.getKey());
                    Cell newRowCell = newRow.createCell(lastnum);
                    newRowCell.setCellValue(value.getValue());
                }
            }
        }
    }

    private List<String> getAllSelfSpecificEntitiesIdsByEntityType(String entityType) {


        String selfSpecificEntityIdsQuery = "";
        String scrollID = "";
        String entitiesQueryIds = "";
        String getEntitiesQuery = "";
        JsonObject selfSpecificEntityIDsResponse;
        List<String> entityIDs;
        JsonObject entitiesObject;
        String globalScrollId = "";
        String clearScrollQuery = "";

        String getUrl = QueryHelper.getMangeURL(enviromentName, tenantID, Constants.ENTITY_SERVICE, Constants.OPERATION_GET);
        selfSpecificEntityIdsQuery = QueryHelper.getSelfContextSpecificEntityIDsQuery(entityType, 2000, systemDataLocale);
        selfSpecificEntityIDsResponse = apiUtil.invokeAPI(selfSpecificEntityIdsQuery, getUrl, tenantID);

        entityIDs = getEntityIDList(selfSpecificEntityIDsResponse);

        scrollID = getScrollId(selfSpecificEntityIDsResponse);
        globalScrollId = scrollID;
        while (scrollID != Constants.INVALID) {
            selfSpecificEntityIdsQuery = QueryHelper.getSelfContextSpecificEntityIdsQuerywithScrollID(scrollID, entityType, 2000);
            selfSpecificEntityIDsResponse = apiUtil.invokeAPI(selfSpecificEntityIdsQuery, getUrl, tenantID);

            entityIDs.addAll(getEntityIDList(selfSpecificEntityIDsResponse));


            globalScrollId = scrollID;
            scrollID = getScrollId(selfSpecificEntityIDsResponse);

        }
        try {

            String getClearscrollURL = QueryHelper.getMangeURL(enviromentName, tenantID, Constants.ENTITY_APP_SERVICE, Constants.OPERATION_CLEAR_SCROLL);
            clearScrollQuery = QueryHelper.getclearScrollQuery(globalScrollId);
            apiUtil.invokeAPI(clearScrollQuery, getClearscrollURL, tenantID);

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return entityIDs;
    }

    private JsonArray getEntitesArray(JsonObject entitiesObject) {
        if (entitiesObject != null && entitiesObject.entrySet().size() > 0) {
            JsonObject responseObject = entitiesObject.getAsJsonObject(Constants.RESPONSE);
            return responseObject.getAsJsonArray(Constants.ENTITIES);
        }
        return null;
    }

    private List<String> getBatchedEntityIds(List<String> entityIDs, int startIndex, int endIndex) {

        return entityIDs.subList(startIndex, endIndex);


    }

    private void performSelfUpdateOperation() throws IOException {
        String entitiesQueryIds = "";
        int batchCount = 0;
        String getEntitiesQuery = "";
        List<String> entityIDs = new ArrayList<>();
        JsonObject entitiesObject;
        int entityCount = 0;
        String getUrl = QueryHelper.getMangeURL(enviromentName, tenantID, Constants.ENTITY_SERVICE, Constants.OPERATION_GET);


        String entityType = getEntityTypeForQuery();
        entityIDs = getAllSelfSpecificEntitiesIdsByEntityType(entityType);
        if (entityIDs.size() > 0) {
            int selfOffset = (int) entityIDs.size() % 100;
            int selfbatchsize = (int) entityIDs.size() / 100;
            int startIndex = 0;
            int endIndex = 0;
            if (selfOffset > 0) {
                selfbatchsize += 1;

            }


            for (int i = 0; i < selfbatchsize; i++) {
                if (entityIDs.size() < 100) {
                    endIndex = entityIDs.size();
                } else if (i == selfbatchsize - 1 && selfOffset > 0) {
                    endIndex += selfOffset;
                } else {
                    endIndex += 100;
                }
                List<String> batchedEntityIds = getBatchedEntityIds(entityIDs, startIndex, endIndex);
                entitiesQueryIds = getEntityIdsForQuery(batchedEntityIds);
                // getEntitiesQuery = QueryHelper.getSelfContextEntitiesUsingIdsQuery(entityType, entitiesQueryIds);
                getEntitiesQuery = QueryHelper.getSelfContextEntitiesUsingIdsQuery(entityType, entitiesQueryIds, systemDataLocale);
                //  getEntitiesQuery="{\"params\":{\"query\":{\"ids\":[\"ro15508158sku\"],\"filters\":{\"typesCriterion\":[\"sku\"],\"nonContextual\":false},\"contexts\":[{\"dcountry\":\"Romania\"}],\"valueContexts\":[{\"source\":\"internal\",\"locale\":\"en-US\"}]},\"options\":{\"maxRecords\":100,\"from\":0},\"fields\":{\"attributes\":[\"_ALL\"]}}}";
                entitiesObject = apiUtil.invokeAPI(getEntitiesQuery, getUrl, tenantID);
                if (entitiesObject != null && entitiesObject.entrySet().size() > 0) {
                    performSelfAttributeUpdateOperation(entitiesObject);

                    try {

                        if (selfReportExcelMap != null && selfReportExcelMap.size() > 0) {
                            int batch=batchCount++;
                            System.out.println("batchprocessed" +batch);
                            logger.info("batchprocessed" + batch);
                            prepareExcelReport(selfReportExcelMap, null);
                            writeToExcel();
                            selfReportExcelMap.clear();
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                    entityCount += 100;
                    if (entityCount == 10000) {
                        outputStream.flush();


                        workbook = null;
                        CreateFile();
                        entityCount = 0;
                    }
                }
                startIndex = endIndex;

            }
            entityIDs.clear();
        }

    }


    private String getEntityID(JsonObject entityObject) {
        return entityObject.get(Constants.ID).getAsString();
    }

    private String getEntityType(JsonObject entityObject) {
        String entityType = "";
        if (entityObject != null && entityObject.entrySet().size() > 0) {
            if (entityObject.has(Constants.TYPE)) {
                entityType = entityObject.get(Constants.TYPE).getAsString();
            }
        }
        return entityType;
    }

    private void performSelfAttributeUpdateOperation(JsonObject entitiesObject) {
        JsonArray entityArray = getEntitesArray(entitiesObject);
        String entityType = "";
        String entityID = "";
        List<String> excludeAttributeList=null;

        if (entityArray != null && entityArray.size() > 0) {
            for (JsonElement entity : entityArray) {
                JsonObject specificEntity = entity.getAsJsonObject();
                entityID = getEntityID(specificEntity);
                entityType = getEntityType(specificEntity);
                isUpdateEntity = false;

                if(excludeAttributeMap.size()>0) {

                    if(excludeAttributeMap.containsKey(entityType)){

                        excludeAttributeList = excludeAttributeMap.get(entityType);

                    }


                }

                specificEntity = performSelfContextDeleteOperation(specificEntity, entityType, entityID, propertyName, propertyValue, deleteValue, delteAttribute, excludeAttributeList);
                try {
                    if (!analysisMode.equalsIgnoreCase(Constants.ON)) {
                        String updateQuery = QueryHelper.generateUpdateQuery(specificEntity);
                        JsonObject updateResponse = apiUtil.invokeAPI(updateQuery, updateURL, tenantID);
                        selfReportMap.put("Entity Type", entityType);
                        selfReportMap.put("Entity ID", entityID);
                        specificEntity = null;
                         String updateStatus=getUpdateStatus(updateResponse);
                        selfReportMap.put("Status", updateStatus);
                        String key = "key" + count++;
                        selfReportExcelMap.put(key,selfReportMap);
                        selfReportMap=new HashMap<>();
                        logger.info("Entity Id:" + entityID + " " + "Entity Type:" + entityType + " " + "Status:"+updateStatus);
                        //System.out.println(updateResponse.toString());
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                specificEntity = null;
            }
        }
    }

    private String getUpdateStatus(JsonObject rwresponseObject) {
        if(rwresponseObject.has("response")) {
            JsonObject responseObject = rwresponseObject.getAsJsonObject("response");
            if (responseObject != null && responseObject.entrySet().size() > 0) {
                if (responseObject.has("status")) {
                    return responseObject.get("status").getAsString();
                }
            }
        }
        return rwresponseObject.toString();
    }

    JsonObject performSelfContextDeleteOperation(JsonObject entityObject, String entityType, String entityId, String propertyName, String propertyValue, String deleteValue, String deleteAttribute, List<String> attributeList) {
        JsonObject dataObject = entityObject.getAsJsonObject(Constants.DATA);

        if (dataObject.has(Constants.ATTRIBUTES)) {
            JsonObject attributesObject = dataObject.getAsJsonObject(Constants.ATTRIBUTES);

            boolean includeAttributeToReport = false;
            if (attributesObject != null) {

                for (Map.Entry<String, JsonElement> attributeEntry : attributesObject.entrySet()) {
                    includeAttributeToReport = false;
                    selfReportMap.put("Entity Type", entityType);
                    selfReportMap.put("Entity ID", entityId);

                    JsonObject specificAttributeObject = attributeEntry.getValue().getAsJsonObject();
                    String attributeName = attributeEntry.getKey();
                    selfReportMap.put("Attribute Name", attributeName);
                    boolean notanexcludeattribute=true;
                    try{
                    if(!attributeList.contains(attributeName)){  notanexcludeattribute = true;  }
                    else { notanexcludeattribute = false;}}
                    catch (Exception e){ notanexcludeattribute=true; logger.error(e.getMessage()); }


                    if(notanexcludeattribute==true){
                    if (specificAttributeObject.entrySet().size() > 0) {
                        if (specificAttributeObject.has(Constants.VALUES)) {
                            JsonArray valuesArray = specificAttributeObject.getAsJsonArray(Constants.VALUES);
                            for (JsonElement element : valuesArray) {
                                JsonObject valueObject = element.getAsJsonObject();
                                if (valueObject != null && valueObject.entrySet().size() > 0) {
                                    if (valueObject.has(propertyName)) {
                                        if (valueObject.get(propertyName).getAsString().equalsIgnoreCase(propertyValue)) {
                                            includeAttributeToReport = true;
                                            if (deleteValue.equalsIgnoreCase("Yes")) {
                                                selfReportMap.put("Property Name", propertyName);
                                                selfReportMap.put("Property Value", propertyValue);
                                                if (!valueObject.has("action")) {
                                                    selfReportMap.put("Action", "Delete");
                                                    if(!analysisMode.equalsIgnoreCase(Constants.ON)) {
                                                        valueObject.addProperty("action", "delete");
                                                    }
                                                }
                                            }
                                            if (deleteAttribute.equalsIgnoreCase("Yes")) {
                                                if (!valueObject.has("action")) {
                                                    selfReportMap.put("Action", "Delete");
                                                    if(!analysisMode.equalsIgnoreCase(Constants.ON)) {
                                                        specificAttributeObject.addProperty("action", "delete");

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }

                        }
                    }}
                    if (includeAttributeToReport) {
                        String key = "key" + count++;
                        selfReportExcelMap.put(key, selfReportMap);
                        selfReportMap = new HashMap<>();
                    } else {
                        selfReportMap.clear();
                    }
                }

            }
        }
        return entityObject;
    }


    private String getEntityIdsForQuery(List<String> entityIDList) {
        String result = "";
        for (String ID : entityIDList) {
            result += "\"" + ID + "\"" + ",";
        }
        return result.substring(0, result.length() - 1);
    }

    private List<String> getEntityIDList(JsonObject entitiesObject) {
        List<String> entityIDs = new ArrayList<>();
        JsonObject responseObject = entitiesObject.getAsJsonObject(Constants.RESPONSE);
        if (responseObject != null) {
            if (responseObject.has(Constants.ENTITIES)) {
                JsonArray entitiesArray = responseObject.getAsJsonArray(Constants.ENTITIES);
                for (JsonElement entity : entitiesArray) {
                    entityIDs.add(entity.getAsJsonObject().get(Constants.ID).getAsString());
                }
            }
            // entityIDs.add(0,"sanju");
        }
        return entityIDs;
    }


    public ToolForModification() {
        apiUtil = new APIUtil();

    }


    private String getAttributeValue(JsonObject entityObject) {
        JsonArray valueArray = entityObject.getAsJsonArray(Constants.VALUES);
        return valueArray.get(0).getAsJsonObject().get(Constants.VALUE).getAsString();
    }

    private double getAttributeDoubleValue(JsonObject entityObject) {
        JsonArray valueArray = entityObject.getAsJsonArray(Constants.VALUES);
        return valueArray.get(0).getAsJsonObject().get(Constants.VALUE).getAsDouble();
    }


    private String getScrollId(JsonObject entityResObject) {
        JsonObject responseObject = entityResObject.getAsJsonObject(Constants.RESPONSE);
        if (responseObject.has(Constants.SCROLLID)) {
            return responseObject.get(Constants.SCROLLID).getAsString();
        }
        return "invalid";
    }

}
