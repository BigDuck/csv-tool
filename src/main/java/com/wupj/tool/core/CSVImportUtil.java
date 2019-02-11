package com.wupj.tool.core;

import com.wupj.tool.util.FieldReflectionUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wupeiji
 */
public class CSVImportUtil {
    static Logger logger = LoggerFactory.getLogger(CSVImportUtil.class);

    /**
     * 导入csv为POJO
     *
     * @param filePath    文件路径
     * @param clazz       对应的pojo
     * @param skipHeader  是否跳过第一行
     * @param ignoreError 是否忽略掉错误的数据行
     * @return
     */
    public static List<Object> importCsv(String filePath, Class<?> clazz, boolean skipHeader, String charsetName, boolean ignoreError) throws IOException {
        Reader reader = null;
        CSVParser parser = null;
        List<Object> resultList = new ArrayList<>();
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(filePath)));
            reader = new InputStreamReader(new BOMInputStream(bufferedInputStream), charsetName);
            if (skipHeader) {
                // 这边以excel就不限于csv使用的分隔符是否是逗号了
                parser = new CSVParser(reader, CSVFormat.EXCEL.withHeader().withIgnoreHeaderCase().withTrim());
            } else {
                parser = new CSVParser(reader, CSVFormat.EXCEL.withIgnoreHeaderCase().withTrim());
            }
            List<Field> clazzField = new ArrayList<>();
            if (clazz.getDeclaredFields() != null && clazz.getDeclaredFields().length > 0) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    clazzField.add(field);
                }
            }
            if (clazzField.size() <= 0) {
                throw new RuntimeException("data field can not be empty");
            }
            // 遍历数据封装成对象
            for (final CSVRecord record : parser) {
                Object recordObj = clazz.newInstance();
                try {
                    for (Field field : clazzField) {
                        field.setAccessible(true);
                        String s = record.get(field.getName());
                        try {
                            field.set(recordObj, FieldReflectionUtil.parseValue(field, s));
                        } catch (IllegalArgumentException e) {
                            if (ignoreError) {
                                field.set(recordObj, null);
                            } else {
                                throw e;
                            }
                        }
                    }
                } catch (SecurityException e) {
                    throw e;
                } catch (IllegalAccessException e) {
                    throw e;
                } catch (IllegalArgumentException e) {
                    if (ignoreError) {
                        logger.error("data error this line will not be record ");
                    } else {
                        throw e;
                    }
                }
                System.out.println(recordObj.toString());
                resultList.add(recordObj);
            }
        } catch (Exception e) {
            logger.error("read data error", e);
            throw new RuntimeException("read csv error", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (parser != null) {
                    parser.close();
                }
            } catch (IOException e) {
                // do nothing
                logger.error("close reader or CSVParser error",e);
            }
        }
        return resultList;
    }

    public static void main(String[] args) {

    }
}
