import com.opencsv.CSVWriter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/***
 * Convert dynamic/nested JsonArray/JsonObject to CSV file, it doesn't depend on Schema.
 * CSV headers/Column headers will be added as Json hierarchy Key
 * and the Values of dynamic/nested JsonArray/JsonObject will be added as row in the CSV
 * and will be mapped to the right CSV header.
 */
public class JsonToCsv {
    /* CSV Header */
    private static ArrayList<String> jsonKeyAsHeader;

    /* JSON Key-Value, Keys added as header in CSV, Values added as row in CSV
    *  Using LinkedHashMap to maintain order of insertion  */
    private static HashMap<String, String> jsonKeyValue;

    /* List of JSON Key-Value, Keys added as header in CSV, Values added as row in CSV */
    private static ArrayList<HashMap<String, String>> jsonKeyValueList;
    private static ArrayList<HashMap<String, String>> jsonKeyValueList_2;

    public static void main(String[] args) throws IOException {

        addDifferentJsonsToCsv();
        mergeDifferentJsonsAndAddInCsv();
    }

    /**
     * Read JsonObject.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList.
     *
     * Read JsonArray.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList.
     *
     * Trigger convertJsontoCsv by passing ArrayList @param jsonKeyValueList.
     *
     * @throws IOException
     */
    private static void addDifferentJsonsToCsv() throws IOException {
        jsonKeyValueList = new ArrayList<>();

        String jsonObjectFile = "src/main/resources/json/JsonObject.json";
        String jsonObjectStr = readFileAsString(jsonObjectFile);
        /* Append JsonObject Keys*/
        //String appendJsonObjectKey = "jsonObject_";
        String appendJsonObjectKey = "";
        /* Passing JsonObject.json JsonObject*/
        JsonObject jsonObject = new JsonObject(jsonObjectStr);

        jsonKeyAsHeader = new ArrayList<>();
        jsonKeyValue = new LinkedHashMap<>();
        processJsonObject(jsonObject,appendJsonObjectKey);
        for (int i =0 ; i < 25 ; i++)
            jsonKeyValueList.add(jsonKeyValue);

        String jsonArrayFile = "src/main/resources/json/JsonArray.json";
        String jsonArrayStr = readFileAsString(jsonArrayFile);
        /* Append JsonArray Keys*/
        //String appendJsonArrayKey = "jsonArray_";
        String appendJsonArrayKey = "";
        /* Passing JsonArray.json JsonArray*/
        JsonArray jsonArray = new JsonArray(jsonArrayStr);

        jsonKeyAsHeader = new ArrayList<>();
        jsonKeyValue = new LinkedHashMap<>();
        processJsonArray(jsonArray,appendJsonArrayKey);
        for (int i =0 ; i < 25 ; i++)
            jsonKeyValueList.add(jsonKeyValue);

        convertJsontoCsv(jsonKeyValueList,"src/main/resources/csvHeaders/CsvListHeaders.txt");
    }

    /**
     * Read JsonObject.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList.
     *
     * Read JsonArray.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList_2.
     *
     * Trigger mergeJsontoCsv by passing ArrayList @param jsonKeyValueList and ArrayList @param jsonKeyValueList_2.
     *
     * @throws IOException
     */
    private static void mergeDifferentJsonsAndAddInCsv() throws IOException {
        jsonKeyValueList = new ArrayList<>();
        jsonKeyValueList_2 = new ArrayList<>();

        String jsonObjectFile = "src/main/resources/json/JsonObject.json";
        String jsonObjectStr = readFileAsString(jsonObjectFile);
        /* Append JsonObject Keys*/
        String appendJsonObjectKey = "jsonObject_";
        /* Passing JsonObject.json JsonObject*/
        JsonObject jsonObject = new JsonObject(jsonObjectStr);

        jsonKeyAsHeader = new ArrayList<>();
        jsonKeyValue = new LinkedHashMap<>();
        processJsonObject(jsonObject,appendJsonObjectKey);
        for (int i =0 ; i < 10 ; i++)
            jsonKeyValueList.add(jsonKeyValue);

        String jsonArrayFile = "src/main/resources/json/JsonArray.json";
        String jsonArrayStr = readFileAsString(jsonArrayFile);
        /* Append JsonArray Keys*/
        String appendJsonArrayKey = "jsonArray_";
        /* Passing JsonArray.json JsonArray*/
        JsonArray jsonArray = new JsonArray(jsonArrayStr);

        jsonKeyAsHeader = new ArrayList<>();
        jsonKeyValue = new LinkedHashMap<>();
        processJsonArray(jsonArray,appendJsonArrayKey);
        for (int i =0 ; i < 10 ; i++)
            jsonKeyValueList_2.add(jsonKeyValue);

        mergeJsontoCsv(jsonKeyValueList,jsonKeyValueList_2,"src/main/resources/csvHeaders/MergeCsvListHeaders.txt");
    }

    /**
     * @param jsonObjectInString Check whether valid JsonObject format
     * @return Return true if valid else false
     */

    private static boolean isJsonObjectValid(String jsonObjectInString) {
        try {
            JsonObject localJsonObject = new JsonObject(jsonObjectInString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param jsonArrayInString Check whether valid JsonArray format, if so process each Json in the array
     * @return Return true if valid else false
     */

    private static boolean isJsonArrayValid(String jsonArrayInString, String json) {
        try {
            JsonArray jsonArray = new JsonArray(jsonArrayInString);
            return processJsonArray(jsonArray,json);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param jsonArray Process each Json in the array
     * @param json if different json has same key name, use this param which will append string to differentiate them.
     */
    private static boolean processJsonArray(JsonArray jsonArray, String json) {

        /* Check if JsonArray has empty values like [], [{}], [{},{}], [{},{"a":"b"},{},{"c":"d"}] ... */
        int emptyValues = 0;

        /* Example key65": ["jsonObject_value65-1", "jsonObject_value65-2", "jsonObject_value65-3"] */
        boolean itsArrayNotJsonArray = true;

        for (int i = 0; i < jsonArray.size(); i++) {
            try
            {
                if (isJsonObjectValid(String.valueOf(jsonArray.getJsonObject(i)))) {
                    processJsonObject(jsonArray.getJsonObject(i),json);
                    itsArrayNotJsonArray = false;
                    if(jsonArray.getJsonObject(i).size()==0){
                        emptyValues++;
                    }
                }
            } catch (Exception e) {
                /* Ignore this errors if JsonArray doesn't have valid JsonObject */
            }
        }

        if (emptyValues > 0 || itsArrayNotJsonArray) {
            return false;
        }

        return true;
    }

    /**
     * @param jsonObject Process @param jsonObject and store as Key - Value in HashMap @param jsonKeyValue
     *                   Key is @headerKey
     *                   Value is @param a.getValue()
     * @param json if different json has same key name, use this param which will append string to differentiate them.
     */
    private static void processJsonObject(JsonObject jsonObject, String json) {
        jsonObject.forEach(a -> {
            jsonKeyAsHeader.add(a.getKey() + ":");
            /* Check for empty values like [], [{}], [{},{}] ... */
            if (isJsonArrayValid(String.valueOf(a.getValue()),json) && new JsonArray(String.valueOf(a.getValue())).size()!=0){
                jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
            /* Check for empty values like {} */
            } else if (isJsonObjectValid(String.valueOf(a.getValue())) && new JsonObject(String.valueOf(a.getValue())).size()!=0) {
                processJsonObject(new JsonObject(String.valueOf(a.getValue())),json);
                jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
            } else {
                /* @headerKey is the key, @a.getValue() is the value */
                StringBuilder headerKey = new StringBuilder();
                headerKey.append(json);
                for (String str : jsonKeyAsHeader) {
                    headerKey.append(str);
                }
                /* If key is there in HashMap, add new key with a counter appended. */
                if(jsonKeyValue.containsKey(String.valueOf(headerKey)))
                {
                    int count = 1;
                    headerKey = appendHeaderKey(count,json);
                }
                //System.out.print(headerKey+"::-->:::");
                //System.out.println(a.getValue());
                jsonKeyValue.put(String.valueOf(headerKey), String.valueOf(a.getValue()));
                jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
            }
        });
    }

    /**
     * If JsonArray/JsonObject has same key name, add new key with a counter appended.
     * @param count
     * @return
     */

    private static StringBuilder appendHeaderKey(int count, String json) {
        StringBuilder headerKey = new StringBuilder();
        headerKey.append(json);
        for (String str : jsonKeyAsHeader) {
            headerKey.append(str);
        }
        //System.out.println("Before :" + headerKey);
        
        headerKey.append(count);
        if(jsonKeyValue.containsKey(String.valueOf(headerKey)))
        {
            headerKey = appendHeaderKey(++count,json);
        }
        //System.out.println("After :"+headerKey);
        
        return headerKey;
    }

    /**
     * Add each Dynamic/nested Json as a row in CSV.
     * @param jsonKeyValueList ArrayList of HashMaps(key-values),
     *                              keys are added as header in CSV,
     *                              values will be added as rows in CSV.
     * @throws IOException
     */
    private static void convertJsontoCsv(ArrayList<HashMap<String, String>> jsonKeyValueList, String keysText) throws IOException {

        Path fileToDeletePath = Paths.get("src/main/resources/csv/CsvList.csv");
        Files.deleteIfExists(fileToDeletePath);
        File listFile = new File("src/main/resources/csv/CsvList.csv");
        FileWriter listFileWriter = new FileWriter(listFile, true);
        CSVWriter csvWriter = new CSVWriter(listFileWriter);
        Set<String> headerStrSet = new LinkedHashSet<>();

        /* Read Keys if file is not empty*/
        Scanner sc = new Scanner(new File(keysText));
        while(sc.hasNext()){
            String line = sc.nextLine();
            headerStrSet.add(line);
        }
        sc.close();
        /*
         Read all HashMap Keys from ArrayList, store in a Set @headerStrSet and add as a header in CSV.
         */

        /*jsonKeyValueList.forEach(map-> headerStrSet.addAll(map.keySet()));
          csvWriter.writeNext(headerStrSet.stream().toArray(String[]::new));*/

        if(headerStrSet.isEmpty()) {
            PrintWriter headerWriter = new PrintWriter(keysText, "UTF-8");
            for (int i = 0; i < jsonKeyValueList.size(); i++) {
                for (String headerStr : jsonKeyValueList.get(i).keySet()) {
                    headerStrSet.add(headerStr);
                }
            }
            for (String headerStr : headerStrSet) {
                headerWriter.println(headerStr);
            }
            headerWriter.close();
        }
        /* Convert Set @headerStrSet to String array @headerStrArray  */
        String[] headerStrArray = new String[headerStrSet.size()];
        int headerCount = 0;
        for (String headerStr : headerStrSet) {
            headerStrArray[headerCount++] = headerStr;
        }
        csvWriter.writeNext(headerStrArray);


        /* Add each HashMap values as a row in CSV. */

           /* jsonKeyValueList.forEach(map->{
                ArrayList<String> stringValueArray = new ArrayList<>();
                headerStrSet.forEach(set->stringValueArray.add(map.get(set)));
                csvWriter.writeNext(stringValueArray.stream().toArray(String[]::new));
            });*/

        for (int i = 0; i < jsonKeyValueList.size(); i++) {
            String[] mapValueArray = new String[headerStrSet.size()];
            int j = 0;
            for (String key : headerStrSet) {
                mapValueArray[j++] = jsonKeyValueList.get(i).get(key);
            }
            csvWriter.writeNext(mapValueArray);
        }
        csvWriter.close();
    }

    /**
     * Merge Different Jsons and add in Csv as a single row based on common value. In this case key10.
     * @param jsonKeyValueList ArrayList of HashMaps(key-values); first Json, keys are added as header in CSV, values will be added as rows in CSV.
     * @param jsonKeyValueList_2 ArrayList of HashMaps(key-values); second Json, keys are added as header in CSV, values will be added as rows in CSV.
     * @throws IOException
     */
    private static void mergeJsontoCsv(ArrayList<HashMap<String, String>> jsonKeyValueList, ArrayList<HashMap<String, String>> jsonKeyValueList_2, String keysText) throws IOException {

        Path fileToDeletePath = Paths.get("src/main/resources/csv/MergeCsvList.csv");
        Files.deleteIfExists(fileToDeletePath);
        File listFile = new File("src/main/resources/csv/MergeCsvList.csv");
        FileWriter listFileWriter = new FileWriter(listFile, true);
        CSVWriter csvWriter = new CSVWriter(listFileWriter);
        Set<String> headerStrSet = new LinkedHashSet<>();

        /* Read Keys if file is not empty*/
        Scanner sc = new Scanner(new File(keysText));
        while(sc.hasNext()){
            String line = sc.nextLine();
            headerStrSet.add(line);
        }
        sc.close();

        if(headerStrSet.isEmpty()) {
            PrintWriter headerWriter = new PrintWriter(keysText, "UTF-8");
            /*
                Read all HashMap Keys from ArrayList @param jsonKeyValueList, store in a Set @param headerStrSet and add as a header in CSV.
            */

            for (int i = 0; i < jsonKeyValueList.size(); i++) {
                for (String headerStr : jsonKeyValueList.get(i).keySet()) {
                    headerStrSet.add(headerStr);
                }
            }

            /*
                Read all HashMap Keys from ArrayList @param jsonKeyValueList_2, store in a Set @param headerStrSet and add as a header in CSV.
            */

            for (int i = 0; i < jsonKeyValueList_2.size(); i++) {
                for (String headerStr : jsonKeyValueList_2.get(i).keySet()) {
                    headerStrSet.add(headerStr);
                }
            }

            for (String headerStr : headerStrSet) {
                headerWriter.println(headerStr);
            }

            headerWriter.close();
        }

        /* Convert Set @headerStrSet to String array @headerStrArray  */
        String[] headerStrArray = new String[headerStrSet.size()];
        int headerCount = 0;
        for (String headerStr : headerStrSet) {
            headerStrArray[headerCount++] = headerStr;
        }
        csvWriter.writeNext(headerStrArray);


        /* If key10 value of JsonObject and JsonArray match, then merge JsonObject and JsonArray as a row in CSV. */

        for (int i = 0; i < jsonKeyValueList.size(); i++) {
            for (int j = 0; j < jsonKeyValueList_2.size(); j++) {
                if(jsonKeyValueList.get(i).containsKey("jsonObject_key2:key3:key6:key10:")
                        &&
                        jsonKeyValueList_2.get(j).containsKey("jsonArray_key5:key6:key10:")
                        &&
                        jsonKeyValueList.get(i).get("jsonObject_key2:key3:key6:key10:")
                                .equals(jsonKeyValueList_2.get(j).get("jsonArray_key5:key6:key10:"))) {

                    String[] mapValueArray = new String[headerStrSet.size()];
                    int k = 0;
                    for (String key : headerStrSet) {
                        if (jsonKeyValueList.get(i).containsKey(key))
                            mapValueArray[k++] = jsonKeyValueList.get(i).get(key);
                        else if (jsonKeyValueList_2.get(j).containsKey(key))
                            mapValueArray[k++] = jsonKeyValueList_2.get(j).get(key);
                        else k++;
                    }
                    csvWriter.writeNext(mapValueArray);
                }
            }
        }
        csvWriter.close();
    }

    /**
     *
     * @param file Read File as a String
     * @return
     */
    private static String readFileAsString(String file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "readFileAsString failed";
    }
}
