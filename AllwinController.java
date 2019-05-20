package com.haier.fshow.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.haier.fshow.core.Tool;
import com.haier.fshow.model.*;
import com.haier.fshow.service.CommonService;
import com.haier.fshow.service.RoleService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@CrossOrigin
@Validated
@RequestMapping("/allwin")
public class AllwinController {
    @Resource
    CommonService commonService;

    @Resource
    RoleService roleService;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public void test(
            HttpServletRequest request, HttpServletResponse response) {
//        List<DimWinaddSubject> subjects = commonService.getsubjecttree();

        File file = new File("temp.xlsx");


        Workbook wb = null;
        try {
            wb = new XSSFWorkbook(new FileInputStream(file));

        } catch (IOException e) {
            e.printStackTrace();

        }

        List<String> name = new ArrayList<>();
        List<String> type = new ArrayList<>();

        Sheet sheet = wb.getSheetAt(0);//获取第一张表
        for (int i = 0; i < sheet.getLastRowNum() + 1; i++) {
            Row row = sheet.getRow(i);//获取索引为i的行，以0开始
            name.add(row.getCell(0).getStringCellValue());//获取第i行的索引为0的单元格数据
            type.add(row.getCell(1).getStringCellValue());


        }
        StringBuilder sb = new StringBuilder();

        sb.append("sb.");
        for (int i = 0; i < name.size(); i++) {
            sb.append("append(\"" + type.get(i) + ":\").append(bean.get" + name.get(i).substring(0, 1).toUpperCase() + name.get(i).substring(1) + "()).append(\"\\n\").");

        }
        String result = sb.toString().substring(0, sb.toString().length() - 1);


    }

    @RequestMapping(value = "/checkpermit", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean checkpermit(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                   String corp, String dept, HttpServletResponse response) {
        RespondBean result = new RespondBean();

        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        String permit;
        if (dept.equals(plate)) {
            permit = plate;

        } else if (dept.equals(platform)) {
            permit = platform;


        } else if (dept.equals(corp)) {


//一级小微  财务 财务
            List<DimWinaddOrg> depts = commonService.getdeptbycorp(plate, platform, corp);

            if (depts.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            permit = depts.get(0).getCompanyCode();


        } else {
            //二级小微
            List<DimWinaddOrg> depts = commonService.getdeptinfo(plate, platform, corp, dept);
            if (depts.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            permit = depts.get(0).getDeptCode();

        }
        try {
            SecurityUtils.getSubject().checkPermission(permit);

        } catch (AuthorizationException e) {
            result.setCode(1);
            result.setMsg("无权限");
            return result;
        }
        result.setMsg("有权限");
        return result;
    }

    @RequestMapping(value = "/exporttable", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean exporttable(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                   String corp, String dept, @NotEmpty(message = "date不能为空") String date,
                                   HttpServletResponse response) throws UnsupportedEncodingException {
        RespondBean result = new RespondBean();
        String postfix = Joiner.on("-").skipNulls().join("共赢增值表", Strings.emptyToNull(plate), Strings.emptyToNull(platform), Strings.emptyToNull(corp), Strings.emptyToNull(dept), Strings.emptyToNull(date));


        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        String dayY = Tool.getlastday(date, "Y");
        String dayM = Tool.getlastday(date, "M");
        List<String> firstline = new ArrayList<>();
        List<String> secline = new ArrayList<>();
        secline.add("项目");

        List<DmWinaddResults> dataY = new ArrayList<>();
        List<DmWinaddResults> dataM = new ArrayList<>();
        List<DimWinaddSubject> subjects = commonService.getsubject();
        TableBean tableBean = new TableBean();
        tableBean.setTableHeader(new ArrayList<>());
        List<List> extable = new ArrayList<>();
        extable.add(firstline);
        extable.add(secline);
        DimWinaddOrg all = new DimWinaddOrg();
        all.setPlate(plate);
        all.setPlatform(platform);
        all.setCompanyCode(corp);
        all.setCompanyName(corp);
        all.setDeptName(dept);
        all.setDeptCode(dept);

        tableBean.setTableBody(new ArrayList<>());
        if (dept.equals(plate)) {

            //总览
            List<DimWinaddOrg> corps = commonService.getplateform(plate);

            corps.add(0, all);
            List<String> corpcodes = corps.stream().map(DimWinaddOrg::getPlatform).collect(Collectors.toList());
            if (corps.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            firstline.addAll(Tool.getextableheader(corps, 3));
            //类金融
            dataY = commonService.getplateformtable(dayY, corps);
            dataM = commonService.getplateformtable(dayM, corps);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }

            //一级科目
            for (DimWinaddSubject subject : subjects) {

                //二级科目
                //补全缺失的数据
                List temp = new ArrayList();
                temp.add(Tool.getDimWinaddSubject(subject, platform));

                List tempM = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));
                List tempY = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));

                for (int i = 0; i < dataM.size(); i++) {
                    if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {

                        if (corpcodes.indexOf(dataM.get(i).getPlatform()) != -1) {
                            tempM.set(corpcodes.indexOf(dataM.get(i).getPlatform()), dataM.get(i).getActMonth());
                        }

                    }
                }
                for (int i = 0; i < dataY.size(); i++) {
                    if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {
                        if (corpcodes.indexOf(dataY.get(i).getPlatform()) != -1) {
                            tempY.set(corpcodes.indexOf(dataY.get(i).getPlatform()), dataY.get(i).getActYear());

                        }

                    }
                }
                if (subject.getDataSource() != null) {
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));
                    }
                }
                extable.add(temp);


            }

        } else if (dept.equals(platform)) {

            List<DimWinaddOrg> corps = commonService.getcorpbyplatform(plate, platform);
            corps.add(0, all);
            List<String> corpcodes = corps.stream().map(DimWinaddOrg::getCompanyCode).collect(Collectors.toList());
            if (corps.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            firstline.addAll(Tool.getextableheader(corps, 1));
            //类金融
            dataY = commonService.gettable(dayY, corps);
            dataM = commonService.gettable(dayM, corps);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }

            //一级科目
            for (DimWinaddSubject subject : subjects) {

                //二级科目
                //补全缺失的数据
                List temp = new ArrayList();
                //前两列名称
                temp.add(Tool.getDimWinaddSubject(subject, platform));

                List tempM = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));
                List tempY = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));

                for (int i = 0; i < dataM.size(); i++) {
                    if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {

                        if (corpcodes.indexOf(dataM.get(i).getCompanyCode()) != -1) {
                            tempM.set(corpcodes.indexOf(dataM.get(i).getCompanyCode()), dataM.get(i).getActMonth());
                        }

                    }
                }
                for (int i = 0; i < dataY.size(); i++) {
                    if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {
                        if (corpcodes.indexOf(dataY.get(i).getCompanyCode()) != -1) {
                            tempY.set(corpcodes.indexOf(dataY.get(i).getCompanyCode()), dataY.get(i).getActYear());

                        }

                    }
                }
                if (subject.getDataSource() != null) {
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));
                    }
                }
                extable.add(temp);

            }


        } else if (dept.equals(corp)) {


//一级小微  财务 财务
            List<DimWinaddOrg> depts = commonService.getdeptbycorp(plate, platform, corp);

            if (depts.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }

            all.setCompanyName(depts.get(0).getCompanyName());
            all.setCompanyCode(depts.get(0).getCompanyCode());
            all.setDeptCode(depts.get(0).getCompanyCode());
            all.setDeptName(depts.get(0).getCompanyName());
            depts.add(0, all);
            List<String> deptcodes = depts.stream().map(DimWinaddOrg::getDeptCode).collect(Collectors.toList());

            firstline.addAll(Tool.getextableheader(depts, 2));

            dataY = commonService.gettabledept(dayY, depts);
            dataM = commonService.gettabledept(dayM, depts);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }


            //一级科目
            for (DimWinaddSubject subject : subjects) {

                List temp = new ArrayList();
                temp.add(Tool.getDimWinaddSubject(subject, platform));
                List tempM = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));
                List tempY = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));

                for (int i = 0; i < dataM.size(); i++) {
                    if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {

                        if (deptcodes.indexOf(dataM.get(i).getDeptCode()) != -1) {
                            tempM.set(deptcodes.indexOf(dataM.get(i).getDeptCode()), dataM.get(i).getActMonth());
                        }

                    }
                }
                for (int i = 0; i < dataY.size(); i++) {
                    if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {
                        if (deptcodes.indexOf(dataY.get(i).getDeptCode()) != -1) {
                            tempY.set(deptcodes.indexOf(dataY.get(i).getDeptCode()), dataY.get(i).getActYear());

                        }

                    }
                }
                if (subject.getDataSource() != null) {
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));
                    }
                }

                extable.add(temp);
            }


        } else {
            //二级小微
            List<DimWinaddOrg> depts = commonService.getdeptinfo(plate, platform, corp, dept);
            List<String> deptcodes = depts.stream().map(DimWinaddOrg::getDeptCode).collect(Collectors.toList());
            if (depts.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }

            firstline.addAll(Tool.getextableheader(depts, 2));
            dataY = commonService.gettabledept(dayY, depts);
            dataM = commonService.gettabledept(dayM, depts);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }

            //一级科目
            for (DimWinaddSubject subject : subjects) {

                List temp = new ArrayList();
                temp.add(Tool.getDimWinaddSubject(subject, platform));
                List tempM = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));
                List tempY = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));

                for (int i = 0; i < dataM.size(); i++) {
                    if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {

                        if (deptcodes.indexOf(dataM.get(i).getDeptCode()) != -1) {
                            tempM.set(deptcodes.indexOf(dataM.get(i).getDeptCode()), dataM.get(i).getActMonth());
                        }

                    }
                }
                for (int i = 0; i < dataY.size(); i++) {
                    if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(subject.getSubjectCode())) {
                        if (deptcodes.indexOf(dataY.get(i).getDeptCode()) != -1) {
                            tempY.set(deptcodes.indexOf(dataY.get(i).getDeptCode()), dataY.get(i).getActYear());

                        }

                    }
                }
                if (subject.getDataSource() != null) {
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));
                    }
                }
                extable.add(temp);
            }


        }
        result.setMsg("查询表格数据成功");
        result.setData(JSON.toJSONString(tableBean));


        String fileName = new String(URLEncoder.encode(postfix, "utf-8").getBytes(), "UTF-8");
        response.setHeader("Content-disposition", "attachment; filename=" + fileName + ".xlsx");

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        // 第二步，在webbook中添加一个sheet,对应Excel文件中的sheet
        Sheet sheet = workbook.createSheet("sheet");
        // 第三步，在sheet中添加表头第0行,注意老版本poi对Excel的行数列数有限制short
        // 第四步，创建单元格，并设置值表头 设置表头居中
        CellStyle hssfCellStyle = workbook.createCellStyle();
        //居中样式
        hssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
        hssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        Cell hssfCell = null;
        CellRangeAddress region = new CellRangeAddress(0, 1, 0, 0);// 下标从0开始 起始行号，终止行号， 起始列号，终止列号
        sheet.addMergedRegion(region);

        for (int i = 0; i < (firstline.size() - 1) / 2; i++) {
            secline.add("月累");
            secline.add("年累");
            CellRangeAddress region1 = new CellRangeAddress(0, 0, i * 2 + 1, i * 2 + 2);// 起始行号，终止行号， 起始列号，终止列号
            //在sheet里增加合并单元格
            sheet.addMergedRegion(region1);

        }

        for (int i = 0; i < extable.size(); i++) {
            Row hssfRow = sheet.createRow(i);
            for (int j = 0; j < extable.get(i).size(); j++) {
                hssfCell = hssfRow.createCell(j);//列索引从0开始
                hssfCell.setCellValue(extable.get(i).get(j).toString());//列名1
                if (i < 2) {
                    hssfCell.setCellStyle(hssfCellStyle);

                }
            }

        }


        try {
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }

    @RequestMapping(value = "/gettable", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean gettable(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                String corp, String dept, @NotEmpty(message = "date不能为空") String date,
                                HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        String dayY = Tool.getlastday(date, "Y");
        String dayM = Tool.getlastday(date, "M");

        List<DmWinaddResults> dataY = new ArrayList<>();
        List<DmWinaddResults> dataM = new ArrayList<>();
        List<DimWinaddSubject> subjects = commonService.getsubjecttree();
        TableBean tableBean = new TableBean();
        tableBean.setTableHeader(new ArrayList<>());

        DimWinaddOrg all = new DimWinaddOrg();
        all.setPlate(plate);
        all.setPlatform(platform);
        all.setCompanyCode(corp);
        all.setCompanyName(corp);
        all.setDeptName(dept);
        all.setDeptCode(dept);

        tableBean.setTableBody(new ArrayList<>());
        if (dept.equals(plate)) {
            try {
                SecurityUtils.getSubject().checkPermission(plate);

            } catch (AuthorizationException e) {
                result.setCode(1);
                result.setMsg("无权限");
                return result;
            }

            //总览
            List<DimWinaddOrg> corps = commonService.getplateform(plate);

            corps.add(0, all);
            List<String> corpcodes = corps.stream().map(DimWinaddOrg::getPlatform).collect(Collectors.toList());
            if (corps.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            tableBean.setTableHeader(Tool.gettableheader(corps, 3));
            //类金融
            dataY = commonService.getplateformtable(dayY, corps);
            dataM = commonService.getplateformtable(dayM, corps);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }

            //一级科目
            for (DimWinaddSubject subject : subjects) {
                if (Optional.of(subject).map(DimWinaddSubject::getList).map(List::size).get() == 0) {
                    subject.setList(new ArrayList<>());
                    subject.getList().add(subject);
                }
                for (DimWinaddSubject dimWinaddSubject : Optional.of(subject).map(DimWinaddSubject::getList).get()) {
                    //二级科目
                    //补全缺失的数据
                    List temp = new ArrayList();
                    //前两列名称
                    temp.add(Tool.getDimWinaddSubject(subject, platform));
                    temp.add(Tool.getDimWinaddSubject(dimWinaddSubject, platform));

                    List tempM = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));
                    List tempY = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));

                    for (int i = 0; i < dataM.size(); i++) {
                        if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {

                            if (corpcodes.indexOf(dataM.get(i).getPlatform()) != -1) {
                                tempM.set(corpcodes.indexOf(dataM.get(i).getPlatform()), dataM.get(i).getActMonth());
                            }

                        }
                    }
                    for (int i = 0; i < dataY.size(); i++) {
                        if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {
                            if (corpcodes.indexOf(dataY.get(i).getPlatform()) != -1) {
                                tempY.set(corpcodes.indexOf(dataY.get(i).getPlatform()), dataY.get(i).getActYear());

                            }

                        }
                    }
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));

                    }
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < temp.size(); i++) {
                        map.put("key" + (i + 1), temp.get(i));
                    }
                    map.put("unit", dimWinaddSubject.getUnit());
                    tableBean.getTableBody().add(map);
                }
            }

        } else if (dept.equals(platform)) {
            try {
                SecurityUtils.getSubject().checkPermission(platform);

            } catch (AuthorizationException e) {
                result.setCode(1);
                result.setMsg("无权限");
                return result;
            }

            List<DimWinaddOrg> corps = commonService.getcorpbyplatform(plate, platform);
            corps.add(0, all);
            List<String> corpcodes = corps.stream().map(DimWinaddOrg::getCompanyCode).collect(Collectors.toList());
            if (corps.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            tableBean.setTableHeader(Tool.gettableheader(corps, 1));
            //类金融
            dataY = commonService.gettable(dayY, corps);
            dataM = commonService.gettable(dayM, corps);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }

            //一级科目
            for (DimWinaddSubject subject : subjects) {
                if (Optional.of(subject).map(DimWinaddSubject::getList).map(List::size).get() == 0) {
                    subject.setList(new ArrayList<>());
                    subject.getList().add(subject);
                }
                for (DimWinaddSubject dimWinaddSubject : Optional.of(subject).map(DimWinaddSubject::getList).get()) {
                    //二级科目
                    //补全缺失的数据
                    List temp = new ArrayList();
                    //前两列名称
                    temp.add(Tool.getDimWinaddSubject(subject, platform));
                    temp.add(Tool.getDimWinaddSubject(dimWinaddSubject, platform));

                    List tempM = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));
                    List tempY = new ArrayList(Collections.nCopies(corpcodes.size(), "-"));

                    for (int i = 0; i < dataM.size(); i++) {
                        if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {

                            if (corpcodes.indexOf(dataM.get(i).getCompanyCode()) != -1) {
                                tempM.set(corpcodes.indexOf(dataM.get(i).getCompanyCode()), dataM.get(i).getActMonth());
                            }

                        }
                    }
                    for (int i = 0; i < dataY.size(); i++) {
                        if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {
                            if (corpcodes.indexOf(dataY.get(i).getCompanyCode()) != -1) {
                                tempY.set(corpcodes.indexOf(dataY.get(i).getCompanyCode()), dataY.get(i).getActYear());

                            }

                        }
                    }
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));

                    }
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < temp.size(); i++) {
                        map.put("key" + (i + 1), temp.get(i));
                    }
                    map.put("unit", dimWinaddSubject.getUnit());

                    tableBean.getTableBody().add(map);
                }
            }


        } else if (dept.equals(corp)) {
//一级小微  财务 财务
            List<DimWinaddOrg> depts = commonService.getdeptbycorp(plate, platform, corp);

            if (depts.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            try {
                SecurityUtils.getSubject().checkPermission(depts.get(0).getCompanyCode());

            } catch (AuthorizationException e) {
                result.setCode(1);
                result.setMsg("无权限");
                return result;
            }
            all.setCompanyName(depts.get(0).getCompanyName());
            all.setCompanyCode(depts.get(0).getCompanyCode());
            all.setDeptCode(depts.get(0).getCompanyCode());
            all.setDeptName(depts.get(0).getCompanyName());
            depts.add(0, all);
            List<String> deptcodes = depts.stream().map(DimWinaddOrg::getDeptCode).collect(Collectors.toList());

            tableBean.setTableHeader(Tool.gettableheader(depts, 2));


            dataY = commonService.gettabledept(dayY, depts);
            dataM = commonService.gettabledept(dayM, depts);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }


            //一级科目
            for (DimWinaddSubject subject : subjects) {
                if (Optional.of(subject).map(DimWinaddSubject::getList).map(List::size).get() == 0) {
                    subject.setList(new ArrayList<>());
                    subject.getList().add(subject);
                }
                for (DimWinaddSubject dimWinaddSubject : Optional.of(subject).map(DimWinaddSubject::getList).get()) {
                    //二级科目
                    List temp = new ArrayList();
                    temp.add(Tool.getDimWinaddSubject(subject, platform));
                    temp.add(Tool.getDimWinaddSubject(dimWinaddSubject, platform));
                    List tempM = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));
                    List tempY = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));

                    for (int i = 0; i < dataM.size(); i++) {
                        if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {

                            if (deptcodes.indexOf(dataM.get(i).getDeptCode()) != -1) {
                                tempM.set(deptcodes.indexOf(dataM.get(i).getDeptCode()), dataM.get(i).getActMonth());
                            }

                        }
                    }
                    for (int i = 0; i < dataY.size(); i++) {
                        if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {
                            if (deptcodes.indexOf(dataY.get(i).getDeptCode()) != -1) {
                                tempY.set(deptcodes.indexOf(dataY.get(i).getDeptCode()), dataY.get(i).getActYear());

                            }

                        }
                    }
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));

                    }
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < temp.size(); i++) {
                        map.put("key" + (i + 1), temp.get(i));
                    }
                    map.put("unit", dimWinaddSubject.getUnit());

                    tableBean.getTableBody().add(map);
                }


            }

        } else {
            //二级小微
            List<DimWinaddOrg> depts = commonService.getdeptinfo(plate, platform, corp, dept);
            List<String> deptcodes = depts.stream().map(DimWinaddOrg::getDeptCode).collect(Collectors.toList());
            if (depts.size() < 1) {
                result.setMsg("没有查到该企业");
                result.setCode(1);
                return result;
            }
            try {
                SecurityUtils.getSubject().checkPermission(depts.get(0).getDeptCode());

            } catch (AuthorizationException e) {
                result.setCode(1);
                result.setMsg("无权限");
                return result;
            }
            tableBean.setTableHeader(Tool.gettableheader(depts, 2));

            dataY = commonService.gettabledept(dayY, depts);
            dataM = commonService.gettabledept(dayM, depts);
//            if (dataY.size() != dataM.size()) {
//                result.setMsg("年累和月累数据个数不一样");
//                result.setCode(1);
//                return result;
//            }

            //一级科目
            for (DimWinaddSubject subject : subjects) {
                if (Optional.of(subject).map(DimWinaddSubject::getList).map(List::size).get() == 0) {
                    subject.setList(new ArrayList<>());
                    subject.getList().add(subject);
                }
                for (DimWinaddSubject dimWinaddSubject : Optional.of(subject).map(DimWinaddSubject::getList).get()) {
                    //二级科目
                    List temp = new ArrayList();
                    temp.add(Tool.getDimWinaddSubject(subject, platform));
                    temp.add(Tool.getDimWinaddSubject(dimWinaddSubject, platform));
                    List tempM = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));
                    List tempY = new ArrayList(Collections.nCopies(deptcodes.size(), "-"));

                    for (int i = 0; i < dataM.size(); i++) {
                        if (Strings.nullToEmpty(dataM.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {

                            if (deptcodes.indexOf(dataM.get(i).getDeptCode()) != -1) {
                                tempM.set(deptcodes.indexOf(dataM.get(i).getDeptCode()), dataM.get(i).getActMonth());
                            }

                        }
                    }
                    for (int i = 0; i < dataY.size(); i++) {
                        if (Strings.nullToEmpty(dataY.get(i).getSubjectCode()).equals(dimWinaddSubject.getSubjectCode())) {
                            if (deptcodes.indexOf(dataY.get(i).getDeptCode()) != -1) {
                                tempY.set(deptcodes.indexOf(dataY.get(i).getDeptCode()), dataY.get(i).getActYear());

                            }

                        }
                    }
                    for (int i = 0; i < tempM.size(); i++) {
                        //添加月累年累
                        temp.add(tempM.get(i));
                        temp.add(tempY.get(i));

                    }
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < temp.size(); i++) {
                        map.put("key" + (i + 1), temp.get(i));
                    }
                    map.put("unit", dimWinaddSubject.getUnit());

                    tableBean.getTableBody().add(map);
                }
            }

        }
        result.setMsg("查询表格数据成功");
        result.setData(JSON.toJSONString(tableBean));
        return result;
    }

    //资源方增值分享占比
    @RequestMapping(value = "/getring", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean gettowpie(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                 String corp, String dept,
                                 @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                                 @NotEmpty(message = "outsubject不能为空") String outsubject, @NotEmpty(message = "date不能为空") String date,
                                 HttpServletRequest request, HttpServletResponse response) {
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        RespondBean result = new RespondBean();

        List<DimWinaddOrg> list = new ArrayList<>();
        List<DmWinaddResults> data = new ArrayList<>();
        List<String> subjects = new ArrayList<>();
        List<String> insubjects = Arrays.asList(subject.split(","));
        List<String> outsubjects = Arrays.asList(outsubject.split(","));
        subjects.addAll(insubjects);
        subjects.removeAll(outsubjects);
        subjects.addAll(outsubjects);

        String day = Tool.getlastday(date, type);
        EchartBean echartBean = new EchartBean();
        List<EchartBean.PieDatasBean> datas = new ArrayList<>();
        List<EchartBean.PieDatasBean> outdatas = new ArrayList<>();
        echartBean.setPieInnerDatas(datas);
        echartBean.setPieOuterDatas(outdatas);
        boolean isYear = "Y".equals(type);
        data = commonService.getpie(day, subjects, plate, platform, corp, dept);
        for (String s : insubjects) {
            EchartBean.PieDatasBean pieDatasBean = new EchartBean.PieDatasBean();
            for (DmWinaddResults datum : data) {
                if (s.equals(datum.getSubjectCode())) {
                    pieDatasBean.setName(datum.getSubjectName());
                    pieDatasBean.setValue((isYear ? datum.getActYear() : datum.getActMonth()).toString());
                    break;
                }
            }
            datas.add(pieDatasBean);
        }
        for (String s : outsubjects) {

            EchartBean.PieDatasBean pieDatasBean = new EchartBean.PieDatasBean();
            for (DmWinaddResults datum : data) {
                if (s.equals(datum.getSubjectCode())) {
                    pieDatasBean.setName(datum.getSubjectName());
                    pieDatasBean.setValue((isYear ? datum.getActYear() : datum.getActMonth()).toString());
                    break;
                }
            }
            outdatas.add(pieDatasBean);
        }
        result.setMsg("获取科目饼图数据成功");
        result.setData(JSON.toJSONString(echartBean));
        return result;

    }


    //资源方数量占比
    @RequestMapping(value = "/getpie", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getpie(@NotEmpty(message = "plate不能为空") String plate, String platform,
                              String corp, String dept,
                              @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                              @NotEmpty(message = "date不能为空") String date,
                              HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        List<DimWinaddOrg> list = new ArrayList<>();
        List<DmWinaddResults> data = new ArrayList<>();
        List<String> subjects = Arrays.asList(subject.split(","));

        String day = Tool.getlastday(date, type);
        EchartBean echartBean = new EchartBean();
        List<EchartBean.PieDatasBean> datas = new ArrayList<>();
        echartBean.setPieDatas(datas);
        boolean isYear = "Y".equals(type);
        if (dept.equals(plate)) {
            //总览

        } else if (dept.equals(platform)) {

        } else if (dept.equals(corp)) {
//一级小微  财务 财务
            data = commonService.getpie(day, subjects, plate, platform, corp, dept);
            for (String s : subjects) {
                String subname = null;
                EchartBean.PieDatasBean pieDatasBean = new EchartBean.PieDatasBean();
                for (DmWinaddResults datum : data) {
                    if (s.equals(datum.getSubjectCode())) {
                        subname = datum.getSubjectName();
                        pieDatasBean.setValue((isYear ? datum.getActYear() : datum.getActMonth()).toString());
                        break;
                    }
                }
                pieDatasBean.setName(subname);
                datas.add(pieDatasBean);
            }


        } else {
            data = commonService.getpie(day, subjects, plate, platform, corp, dept);
            for (String s : subjects) {
                String subname = null;
                EchartBean.PieDatasBean pieDatasBean = new EchartBean.PieDatasBean();
                for (DmWinaddResults datum : data) {
                    if (s.equals(datum.getSubjectCode())) {
                        subname = datum.getSubjectName();
                        pieDatasBean.setValue((isYear ? datum.getActYear() : datum.getActMonth()).toString());
                        break;
                    }
                }
                pieDatasBean.setName(subname);
                datas.add(pieDatasBean);
            }
        }
        result.setMsg("获取科目饼图数据成功");
        result.setData(JSON.toJSONString(echartBean));
        return result;

    }


    //总览 生态收入占比
    @RequestMapping(value = "/getpieall", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getpieall(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                 String corp, String dept, @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                                 @NotEmpty(message = "date不能为空") String date,
                                 HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        List<DimWinaddOrg> list = new ArrayList<>();
        List<DmWinaddResults> data = new ArrayList<>();
        List<String> subjects = Arrays.asList(subject.split(","));

        String day = Tool.getlastday(date, type);
        EchartBean echartBean = new EchartBean();
        List<EchartBean.PieDatasBean> datas = new ArrayList<>();
        echartBean.setPieDatas(datas);
        boolean isYear = "Y".equals(type);
        if (dept.equals(plate)) {
            //总览
            list = commonService.getcorpbyplat(plate);
            data = commonService.getpieall(day, subjects, list);
            for (DimWinaddOrg dimWinaddOrg : list) {
                EchartBean.PieDatasBean pieDatasBean = new EchartBean.PieDatasBean();
                pieDatasBean.setName(dimWinaddOrg.getCompanyName());
                for (DmWinaddResults datum : data) {
                    if (Strings.nullToEmpty(datum.getCompanyCode()).equals(dimWinaddOrg.getCompanyCode())) {
                        pieDatasBean.setValue((isYear ? datum.getActYear() : datum.getActMonth()).toString());
                        break;
                    }
                }
                datas.add(pieDatasBean);
            }
        } else if (dept.equals(platform)) {

            list = commonService.getcorpbyplatform(plate, platform);
            data = commonService.getpieall(day, subjects, list);
            for (DimWinaddOrg dimWinaddOrg : list) {
                EchartBean.PieDatasBean pieDatasBean = new EchartBean.PieDatasBean();
                pieDatasBean.setName(dimWinaddOrg.getCompanyName());
                for (DmWinaddResults datum : data) {
                    if (Strings.nullToEmpty(datum.getCompanyCode()).equals(dimWinaddOrg.getCompanyCode())) {
                        pieDatasBean.setValue((isYear ? datum.getActYear() : datum.getActMonth()).toString());
                        break;
                    }
                }
                datas.add(pieDatasBean);
            }

        } else if (dept.equals(corp)) {
//一级小微  财务 财务


        } else {

        }
        result.setMsg("获取机构饼图数据成功");
        result.setData(JSON.toJSONString(echartBean));
        return result;
    }


    //生态收入占比趋势
    @RequestMapping(value = "/getlines", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getlines(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                String corp, String dept, @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                                @NotEmpty(message = "date不能为空") String date,
                                HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        List<DimWinaddOrg> list = new ArrayList<>();
        List<DmWinaddResults> data = new ArrayList<>();
        List<String> subjects = Arrays.asList(subject.split(","));
        List<String> x = new ArrayList<>();
        List<String> dates = Tool.getdates(date, type);
        List<String> legend = new ArrayList<>();

        EchartBean echartBean = new EchartBean();
        List<List<?>> datas = new ArrayList<>();
        echartBean.setLineDatas(datas);
        echartBean.setLegend(legend);
        List<List<BigDecimal>> nums = new ArrayList<>();
        boolean isYear = "Y".equals(type);
        if (dept.equals(plate)) {
            //总览
            list = commonService.getcorpbyplat(plate);
            data = commonService.getlinesall(dates, subjects, list);
            x = Tool.getdatex(dates, type);
            datas.add(x);
            legend.addAll(list.stream().map(DimWinaddOrg::getCompanyName).collect(Collectors.toList()));
            for (String sub : subjects) {
                for (String l : legend) {
                    List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(dates.size(), null));
                    for (DmWinaddResults datum : data) {
                        if (l.equals(datum.getCompanyName())) {
                            bigDecimals.set(dates.indexOf(datum.getDataDate()), isYear ? datum.getActYear() : datum.getActMonth());
                        }
                    }
                    nums.add(bigDecimals);
                }
                if ("W004002".equals(sub)) {
                    nums = Tool.calrate(nums);
                }

            }
            datas.addAll(nums);

        } else if (dept.equals(platform)) {

            list = commonService.getcorpbyplatform(plate, platform);
            data = commonService.getlinesall(dates, subjects, list);
            x = Tool.getdatex(dates, type);
            datas.add(x);
            legend.addAll(list.stream().map(DimWinaddOrg::getCompanyName).collect(Collectors.toList()));
            for (String sub : subjects) {
                for (String l : legend) {
                    List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(dates.size(), null));
                    for (DmWinaddResults datum : data) {
                        if (l.equals(datum.getCompanyName())) {
                            bigDecimals.set(dates.indexOf(datum.getDataDate()), isYear ? datum.getActYear() : datum.getActMonth());
                        }
                    }
                    nums.add(bigDecimals);

                }
                if ("W004002".equals(sub)) {
                    nums = Tool.calrate(nums);
                }

            }
            datas.addAll(nums);


        } else if (dept.equals(corp)) {
//一级小微  财务 财务
            list = commonService.getdeptbycorp(plate, platform, corp);
            data = commonService.getlines(dates, subjects, list);
            x = Tool.getdatex(dates, type);
            legend.addAll(list.stream().map(DimWinaddOrg::getDeptName).collect(Collectors.toList()));
            datas.add(x);
            for (String sub : subjects) {
                for (String l : legend) {
                    List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(dates.size(), null));
                    for (DmWinaddResults datum : data) {
                        if (l.equals(datum.getDeptName())) {
                            bigDecimals.set(dates.indexOf(datum.getDataDate()), isYear ? datum.getActYear() : datum.getActMonth());
                        }
                    }
                    nums.add(bigDecimals);

                }
                if ("W004002".equals(sub)) {
                    nums = Tool.calrate(nums);
                }

            }
            datas.addAll(nums);


        } else {
            list = commonService.getdeptinfo(plate, platform, corp, dept);
            data = commonService.getlines(dates, subjects, list);
            x = Tool.getdatex(dates, type);
            legend.addAll(list.stream().map(DimWinaddOrg::getDeptName).collect(Collectors.toList()));
            echartBean.setLegend(legend);
            datas.add(x);
            for (String sub : subjects) {
                for (String l : legend) {
                    List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(dates.size(), null));
                    for (DmWinaddResults datum : data) {
                        if (l.equals(datum.getDeptName())) {
                            bigDecimals.set(dates.indexOf(datum.getDataDate()), isYear ? datum.getActYear() : datum.getActMonth());
                        }
                    }

                    nums.add(bigDecimals);
                }
                if ("W004002".equals(sub)) {
                    nums = Tool.calrate(nums);
                }

            }
            datas.addAll(nums);

        }
        result.setMsg("获取多个折线图数据成功");
        result.setData(JSON.toJSONString(echartBean));
        return result;

    }

    //生态收入|生态利润趋势
    @RequestMapping(value = "/getlinebardate", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getlinebardate(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                      String corp, String dept, @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                                      @NotEmpty(message = "date不能为空") String date,
                                      HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        List<List<?>> datas = new ArrayList<>();
        List<String> legend = new ArrayList<>();

        EchartBean echartBean = new EchartBean();
        echartBean.setLineDatas(datas);
        echartBean.setLegend(legend);
        boolean isYear = "Y".equals(type);
        List<String> dates = Tool.getdates(date, type);
        List<String> subjects = Arrays.asList(subject.split(","));
        List<DmWinaddResults> data = commonService.getdeptlinebar(dates, subjects, plate, platform, corp, dept);
        echartBean.setLegend(legend);
        echartBean.setLineDatas(datas);
        List<String> x = Tool.getdatex(dates, type);
        datas.add(x);
        for (String sub : subjects) {
            List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(dates.size(), null));
            String subname = null;
            for (DmWinaddResults datum : data) {
                if (sub.equals(datum.getSubjectCode())) {
                    subname = datum.getSubjectName();
                    bigDecimals.set(dates.indexOf(datum.getDataDate()), isYear ? datum.getActYear() : datum.getActMonth());
                }
            }
            legend.add(subname);
            datas.add(bigDecimals);
        }
        result.setMsg("获取线柱图日期数据成功");
        result.setData(JSON.toJSONString(echartBean));
        return result;

    }


    //生态收入比率
    @RequestMapping(value = "/getlinebar", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getlinebar(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                  String corp, String dept, @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                                  @NotEmpty(message = "date不能为空") String date, HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }

        List<DimWinaddOrg> list = new ArrayList<>();
        List<DmWinaddResults> data = new ArrayList<>();
        List<String> subjects = Arrays.asList(subject.split(","));
        List<String> x = new ArrayList<>();
        EchartBean echartBean = new EchartBean();
        List<String> legend = new ArrayList<>();
        List<List<?>> datas = new ArrayList<>();
        echartBean.setLineDatas(datas);
        echartBean.setLegend(legend);
        String day = Tool.getlastday(date, type);
        boolean isYear = "Y".equals(type);

        List<DimWinaddSubject> subnames = commonService.getsubnames(subjects);
        legend.addAll(Tool.getsubnames(subnames, platform));


        if (dept.equals(plate)) {
//总览 万联 万联 万联 万联
            list = commonService.getcorpbyplat(plate);
            data = commonService.getlinebarall(day, subjects, list);
            x = list.stream().map(DimWinaddOrg::getCompanyName).collect(Collectors.toList());
            // TODO: 2018/9/18  公司名的排序和查询返回的排序是否能对应上
            datas.add(x);
            for (String sub : subjects) {
                String subname = null;
                List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(x.size(), null));
                for (DmWinaddResults datum : data) {
                    if (sub.equals(datum.getSubjectCode())) {
                        subname = datum.getSubjectName();
                        bigDecimals.set(x.indexOf(datum.getCompanyName()), isYear ? datum.getActYear() : datum.getActMonth());
                    }
                }
//                legend.add(subname);
                datas.add(bigDecimals);
            }


        } else if (dept.equals(platform)) {
            list = commonService.getcorpbyplatform(plate, platform);
            x = list.stream().map(DimWinaddOrg::getCompanyName).collect(Collectors.toList());
            datas.add(x);
            data = commonService.getlinebarall(day, subjects, list);
            for (String sub : subjects) {
                String subname = null;
                List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(x.size(), null));
                for (DmWinaddResults datum : data) {
                    if (sub.equals(datum.getSubjectCode())) {
                        subname = datum.getSubjectName();
                        bigDecimals.set(x.indexOf(datum.getCompanyName()), isYear ? datum.getActYear() : datum.getActMonth());
                    }
                }
//                legend.add(subname);
                datas.add(bigDecimals);
            }
        } else if (dept.equals(corp)) {
//一级小微 万联 类金融 财务 财务
            list = commonService.getdeptbycorp(plate, platform, corp);
            x = list.stream().map(DimWinaddOrg::getDeptName).collect(Collectors.toList());
            datas.add(x);
            data = commonService.getcorplinebar(day, subjects, plate, platform, corp);
            for (String sub : subjects) {
                String subname = null;
                List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(x.size(), null));
                for (DmWinaddResults datum : data) {
                    if (sub.equals(datum.getSubjectCode())) {
                        subname = datum.getSubjectName();
                        bigDecimals.set(x.indexOf(datum.getDeptName()), isYear ? datum.getActYear() : datum.getActMonth());
                    }
                }
//                legend.add(subname);
                datas.add(bigDecimals);
            }

        } else {
//二级小微 万联 类金融 财务  云享
            List<String> dates = Tool.getdates(date, type);
            data = commonService.getdeptlinebar(dates, subjects, plate, platform, corp, dept);
            x = Tool.getdatex(dates, type);
            datas.add(x);
            for (String sub : subjects) {
                String subname = null;
                List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(dates.size(), null));
                for (DmWinaddResults datum : data) {
                    if (sub.equals(datum.getSubjectCode())) {
                        subname = datum.getSubjectName();
                        bigDecimals.set(dates.indexOf(datum.getDataDate()), isYear ? datum.getActYear() : datum.getActMonth());
                    }
                }
//                legend.add(subname);
                datas.add(bigDecimals);
            }

        }

        //生态收入比率=生态收入/收入 不推荐计算 数据加科目
//        if (subjects.contains("W004") && subjects.contains("W004002")) {
//
//            if (datas.size() > 2) {
//                legend.add("生态收入比率");
//                List<BigDecimal> rate = new ArrayList<>();
//                for (int i = 0; i < datas.get(1).size(); i++) {
//                    rate.add(Tool.divide(datas.get(1).get(i), datas.get(2).get(i)));
//                }
//                datas.add(rate);
//            }
//        }

        result.setMsg("获取线柱图数据成功");
        result.setData(JSON.toJSONString(echartBean));
        return result;

    }

    @RequestMapping(value = "/getline", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getline(@NotEmpty(message = "plate不能为空") String plate, String platform,
                               String corp, String dept, @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                               @NotEmpty(message = "date不能为空") String date,
                               HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        EchartBean echartBean = new EchartBean();
        List<List<?>> data = new ArrayList<>();
        List<String> dates = Tool.getdates(date, type);
        List<String> x = Tool.getdatex(dates, type);
        List<BigDecimal> bigDecimals = new ArrayList<>(Collections.nCopies(dates.size(), null));
        boolean isYear = "Y".equals(type);
        data.add(x);
        data.add(bigDecimals);
        echartBean.setLineDatas(data);
        List<DmWinaddResults> list = commonService.getline(dates, subject, plate, platform, corp, dept);
        if (list.size() > 0) {
            echartBean.setTitle(list.get(0).getSubjectName());
        }
        for (DmWinaddResults dmWinaddResults : list) {
            bigDecimals.set(dates.indexOf(dmWinaddResults.getDataDate()), isYear ? dmWinaddResults.getActYear() : dmWinaddResults.getActMonth());
        }


        result.setMsg("查询折线图数据成功");
        result.setData(JSON.toJSONString(echartBean));
        return result;
    }

    @RequestMapping(value = "/getsingle", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getsingle(@NotEmpty(message = "plate不能为空") String plate, String platform,
                                 String corp, String dept, @NotEmpty(message = "type不能为空") String type, @NotEmpty(message = "subject不能为空") String subject,
                                 @NotEmpty(message = "date不能为空") String date, HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();
        if (Strings.isNullOrEmpty(platform)) {
            platform = plate;
        }
        if (Strings.isNullOrEmpty(corp)) {
            corp = platform;
        }
        if (Strings.isNullOrEmpty(dept)) {
            dept = corp;
        }
        List<String> subjects = Arrays.asList(subject.split(","));
        boolean isYear = "Y".equals(type);

        String day = Tool.getlastday(date, type);
        List<DmWinaddResults> list = commonService.gettitle(day, subjects, plate, platform, corp, dept);
        if (list != null && list.size() > 0) {
            for (DmWinaddResults dmWinaddResults : list) {
                if (dmWinaddResults.getActMonthRate() != null) {
                    dmWinaddResults.setSymbol(dmWinaddResults.getActMonthRate().compareTo(BigDecimal.ZERO));
                }
                dmWinaddResults.setActValue(isYear ? dmWinaddResults.getActYear() : dmWinaddResults.getActMonth());
            }
            result.setData(JSON.toJSONString(list));
            result.setMsg("获取单个科目信息成功");
        } else {
            result.setCode(1);
            result.setMsg("没有查到信息");

        }
        return result;
    }

    @RequestMapping(value = "/loginUser", method = RequestMethod.POST)
    @ResponseBody
    public RespondBean loginUser(@NotEmpty(message = "username不能为空") String username, @NotEmpty(message = "密码不能为空") String pwd, HttpSession session) {

        RespondBean result = new RespondBean();

        Subject currentUser = SecurityUtils.getSubject();
        // 把用户名和密码封装为UsernamePasswordToken 对象
        UsernamePasswordToken token = new UsernamePasswordToken(username, pwd);
        token.setRememberMe(true);
        String sid = "";
        try {
            // 执行登陆
            currentUser.login(token);
            sid = currentUser.getSession().getId().toString();
//            currentUser.getSession().setTimeout(7200);

        } catch (AuthenticationException ae) {
            result.setCode(1);
            result.setMsg("用户名或密码错误");
            return result;
        }
        String roleid = roleService.getrolebyuser(username);
        List<String> roleList = new ArrayList<>();
        if (!Strings.isNullOrEmpty(roleid)) {
            roleList = Arrays.asList(roleid.split(","));
        }

        List<TreeviewNode> root = new ArrayList<>();

        List<DimWinaddOrg> dimWinaddOrgs = commonService.getplattree();
        List<DimWinaddOrg> authlist = new ArrayList<>();

        List<String> percodes = new ArrayList<>();
        List<PermitInfo> permits = roleService.getAuthor(roleList);
        if (permits != null && permits.size() > 0) {
            percodes = permits.stream().map(PermitInfo::getPercode).collect(Collectors.toList());

        }

        DimWinaddOrg all = new DimWinaddOrg();
        all.setPlate("万链平台");
        all.setPlatform("万链平台");
        dimWinaddOrgs.add(0, all);
        for (DimWinaddOrg dimWinaddOrg : dimWinaddOrgs) {
            dimWinaddOrg.setEnable(percodes.contains(dimWinaddOrg.getPlatform()) ? 1 : 0);
//            if (percodes.contains(dimWinaddOrg.getPlatform())) {
            List<DimWinaddOrg> child1 = new ArrayList<>();
            boolean hascorp = false;
            if (dimWinaddOrg.getList() != null && dimWinaddOrg.getList().size() > 0) {
                for (DimWinaddOrg winaddOrg : dimWinaddOrg.getList()) {
                    boolean hasdept = false;
                    winaddOrg.setEnable(percodes.contains(winaddOrg.getCompanyCode()) ? 1 : 0);
//                        if (percodes.contains(winaddOrg.getCompanyCode())) {
                    List<DimWinaddOrg> child2 = new ArrayList<>();
                    if (winaddOrg.getList() != null) {
                        for (DimWinaddOrg org : winaddOrg.getList()) {
                            org.setEnable(percodes.contains(org.getDeptCode()) ? 1 : 0);
                            if (percodes.contains(org.getDeptCode())) {
                                hasdept = true;
                                hascorp = true;
                                child2.add(org);

                            }
                        }

                    }
                    if (hasdept) {
                        child1.add(winaddOrg);
                        winaddOrg.setList(child2);
                    }
//                        }

                }


            }
            if (hascorp || (percodes.contains("万链平台") && "万链平台".equals(dimWinaddOrg.getPlatform()))) {
                authlist.add(dimWinaddOrg);
                dimWinaddOrg.setList(child1);

            }

//            }


        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionid", sid);
        map.put("permit", authlist);
        result.setData(JSON.toJSONString(map));
        result.setMsg("登录成功");
        return result;

    }

    private void setNodeState(TreeviewNode node, boolean b) {
        TreeviewNode.StateBean state = new TreeviewNode.StateBean();
        state.setChecked(true);
        node.setState(state);
    }

    @RequestMapping(value = "/getorg", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getorg(HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();

        List<DimWinaddOrg> orgs = commonService.getorg();
        result.setData(JSON.toJSONString(orgs));
        result.setMsg("组织机构查询成功");
        return result;
    }


    @RequestMapping(value = "/getplattree", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getplattree(HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();

        List<DimWinaddOrg> orgs = commonService.getplattree();
        result.setData(JSON.toJSONString(orgs));
        result.setMsg("平台树查询成功");
        return result;
    }

    @RequestMapping(value = "/getonecorp", method = RequestMethod.GET)
    @ResponseBody
    public RespondBean getonecorp(@NotEmpty(message = "plate不能为空") String plate, HttpServletRequest request, HttpServletResponse response) {
        RespondBean result = new RespondBean();

        List<DimWinaddOrg> orgs = commonService.getonecorp(plate);
        result.setData(JSON.toJSONString(orgs));
        result.setMsg("组织机构查询成功");
        return result;
    }

}