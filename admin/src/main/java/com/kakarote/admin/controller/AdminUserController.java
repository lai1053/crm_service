package com.kakarote.admin.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.common.AdminCodeEnum;
import com.kakarote.admin.common.log.AdminUserLog;
import com.kakarote.admin.entity.BO.*;
import com.kakarote.admin.entity.PO.AdminConfig;
import com.kakarote.admin.entity.PO.AdminUser;
import com.kakarote.admin.entity.PO.AdminUserConfig;
import com.kakarote.admin.entity.VO.AdminSuperUserVo;
import com.kakarote.admin.entity.VO.AdminUserVO;
import com.kakarote.admin.entity.VO.HrmSimpleUserVO;
import com.kakarote.admin.service.*;
import com.kakarote.core.common.*;
import com.kakarote.core.common.log.BehaviorEnum;
import com.kakarote.core.common.log.SysLog;
import com.kakarote.core.common.log.SysLogHandler;
import com.kakarote.core.entity.BasePage;
import com.kakarote.core.entity.UserInfo;
import com.kakarote.core.exception.NoLoginException;
import com.kakarote.core.feign.admin.entity.SimpleUser;
import com.kakarote.core.feign.email.EmailService;
import com.kakarote.core.servlet.ApplicationContextHolder;
import com.kakarote.core.servlet.upload.UploadEntity;
import com.kakarote.core.utils.UserCacheUtil;
import com.kakarote.core.utils.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * ????????? ???????????????
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-04-27
 */
@RestController
@RequestMapping("/adminUser")
@Api(tags = "????????????????????????")
@Slf4j
@SysLog(subModel = SubModelType.ADMIN_STAFF_MANAGEMENT,logClass = AdminUserLog.class)
public class AdminUserController {

    @Autowired
    private IAdminUserService adminUserService;

    @Autowired
    private IAdminUserConfigService adminUserConfigService;

    @Autowired
    private IAdminFileService adminFileService;

    @RequestMapping("/findByUsername")
    @ApiOperation(value = "??????name????????????", httpMethod = "POST")
    public Result<List<Map<String, Object>>> findByUsername(String username) {
        List<Map<String, Object>> userInfoList = adminUserService.findByUsername(username);
        return Result.ok(userInfoList);
    }

    @ApiOperation("????????????????????????????????????")
    @PostMapping("/queryUserList")
    public Result<BasePage<AdminUserVO>> queryUserList(@RequestBody AdminUserBO adminUserBO) {
        return R.ok(adminUserService.queryUserList(adminUserBO));
    }

    @ApiOperation("????????????????????????")
    @PostMapping("/countNumOfUser")
    public Result<JSONObject> countUserByLabel() {
        return R.ok(adminUserService.countUserByLabel());
    }

    @ApiExplain("????????????????????????????????????")
    @PostMapping("/queryAllUserList")
    public Result<List<Long>> queryAllUserList(@RequestParam(value = "type",required = false) Integer type) {
        LambdaQueryWrapper<AdminUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(AdminUser::getUserId);
        /* type=2??????????????????????????? */
        if (Objects.equals(2,type)) {
            queryWrapper.ne(AdminUser::getStatus,0);
        }
        return R.ok(adminUserService.listObjs(queryWrapper, TypeUtils::castToLong));
    }

    @ApiExplain("????????????????????????????????????")
    @PostMapping("/queryAllUserInfoList")
    public Result<List<UserInfo>> queryAllUserInfoList() {
        List<UserInfo> userInfoList = adminUserService.queryAllUserInfoList();
        return R.ok(userInfoList);
    }

    @PostMapping("/setUser")
    @ApiOperation("????????????")
    public Result setUser(@RequestBody AdminUserVO adminUserVO) {
        adminUserService.setUser(adminUserVO);
        return R.ok();
    }

    @PostMapping("/setUserDept")
    @ApiOperation("????????????????????????")
    public Result setUserDept(@RequestBody AdminUserBO adminUserBO) {
        adminUserService.setUserDept(adminUserBO);
        return R.ok();
    }

    @PostMapping("/addUser")
    @ApiOperation("????????????")
    @SysLogHandler(behavior = BehaviorEnum.SAVE,object = "#adminUserVO.realname",detail = "'???????????????:'+#adminUserVO.realname")
    public Result addUser(@RequestBody AdminUserVO adminUserVO) {
        adminUserService.addUser(adminUserVO);
        return R.ok();
    }

    @PostMapping("/usernameEdit")
    @ApiOperation("??????????????????")
    @SysLogHandler(behavior = BehaviorEnum.UPDATE)
    public Result<Integer> usernameEdit(@RequestParam("id") Integer id, @RequestParam("username") String username, @RequestParam("password") String password) {
        Integer integer = adminUserService.usernameEdit(id, username, password);
        return R.ok(integer);
    }

    @PostMapping("/excelImport")
    @ApiOperation("excel????????????")
    @SysLogHandler(behavior = BehaviorEnum.EXCEL_IMPORT,object = "excel????????????",detail = "excel????????????")
    public Result<JSONObject> excelImport(@RequestParam("file") MultipartFile file) {
        JSONObject object = adminUserService.excelImport(file);
        return R.ok(object);
    }

    @PostMapping("/downExcel")
    @ApiOperation("excel??????????????????")
    public void downExcel(@RequestParam("token") String token, HttpServletResponse response) {
        String path = FileUtil.getTmpDirPath() + "/" + token;
        if (FileUtil.exist(path)) {
            File file = FileUtil.file(path);
            final String fileName = file.getName();
            final String contentType = ObjectUtil.defaultIfNull(FileUtil.getMimeType(fileName), "application/octet-stream");
            BufferedInputStream in = null;
            try {
                in = FileUtil.getInputStream(file);
                ServletUtil.write(response, in, contentType, "import_error.xls");
            } finally {
                IoUtil.close(in);
            }
            FileUtil.del(path);
        }
    }

    @PostMapping("/hrmAddUser")
    @ApiOperation("???????????????????????????")
    public Result hrmAddUser(@RequestBody HrmAddUserBO hrmAddUserBO) {
        adminUserService.hrmAddUser(hrmAddUserBO);
        return R.ok();
    }

    @PostMapping("/setUserStatus")
    @ApiOperation("????????????")
    @SysLogHandler(behavior = BehaviorEnum.UPDATE)
    public Result setUserStatus(@RequestBody AdminUserStatusBO adminUserStatusBO) {
        adminUserService.setUserStatus(adminUserStatusBO);
        return R.ok();
    }

    @PostMapping("/resetPassword")
    @ApiOperation("????????????")
    @SysLogHandler(behavior = BehaviorEnum.UPDATE)
    public Result resetPassword(@RequestBody AdminUserStatusBO adminUserStatusBO) {
        adminUserService.resetPassword(adminUserStatusBO);
        return R.ok();
    }

    @PostMapping("/updateImg")
    @ApiOperation("????????????")
    @SysLogHandler(behavior = BehaviorEnum.UPDATE,object = "????????????",detail = "????????????")
    public Result updateImg(@RequestParam("file") MultipartFile file) throws IOException {
        UploadEntity img = adminFileService.upload(file, null, "img", "0");
        AdminUser byId = adminUserService.getById(UserUtil.getUserId());
        byId.setImg(img.getUrl());
        adminUserService.updateById(byId);
        return R.ok();
    }

    @PostMapping("/updatePassword")
    @ApiOperation("??????????????????")
    @SysLogHandler(behavior = BehaviorEnum.UPDATE,object = "??????????????????",detail = "??????????????????")
    public Result updatePassword(@RequestParam("oldPwd") String oldPass, @RequestParam("newPwd") String newPass) {
        AdminUser adminUser = adminUserService.getById(UserUtil.getUserId());
        if (!UserUtil.verify(adminUser.getUsername() + oldPass, adminUser.getSalt(), adminUser.getPassword())) {
            return R.error(AdminCodeEnum.ADMIN_PASSWORD_ERROR);
        }
        adminUser.setPassword(newPass);
        return updateUser(adminUser);
    }

    @PostMapping("/updateUser")
    @ApiOperation("??????????????????")
    public Result updateUser(@RequestBody AdminUser adminUser) {
        adminUserService.updateUser(adminUser);
        return R.ok();
    }

    @Autowired
    private IAdminDeptService deptService;

    @PostMapping("/queryLoginUser")
    @ApiOperation("????????????????????????")
    public Result<AdminUserVO> queryLoginUser(HttpServletRequest request, HttpServletResponse response) {
        String name = "readNotice";
        AdminUser user = adminUserService.getById(UserUtil.getUserId());
        if (user == null) {
            throw new NoLoginException();
        }
        AdminSuperUserVo adminUser = BeanUtil.copyProperties(user, AdminSuperUserVo.class);
        adminUser.setIsAdmin(UserUtil.isAdmin());
        AdminUserConfig userConfig = adminUserConfigService.queryUserConfigByName(name);
        adminUser.setIsReadNotice(userConfig != null ? userConfig.getStatus() : 0);
        adminUser.setPassword(null);
        String deptName = deptService.getNameByDeptId(adminUser.getDeptId());
        adminUser.setDeptName(deptName);
        adminUser.setParentName(UserCacheUtil.getUserName(adminUser.getParentId()));
        AdminConfig config = ApplicationContextHolder.getBean(IAdminConfigService.class).queryConfigByName("email");
        if (config != null && config.getStatus() == 1) {
            Integer data = ApplicationContextHolder.getBean(EmailService.class).getEmailId(adminUser.getUserId()).getData();
            adminUser.setEmailId(data);
        }
        AdminUserConfig userConfigByName = adminUserConfigService.queryUserConfigByName("InitUserConfig");
        if(userConfigByName != null){
            adminUser.setServerUserInfo(JSON.parseObject(userConfigByName.getValue()));
        }
        return R.ok(adminUser);
    }

    @RequestMapping("/queryUserRoleIds")
    @ApiExplain("????????????????????????")
    public Result<List<Integer>> queryUserRoleIds(@RequestParam("userId") @NotNull Long userId) {
        return R.ok(adminUserService.queryUserRoleIds(userId));
    }

    @RequestMapping("/queryListName")
    @ApiExplain("???????????????")
    public Result queryListName(@RequestBody UserBookBO userBookBO) {
        return R.ok(adminUserService.queryListName(userBookBO));
    }

    @RequestMapping("/attention")
    @ApiExplain("??????????????????")
    public Result attention(@RequestParam("userId") Long userId) {
        adminUserService.attention(userId);
        return R.ok();
    }

    @RequestMapping("/getNameByUserId")
    @ApiExplain("????????????ID??????????????????")
    public Result getNameByUserId(@NotNull Long userId) {
        return R.ok(adminUserService.getNameByUserId(userId));
    }

    @RequestMapping("/queryChildUserId")
    @ApiExplain("????????????ID???????????????")
    public Result<List<Long>> queryChildUserId(@NotNull Long userId) {
        List<Long> longList = adminUserService.queryChildUserId(userId);
        return R.ok(longList);
    }

    @RequestMapping("/queryUserInfo")
    @ApiOperation("??????????????????")
    public Result<AdminUser> queryUserInfo(@RequestParam("userId") Long userId) {
        AdminUser byId = adminUserService.getById(userId);
        String nameByDeptId = ApplicationContextHolder.getBean(IAdminDeptService.class).getNameByDeptId(byId.getDeptId());
        byId.setDeptName(nameByDeptId);
        byId.setSalt(null);
        byId.setPassword(null);
        return R.ok(byId);
    }

    @RequestMapping("/queryInfoByUserId")
    @ApiExplain("????????????ID????????????")
    public Result<UserInfo> queryInfoByUserId(@NotNull Long userId) {
        AdminUser byId = adminUserService.getById(userId);
        UserInfo userInfo = null;
        if (byId != null && byId.getDeptId() != null) {
            userInfo = BeanUtil.copyProperties(byId, UserInfo.class);
            String nameByDeptId = UserCacheUtil.getDeptName(byId.getDeptId());
            userInfo.setDeptName(nameByDeptId);
            userInfo.setRoles(adminUserService.queryUserRoleIds(userInfo.getUserId()));
        }
        return R.ok(userInfo);
    }

    @PostMapping("/queryUserByIds")
    @ApiExplain("????????????ID????????????")
    public Result<List<SimpleUser>> queryUserByIds(@RequestBody List<Long> ids) {
        List<SimpleUser> simpleUsers = adminUserService.queryUserByIds(ids);
        return R.ok(simpleUsers);
    }

    @PostMapping("/queryNormalUserByIds")
    @ApiExplain("????????????ID??????????????????")
    public Result<List<Long>> queryNormalUserByIds(@RequestBody List<Long> ids) {
        return R.ok(adminUserService.queryNormalUserByIds(ids));
    }


    @PostMapping("/queryUserById")
    @ApiExplain("????????????ID????????????")
    public Result<SimpleUser> queryUserById(@RequestParam("userId") Long userId) {
        AdminUser adminUser = adminUserService.getById(userId);
        return R.ok(BeanUtil.copyProperties(adminUser, SimpleUser.class));
    }

    @PostMapping("/queryUserByDeptIds")
    @ApiExplain("????????????ID????????????ids")
    public Result<List<Long>> queryUserByDeptIds(@RequestBody List<Integer> ids) {
        List<Long> userIds = adminUserService.queryUserByDeptIds(ids);
        return R.ok(userIds);
    }

    @PostMapping("/readNotice")
    @ApiOperation("???????????????????????????")
    public Result readNotice() {
        Long userId = UserUtil.getUserId();
        String name = "readNotice";
        Integer count = adminUserConfigService.lambdaQuery().eq(AdminUserConfig::getUserId, userId).eq(AdminUserConfig::getName, name).count();
        if (count > 1) {
            adminUserConfigService.lambdaUpdate().set(AdminUserConfig::getStatus, 1).eq(AdminUserConfig::getUserId, userId).eq(AdminUserConfig::getName, name).update();
        } else {
            AdminUserConfig adminUserConfig = new AdminUserConfig();
            adminUserConfig.setValue("");
            adminUserConfig.setName(name);
            adminUserConfig.setUserId(userId);
            adminUserConfig.setStatus(1);
            adminUserConfig.setDescription("????????????????????????");
            adminUserConfigService.save(adminUserConfig);
        }
        return R.ok();
    }


    @PostMapping("/queryAuthUserList")
    @ApiOperation("?????????????????????")
    public Result<List<SimpleUser>> queryAuthUserList() {
        List<SimpleUser> userList = new ArrayList<>();
        if (UserUtil.isAdmin()) {
            userList.addAll(adminUserService.list().stream().map(user -> BeanUtil.copyProperties(user, SimpleUser.class)).collect(Collectors.toList()));
        } else {
            List<Long> childUserId = adminUserService.queryChildUserId(UserUtil.getUserId());
            userList.addAll(adminUserService.queryUserByIds(childUserId));
        }
        return R.ok(userList);
    }

    @PostMapping("/queryDeptUserList/{deptId}")
    @ApiOperation("????????????????????????(????????????)")
    public Result<DeptUserListVO> queryDeptUserList(@PathVariable Integer deptId) {
        DeptUserListVO deptUserListVO = adminUserService.queryDeptUserList(deptId,true);
        return Result.ok(deptUserListVO);
    }

    @PostMapping("/queryDeptUserByExamine/{deptId}")
    @ApiOperation("????????????????????????(????????????)")
    public Result<DeptUserListVO> queryDeptUserByExamine(@PathVariable Integer deptId) {
        DeptUserListVO deptUserListVO = adminUserService.queryDeptUserList(deptId,false);
        return Result.ok(deptUserListVO);
    }

    @PostMapping("/queryDeptUserListByHrm")
    @ApiOperation("????????????????????????(hrm??????????????????)")
    public Result<Set<HrmSimpleUserVO>> queryDeptUserListByHrm(@RequestBody DeptUserListByHrmBO deptUserListByHrmBO) {
        Set<HrmSimpleUserVO> userList = adminUserService.queryDeptUserListByHrm(deptUserListByHrmBO);
        return Result.ok(userList);
    }

    @PostMapping("/queryUserIdByRealName")
    @ApiOperation("????????????id??????????????????")
    public Result<List<Long>> queryUserIdByRealName(@RequestParam("realNames") List<String> realNames) {
        List<Long> userIdList = adminUserService.queryUserIdByRealName(realNames);
        return Result.ok(userIdList);
    }

    @PostMapping("/queryLoginUserInfo")
    @ApiExplain("??????????????????????????????")
    public Result<UserInfo> queryLoginUserInfo(@RequestParam("userId") Long userId) {
        UserInfo userInfo = adminUserService.queryLoginUserInfo(userId);
        return Result.ok(userInfo);
    }

    @PostMapping("/querySystemStatus")
    @ApiOperation("??????????????????????????????")
    @ParamAspect
    public Result<Integer> querySystemStatus() {
        Integer status = adminUserService.querySystemStatus();
        return R.ok(status);
    }

    @PostMapping("/initUser")
    @ApiOperation("?????????????????????")
    @ParamAspect
    public Result initUser(@Validated @RequestBody SystemUserBO systemUserBO){
        adminUserService.initUser(systemUserBO);
        return R.ok();
    }


    @PostMapping("/queryUserIdByUserName")
    @ApiExplain("????????????id???????????????")
    public Result<Long> queryUserIdByUserName(@RequestParam("userName")String userName){
        Long userId = adminUserService.lambdaQuery().select(AdminUser::getUserId).eq(AdminUser::getUsername, userName).oneOpt().map(AdminUser::getUserId).orElse(0L);
        return Result.ok(userId);
    }
}

